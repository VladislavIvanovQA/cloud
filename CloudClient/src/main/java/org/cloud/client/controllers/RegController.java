package org.cloud.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.cloud.client.Client;
import org.cloud.client.dialogs.Dialogs;
import org.cloud.client.model.Network;
import org.cloud.client.model.ReadCommandListener;
import org.cloud.core.CommandType;
import org.cloud.core.commands.AuthOkCommandData;

import java.io.IOException;

public class RegController {
    @FXML
    private TextField loginField, userName;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button registrationButton;
    private ReadCommandListener readMessageListener;

    @FXML
    public void registration(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        String username = userName.getText();

        if (login == null || login.isBlank() ||
                password == null || password.isBlank() ||
                username == null || username.isBlank()) {
            Dialogs.RegError.EMPTY_CREDENTIALS.show();
            return;
        }

        if (!connectToServer()) {
            Dialogs.NetworkError.SERVER_CONNECT.show();
            return;
        }

        try {
            Network.getInstance().sendRegMessage(login, password, username);
        } catch (IOException e) {
            Dialogs.NetworkError.SEND_MESSAGE.show();
            e.printStackTrace();
        }
    }

    private boolean connectToServer() {
        Network network = getNetwork();
        return network.isConnected() || network.connect();
    }

    private Network getNetwork() {
        return Network.getInstance();
    }

    public void initMessageHandler() {
        loginField.clear();
        userName.clear();
        passwordField.clear();

        readMessageListener = getNetwork().addReadMessageListener(command -> {
            if (command.getType() == CommandType.AUTH_OK) {
                AuthOkCommandData data = (AuthOkCommandData) command.getData();
                Platform.runLater(() -> Client.INSTANCE.switchToMainChatWindow(data.getUsername()));
            } else if (command.getType() == CommandType.AUTH_TIME_OUT) {
                Platform.runLater(Dialogs.AuthError.TIME_OUT::show);
            } else if (command.getType() == CommandType.ERROR) {
                Platform.runLater(Dialogs.RegError.USER_EXISTS::show);
            } else {
                Platform.runLater(Dialogs.AuthError.INVALID_CREDENTIALS::show);
            }
        });
    }

    public void close() {
        getNetwork().removeReadMessageListener(readMessageListener);
    }
}
