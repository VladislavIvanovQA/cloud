package org.cloud.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.cloud.client.Client;
import org.cloud.client.dialogs.Dialogs;
import org.cloud.client.model.Network;
import org.cloud.client.model.ReadCommandListener;
import org.cloud.core.CommandType;
import org.cloud.core.commands.AuthOkCommandData;

import java.io.IOException;
import java.time.LocalDate;

public class ShareController {
    private final DatePicker maxDate = new DatePicker();
    @FXML
    public Button shareBtn, closeBtn;
    @FXML
    public DatePicker datepicker;
    @FXML
    public Label pathLabel;
    @FXML
    public CheckBox singDownload;
    private ReadCommandListener readMessageListener;
    private String filename;

    @FXML
    public void shareFileAction(ActionEvent actionEvent) {
        try {
            getNetwork().sendShareCommand(filename, singDownload.isSelected(), datepicker.getValue());
            Platform.runLater(() -> Client.INSTANCE.getShareStage().close());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void closeAction(ActionEvent actionEvent) {

    }

    public void init(String filename) {
        pathLabel.setText(filename);
        this.filename = filename;
        maxDate.setValue(LocalDate.now());
        Callback<DatePicker, DateCell> dayCellFactory = (final DatePicker datePicker) -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item.isBefore(maxDate.getValue())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
            }
        };
        datepicker.setDayCellFactory(dayCellFactory);
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
