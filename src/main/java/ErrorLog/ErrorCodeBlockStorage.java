package ErrorLog;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import data.ErrorCodeBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@State(
        name = "ErrorCodeBlockStorage",
        storages = @Storage("ErrorCodeBlockStorage.xml")
)
public class ErrorCodeBlockStorage implements PersistentStateComponent<ErrorCodeBlockStorage> {
    public List<ErrorCodeBlock> errorCodeBlockList = new ArrayList<>();

    @Nullable
    @Override
    public ErrorCodeBlockStorage getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull ErrorCodeBlockStorage state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static ErrorCodeBlockStorage getInstance(Project project) {
        return ServiceManager.getService(project, ErrorCodeBlockStorage.class);
    }

    public void addErrorCodeBlock(ErrorCodeBlock errorCodeBlock) {
        errorCodeBlockList.add(errorCodeBlock);
    }

    public List<ErrorCodeBlock> getErrorCodeBlockList() {
        return errorCodeBlockList;
    }

    public void deleteErrorCodeBlockStorageFile(Project project) {
        String path = project.getBasePath() + "/.idea/ErrorCodeBlockStorage.xml";
        File file = new File(path);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("✅ ErrorCodeBlockStorage.xml 삭제 완료");
            }
        }
    }

    public void clearAll() {
        errorCodeBlockList.clear();
    }
}