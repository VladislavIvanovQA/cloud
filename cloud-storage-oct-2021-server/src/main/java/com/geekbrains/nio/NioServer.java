package com.geekbrains.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


// ls -> список файлов в текущей папке +
// cat file -> вывести на экран содержание файла +
// cd path -> перейти в папку
// touch file -> создать пустой файл
public class NioServer {
    private Path root;
    private ServerSocketChannel server;
    private Selector selector;
    private ByteBuffer buffer;


    public NioServer() throws Exception {
        root = Path.of("root");
        buffer = ByteBuffer.allocate(256);
        server = ServerSocketChannel.open(); // accept -> SocketChannel
        server.bind(new InetSocketAddress(8189));
        selector = Selector.open();
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
                System.out.println("next");
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    System.out.println("accept");
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    System.out.println("Reader");
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws Exception {

        SocketChannel channel = (SocketChannel) key.channel();

        StringBuilder sb = new StringBuilder();

        while (true) {
            int read = channel.read(buffer);
            if (read == -1) {
                channel.close();
                return;
            }
            if (read == 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                sb.append((char) buffer.get());
            }
            buffer.clear();
        }

        String inputText = sb.toString();
        if (inputText.startsWith("ls")) {
            List<String> files = Files.list(root)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            files.forEach(file -> {
                try {
                    sendMessage(channel, file + "\r\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else if (inputText.startsWith("cat")) {
            String cat = inputText.replace("cat ", "")
                    .replaceAll("([\\r\\n])", "");
            if (cat.length() <= 3) {
                sendMessage(channel, "Please entry file name!");
                sendMessage(channel, "\r\n");
            } else {
                Path path = root.resolve(cat);
                if (Files.exists(path)) {
                    if (!Files.isDirectory(path)) {
                        byte[] array = Files.readAllBytes(path);
                        if (array.length == 0) {
                            sendMessage(channel, "File empty!");
                            sendMessage(channel, "\r\n");
                        } else {
                            channel.write(ByteBuffer.wrap(array));
                        }
                    } else {
                        sendMessage(channel, String.format("This folder %s", path));
                        sendMessage(channel, "\r\n");
                    }
                } else {
                    sendMessage(channel, String.format("File %s not found!", path));
                }
            }
            sendMessage(channel, "\r\n");
        } else if (inputText.startsWith("cd")) {
            Path cd = root.resolve(inputText.replace("cd ", "")
                    .replaceAll("([\\r\\n])", ""));
            if (Files.exists(cd)) {
                if (Files.isDirectory(cd)) {
                    root = cd;
                } else {
                    sendMessage(channel, "Please select folder!");
                    sendMessage(channel, "\r\n");
                }
            } else {
                sendMessage(channel, String.format("Path %s incorrect!", cd));
                sendMessage(channel, "\r\n");
            }
        } else if (inputText.startsWith("touch")) {
            Files.createFile(root.resolve(inputText.replace("touch ", "").replaceAll("([\\r\\n])", "")));
        } else {
            sendMessage(channel, "[From server]: " + sb);
        }
    }

    private void sendMessage(SocketChannel channel, String s) throws IOException {
        channel.write(ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8)));
    }

    private void handleAccept(SelectionKey key) throws Exception {
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ, "Hello world!");
    }


    public static void main(String[] args) throws Exception {
        new NioServer();
    }
}
