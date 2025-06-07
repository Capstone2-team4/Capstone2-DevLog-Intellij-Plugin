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
        // Map 기반이므로 getAllBookmarks()로 값을 리스트로 받음
        List<MyBookMark> bookmarks = storage.getAllBookmarks();

        if (bookmarks.isEmpty()) {
            Messages.showInfoMessage("저장된 북마크가 없습니다.", "북마크 목록");
            return;
        }

        // 1) 현재 저장된 북마크를 출력
        StringBuilder sb = new StringBuilder();
        for (MyBookMark bm : bookmarks) {
            sb.append("🆔 ID: ").append(bm.id).append("\n")
                    .append("📄 파일: ").append(bm.filePath).append("\n")
                    .append("\n")
                    .append("📌 시작 오프셋: ").append(bm.startOffset).append(", 끝 오프셋: ").append(bm.endOffset).append("\n")
                    .append("\n")
                    .append("🎉 카테고리: ").append(bm.category).append("\n")
                    .append("\n")
                    .append("🌹 제목: ").append(bm.title).append("\n")
                    .append("\n")
                    .append("🔖 내용: ").append(bm.content).append("\n")
                    .append("\n")
                    .append("💡 코드: \n").append(bm.code).append("\n")
                    .append("\n")
                    .append("🏷️ 상태: ").append(bm.status).append("\n")
                    .append("──────────────────────────\n");
        }

        // 2) 대화상자에 두 가지 버튼을 띄웁니다:
        //    [OK] = 닫기,  [모두 삭제] = clearAll() 실행 후 알림
        int choice = Messages.showYesNoCancelDialog(
                sb.toString(),
                "저장된 북마크 목록",
                "모두 삭제",  // YES 버튼
                "취소",      // NO 버튼
                "닫기",      // CANCEL 버튼
                Messages.getQuestionIcon()
        );

        if (choice == Messages.YES) {
            // “모두 삭제” 선택 시
            storage.clearAll();
            Messages.showInfoMessage("모든 북마크를 삭제했습니다.", "삭제 완료");
        }
        // NO나 CANCEL을 누르면 아무 동작 안 함하고 그냥 대화상자 닫힘
    }

    private String trimIfLong(String code) {
        return code.length() > 100 ? code.substring(0, 100) + "...(생략)" : code;
    }
}
