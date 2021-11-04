package org.cloud.server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.cloud.core.Command;
import org.cloud.core.CommandType;
import org.cloud.core.commands.AuthCommandData;
import org.cloud.core.commands.DeleteFileCommand;
import org.cloud.core.commands.SendFileCommand;
import org.cloud.core.dto.User;
import org.cloud.server.db.DbService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MessageHandler extends SimpleChannelInboundHandler<Command> {
    private Path root;
    private User user;
    private DbService dbService;

    public MessageHandler(Path root) {
        this.root = root;
        try {
            dbService = new DbService();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Command command) {
        log.info("received: {}", command);
        if (user != null) {
            switch (command.getType()) {
                case MESSAGE: {
                    break;
                }
                case DELETE_FILE: {
                    DeleteFileCommand msg = (DeleteFileCommand) command.getData();
                    List<File> files = new ArrayList<>();
                    getAllFilesInDirectory(root.toAbsolutePath().toString(), files);
                    List<File> fileToDelete = files.stream()
                            .filter(file -> file.getName().equalsIgnoreCase(msg.getFileName()))
                            .collect(Collectors.toList());

                    if (fileToDelete.size() == 0) {
                        sendCommand(ctx, Command.errorCommand("File " + msg.getFileName() + " not exist!"));
                        return;
                    }

                    fileToDelete.forEach(file -> {
                        try {
                            Files.delete(file.toPath());
                            dbService.deleteFileUser(user, msg);
                        } catch (IOException e) {
                            sendCommand(ctx, Command.errorCommand("File " + msg.getFileName() + "  not deleted!"));
                        }
                    });
                    sendCommand(ctx, Command.messageCommand("File " + msg.getFileName() + " delete!"));
                    break;
                }
                case PREPARE_SEND_FILE: {
                    SendFileCommand msg = (SendFileCommand) command.getData();
                    Path file = root.resolve(msg.getName());
                    if (!dbService.checkAvailableSpaceToFile(user, msg.getSize())) {
                        ctx.flush();
                        sendCommand(ctx, Command.sendDiscardFileCommand("There is not enough disk space for your file!"));
                        break;
                    }
                    if (!dbService.setInfoFileForUser(user, msg)) {
                        try {
                            Files.deleteIfExists(file);
                            sendCommand(ctx, Command.sendDiscardFileCommand("File " + msg.getName() + " already exist!"));
                        } catch (IOException e) {
                            sendCommand(ctx, Command.sendDiscardFileCommand(e.getMessage()));
                        }
                        break;
                    }
                    sendCommand(ctx, new Command(null, CommandType.SEND_FILE_APPROVE));
                    break;
                }
                case SEND_FILE: {
                    SendFileCommand msg = (SendFileCommand) command.getData();
                    Path file = root.resolve(msg.getName());

                    if (msg.isFirstButch()) {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            sendCommand(ctx, Command.errorCommand(e.getMessage()));
                        }
                    }

                    try (FileOutputStream os = new FileOutputStream(file.toFile(), true)) {
                        os.write(msg.getBytes(), 0, msg.getEndByteNum());
                    } catch (Exception e) {
                        sendCommand(ctx, Command.errorCommand(e.getMessage()));
                    }
                    if (msg.isFinishBatch()) {
                        sendCommand(ctx, new Command(null, CommandType.SENDER_FILE));
                    }
                    break;
                }
                default: {
                    throw new IllegalCallerException(String.format("Type: %s not supported", command.getType()));
                }
            }
        } else {
            switch (command.getType()) {
                case AUTH: {
                    AuthCommandData data = (AuthCommandData) command.getData();
                    String login = data.getLogin();
                    String password = data.getPassword();

                    User tempUser = dbService.getUsernameByLoginAndPassword(login, password);
                    if (tempUser == null) {
                        sendCommand(ctx, Command.errorCommand("Некорректные логин и пароль!"));
                    } else {
                        sendCommand(ctx, Command.authOkCommand(tempUser.getUsername()));
                        if (!Files.exists(root = root.resolve(tempUser.getUsername()))) {
                            try {
                                Files.createDirectory(root);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        user = tempUser;
                        return;
                    }
                    break;
                }
                case REG: {
                    User data = (User) command.getData();
                    User userOrError = dbService.createUserOrError(data);
                    if (userOrError == null) {
                        sendCommand(ctx, Command.errorCommand("Такой юзер уже существует!"));
                    } else {
                        sendCommand(ctx, Command.authOkCommand(userOrError.getUsername()));
                        user = userOrError;
                    }
                    break;
                }
                default: {
                    if (command.getType() == CommandType.SEND_FILE) {
                        if (((SendFileCommand) command.getData()).isFinishBatch()) {
                            sendCommand(ctx, Command.authTimeOutCommand("Please auth again!"));
                        }
                    } else {
                        sendCommand(ctx, Command.authTimeOutCommand("Please auth again!"));
                    }
                }
            }
        }
    }

    public List<File> getAllFilesInDirectory(String directoryName, List<File> files) {
        File directory = new File(directoryName);
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    getAllFilesInDirectory(file.getAbsolutePath(), files);
                }
            }
        }
        return files;
    }

    public void sendCommand(ChannelHandlerContext ctx, Command command) {
        log.info("Server: {}", command);
        ctx.writeAndFlush(command);
    }
}