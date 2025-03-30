package actions;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import data.MyBookMark;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(
        name = "MyBookmarkStorage",
        storages = {@Storage("MyBookmarkStorage.xml")}
)
public class MyBookmarkStorage implements PersistentStateComponent<MyBookmarkStorage> {
    public List<MyBookMark> bookmarks = new ArrayList<>();

    public static MyBookmarkStorage getInstance(Project project) {
        return ServiceManager.getService(project, MyBookmarkStorage.class);
    }

    @Nullable
    @Override
    public MyBookmarkStorage getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull MyBookmarkStorage state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public void addBookmark(MyBookMark bookmark) {
        bookmarks.add(bookmark);
    }

    public List<MyBookMark> getBookmarks() {
        return bookmarks;
    }

    public boolean isBookmarked(String filePath, int line) {
        return bookmarks.stream()
                .anyMatch(b -> b.filePath.equals(filePath) && b.line == line);
    }

}
