package org.cloud.core;

import org.cloud.core.commands.*;
import org.cloud.core.dto.User;

import java.io.Serializable;

public class Command implements Serializable {

    private Object data;
    private CommandType type;

    public Command() {
    }

    public static Command authCommand(String login, String password) {
        Command command = new Command();
        command.data = new AuthCommandData(login, password);
        command.type = CommandType.AUTH;
        return command;
    }

    public static Command authOkCommand(String username) {
        Command command = new Command();
        command.data = new AuthOkCommandData(username);
        command.type = CommandType.AUTH_OK;
        return command;
    }

    public static Command authTimeOutCommand(String errorMessage) {
        Command command = new Command();
        command.type = CommandType.AUTH_TIME_OUT;
        command.data = new ErrorCommandData(errorMessage);
        return command;
    }

    public static Command regCommand(String login, String password, String username) {
        Command command = new Command();
        command.data = new User(login, password, username);
        command.type = CommandType.REG;
        return command;
    }

    public static Command errorCommand(String errorMessage) {
        Command command = new Command();
        command.type = CommandType.ERROR;
        command.data = new ErrorCommandData(errorMessage);
        return command;
    }

    public static Command sendFileCommand(SendFileCommand fileCommand) {
        Command command = new Command();
        command.type = CommandType.SEND_FILE;
        command.data = fileCommand;
        return command;
    }

    public static Command messageCommand(String message) {
        Command command = new Command();
        command.type = CommandType.MESSAGE;
        command.data = new MessageCommand(message);
        return command;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public CommandType getType() {
        return type;
    }

    public void setType(CommandType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Command{" +
                "data=" + data +
                ", type=" + type +
                '}';
    }
}
