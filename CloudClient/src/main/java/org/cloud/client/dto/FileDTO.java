package org.cloud.client.dto;

import javax.swing.filechooser.FileSystemView;
import java.io.File;

public class FileDTO {
    private String diskName;
    private File file;

    public FileDTO(File file) {
        this.diskName = FileSystemView.getFileSystemView().getSystemDisplayName(file);
        this.file = file;
    }

    public String getDiskName() {
        return diskName;
    }

    public void setDiskName(String diskName) {
        this.diskName = diskName;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public String toString() {
        return getDiskName();
    }
}
