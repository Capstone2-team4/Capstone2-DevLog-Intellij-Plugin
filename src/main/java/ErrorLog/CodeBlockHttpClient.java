package ErrorLog;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.project.Project;
import data.ErrorCodeBlock;
import data.MyBookMark;
import data.UserStorage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeBlockHttpClient {
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
    public static ErrorCodeBlock fetchAllCodeBlcok(Project project) {
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
                System.out.println("api 호출 성공--------------------");
                System.out.println("Response Code: " + responseCode);
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
                    return null;
                }

                // "result" 필드가 JSON 배열이라면, 그 안을 ErrorCodeBlock객체로 파싱
                if (root.has("result") && root.get("result").isJsonArray()) {
                    Type listType = new TypeToken<List<MyBookMark>>() {}.getType();
                    List<MyBookMark> codeBlockList = gson.fromJson(root.get("result"), listType);

                    // 출력 코드
                    for (MyBookMark bookmark : codeBlockList) {
                        System.out.println("📌 Bookmark -------------------");
                        System.out.println("ID         : " + bookmark.id);
                        System.out.println("Title      : " + bookmark.title);
                        System.out.println("File Path  : " + bookmark.filePath);
                        System.out.println("Start Offset: " + bookmark.startOffset);
                        System.out.println("End Offset : " + bookmark.endOffset);
                        System.out.println("Content    : " + bookmark.content);
                        System.out.println("Code       : " + bookmark.code);
                        System.out.println("Category   : " + bookmark.category);
                        System.out.println("Status     : " + bookmark.status);
                        System.out.println("----------------------------------\n");
                    }

                    // 각 필드별 리스트 초기화
                    List<String> idList = new ArrayList<>();
                    List<String> titleList = new ArrayList<>();
                    List<String> filePathList = new ArrayList<>();
                    List<Integer> startOffsetList = new ArrayList<>();
                    List<Integer> endOffsetList = new ArrayList<>();
                    List<String> contentList = new ArrayList<>();
                    List<String> codeList = new ArrayList<>();
                    List<String> categoryList = new ArrayList<>();
                    List<String> statusList = new ArrayList<>();

                    // DTO 리스트를 순회하며 각 필드별로 리스트 채움
                    for (MyBookMark codeBlcok : codeBlockList) {
                        idList.add(codeBlcok.id);
                        titleList.add(codeBlcok.title);
                        filePathList.add(codeBlcok.filePath);
                        startOffsetList.add(codeBlcok.startOffset);
                        endOffsetList.add(codeBlcok.endOffset);
                        contentList.add(codeBlcok.content);
                        codeList.add(codeBlcok.code);
                        categoryList.add(codeBlcok.category);
                        statusList.add(codeBlcok.status);
                    }

                    // ErrorCodeBlock 생성 후 리스트로 감싸서 반환
                    ErrorCodeBlock errorCodeBlock = new ErrorCodeBlock();
                    errorCodeBlock.setId(idList);
                    errorCodeBlock.setTitle(titleList);
                    errorCodeBlock.setFilePath(filePathList);
                    errorCodeBlock.setStartOffset(startOffsetList);
                    errorCodeBlock.setEndOffset(endOffsetList);
                    errorCodeBlock.setContent(contentList);
                    errorCodeBlock.setCode(codeList);
                    errorCodeBlock.setCategory(categoryList);
                    errorCodeBlock.setStatus(statusList);

                    return errorCodeBlock;
                } else {
                    return null;
                }
            } else {
                conn.disconnect();
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
