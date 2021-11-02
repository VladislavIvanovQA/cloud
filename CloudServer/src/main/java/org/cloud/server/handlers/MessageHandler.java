package org.cloud.server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.cloud.core.Command;
import org.cloud.core.commands.AuthCommandData;
import org.cloud.core.commands.SendFileCommand;
import org.cloud.core.dto.User;
import org.cloud.server.db.DbService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

@Slf4j
public class MessageHandler extends SimpleChannelInboundHandler<Command> {
    private final Path root;
    private boolean auth = false;
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
        if (auth) {
            switch (command.getType()) {
                case MESSAGE: {
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
                        sendCommand(ctx, Command.messageCommand("File ok!"));
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

                    String username = dbService.getUsernameByLoginAndPassword(login, password);
                    if (username == null) {
                        sendCommand(ctx, Command.errorCommand("Некорректные логин и пароль!"));
                    } else {
                        sendCommand(ctx, Command.authOkCommand(username));
                        auth = true;
                        return;
                    }
                    break;
                }
                case REG: {
                    User data = (User) command.getData();
                    String userOrError = dbService.createUserOrError(data);
                    if (userOrError == null) {
                        sendCommand(ctx, Command.errorCommand("Такой юзер уже существует!"));
                    } else {
                        sendCommand(ctx, Command.authOkCommand(userOrError));
                        auth = true;
                    }
                    break;
                }
            }
        }
    }

    public void sendCommand(ChannelHandlerContext ctx, Command command) {
        log.info("Server: {}", command);
        ctx.writeAndFlush(command);
    }
}