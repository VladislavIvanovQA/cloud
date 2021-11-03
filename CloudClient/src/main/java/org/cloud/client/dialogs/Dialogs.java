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
        EMPTY_CREDENTIALS("Логин и пароль должны быть указаны!"),
        INVALID_CREDENTIALS("Логин и пароль заданы некорректно!"),
        TIME_OUT("Привышено время ожидания"),
        ;

        private static final String TITLE = "Ошибка аутентификации";
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
        EMPTY_CREDENTIALS("Логин, пароль и имя пользователя должны быть указаны!"),
        USER_EXISTS("Пользователь уже существует!");

        private static final String TITLE = "Ошибка регистрации";
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
        SEND_MESSAGE("Не удалось отправить сообщение!"),
        SERVER_CONNECT("Не удалось установить соединение с сервером!"),
        ;

        private static final String TITLE = "Сетевая ошибка";
        private static final String TYPE = "Ошибка передачи данных по сети";
        private final String message;

        NetworkError(String message) {
            this.message = message;
        }

        public void show() {
            showDialog(Alert.AlertType.ERROR, TITLE, TYPE, message);
        }

    }

}
