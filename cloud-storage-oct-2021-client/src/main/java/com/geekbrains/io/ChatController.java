package com.geekbrains.io;

import com.geekbrains.model.AbstractMessage;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatController implements Initializable {
    public ListView<String> listView;
    public TextField input;
    private Path root;
    private String userName;
    private ObjectDecoderInputStream dis;
    private ObjectEncoderOutputStream dos;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userName = UUID.randomUUID().toString();
        root = Paths.get("root");
        if (!Files.exists(root)) {
            try {
                Files.createDirectory(root);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

        try {
            fillFilesInView();
        } catch (Exception e) {
            e.printStackTrace();
        }

        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileName = listView.getSelectionModel().getSelectedItem();
                if (!Files.isDirectory(root.resolve(fileName))) {
                    input.setText(fileName);
                } else {
                    input.setText("Select file! Not directory");
                }
            }
        });

        try {
            Socket socket = new Socket("localhost", 8189);
            dis = new ObjectDecoderInputStream(socket.getInputStream());
            dos = new ObjectEncoderOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        AbstractMessage message = (AbstractMessage) dis.readObject();
                        System.out.println(message);
                        Platform.runLater(() -> input.setText(message.getFilename()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            readThread.setDaemon(true);
            readThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fillFilesInView() throws Exception {
        listView.getItems().clear();
        List<String> list = Files.list(root)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        listView.getItems().addAll(list);
    }

    public void sendMessage(ActionEvent actionEvent) throws IOException {
        String fileName = input.getText();
        input.clear();
        Path filePath = root.resolve(fileName);

        byte[] file = null;
        if (Files.exists(filePath)) {
            file = Files.readAllBytes(filePath);
        }
        dos.writeObject(new AbstractMessage(fileName, userName, file));
    }
}
