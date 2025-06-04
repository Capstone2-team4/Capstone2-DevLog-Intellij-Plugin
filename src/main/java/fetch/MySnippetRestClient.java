package fetch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.project.Project;
import data.MyBookMark;
import data.UserStorage;
import groovyjarjarantlr.Token;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class MySnippetRestClient {
    private static final String BASE_URL = "http://localhost:8080/codes/block";

    /**
     * 프로젝트가 열릴 때 호출해서 서버에서 저장된 스니펫 전체 목록을 가져옵니다.
     * 반환 JSON 예시:
     * {
     *   "isSuccess": true,
     *   "code": "COMMON200",
     *   "message": "성공입니다.",
     *   "result": [ { ... }, { ... }, ... ]
     * }
     *
     * @param project 현재 Project (필요시 인증 토큰 참조 등)
     * @return 서버에 저장된 모든 MyBookMark 객체 리스트 (빈 리스트일 수 있음)
     */
    public static List<MyBookMark> fetchAllSnippets(Project project) {
        String token = UserStorage.getAccessToken();
        try {
//            String projectName = project.getName();
            URL url = new URL(BASE_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                Gson gson = new Gson();
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                reader.close();
                conn.disconnect();

                boolean isSuccess = root.has("isSuccess") && root.get("isSuccess").getAsBoolean();
                if (!isSuccess) {
                    // 예: "message"가 실패 이유를 담고 있을 수 있음
                    // String msg = root.has("message") ? root.get("message").getAsString() : "Unknown error";
                    return Collections.emptyList();
                }

                // "result" 필드가 JSON 배열이라면, 그 안을 MyBookMark 리스트로 파싱
                if (root.has("result") && root.get("result").isJsonArray()) {
                    Type listType = new TypeToken<List<MyBookMark>>() {}.getType();
                    return gson.fromJson(root.get("result"), listType);
                } else {
                    return Collections.emptyList();
                }
            } else {
                conn.disconnect();
                return Collections.emptyList();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }
}