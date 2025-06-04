package fetch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import data.MyBookMark;
import data.SnippetMarkerService;
import data.UserStorage;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 사용자가 새 스니펫을 생성했을 때 호출.
 * 서버(또는 Redis) 쪽으로 “생성 요청”을 보낸다.
 */
public class SendSnippetCreateService {
    private static final String CREATE_URL = "http://localhost:8080/codes/block"; // 실제 엔드포인트로 수정

    public static boolean sendCreate(Project project, MyBookMark bookmark) {
        String token = UserStorage.getAccessToken();
        try {
            Gson gson = new Gson();
            JsonObject json = new JsonObject();
            json.addProperty("id", bookmark.id);
            json.addProperty("filePath", bookmark.filePath);
            json.addProperty("startOffset", bookmark.startOffset);
            json.addProperty("endOffset", bookmark.endOffset);
            json.addProperty("title", bookmark.title);
            json.addProperty("content", bookmark.content);
            json.addProperty("category", bookmark.category);
            json.addProperty("code", bookmark.code);

            String payload = gson.toJson(json);
            URL url = new URL(CREATE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    JsonObject resp = new Gson().fromJson(reader, JsonObject.class);
                    boolean isSuccess = resp.has("isSuccess") && resp.get("isSuccess").getAsBoolean();
                    if (!isSuccess) {
                        String message = resp.has("message") ? resp.get("message").getAsString() : "Unknown error";
                        Messages.showMessageDialog(project, message, "서버 오류", Messages.getErrorIcon());
                        return false;
                    }
                    restoreSingleSnippet(project, bookmark);
                }
                conn.disconnect();
                return true;
            } else {
                Messages.showMessageDialog(project,
                        "서버 응답 코드: " + responseCode,
                        "요청 실패",
                        Messages.getErrorIcon());
            }
            conn.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
            Messages.showMessageDialog(project,
                    "예외 발생: " + ex.getMessage(),
                    "오류",
                    Messages.getErrorIcon());
        }
        return false;
    }

    public static void restoreSingleSnippet(@NotNull Project project, @NotNull MyBookMark sn) {
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(sn.filePath);
        if (vf == null) return;

        Document document = FileDocumentManager.getInstance().getDocument(vf);
        if (document == null) return;

        SnippetMarkerService markerService = SnippetMarkerService.getInstance(project);
        markerService.addMarker(sn.id, document, sn.startOffset, sn.endOffset);
        addHighlightForRange(project, vf, document, sn.startOffset, sn.endOffset);
    }

    private static void addHighlightForRange(@NotNull Project project,
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
            attributes.setBackgroundColor(new Color(0xE2DC9F));

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


}
