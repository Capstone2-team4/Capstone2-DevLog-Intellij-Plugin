package fetch;

import com.intellij.openapi.project.Project;
import data.UserStorage;

import java.net.HttpURLConnection;
import java.net.URL;

public class SendSnippetDeleteService {
    private static final String BASE_URL = "http://localhost:8080/codes"; // 실제 API URL로 바꿀 것

    public static boolean sendDeleteSnippet(Project project,
                                           String snippetId
    ) {
        try {
            // PUT /codes/block/{id}/status
            URL url = new URL(BASE_URL + "/redis/blocks/" + snippetId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String token = UserStorage.getAccessToken();

            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return (responseCode == HttpURLConnection.HTTP_OK
                    || responseCode == HttpURLConnection.HTTP_NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
