package data;

import java.util.List;

public class ErrorLog {
//    public String branch; // 저장해야할 ErrorLog의 branch 정보 (ErrorLog 저장시 commit과 매치하기 위해 필요)
    public String commit; // commit에 빈값이면 저장해야될 ErrorLog로 분류 / ErrorLog db에 저장시 commit hash값을 저장
    public String error;
    public String errorMessage; // ErrorLog의 메시지
    public List<String> errorFilePathList; // ErrorLog의 파일 경로
    public List<String> errorLocationInFileList;
    public List<String> errorFileList;

    // 🔧 기본 생성자 추가
    public ErrorLog() {
    }

    public ErrorLog(String commit, String error, String errorMessage, List<String> errorFilePathList, List<String> errorLocationInFile, List<String> errorFile) {
        this.commit = commit;
        this.error = error;
        this.errorMessage = errorMessage;
        this.errorFilePathList = errorFilePathList;
        this.errorLocationInFileList = errorLocationInFile;
        this.errorFileList = errorFile;
    }
}
