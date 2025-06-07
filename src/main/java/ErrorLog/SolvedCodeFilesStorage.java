package ErrorLog;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import data.AllFiles;
import data.SolvedCodeFiles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "SolvedCodeFilesStorage",
        storages = @Storage("SolvedCodeFilesStorage.xml")
)
public class SolvedCodeFilesStorage implements PersistentStateComponent<SolvedCodeFilesStorage> {
    public List<SolvedCodeFiles> solvedCodeFilesList = new ArrayList<>();

    @Nullable
    @Override
    public SolvedCodeFilesStorage getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull SolvedCodeFilesStorage state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static SolvedCodeFilesStorage getInstance(Project project) {
        return ServiceManager.getService(project, SolvedCodeFilesStorage.class);
    }

    public void addSolvedCodeFiles(SolvedCodeFiles solvedCodeFiles) {
        solvedCodeFilesList.add(solvedCodeFiles);
    }

    public List<SolvedCodeFiles> getSolvedCodeFilesList() {
        return solvedCodeFilesList;
    }
}