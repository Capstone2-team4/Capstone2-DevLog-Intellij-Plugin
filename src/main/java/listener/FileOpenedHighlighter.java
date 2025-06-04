package listener;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import data.SnippetMarkerService;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 파일 탭이 열릴 때마다 해당 파일에 등록된 스니펫 영역을 하이라이트합니다.
 */
public class FileOpenedHighlighter implements FileEditorManagerListener {
    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        Project project = source.getProject();
        // SnippetMarkerService에 등록된 모든 마커 중, 현재 연 파일(vf)과 일치하는 것만 필터링
        SnippetMarkerService markerService = SnippetMarkerService.getInstance(project);
        Map<String, com.intellij.openapi.editor.RangeMarker> allMarkers = markerService.getAllMarkers();

        for (Map.Entry<String, com.intellij.openapi.editor.RangeMarker> entry : allMarkers.entrySet()) {
            com.intellij.openapi.editor.RangeMarker marker = entry.getValue();
            // marker가 붙어 있는 Document가 현재 연 파일의 Document여야 하이라이트
            Document document = marker.getDocument();
            VirtualFile markerFile = FileDocumentManager.getInstance().getFile(document);
            if (markerFile == null || !markerFile.equals(file)) {
                continue;
            }

            // TextEditor 인스턴스로부터 실제 Editor 객체 얻기
            FileEditor[] editors = FileEditorManager.getInstance(project).getAllEditors(file);
            for (FileEditor fe : editors) {
                if (!(fe instanceof TextEditor textEditor)) {
                    continue;
                }
                var editor = textEditor.getEditor();
                MarkupModel markupModel = editor.getMarkupModel();

                // 이미 같은 위치에 하이라이트가 있다면 중복 방지(선택)
                RangeHighlighter[] existing = markupModel.getAllHighlighters();
                boolean already = false;
                for (RangeHighlighter rh : existing) {
                    if (rh.getStartOffset() == marker.getStartOffset() &&
                            rh.getEndOffset() == marker.getEndOffset()) {
                        already = true;
                        break;
                    }
                }
                if (already) {
                    continue;
                }

                // TextAttributesKey 또는 직접 TextAttributes 생성
                com.intellij.openapi.editor.markup.TextAttributes attributes =
                        new com.intellij.openapi.editor.markup.TextAttributes();
                attributes.setBackgroundColor(new java.awt.Color(0xFFFBCC));

                int layer = com.intellij.openapi.editor.markup.HighlighterLayer.SELECTION - 1;
                markupModel.addRangeHighlighter(
                        marker.getStartOffset(),
                        marker.getEndOffset(),
                        layer,
                        attributes,
                        com.intellij.openapi.editor.markup.HighlighterTargetArea.EXACT_RANGE
                );
            }
        }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // 파일이 닫힐 때 하이라이트를 제거하고 싶다면 여기에 구현
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        // 선택된 파일이 바뀔 때 필요하다면 추가 로직
    }
}