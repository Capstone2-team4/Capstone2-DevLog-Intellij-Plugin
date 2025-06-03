package ErrorLog;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MyExecutionListener implements ExecutionListener {

    private Project project;

    public MyExecutionListener(Project project) {
        this.project = project;
    }

    @Override
    public void processStarted(@NotNull String executorId,
                               @NotNull ExecutionEnvironment environment,
                               @NotNull ProcessHandler handler) {

        // Run 버튼 클릭으로 실행된 경우만 필터
        if (!executorId.equals(DefaultRunExecutor.EXECUTOR_ID)) return;

        // 로그 캡처 시작
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!실행 감지: " + environment.getRunProfile().getName());
        MyProcessLogger logger = new MyProcessLogger(project);
        logger.attachToProcess(handler);
    }
}
