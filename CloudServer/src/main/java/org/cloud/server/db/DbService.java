package org.cloud.server.db;

import org.cloud.core.dto.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DbService {
    private static final String DB_URL = "jdbc:sqlite:CloudServer/cloud.db";
    private Logger log = LoggerFactory.getLogger(DbService.class);
    private Connection connection;

    public DbService() throws SQLException {
        log.info("Try to connection DB: {}", DB_URL);
        connection = DriverManager.getConnection(DB_URL);
        log.info("Connection DB successes!");
    }

    public String getUsernameByLoginAndPassword(String login, String password) {
        User requiredUser = new User(login, password);
        try {
            return findUserByLoginAndPassword(requiredUser).getUsername();
        } catch (SQLException e) {
            log.error("Sql exception!", e);
        }
        return null;
    }

    public String createUserOrError(User user) {
        try {
            if (!findUserByLogin(user.getLogin())) {
                return createUser(user).getUsername();
            } else {
                return null;
            }
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

    private boolean findUserByLogin(String login) throws SQLException {
        ResultSet resultSet = connection.createStatement()
                .executeQuery("SELECT * FROM user WHERE login='" + login + "'");
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
        return user;
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
