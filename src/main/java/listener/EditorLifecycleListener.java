package listener;


import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import data.MyBookMark;
import data.SnippetMarkerService;
import data.UserStorage;
import fetch.MySnippetRestClient;
import fetch.SendSnippetUpdateService;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 선언적 등록용 EditorFactoryListener 구현.
 * - 에디터가 생성될 때마다 해당 문서에 DocumentListener를 한 번만 붙인다.
 */
public final class EditorLifecycleListener implements EditorFactoryListener {
    // 디바운스 타이머
    private final ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        Project project = event.getEditor().getProject();
        if (project == null) {
            return;
        }

        // (1) 프로젝트당 한 번만 복원 로직 실행
        SnippetMarkerService markerService = SnippetMarkerService.getInstance(project);
        if (!markerService.isRestored()) {
            markerService.markRestored();
            // 백그라운드 스레드에서 복원 작업 수행
            scheduler.execute(() -> tryRestoreMarkers(project));
            // 3) 로그인 이벤트 구독: 만약 로그인 성공 후 복원 필요시 재시도
            subscribeLogin(project);
        }


        // (추가) 콘솔에 찍어 보기
        System.out.println("[EditorLifecycleListener] editorCreated: " + event.getEditor().getDocument());

        Document document = event.getEditor().getDocument();
        document.addDocumentListener(new DocumentListener() {
            private Runnable pendingUpdate;

            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                System.out.println("[EditorLifecycleListener] documentChanged in file: " +
                        event.getDocument() + " at offset " + event.getOffset());

                if (pendingUpdate != null) {
                    pendingUpdate = null; // 이전 예약 취소 (별도 Future.cancel 구현 가능)
                }
                // 300ms 뒤에 한 번만 실행
                pendingUpdate = () -> onDocumentChanged(project, document);
                scheduler.schedule(pendingUpdate, 300, TimeUnit.MILLISECONDS);
            }
        }, project);
    }

    /** 서버에서 스니펫 메타데이터를 가져와 RangeMarker를 복원 */
//    private void restoreMarkers(@NotNull Project project) {
//        // 서버에서 전체 스니펫 목록을 가져옴
//        List<MyBookMark> serverSnippets = MySnippetRestClient.fetchAllSnippets(project);
//        SnippetMarkerService markerService = SnippetMarkerService.getInstance(project);
//
//        for (MyBookMark sn : serverSnippets) {
//            String path = sn.filePath;
//            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(path);
//            if (vf == null) {
//                continue; // 파일이 존재하지 않으면 건너뜀
//            }
//            Document document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance()
//                    .getDocument(vf);
//            if (document == null) {
//                continue;
//            }
//            // 1) RangeMarker 생성 및 등록
//            markerService.addMarker(sn.id, document, sn.startOffset, sn.endOffset);
//
//            // 2) 하이라이트 붙이기 (RangeHighlighter)
//            addHighlightForRange(project, vf, document, sn.startOffset, sn.endOffset);
//        }
//    }

    private void tryRestoreMarkers(@NotNull Project project) {
        String token = UserStorage.getAccessToken();
        System.out.println("token = " + token);
        if (token == null || token.isBlank()) {
            // 아직 로그인 안 됐으면 복원을 보류
            System.out.println("[EditorLifecycleListener] 토큰 없음, 스니펫 복원 보류");
            return;
        }

        // 토큰이 있으면 서버 호출해서 스니펫 복원
        scheduler.execute(() -> {
            List<MyBookMark> serverSnippets = MySnippetRestClient.fetchAllSnippets(project);
            SnippetMarkerService markerService = SnippetMarkerService.getInstance(project);

            for (MyBookMark sn : serverSnippets) {
                VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(sn.filePath);
                if (vf == null) continue;
                Document document = com.intellij.openapi.fileEditor.FileDocumentManager
                        .getInstance()
                        .getDocument(vf);
                if (document == null) continue;
                markerService.addMarker(sn.id, document, sn.startOffset, sn.endOffset);
                addHighlightForRange(project, vf, document,
                        sn.startOffset, sn.endOffset);
            }
            System.out.println("[EditorLifecycleListener] 서버에서 스니펫 복원 완료: 총 " +
                    serverSnippets.size() + "개");
        });
    }

    /**
     * 주어진 Document 상의 (startOffset, endOffset) 범위를 하이라이트.
     * - FileEditorManager를 통해 현재 열려 있는 에디터에서만 하이라이트 가능.
     * - 파일이 열려 있지 않으면, 나중에 열릴 때 붙도록 FileEditorManagerListener 등을 추가로 등록해야 함.
     */
    private void addHighlightForRange(@NotNull Project project,
                                      @NotNull VirtualFile vf,
                                      @NotNull Document document,
                                      int startOffset,
                                      int endOffset) {
        // FileEditorManager를 통해 해당 파일이 열려 있는 모든 에디터를 가져옴
        FileEditor[] editors = FileEditorManager.getInstance(project).getAllEditors(vf);
        for (FileEditor fe : editors) {
            // 실제 편집 가능한 에디터(Editor)를 얻으려면 다음 변환이 필요
            if (!(fe instanceof TextEditor textEditor)) {
                continue;
            }
            var editor = textEditor.getEditor();
            MarkupModel markupModel = editor.getMarkupModel();

            // TextAttributesKey 또는 직접 TextAttributes 생성
            TextAttributes attributes = new TextAttributes();
            // 배경색을 조금 연한 노란색으로 설정 (필요에 따라 색상 조정)
            attributes.setBackgroundColor(new Color(0xE8DF7D));

            // RangeHighlighter 타입: 영역 전체 배경 채우기 위한 LAYER
            int layer = HighlighterLayer.SELECTION - 1; // Selection 바로 아래 레이어
            markupModel.addRangeHighlighter(
                    startOffset,
                    endOffset,
                    layer,
                    attributes,
                    HighlighterTargetArea.EXACT_RANGE
            );
        }
    }

    private void onDocumentChanged(@NotNull Project project, @NotNull Document document) {
        // 해당 Document가 .log 파일인지 확인
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null || file.getName().endsWith(".log")) {
            return; // .log 파일이면 무시
        }

        SnippetMarkerService markerService = SnippetMarkerService.getInstance(project);
        Map<String, RangeMarker> allMarkers = markerService.getAllMarkers();

        for (Map.Entry<String, RangeMarker> entry : allMarkers.entrySet()) {
            String snippetId = entry.getKey();
            RangeMarker marker = entry.getValue();
            if (!marker.isValid()) {
                // → marker가 유효하지 않으면, 서버에 status="deleted"로 갱신
                boolean statusUpdated = SendSnippetUpdateService.sendUpdateStatus(
                        project, snippetId
                );
                if (!statusUpdated) {
                    System.err.println("[onDocumentChanged] Failed to mark snippet deleted: " + snippetId);
                }

                markerService.removeMarker(snippetId);
                continue;
            }

            int newStartOffset = marker.getStartOffset();
            int newEndOffset   = marker.getEndOffset();
            String newSnippetText = document.getText().substring(newStartOffset, newEndOffset);

            System.out.println("newStartOffset = " + newStartOffset);
            System.out.println("newEndOffset = " + newEndOffset);

            // 2) “startOffset == endOffset”이면서 텍스트가 빈 문자열인 경우도 삭제로 간주
            if (newStartOffset == newEndOffset && newSnippetText.isEmpty()) {
                boolean statusUpdated = SendSnippetUpdateService.sendUpdateStatus(
                        project, snippetId
                );
                if (!statusUpdated) {
                    System.err.println("[onDocumentChanged] Failed to mark snippet deleted: " + snippetId);
                }
                markerService.removeMarker(snippetId);
                continue;
            }

            // 서버로 업데이트 요청
            boolean success = SendSnippetUpdateService.sendUpdate(
                    project, snippetId, newStartOffset, newEndOffset, newSnippetText
            );
            if (!success) {
                System.err.println("[onDocumentChanged] Failed to update snippet " + snippetId);
            }
        }
    }

    private String computeTextHash(String text) {
        return org.apache.commons.codec.digest.DigestUtils.sha1Hex(text);
    }

    private void subscribeLogin(@NotNull Project project) {
        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        connection.subscribe(LoginListener.TOPIC, (LoginListener) () -> {
            // 로그인 성공 시점에 다시 복원 시도
            System.out.println("[EditorLifecycleListener] 로그인 성공, 스니펫 복원 재시도");
            tryRestoreMarkers(project);

            // 더 이상 이 리스너가 필요 없으면 연결 해제
            connection.disconnect();
        });
    }
}