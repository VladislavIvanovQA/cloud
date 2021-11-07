package org.cloud.core.commands;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class DiskSpaceCommand implements Serializable {
    private final Long availableSpace;
    private final Long currentUse;
}
