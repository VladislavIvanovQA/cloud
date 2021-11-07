package org.cloud.core.commands;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class ListFilesCommand implements Serializable {
    private final List<String> files;
}
