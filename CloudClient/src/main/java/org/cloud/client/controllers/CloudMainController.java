package org.cloud.client.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.text.Text;
import org.cloud.client.Client;
import org.cloud.client.dialogs.Dialogs;
import org.cloud.client.dto.FileDTO;
import org.cloud.client.model.Network;
import org.cloud.client.model.ReadCommandListener;
import org.cloud.client.utils.FileTree;
import org.cloud.core.CommandType;
import org.cloud.core.commands.ErrorCommandData;
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
    public static final String ERROR_TITLE = "Error";
    public static final String ERROR_DISCARD_FILE = "Discard file!";
    private final byte[] buffer = new byte[64000];
    @FXML
    public TreeView<File> tree;
    @FXML
    public ComboBox<FileDTO> diskList;
    private ReadCommandListener readCommandListener;
    private Path filePath;
    private ReadCommandListener fileMessageListener;

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
                    Platform.runLater(() -> Client.INSTANCE.switchToAuthWindow());
                    break;
                }
                case ERROR: {
                    ErrorCommandData commandData = (ErrorCommandData) command.getData();
                    Platform.runLater(() -> Dialogs.show(
                            Alert.AlertType.ERROR,
                            ERROR_TITLE,
                            commandData.getErrorMessage(),
                            commandData.getErrorMessage()));
                    break;

                }
                case DISCARD_FILE: {
                    getNetwork().removeReadMessageListener(fileMessageListener);
                    Platform.runLater(() -> Dialogs.show(
                            Alert.AlertType.ERROR,
                            ERROR_DISCARD_FILE,
                            command.getData().toString(),
                            command.getData().toString()));
                    break;
                }
                case SENDER_FILE: {
                    getNetwork().removeReadMessageListener(fileMessageListener);
                    break;
                }
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
                        if (System.getProperty("os.name").toLowerCase().contains("win")) {
                            pathToFile.set(pathToFile.get().replace("\\", "\\\\"));
                        }
                        filePath = Path.of(pathToFile.get());
                        try {
                            long size = Files.size(filePath);
                            getNetwork().sendPrepareFile(SendFileCommand.builder()
                                    .name(filePath.getFileName().toString())
                                    .size(size)
                                    .build());

                            fileMessageListener = getNetwork().addReadMessageListener(command -> {
                                if (command.getType() == CommandType.SEND_FILE_APPROVE) {
                                    boolean isFirstButch = true;
                                    try (FileInputStream is = new FileInputStream(filePath.toFile())) {
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
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
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
