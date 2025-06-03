package data;

public class ErrorLog {
    public String branch; // 저장해야할 ErrorLog의 branch 정보 (ErrorLog 저장시 commit과 매치하기 위해 필요)
    public String commit; // commit에 빈값이면 저장해야될 ErrorLog로 분류 / ErrorLog db에 저장시 commit hash값을 저장
    public String errorLog;

    // 🔧 기본 생성자 추가
    public ErrorLog() {
    }

    public ErrorLog(String branch, String commit, String errorLog) {
        this.branch = branch;
        this.commit = commit;
        this.errorLog = errorLog;
    }
}
