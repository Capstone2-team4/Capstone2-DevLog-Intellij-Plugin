package data;

import java.util.List;

public class SolvedCodeFiles {
    public List<String> filePath; // 파일 이름
    public List<String> fileContent; // 파일 내용

    public SolvedCodeFiles() {
    }

    public SolvedCodeFiles(List<String> filePath, List<String> fileContent) {
        this.filePath = filePath;
        this.fileContent = fileContent;
    }
}
