package org.cloud.client.model;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import org.cloud.core.Command;
import org.cloud.core.commands.SendFileCommand;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Network {
    private static final int SERVER_PORT = 8189;
    private static final String SERVER_HOST = "localhost";
    public static String username;
    private static Network INSTANCE;
    private final String host;
    private final int port;
    private final List<ReadCommandListener> listeners = new CopyOnWriteArrayList<>();
    private Socket socket;
    private ObjectDecoderInputStream dis;
    private ObjectEncoderOutputStream dos;
    private Thread readMessageProcess;
    private boolean connected;

    private Network(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private Network() {
        this(SERVER_HOST, SERVER_PORT);
    }

    public static Network getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Network();
        }

        return INSTANCE;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            dos = new ObjectEncoderOutputStream(socket.getOutputStream());
            dis = new ObjectDecoderInputStream(socket.getInputStream());
            readMessageProcess = startReadMessageProcess();
            connected = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to establish connection");
            return false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    private Thread startReadMessageProcess() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    Command command = readCommand();
                    if (command == null) {
                        continue;
                    }
                    for (ReadCommandListener messageListener : listeners) {
                        messageListener.processReceivedCommand(command);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to read message from server");
                    close();
                    break;
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private Command readCommand() throws IOException {
        Command command = null;
        try {
            command = (Command) dis.readObject();
            System.out.println("Read command: " + command);
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to read Command class");
            e.printStackTrace();
        }
        return command;
    }

    public void sendMessage(String message) throws IOException {
        sendCommand(Command.messageCommand(message));
    }

    public void sendFile(SendFileCommand fileCommand) throws IOException {
        sendCommand(Command.sendFileCommand(fileCommand));
    }

    public void sendAuthMessage(String login, String password) throws IOException {
        sendCommand(Command.authCommand(login, password));
    }

    public void sendRegMessage(String login, String password, String username) throws IOException {
        sendCommand(Command.regCommand(login, password, username));
    }

    private void sendCommand(Command command) throws IOException {
        try {
            System.out.println("Client: " + command);
            dos.writeObject(command);
        } catch (IOException e) {
            System.err.println("Failed to send message to server");
            throw e;
        }
    }

    public ReadCommandListener addReadMessageListener(ReadCommandListener listener) {
        listeners.add(listener);
        return listener;
    }

    public void removeReadMessageListener(ReadCommandListener listener) {
        listeners.remove(listener);
    }

    public void clearReadMessageListener() {
        listeners.clear();
    }


    public void close() {
        try {
            connected = false;
            readMessageProcess.interrupt();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
