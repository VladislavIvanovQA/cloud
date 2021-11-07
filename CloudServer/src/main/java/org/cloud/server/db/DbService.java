package org.cloud.server.db;

import org.cloud.core.commands.DeleteFileCommand;
import org.cloud.core.commands.DiskSpaceCommand;
import org.cloud.core.commands.SendFileCommand;
import org.cloud.core.commands.ShareFileCommand;
import org.cloud.core.dto.User;
import org.cloud.server.dto.ShareFileDTO;
import org.cloud.server.dto.UserFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DbService {
    private static final String DB_URL = "jdbc:sqlite:CloudServer/cloud.db";
    private final Logger log = LoggerFactory.getLogger(DbService.class);
    private final Connection connection;

    public DbService() throws SQLException {
        log.info("Try to connection DB: {}", DB_URL);
        connection = DriverManager.getConnection(DB_URL);
        log.info("Connection DB successes!");
    }

    public User getUsernameByLoginAndPassword(String login, String password) {
        User requiredUser = new User(login, password);
        try {
            return findUserByLoginAndPassword(requiredUser);
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return null;
    }

    public User createUserOrError(User user) {
        try {
            if (!findUserByLogin(user)) {
                return createUser(user);
            } else {
                return null;
            }
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return null;
    }

    public boolean checkAvailableSpaceToFile(User user, long size) {
        try {
            long availableSpaceFromUser = calculateAvailableSpaceFromUser(user);
            log.info("Available size: {}, file size: {}", availableSpaceFromUser, size);
            return availableSpaceFromUser - size > 0;
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return false;
    }

    public boolean setInfoFileForUser(User user, SendFileCommand fileInfo) {
        try {
            if (findFileInfo(user.getUsername(), fileInfo.getName(), fileInfo.getSize()) == 0) {
                setFileInfo(user.getUsername(), fileInfo.getName(), fileInfo.getSize());
                return true;
            }
            return false;
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return false;
    }

    public boolean deleteFileUser(User user, DeleteFileCommand fileCommand) {
        try {
            List<UserFiles> filesUser = findAllFilesUser(user.getUsername()).stream()
                    .filter(file -> file.getFileName().equals(fileCommand.getFileName()))
                    .collect(Collectors.toList());
            if (filesUser.size() == 0) {
                return false;
            }
            List<String> ids = filesUser.stream()
                    .map(file -> String.valueOf(file.getId()))
                    .collect(Collectors.toList());
            deleteFiles(ids);
            return true;
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return false;
    }

    public List<String> getListFileUser(User user) {
        try {
            return findAllFilesUser(user.getUsername())
                    .stream()
                    .map(UserFiles::getFileName)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return null;
    }

    public DiskSpaceCommand getSpaceInDisk(User user) {
        try {
            return getDiskSpace(user);
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return null;
    }

    public void sharedFile(User user, ShareFileCommand shareFileCommand, String link) {
        try {
            setShareFiles(user.getUsername(), shareFileCommand, link);
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
    }

    public ShareFileDTO getSharedFile(String link) {
        try {
            return findShareFile(link);
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return null;
    }

    public UserFiles getFileByFileId(String username, Integer fileId) {
        try {
            List<UserFiles> allFilesUser = findAllFilesUser(username);
            return allFilesUser.stream()
                    .filter(file -> file.getId().equals(fileId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("File id %s not found", fileId)));
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return null;
    }

    private User findUserByLoginAndPassword(User user) throws SQLException {
        ResultSet resultSet = connection.createStatement()
                .executeQuery("SELECT * FROM user WHERE login='" + user.getLogin() + "' and password='" + user.getPassword() + "'");
        while (resultSet.next()) {
            user.setUsername(resultSet.getString("username"));
        }
        return user;
    }

    private boolean findUserByLogin(User user) throws SQLException {
        ResultSet resultSet = connection.createStatement()
                .executeQuery("SELECT * FROM user WHERE login='" + user.getLogin() + "' and username='" + user.getUsername() + "'");
        return resultSet.next();
    }

    private User createUser(User user) throws SQLException {
        connection.createStatement()
                .executeUpdate(String
                        .format("INSERT INTO user (login, password, username) VALUES ('%s', '%s', '%s')",
                                user.getLogin(),
                                user.getPassword(),
                                user.getUsername()
                        ));
        setDefaultSettingUser(user);
        return user;
    }

    private void setDefaultSettingUser(User user) throws SQLException {
        connection.createStatement()
                .executeUpdate("INSERT INTO user_setting (user_id) VALUES ((SELECT user.id FROM user WHERE username='" + user.getUsername() + "'))");
    }

    private long calculateAvailableSpaceFromUser(User user) throws SQLException {
        DiskSpaceCommand diskSpace = getDiskSpace(user);
        return diskSpace.getAvailableSpace() - diskSpace.getCurrentUse();
    }

    private DiskSpaceCommand getDiskSpace(User user) throws SQLException {
        ResultSet resultSet = connection.createStatement()
                .executeQuery("SELECT * FROM user_files WHERE user_id=(SELECT user.id FROM user WHERE username='" + user.getUsername() + "')");
        long currentWeight = 0;
        long availableSpace = 0;
        while (resultSet.next()) {
            currentWeight += resultSet.getLong("size");
        }

        ResultSet space = connection.createStatement()
                .executeQuery("SELECT * FROM user_setting WHERE user_id=(SELECT user.id FROM user WHERE username='" + user.getUsername() + "')");

        while (space.next()) {
            availableSpace += space.getLong("available_space");
        }
        return new DiskSpaceCommand(availableSpace, currentWeight);
    }

    private Integer findFileInfo(String username, String filename, long size) throws SQLException {
        List<UserFiles> filesUser = findAllFilesUser(username);
        return (int) filesUser.stream()
                .filter(file -> file.getFileName().equals(filename))
                .filter(file -> file.getSize().equals(size))
                .count();
    }

    private List<UserFiles> findAllFilesUser(String username) throws SQLException {
        ResultSet resultSet = connection.createStatement()
                .executeQuery("SELECT * FROM user_files WHERE user_id=(SELECT user.id FROM user WHERE username='" + username + "')");
        List<UserFiles> userFiles = new ArrayList<>();
        while (resultSet.next()) {
            userFiles.add(UserFiles.builder()
                    .id(resultSet.getInt("id"))
                    .userId(resultSet.getInt("user_id"))
                    .size(resultSet.getLong("size"))
                    .fileName(resultSet.getString("filename"))
                    .build());
        }
        return userFiles;
    }

    private void deleteFiles(List<String> ids) throws SQLException {
        connection.createStatement()
                .executeUpdate("DELETE FROM user_files WHERE id in (" + String.join(",", ids) + ")");
    }

    private void setFileInfo(String username, String filename, long size) throws SQLException {
        connection.createStatement()
                .executeUpdate("INSERT INTO user_files (user_id, size, filename) VALUES ((SELECT user.id FROM user WHERE username='" + username + "'), '" + size + "', '" + filename + "')");
    }

    private void setShareFiles(String userName, ShareFileCommand shareFileCommand, String link) throws SQLException {
        List<UserFiles> allFilesUser = findAllFilesUser(userName);
        UserFiles userFiles = allFilesUser.stream()
                .filter(files -> files.getFileName().equals(shareFileCommand.getFileName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("File: %s not found for user: %s", shareFileCommand.getFileName(), userName)));

        connection.createStatement()
                .executeUpdate("INSERT INTO share_files (file_id, limit_download, link, expire_date, username) " +
                        "values (" + userFiles.getId() + ", " +
                        (shareFileCommand.isSingleDownload() ? 1 : 0) + ", '" +
                        link + "', " +
                        "DATE('" + shareFileCommand.getExpireDateTime() + "')" + ", '" +
                        userName + "')");
    }

    private ShareFileDTO findShareFile(String link) throws SQLException {
        ShareFileDTO shared = null;
        ResultSet resultSet = connection.createStatement()
                .executeQuery("SELECT * FROM share_files WHERE link='" + link + "'");
        while (resultSet.next()) {
            shared = ShareFileDTO.builder()
                    .id(resultSet.getInt("id"))
                    .fileId(resultSet.getInt("file_id"))
                    .limitDownload(resultSet.getInt("limit_download"))
                    .link(resultSet.getString("link"))
                    .expireDate(LocalDate.parse(resultSet.getString("expire_date")))
                    .username(resultSet.getString("username"))
                    .build();
        }
        return shared;
    }

    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            log.error("Failed to close connection", e);
        }
    }
}
