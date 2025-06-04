package fetch;

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import data.UserStorage;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 스니펫이 이동되거나 내용이 바뀌었을 때, 서버로 PATCH/PUT 요청을 보내는 유틸 클래스 예시
 */
public class SendSnippetUpdateService {
    private static final String BASE_URL = "http://localhost:8080/codes"; // 실제 API URL로 바꿀 것

    /**
     * 서버에 "이 스니펫(id) 의 startOffset, endOffset, code 를 업데이트 해 달라"고 요청합니다.
     *
     * @param project      현재 Project (필요시 토큰 조회)
     * @param snippetId    수정할 스니펫 ID(UUID)
     * @param newStartOffset 이동된 블록의 새 시작 오프셋
     * @param newEndOffset   이동된 블록의 새 끝 오프셋
     * @param newCode        변경된 코드 문자열
     * @return 성공 여부
     */
    public static boolean sendUpdate(Project project,
                                     String snippetId,
                                     int newStartOffset,
                                     int newEndOffset,
                                     String newCode) {
        try {
            // 예시: PUT /codes/{id}
            URL url = new URL(BASE_URL + "/block/" + snippetId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String token = UserStorage.getAccessToken();

            conn.setRequestMethod("PUT"); // 또는 PUT, 서버 API 규격에 맞출 것
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            // JSON 페이로드 생성
            JsonObject payload = new JsonObject();
            payload.addProperty("startOffset", newStartOffset);
            payload.addProperty("endOffset", newEndOffset);
            payload.addProperty("code", newCode);

            String body = payload.toString();
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}