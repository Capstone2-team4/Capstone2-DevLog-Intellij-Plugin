package ErrorLog;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import data.AllFiles;
import data.ErrorLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@State(
        name = "AllFilesStorage",
        storages = @Storage("AllFilesStorage.xml")
)
public class AllFilesStorage implements PersistentStateComponent<AllFilesStorage> {
    public List<AllFiles> allFiles = new ArrayList<>();

    @Nullable
    @Override
    public AllFilesStorage getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull AllFilesStorage state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static AllFilesStorage getInstance(Project project) {
        return ServiceManager.getService(project, AllFilesStorage.class);
    }

    public void addAllFiles(AllFiles files) {
        allFiles.add(files);
    }

    public List<AllFiles> getAllFiles() {
        return allFiles;
    }

    public void deleteAllFilesStorageFile(Project project) {
        String path = project.getBasePath() + "/.idea/AllFilesStorage.xml";
        File file = new File(path);
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("✅ AllFilesStorage.xml 삭제 완료");
            }
        }
    }

    public void clearAll() {
        allFiles.clear();
    }
}
