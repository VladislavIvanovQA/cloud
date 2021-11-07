package org.cloud.client.dialogs;

import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;
import org.cloud.client.Client;
import org.cloud.core.Command;

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

    public static void showWithLink(Alert.AlertType dialogType, String title, String type, String message) {
        FlowPane flowPane = new FlowPane();
        Label label = new Label("Your link: ");
        Alert alert = new Alert(dialogType);
        Hyperlink hyperlink = new Hyperlink(message);
        Tooltip value = new Tooltip("Link copied to clipboard!");
        hyperlink.setTooltip(value);
        hyperlink.setOnAction(event -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(hyperlink.getText());
            clipboard.setContent(content);
            value.setAutoHide(true);
            value.show(alert.getOwner(), alert.getX(), alert.getY());
            PauseTransition pt = new PauseTransition(Duration.millis(2000));
            pt.setOnFinished(e -> {
                value.hide();
            });
            pt.play();
        });
        flowPane.getChildren().addAll(label, hyperlink);
        alert.initOwner(Client.INSTANCE.getMainStage());
        alert.setTitle(title);
        alert.setHeaderText(type);
        alert.getDialogPane().contentProperty().set(flowPane);
        alert.showAndWait();
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
        ERROR_ACCESS_FILE("Please run to Administration rule!"),
        ERROR_REPLACE_FILE("Error replace file!"),
        ERROR_CREATE_FILE("Error create file!"),
        INVALID_LINK("Please set valid link in format: " + Command.SHARE_LINK),
        FILE_NOT_UPLOAD("Please upload file and try again or select file uploaded!");

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
