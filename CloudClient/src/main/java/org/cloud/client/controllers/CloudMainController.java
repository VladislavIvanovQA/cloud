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
import org.cloud.core.Command;
import org.cloud.core.CommandType;
import org.cloud.core.commands.DiskSpaceCommand;
import org.cloud.core.commands.ErrorCommandData;
import org.cloud.core.commands.ListFilesCommand;
import org.cloud.core.commands.SendFileCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class CloudMainController {
    public static final String ERROR_TITLE = "Error";
    public static final String ERROR_DISCARD_FILE = "Discard file!";
    public static final String LINK_TITLE = "Successes!";

    @FXML
    public TreeView<File> tree;
    @FXML
    public ComboBox<FileDTO> diskList;
    private final List<Path> autoSyncFolder = new CopyOnWriteArrayList<>();
    @FXML
    public Button refreshFilesInCloud;
    @FXML
    public ProgressBar diskLarge;
    public static Path downloadPath;
    @FXML
    public Button getFileByLink;

    private ReadCommandListener readCommandListener;
    private Path filePath;
    @FXML
    public Label available, current;
    private ReadCommandListener fileMessageListener;
    private List<String> foldersInCloud = new CopyOnWriteArrayList<>();

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
        sendRequestToSpace();

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
                            null,
                            commandData.getErrorMessage()));
                    break;
                }
                case DISCARD_FILE: {
                    getNetwork().removeReadMessageListener(fileMessageListener);
                    Platform.runLater(() -> Dialogs.show(
                            Alert.AlertType.ERROR,
                            ERROR_DISCARD_FILE,
                            null,
                            command.getData().toString()));
                    break;
                }
                case SENDER_FILE: {
                    getNetwork().removeReadMessageListener(fileMessageListener);
                    break;
                }
                case LIST_FILE_RESPONSE: {
                    ListFilesCommand listFilesCommand = (ListFilesCommand) command.getData();
                    foldersInCloud = listFilesCommand.getFiles();
                    selectDisk();
                    break;
                }
                case SPACE_RESPONSE: {
                    Platform.runLater(() -> {
                        DiskSpaceCommand spaceCommand = (DiskSpaceCommand) command.getData();
                        double progress = (double) ((spaceCommand.getCurrentUse() * 100) / spaceCommand.getAvailableSpace()) / 100;
                        diskLarge.setProgress(progress);
                        long availableSpace = spaceCommand.getAvailableSpace() / 1024 / 1024;
                        long currentSpace = spaceCommand.getCurrentUse() / 1024 / 1024;
                        available.setText("Available space: " + (availableSpace - currentSpace) + " MB");
                        current.setText("Used space: " + currentSpace + " MB");
                    });
                    break;
                }
                case SHARE_FILE_RESPONSE: {
                    Platform.runLater(() -> Dialogs.showWithLink(
                            Alert.AlertType.INFORMATION,
                            LINK_TITLE,
                            null,
                            command.getData().toString())
                    );
                    break;
                }
                case SEND_FILE: {
                    SendFileCommand msg = (SendFileCommand) command.getData();
                    Path file = downloadPath.resolve(msg.getName());

                    if (msg.isFirstButch()) {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            Platform.runLater(Dialogs.AppError.ERROR_REPLACE_FILE::show);
                        }
                    }

                    try (FileOutputStream os = new FileOutputStream(file.toFile(), true)) {
                        os.write(msg.getBytes(), 0, msg.getEndByteNum());
                    } catch (Exception e) {
                        Platform.runLater(Dialogs.AppError.ERROR_CREATE_FILE::show);
                    }
                    break;
                }
            }
        });
    }

    public void initContextMenu(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.SECONDARY)) {
            MenuItem sendFleMenu = new MenuItem("Send file to cloud");
            EventTarget target = mouseEvent.getTarget();
            if (target instanceof Text) {
                AtomicReference<String> pathToFile = new AtomicReference<>(((Text) target).getText());
                sendFleMenu.setOnAction(event -> {
                    System.out.println("Send file: " + pathToFile);
                    Thread thread = new Thread(() -> {
                        if (System.getProperty("os.name").toLowerCase().contains("win")) {
                            pathToFile.set(pathToFile.get().replace("\\", "\\\\"));
                        }
                        filePath = Path.of(pathToFile.get());
                        prepareSendFile(filePath);
                        sendFile(filePath);
                        sendRequestToSpace();
                    });
                    thread.start();
                });

                MenuItem deleteFileMenu = new MenuItem("Delete file in cloud");
                deleteFileMenu.setOnAction(event -> {
                    System.out.println("Delete file: " + pathToFile);
                    try {
                        getNetwork().deleteFileMessage(Path.of(pathToFile.get()).getFileName().toString());
                        refreshFiles();
                        sendRequestToSpace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                MenuItem shareFileMenu = new MenuItem("Share file");
                shareFileMenu.setOnAction(event -> {
                    System.out.println("Share file: " + pathToFile);
                    String filename = Path.of(pathToFile.get()).getFileName().toString();
                    if (foldersInCloud.contains(filename)) {
                        Platform.runLater(() -> Client.INSTANCE.switchToShareWindow(filename));
                    } else {
                        Platform.runLater(Dialogs.AppError.FILE_NOT_UPLOAD::show);
                    }
                    sendRequestToSpace();
                });

                MenuItem autoUploadOrDeleteFileMenu = new MenuItem("Auto synchronize folder");
                autoUploadOrDeleteFileMenu.setOnAction(event -> {
                    System.out.println("Auto synchronize folder: " + pathToFile);
                    Path path = Path.of(pathToFile.get());
                    if (!path.toFile().isDirectory()) {
                        Platform.runLater(Dialogs.AppError.SELECT_FOLDER::show);
                        return;
                    }
                    if (!autoSyncFolder.remove(path)) {
                        autoSyncFolder.add(path);
                    }
                    autoSyncFolder.forEach(System.out::println);
                    selectDisk();
                });
                tree.setContextMenu(new ContextMenu(sendFleMenu, autoUploadOrDeleteFileMenu, shareFileMenu, deleteFileMenu));
            }
        }
    }

    public void selectDisk() {
        Platform.runLater(() -> {
            FileTree fileTree = new FileTree(diskList.getValue().getFile());
            fileTree.setFoldersSync(autoSyncFolder);
            fileTree.setFilesInCloud(foldersInCloud);
            tree.setRoot(fileTree);
            tree.refresh();
        });
    }

    public void sendRequestToSpace() {
        try {
            getNetwork().sendRequestSpace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        getNetwork().removeReadMessageListener(readCommandListener);
    }

    public void refreshFiles() {
        try {
            getNetwork().sendRequestListFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepareSendFile(Path path) {
        try {
            SendFileCommand.SendFileCommandBuilder build = SendFileCommand.builder()
                    .name(path.getFileName().toString())
                    .size(Files.size(path));

//            if (path.toFile().isDirectory()) {
//                build.size(Arrays.stream(Objects.requireNonNull(path.toFile().listFiles()))
//                        .map(File::length)
//                        .collect(Collectors.summarizingLong(Long::longValue)).getSum());
//            } else {
            getNetwork().sendPrepareFile(build.build());
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile(Path path) {
        fileMessageListener = getNetwork().addReadMessageListener(command -> {
            if (command.getType() == CommandType.SEND_FILE_APPROVE) {
                boolean isFirstButch = true;
                try (FileInputStream is = new FileInputStream(path.toFile())) {
                    int read;
                    while ((read = is.read(Command.buffer)) != -1) {
                        SendFileCommand message = SendFileCommand.builder()
                                .bytes(Command.buffer)
                                .name(path.getFileName().toString())
                                .size(Files.size(path))
                                .isFirstButch(isFirstButch)
                                .isFinishBatch(is.available() <= 0)
                                .endByteNum(read)
                                .build();
                        isFirstButch = false;
                        getNetwork().sendFile(message);
                    }
                } catch (Exception e) {
                    Platform.runLater(Dialogs.AppError.ERROR_ACCESS_FILE::show);
                }
                refreshFiles();
            }
        });
    }

    public void getFileByLinkAction(ActionEvent actionEvent) {
        Platform.runLater(Client.INSTANCE::switchToGettingShareWindow);
    }
}
