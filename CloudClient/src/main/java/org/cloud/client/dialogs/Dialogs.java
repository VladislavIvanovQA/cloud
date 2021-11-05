package org.cloud.client.dialogs;

import javafx.scene.control.Alert;
import org.cloud.client.Client;

public class Dialogs {

    private static void showDialog(Alert.AlertType dialogType, String title, String type, String message) {
        Alert alert = new Alert(dialogType);
        alert.initOwner(Client.INSTANCE.getMainStage());
        alert.setTitle(title);
        alert.setHeaderText(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void show(Alert.AlertType dialogType, String title, String type, String message) {
        showDialog(dialogType, title, type, message);
    }

    public enum AuthError {
        EMPTY_CREDENTIALS("Please enter login and password!"),
        INVALID_CREDENTIALS("Login and password invalid!"),
        TIME_OUT("Time-out response."),
        ;

        private static final String TITLE = "Authentication error";
        private static final String TYPE = TITLE;
        private final String message;

        AuthError(String message) {
            this.message = message;
        }

        public void show() {
            showDialog(Alert.AlertType.ERROR, TITLE, TYPE, message);
        }
    }

    public enum RegError {
        EMPTY_CREDENTIALS("Please enter all fields: login, password and username!"),
        USER_EXISTS("User exist!");

        private static final String TITLE = "Registration error";
        private static final String TYPE = TITLE;
        private final String message;

        RegError(String message) {
            this.message = message;
        }

        public void show() {
            showDialog(Alert.AlertType.ERROR, TITLE, TYPE, message);
        }
    }

    public enum NetworkError {
        SEND_MESSAGE("Failed to send message"),
        SERVER_CONNECT("Server un available!"),
        ;

        private static final String TITLE = "Network error";
        private static final String TYPE = "Failed network send message";
        private final String message;

        NetworkError(String message) {
            this.message = message;
        }

        public void show() {
            showDialog(Alert.AlertType.ERROR, TITLE, TYPE, message);
        }

    }

    public enum AppError {
        SELECT_FOLDER("Please select folder!"),
        SELECT_FILE("Please select file!"),
        ERROR_ACCESS_FILE("Please run to Administration rule!");

        private static final String TITLE = "Application error!";
        private final String message;

        AppError(String message) {
            this.message = message;
        }

        public void show() {
            showDialog(Alert.AlertType.ERROR, TITLE, null, message);
        }
    }

}
