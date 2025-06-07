package data;

import java.util.List;

public class AllFiles {
    public List<String> filePath; // 파일 이름
    public List<String> fileContent; // 파일 내용

    public AllFiles() {
    }

    public AllFiles(List<String> filePath, List<String> fileContent) {
        this.filePath = filePath;
        this.fileContent = fileContent;
    }
}
