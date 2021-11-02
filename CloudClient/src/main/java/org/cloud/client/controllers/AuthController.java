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

public class AuthController {

    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button authButton, registrationButton;
    private ReadCommandListener readMessageListener;


    @FXML
    public void executeAuth(ActionEvent actionEvent) {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            Dialogs.AuthError.EMPTY_CREDENTIALS.show();
            return;
        }

        if (!connectToServer()) {
            Dialogs.NetworkError.SERVER_CONNECT.show();
            return;
        }

        try {
            Network.getInstance().sendAuthMessage(login, password);
        } catch (IOException e) {
            Dialogs.NetworkError.SEND_MESSAGE.show();
            e.printStackTrace();
        }
    }

    @FXML
    public void registration(ActionEvent actionEvent) {
        Platform.runLater(() -> Client.INSTANCE.switchToRegistrationWindow());
    }

    private boolean connectToServer() {
        Network network = getNetwork();
        return network.isConnected() || network.connect();
    }

    private Network getNetwork() {
        return Network.getInstance();
    }

    public void initMessageHandler() {
        readMessageListener = getNetwork().addReadMessageListener(command -> {
            if (command.getType() == CommandType.AUTH_OK) {
                AuthOkCommandData data = (AuthOkCommandData) command.getData();
                Platform.runLater(() -> Client.INSTANCE.switchToMainChatWindow(data.getUsername()));
            } else if (command.getType() == CommandType.AUTH_TIME_OUT) {
                Platform.runLater(Dialogs.AuthError.TIME_OUT::show);
            } else {
                Platform.runLater(Dialogs.AuthError.INVALID_CREDENTIALS::show);
            }
        });
    }

    public void close() {
        getNetwork().removeReadMessageListener(readMessageListener);
    }
}
