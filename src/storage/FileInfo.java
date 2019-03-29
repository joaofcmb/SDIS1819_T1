package storage;

import java.io.File;

public class FileInfo {
    private File file;

    FileInfo(String path) {
        this.file = new File(path);
    }

    public File getFile() {
        return file;
    }
}
