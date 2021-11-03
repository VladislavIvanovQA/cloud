package org.cloud.client.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import org.cloud.client.Client;
import org.cloud.client.dto.FileDTO;
import org.cloud.client.model.Network;
import org.cloud.client.model.ReadCommandListener;
import org.cloud.client.utils.FileTree;
import org.cloud.core.CommandType;
import org.cloud.core.commands.MessageCommand;
import org.cloud.core.commands.SendFileCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class CloudMainController {
    private final byte[] buffer = new byte[8192];
    @FXML
    public TreeView<File> tree;
    @FXML
    public ComboBox<FileDTO> diskList;
    private ReadCommandListener readCommandListener;

    public void init() {
        File firstDisk = Arrays.stream(File.listRoots())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("We not have access to any disk!"));
        List<FileDTO> disks = Arrays.stream(File.listRoots())
                .map(FileDTO::new)
                .collect(Collectors.toList());
        diskList.setItems(FXCollections.observableList(disks));
        diskList.setValue(disks.get(0));

        tree.setRoot(new FileTree(firstDisk));

        tree.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (new KeyCodeCombination(KeyCode.K, KeyCombination.SHIFT_DOWN).match(event)) {
                Client.INSTANCE.getAuthStage().show();
            }
        });
    }

    private Network getNetwork() {
        return Network.getInstance();
    }

    public void initMessageHandler() {
        readCommandListener = getNetwork().addReadMessageListener(command -> {
            System.out.println("Listener: " + command);
            switch (command.getType()) {
                case AUTH_TIME_OUT: {
                    System.out.println("switch");
                    Platform.runLater(() -> Client.INSTANCE.switchToAuthWindow());
                }
            }
            if (command.getType() == CommandType.MESSAGE) {
                MessageCommand data = (MessageCommand) command.getData();
//                Platform.runLater(() -> {
//                    String message = "Message";
//                    Dialogs.show(Alert.AlertType.INFORMATION, message, message, data.getMessage());
//                });
            }
        });
    }

    public void initContextMenu(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
            MenuItem test = new MenuItem("Send file to cloud");
            EventTarget target = mouseEvent.getTarget();
            if (target instanceof Text) {
                AtomicReference<String> pathToFile = new AtomicReference<>(((Text) target).getText());
                test.setOnAction(event -> {
                    System.out.println("Send file: " + pathToFile);
                    Thread thread = new Thread(() -> {
                        boolean isFirstButch = true;
                        if (System.getProperty("os.name").toLowerCase().contains("win")) {
                            pathToFile.set(pathToFile.get().replace("\\", "\\\\"));
                        }
                        Path filePath = Path.of(pathToFile.get());
                        try (FileInputStream is = new FileInputStream(filePath.toFile())) {
                            long size = Files.size(filePath);
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                SendFileCommand message = SendFileCommand.builder()
                                        .bytes(buffer)
                                        .name(filePath.getFileName().toString())
                                        .size(size)
                                        .isFirstButch(isFirstButch)
                                        .isFinishBatch(is.available() <= 0)
                                        .endByteNum(read)
                                        .build();
                                isFirstButch = false;
                                getNetwork().sendFile(message);
                            }
                        } catch (Exception e) {
                            System.err.println("e: " + e);
                        }
                    });
                    thread.start();
                });

                MenuItem test1 = new MenuItem("Delete file in cloud");
                test1.setOnAction(event -> {
                    System.out.println("Delete file: " + pathToFile);
                    try {
                        getNetwork().deleteFileMessage(Path.of(pathToFile.get()).getFileName().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                MenuItem test2 = new MenuItem("Share file");
                test2.setOnAction(event -> {
                    System.out.println("Share file: " + pathToFile);
                });
                tree.setContextMenu(new ContextMenu(test, test2, test1));
            }
        }
    }

    public void selectDisk(ActionEvent actionEvent) {
        tree.setRoot(new FileTree(diskList.getValue().getFile()));
        tree.refresh();
    }

    public void close() {
        getNetwork().removeReadMessageListener(readCommandListener);
    }
}
