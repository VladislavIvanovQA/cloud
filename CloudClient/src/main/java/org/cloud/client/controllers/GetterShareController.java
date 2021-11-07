package org.cloud.client.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import org.cloud.client.Client;
import org.cloud.client.dialogs.Dialogs;
import org.cloud.client.model.Network;
import org.cloud.core.Command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class GetterShareController {
    @FXML
    public Button getFile, closeBtn, selectFolderInput;
    @FXML
    public TextField linkInput;

    @FXML
    public void checkLink(ActionEvent actionEvent) {
        String text = linkInput.getText();
        System.out.println("check text: text");
        if (!text.isBlank() && text.contains(Command.SHARE_LINK)) {
            Platform.runLater(Dialogs.AppError.INVALID_LINK::show);
        }
    }

    @FXML
    public void selectFolder(ActionEvent actionEvent) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        configuringDirectoryChooser(directoryChooser);
        File dir = directoryChooser.showDialog(Client.INSTANCE.getGetterShareStage());
        System.out.println(dir);
        if (dir != null) {
            selectFolderInput.setText(dir.getAbsolutePath());
        } else {
            selectFolderInput.setText("Please select folder to download");
        }
    }

    @FXML
    public void getFileAction(ActionEvent actionEvent) throws IOException {
        String text = linkInput.getText();
        if (!text.isBlank() && text.contains(Command.SHARE_LINK)) {
            getNetwork().sendGetShareFileCommand(text);
            CloudMainController.downloadPath = Path.of(selectFolderInput.getText());
            Platform.runLater(Client.INSTANCE.getGetterShareStage()::close);
        } else {
            Platform.runLater(Dialogs.AppError.INVALID_LINK::show);
        }

    }

    @FXML
    public void closeAction(ActionEvent actionEvent) {
        Platform.runLater(Client.INSTANCE.getGetterShareStage()::close);
    }

    private boolean connectToServer() {
        Network network = getNetwork();
        return network.isConnected() || network.connect();
    }

    private Network getNetwork() {
        return Network.getInstance();
    }

    private void configuringDirectoryChooser(DirectoryChooser directoryChooser) {
        // Set title for DirectoryChooser
        directoryChooser.setTitle("Select Some Directories");

        // Set Initial Directory
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
    }
}
