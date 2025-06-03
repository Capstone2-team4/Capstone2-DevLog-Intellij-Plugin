package ErrorLog;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import data.ErrorLog;

public class MyProcessLogger {

    private Project project;
    private final StringBuilder logBuilder = new StringBuilder();
    private final StringBuilder errorLogBuilder = new StringBuilder();

    public MyProcessLogger(Project project) {
        this.project = project;
    }

    public void attachToProcess(ProcessHandler processHandler) {
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                String text = event.getText();

                // 로그 구분 (stdout, stderr)
                if (outputType == ProcessOutputTypes.STDOUT) {
                    logBuilder.append("[OUT] ").append(text);
                } else if (outputType == ProcessOutputTypes.STDERR) {
                    errorLogBuilder.append("[ERR] ").append(text);
                } else {
                    logBuilder.append("[OTHER] ").append(text);
                }

                // 필요시 실시간 처리도 가능
                System.out.print("[LOG] " + text); // 콘솔 출력
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                System.out.println("Process finished with exit code: " + event.getExitCode());
                System.out.println("Captured log: \n" + logBuilder.toString());
                System.out.println();
                System.out.println("Captured errorLog: \n" + errorLogBuilder.toString());

                // ErrorLogStorage에 ErrorLog 저장 (= ErrorLogStorage.xml 파일에 에러 로그 저장)
                ErrorLog errorLog = new ErrorLog("main", "", errorLogBuilder.toString());
                ErrorLogStorage errorLogStorage = ErrorLogStorage.getInstance(project);
                errorLogStorage.addErrorLog(errorLog);
            }
        });
    }

    public String getCapturedLog() {
        return logBuilder.toString();
    }
}
