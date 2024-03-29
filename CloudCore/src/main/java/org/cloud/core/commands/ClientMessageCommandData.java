package org.cloud.core.commands;

import java.io.Serializable;

public class ClientMessageCommandData implements Serializable {

    private final String sender;
    private final String message;

    public ClientMessageCommandData(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "ClientMessageCommandData{" +
                "sender='" + sender + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
