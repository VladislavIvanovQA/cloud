package org.cloud.core.commands;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class SendFileCommand implements Serializable {
    private static final int BUTCH_SIZE = 8192;

    private final String name;
    private final long size;
    private final byte[] bytes;
    private final boolean isFirstButch;
    private final int endByteNum;
    private final boolean isFinishBatch;

    public SendFileCommand(String name,
                           long size,
                           byte[] bytes,
                           boolean isFirstButch,
                           int endByteNum,
                           boolean isFinishBatch) {
        this.name = name;
        this.size = size;
        this.bytes = bytes;
        this.isFirstButch = isFirstButch;
        this.endByteNum = endByteNum;
        this.isFinishBatch = isFinishBatch;
    }

    @Override
    public String toString() {
        return "SendFileCommand{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", isFirstButch=" + isFirstButch +
                ", endByteNum=" + endByteNum +
                ", isFinishBatch=" + isFinishBatch +
                '}';
    }
}
