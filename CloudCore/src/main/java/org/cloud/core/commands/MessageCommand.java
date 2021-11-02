package org.cloud.core.commands;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageCommand implements Serializable {
    private final String message;
}
