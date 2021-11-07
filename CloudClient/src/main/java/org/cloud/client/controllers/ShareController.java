package org.cloud.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.cloud.client.Client;
import org.cloud.client.model.Network;

import java.io.IOException;
import java.time.LocalDate;

public class ShareController {
    private final DatePicker maxDate = new DatePicker();
    @FXML
    public Button shareBtn, closeBtn;
    @FXML
    public DatePicker datepicker;
    @FXML
    public Label pathLabel, expireLink;
    @FXML
    public CheckBox isSingleDownload, isLimitDays;
    private String filename;

    @FXML
    public void shareFileAction(ActionEvent actionEvent) {
        try {
            LocalDate expireDateTime = datepicker.getValue();
            if (!isLimitDays.isSelected()) {
                expireDateTime = LocalDate.now().plusYears(10);
            }
            getNetwork().sendShareCommand(filename, isSingleDownload.isSelected(), expireDateTime);
            Platform.runLater(() -> Client.INSTANCE.getShareStage().close());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void closeAction() {
        Platform.runLater(() -> Client.INSTANCE.getShareStage().close());
    }

    public void init(String filename) {
        pathLabel.setText(filename);
        this.filename = filename;
        isSingleDownload.setSelected(false);
        isLimitDays.setSelected(false);
        expireLink.setDisable(true);
        datepicker.setDisable(true);
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
        datepicker.setValue(LocalDate.now());
    }

    private Network getNetwork() {
        return Network.getInstance();
    }

    public void limitAction() {
        Platform.runLater(() -> {
            expireLink.setDisable(!isLimitDays.isSelected());
            datepicker.setDisable(!isLimitDays.isSelected());
        });
    }
}
