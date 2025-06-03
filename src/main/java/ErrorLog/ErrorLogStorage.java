package ErrorLog;

import actions.MyBookmarkStorage;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import data.ErrorLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "ErrorLogStorage",
        storages = @Storage("ErrorLogStorage.xml")
)
public class ErrorLogStorage implements PersistentStateComponent<ErrorLogStorage> {
    public List<ErrorLog> errorLogs = new ArrayList<>();

    @Nullable
    @Override
    public ErrorLogStorage getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ErrorLogStorage state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static ErrorLogStorage getInstance(Project project) {
        return ServiceManager.getService(project, ErrorLogStorage.class);
    }

    public void addErrorLog(ErrorLog errorLog) {
        errorLogs.add(errorLog);
    }

    public List<ErrorLog> getErrorLogs() {
        return errorLogs;
    }
}