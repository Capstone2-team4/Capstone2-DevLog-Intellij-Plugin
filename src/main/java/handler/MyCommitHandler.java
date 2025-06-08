package handler;

import ErrorLog.ErrorLogStorage;
import ErrorLog.AllFilesStorage;
import ErrorLog.SolvedCodeFilesStorage;
import actions.MyBookmarkStorage;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.util.io.HttpRequests;
import data.ErrorLog;
import data.MyBookMark;
import data.SolvedCodeFiles;
import data.UserStorage;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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
            saveErrorInfo(commitId); // 커밋 성공시, 에러 정보 저장
        }
        MyBookmarkStorage storage = MyBookmarkStorage.getInstance(project);
        storage.clearAll(); // 커밋 성공시, 스토리지 비우기
    }

    private void saveErrorInfo(String commitId) { // commit 이후에 에러, 에러코드 에러 해결코드 저장
        System.out.println("*()*()*()*()*()*()*()*()*()*()*()*()*()*()*()*()");
        ErrorLogStorage errorLogStorage = ErrorLogStorage.getInstance(project);
        SolvedCodeFilesStorage solvedCodeFilesStorage = SolvedCodeFilesStorage.getInstance(project);

        if (errorLogStorage == null || solvedCodeFilesStorage == null) {
            System.out.println("❗ 저장소를 가져올 수 없습니다.");
            return;
        }

        List<ErrorLog> errorLogs = errorLogStorage.getErrorLogs();
        List<SolvedCodeFiles> solvedFilesList = solvedCodeFilesStorage.getSolvedCodeFilesList();

        for (ErrorLog errorLog : errorLogs) {
            System.out.println("-----------------------------------------------");
            JSONObject requestBody = new JSONObject();
            requestBody.put("commitId", commitId);
            requestBody.put("title", errorLog.error);
            requestBody.put("content", errorLog.errorMessage);

            // 🔧 errorCode 배열 구성
            JSONArray errorCodeArray = new JSONArray();
            int len = Math.min(errorLog.errorFilePathList.size(),
                    Math.min(errorLog.errorLocationInFileList.size(), errorLog.errorFileList.size()));

            IntStream.range(0, len).forEach(i -> {
                JSONObject codeObj = new JSONObject();
                codeObj.put("filePath", errorLog.errorFilePathList.get(i));
                codeObj.put("errorLocation", Integer.parseInt(errorLog.errorLocationInFileList.get(i)));
                codeObj.put("code", errorLog.errorFileList.get(i));
                errorCodeArray.put(codeObj);
            });
            requestBody.put("errorCode", errorCodeArray);

            // 🔧 errorSolvedCode 배열 구성
            JSONArray solvedCodeArray = new JSONArray();
            for (SolvedCodeFiles solved : solvedFilesList) {
                int solvedLen = Math.min(solved.filePath.size(), solved.fileContent.size());
                IntStream.range(0, solvedLen).forEach(i -> {
                    JSONObject solvedObj = new JSONObject();
                    solvedObj.put("filePath", solved.filePath.get(i));
                    solvedObj.put("code", solved.fileContent.get(i));
                    solvedCodeArray.put(solvedObj);
                });
            }
            requestBody.put("errorSolvedCode", solvedCodeArray);

            // ✅ HTTP POST 요청 전송
            try {
                HttpRequests.post("http://localhost:8080/errors/save/errorInfo", "application/json")
                        .tuner(connection -> {
                            connection.setRequestProperty("Content-Type", "application/json");
                            connection.setRequestProperty("Authorization", "Bearer " + UserStorage.getAccessToken()); // ✅ 토큰 설정
                        })                        .connect(request -> {
                            request.write(requestBody.toString());
                            String response = request.readString();
                            System.out.println("✅ 서버 응답: " + response);
                            return null;
                        });
            } catch (Exception e) {
                System.err.println("❌ 전송 실패: " + e.getMessage());
            }
            // 커밋 성공시, 에러 관련 파일(ErrorLogStorage, AllFilesStorage, SolvedCodeFilesStorage) 비우기
            errorLogStorage.deleteErrorLogStorageFile(project);
            AllFilesStorage allFilesStorage = AllFilesStorage.getInstance(project);
            allFilesStorage.deleteAllFilesStorageFile(project);
            solvedCodeFilesStorage.deleteSolvedCodeFilesStorageFile(project);
            errorLogStorage.clearAll();
            allFilesStorage.clearAll();
            solvedCodeFilesStorage.clearAll();
        }
    }

    private void sendCommitEvent(String commitId) {
        try {
            // 1) 로컬에 저장된 Map 그대로 꺼내기
            MyBookmarkStorage storage = MyBookmarkStorage.getInstance(project);
            Map<String,MyBookMark> bookmarkMap = storage.bookmarks;  // Map<snippetId, MyBookMark>

            // 2) Gson을 이용해 commitId와 Map을 JSON으로 직렬화
            Gson gson = new Gson();
            JsonObject payload = new JsonObject();
            payload.addProperty("commitId", commitId);

            // Map<String, MyBookMark> → JsonObject 형태로 변환
            // 예: { "c2333983-...": {id:"c2333983-...", filePath:"...", ...}, "982f3c36-...": { ... }, … }
            JsonObject mapJson = new JsonObject();
            for (Map.Entry<String,MyBookMark> entry : bookmarkMap.entrySet()) {
                String key = entry.getKey();              // snippetId
                MyBookMark value = entry.getValue();      // MyBookMark 객체
                // MyBookMark 전체를 JSON으로 바꿔 넣기
                mapJson.add(key, gson.toJsonTree(value, MyBookMark.class));
            }
            payload.add("bookmarksMap", mapJson);

            // 3) HTTP POST 요청 보내기 (이전과 동일)
            String token = UserStorage.getAccessToken();
            URL url = new URL("http://localhost:8080/codes/blocks/commit");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setDoOutput(true);

            String body = payload.toString();
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK
                    || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                System.out.println("[MyCommitHandler] Commit event sent successfully: " + commitId);
            } else {
                System.err.println("[MyCommitHandler] Failed to send commit event. Response code: " + responseCode);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                }
            }

            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


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