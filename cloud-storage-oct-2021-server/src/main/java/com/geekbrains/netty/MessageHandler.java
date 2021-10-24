package com.geekbrains.netty;

import com.geekbrains.model.AbstractMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class MessageHandler extends SimpleChannelInboundHandler<AbstractMessage> {
    private Path root;

    public MessageHandler(Path root) {
        this.root = root;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AbstractMessage msg) throws Exception {
        log.debug("received: {}", msg);
        if (msg.getFile() != null) {
            if (!Files.exists(root.resolve(msg.getUsername()))) {
                Files.createDirectory(root.resolve(msg.getUsername()));
            }
            File outputFile = root.resolve(msg.getUsername()).resolve(msg.getFilename()).toFile();
            if (Files.exists(outputFile.toPath())) {
                Files.delete(outputFile.toPath());
                Files.createFile(outputFile.toPath());
                Files.write(outputFile.toPath(), msg.getFile());
            } else {
                Files.createFile(outputFile.toPath());
                Files.write(outputFile.toPath(), msg.getFile());
            }
        }
        ctx.writeAndFlush(msg);
    }
}