package org.cloud.server.db;

import org.cloud.core.commands.DeleteFileCommand;
import org.cloud.core.commands.SendFileCommand;
import org.cloud.core.dto.User;
import org.cloud.server.dto.UserFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        return availableSpace - currentWeight;
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
