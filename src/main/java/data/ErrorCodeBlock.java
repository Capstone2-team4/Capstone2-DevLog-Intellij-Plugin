package data;

import java.util.List;

public class ErrorCodeBlock {
    private List<String> id;
    private List<String> title;
    private List<String> filePath;
    private List<Integer> startOffset; // 추가: 범위의 시작 오프셋
    private List<Integer> endOffset;
    private List<String> content;
    private List<String> code;
    private List<String> category;
    private List<String> status;


    // 기본 생성자
    public ErrorCodeBlock() {
    }

    // 전체 필드를 받는 생성자
    public ErrorCodeBlock(
            List<String> id,
            List<String> title,
            List<String> filePath,
            List<Integer> startOffset,
            List<Integer> endOffset,
            List<String> content,
            List<String> code,
            List<String> category,
            List<String> status
    ) {
        this.id = id;
        this.title = title;
        this.filePath = filePath;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.content = content;
        this.code = code;
        this.category = category;
        this.status = status;
    }

    // Getters
    public List<String> getId() {
        return id;
    }

    public List<String> getTitle() {
        return title;
    }

    public List<String> getFilePath() {
        return filePath;
    }

    public List<Integer> getStartOffset() {
        return startOffset;
    }

    public List<Integer> getEndOffset() {
        return endOffset;
    }

    public List<String> getContent() {
        return content;
    }

    public List<String> getCode() {
        return code;
    }

    public List<String> getCategory() {
        return category;
    }

    public List<String> getStatus() {
        return status;
    }

    // Setters
    public void setId(List<String> id) {
        this.id = id;
    }

    public void setTitle(List<String> title) {
        this.title = title;
    }

    public void setFilePath(List<String> filePath) {
        this.filePath = filePath;
    }

    public void setStartOffset(List<Integer> startOffset) {
        this.startOffset = startOffset;
    }

    public void setEndOffset(List<Integer> endOffset) {
        this.endOffset = endOffset;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public void setCode(List<String> code) {
        this.code = code;
    }

    public void setCategory(List<String> category) {
        this.category = category;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }


}


