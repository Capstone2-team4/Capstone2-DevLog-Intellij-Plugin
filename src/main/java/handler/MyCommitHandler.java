package handler;

import actions.MyBookmarkStorage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import data.MyBookMark;

import java.util.List;

public class MyCommitHandler extends CheckinHandler {
    private final Project project;

    public MyCommitHandler(Project project) {
        this.project = project;
    }

    @Override
    public void checkinSuccessful() {
        // 커밋 성공 후 북마크 API 전송
        List<MyBookMark> bookmarks = MyBookmarkStorage.getInstance(project).getBookmarks();
        sendBookmarksToServer(bookmarks);
    }

    private void sendBookmarksToServer(List<MyBookMark> bookmarks) {
        // 여기에 HTTP POST 로직 구현
        for (MyBookMark b : bookmarks) {
            System.out.println("북마크 전송: " + b.filePath + ":" + b.line);
        }
    }
}