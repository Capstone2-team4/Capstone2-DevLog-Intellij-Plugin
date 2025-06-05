package actions;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import com.intellij.util.xmlb.annotations.XMap;
import data.MyBookMark;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@State(
        name = "MyBookmarkStorage",
        storages = {@Storage("MyBookmarkStorage.xml")}
)
public class MyBookmarkStorage implements PersistentStateComponent<MyBookmarkStorage> {
    /**
     * Map 형태로 snippetId → MyBookMark를 저장합니다.
     *
     * @XMap 어노테이션을 사용하면 XML 직렬화 시
     * <bookmarks>
     *   <entry key="snippet-uuid-1">
     *     <filePath>…</filePath>
     *     <startOffset>…</startOffset>
     *     …
     *   </entry>
     *   <entry key="snippet-uuid-2">…</entry>
     * </bookmarks>
     * 의 형태로 저장/읽어오게 됩니다.
     */
    @XMap(keyAttributeName = "id", entryTagName = "entry")
    public Map<String, MyBookMark> bookmarks = new HashMap<>();

    public static MyBookmarkStorage getInstance(@NotNull com.intellij.openapi.project.Project project) {
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

    public void addBookmark(@NotNull MyBookMark bookmark) {
        if (bookmark.id == null) {
            return;
        }
        bookmarks.put(bookmark.id, bookmark);
    }

    public void removeBookmark(@NotNull String snippetId) {
        bookmarks.remove(snippetId);
    }

    public List<MyBookMark> getAllBookmarks() {
        return new ArrayList<>(bookmarks.values());
    }

    public void clearAll() {
        bookmarks.clear();
    }
}
