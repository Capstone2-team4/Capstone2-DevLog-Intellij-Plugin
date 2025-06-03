package ErrorLog;


import com.intellij.execution.ExecutionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

public class MyStartupActivity implements StartupActivity {

    @Override
    public void runActivity(@NotNull Project project) {
        System.out.println("<<<<<<<<<<<<<<<StartupActivity 실행됨>>>>>>>>>>>>>>>>>: " + project.getName());
        MessageBusConnection connection = project.getMessageBus().connect();
        connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new MyExecutionListener(project));
    }
}
