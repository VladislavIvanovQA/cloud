package org.cloud.client.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileTree extends TreeItem<File> {
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeaf = true;
    private boolean isLeaf;
    private List<File> filesInCloud = new ArrayList<>();
    private List<File> foldersSync = new ArrayList<>();

    public FileTree(File f) {
        super(f);
    }

    @Override
    public ObservableList<TreeItem<File>> getChildren() {
        ObservableList<TreeItem<File>> children = super.getChildren();
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            children.setAll(buildChildren(this));
        }
        children.stream()
                .filter(fileTreeItem -> filesInCloud.contains(fileTreeItem.getValue()))
                .forEach(fileTreeItem -> fileTreeItem.setGraphic(new Rectangle(16, 16, Color.RED)));

        children.stream()
                .filter(fileTreeItem -> foldersSync.contains(fileTreeItem.getValue()))
                .forEach(fileTreeItem -> fileTreeItem.setGraphic(new Rectangle(16, 16, Color.GREEN)));
        return children;
    }

    @Override
    public boolean isLeaf() {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false;
            File f = getValue();
            isLeaf = f.isFile();
        }
        return isLeaf;
    }

    private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
        File f = TreeItem.getValue();
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();
                for (File childFile : files) {
                    children.add(new FileTree(childFile));
                }
                return children;
            }
        }
        return FXCollections.emptyObservableList();
    }

    public void setFilesInCloud(List<String> filesInCloud) {
        this.filesInCloud = buildChildren(this).stream()
                .filter(item -> filesInCloud.stream()
                        .anyMatch(str -> item.getValue().getName().equals(str)))
                .map(TreeItem::getValue)
                .collect(Collectors.toList());
        System.out.println("list files: " + this.filesInCloud);
    }

    public void setFoldersSync(List<Path> foldersSync) {
        this.foldersSync = foldersSync.stream()
                .map(Path::toFile).collect(Collectors.toList());
    }
}