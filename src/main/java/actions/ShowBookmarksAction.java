package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import data.MyBookMark;

import java.util.List;

public class ShowBookmarksAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        MyBookmarkStorage storage = MyBookmarkStorage.getInstance(project);
        List<MyBookMark> bookmarks = storage.getBookmarks();

        if (bookmarks.isEmpty()) {
            Messages.showInfoMessage("저장된 북마크가 없습니다.", "북마크 목록");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (MyBookMark bm : bookmarks) {
            sb.append("📄 파일: ").append(bm.filePath).append("\n")
//                    .append("📍 줄 번호: ").append(bm.line + 1).append("\n")
                    .append("🎉 카테고리: ").append(bm.category).append("\n")
                    .append("🌹 제목: ").append(bm.title).append("\n")
                    .append("🔖 내용: ").append(bm.content).append("\n")
                    .append("💡 코드: \n").append(trimIfLong(bm.code)).append("\n\n");
        }

        Messages.showMessageDialog(sb.toString(), "저장된 북마크 목록", Messages.getInformationIcon());
    }

    private String trimIfLong(String code) {
        return code.length() > 100 ? code.substring(0, 100) + "...(생략)" : code;
    }
}
