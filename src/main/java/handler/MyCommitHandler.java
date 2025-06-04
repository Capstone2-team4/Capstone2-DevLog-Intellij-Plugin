package handler;

import actions.MyBookmarkStorage;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import data.MyBookMark;
import data.UserStorage;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MyCommitHandler extends CheckinHandler {
    private final Project project;

    public MyCommitHandler(Project project) {
        this.project = project;
    }

    @Override
    public void checkinSuccessful() {
        JOptionPane.showMessageDialog(null, "커밋 완료됨!"); // 팝업으로 확인
        String commitId = getLatestCommitId(project);
        if (commitId != null) {
            sendCommitEvent(commitId);
        }
    }

    private void sendCommitEvent(String commitId) {
        try {
            String token = UserStorage.getAccessToken();
            URL url = new URL("http://localhost:8080/codes/blocks/commit");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);

            JsonObject payload = new JsonObject();
            payload.addProperty("commitId", commitId);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            System.out.println("Commit event sent with ID: " + commitId);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



//    private void sendCommitEvent() {
//        try {
//            String token = UserStorage.getAccessToken();
//
//            URL url = new URL("http://localhost:8080/codes/blocks/commit"); // 실제 API 주소
//            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("POST");
//            conn.setRequestProperty("Content-Type", "application/json");
//            conn.setRequestProperty("Authorization", "Bearer " + token);
//            conn.setDoOutput(true);
//
//            String payload = "{\"message\": \"commit success\"}"; // 원하는 데이터
//            try (OutputStream os = conn.getOutputStream()) {
//                os.write(payload.getBytes(StandardCharsets.UTF_8));
//            }
//
//            int code = conn.getResponseCode();
//            System.out.println("Commit event sent. Response code: " + code);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private String getLatestCommitId(Project project) {
        try {
            String projectPath = project.getBasePath(); // 루트 디렉토리
            if (projectPath == null) return null;

            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String sha = reader.readLine(); // 커밋 SHA
                System.out.println("Latest Commit SHA: " + sha);
                return sha;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}