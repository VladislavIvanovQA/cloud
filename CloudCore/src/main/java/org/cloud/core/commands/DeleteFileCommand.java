package org.cloud.core.commands;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class DeleteFileCommand implements Serializable {
    private final String fileName;
}
