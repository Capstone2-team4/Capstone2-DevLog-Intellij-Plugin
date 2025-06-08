package ErrorLog;

import actions.LoginForm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.io.HttpRequests;
import data.AllFiles;
import data.ErrorLog;
import data.SolvedCodeFiles;
import data.UserStorage;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class MyProcessLogger {

    private Project project;
    private final StringBuilder logBuilder = new StringBuilder();
    private final StringBuilder errorLogBuilder = new StringBuilder();
    private List<String> logList;
    private List<String> errorLogList;
    private boolean isErrorExist = false; // 에러가 해결되었는지 여부 -> 동작안함 고쳐야함

    public MyProcessLogger(Project project) {
        this.project = project;
    }

    public void attachToProcess(ProcessHandler processHandler) {
        logList = new ArrayList<>();
        errorLogList = new ArrayList<>();
        processHandler.addProcessListener(new ProcessAdapter() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {

                String text = event.getText();

                // 로그 구분 (stdout, stderr)
                if (outputType == ProcessOutputTypes.STDOUT) {
                    logBuilder.append("[OUT] ").append(text);
                    logList.add("[OUT] " + text.trim()); // 로그 리스트에 추가
                } else if (outputType == ProcessOutputTypes.STDERR) {
                    errorLogBuilder.append("[ERR] ").append(text);
                    errorLogList.add(text.trim()); // 에러 로그 리스트에 추가
                } else {
                    logBuilder.append("[OTHER] ").append(text);
                }

                // 필요시 실시간 처리도 가능
                System.out.print("[LOG] |" + outputType + "| " + text); // 콘솔 출력
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                // 에러발생시 알림, 알림에서 저장 및 해결 또는 무시 선택 가능 / 저장 및 해결 선택 시 에러 관련 정보 저장
                checkNormalTermination();
                if (!errorLogList.isEmpty()) { // 에러 없이 종료시켰을 때 알람 안뜨게 해결해야됨.
                    showErrorNoti(project, event);
                    for (String errorLog : errorLogList) { // 에러 로그 출력
                        System.out.println(errorLog);
                    }
                }
                System.out.println("-----------------isErrorExist: " + isErrorExist);
                ErrorLogStorage errorLogStorage = ErrorLogStorage.getInstance(project);
                if (!errorLogStorage.getErrorLogs().isEmpty()) { // 에러가 해결되고 종료되었을 때 = ErrorLogStorage.xml파일이 존재
                    showResolvedCheckNotification(project);
                }
            }
        });
    }

    private void checkNormalTermination() {
        boolean isNormalTermination = false; // 프로세스가 정상적으로 종료되었는지 여부
        for (String errorLog : errorLogList) { // 에러 로그 출력
            if (errorLog.contains("Exception: ")) return;
            if (errorLog.contains("Build cancelled")) {
                System.out.println("프로세스가 정상적으로 종료되었습니다.");
//                showResolvedCheckNotification(project); // ------------- 고쳐야함: ErrorLogStorage 존재 여부로 에러 저장했는지 안했는지 파악하기
                isNormalTermination = true; // 프로세스가 정상적으로 종료되지 않았음
                break;
            }
        }
        if (isNormalTermination) errorLogList.clear();
    }

    private void showErrorNoti(Project project, ProcessEvent event) {
        Notification notification = new Notification(
                "MyPluginGroup",  // 알림 그룹 ID (등록 필요)
                "에러 발생",
                "에러가 발생했습니다. 저장하시겠습니까?",
                NotificationType.WARNING
        );
        notification.addAction(new AnAction("저장 및 해결") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                // 에러 발생 시 에러 관련 정보들 저장
                saveErrorToFile(event);
                // 에러 해결과정의 코드 저장하기 위해서 모든 파일 저장해두기
                saveAllFiles(project);
                isErrorExist = true;
                notification.expire(); // 알림 닫기
            }
        });
        notification.addAction(new AnAction("무시") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                notification.expire(); // 아무것도 하지 않고 알림 닫기
            }
        });
        Notifications.Bus.notify(notification, project);
    }

    private void showResolvedCheckNotification(Project project) {
        Notification notification = new Notification(
                "MyPluginGroup",  // plugin.xml에 등록한 알림 그룹 ID
                "에러 해결 여부 확인",
                "에러를 해결하셨습니까?",
                NotificationType.INFORMATION
        );

        notification.addAction(new AnAction("해결 완료") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Notifications.Bus.notify(new Notification(
                        "MyPluginGroup",
                        "해결 완료!",
                        "에러가 성공적으로 해결되었습니다.",
                        NotificationType.INFORMATION
                ), project);
                isErrorExist = false; // 에러가 해결되었음을 표시
                // 에러 해결 코드 생성 후 파일에 저장
                createSolvedCodeAndSaveInFile(project);
                notification.expire(); // 이전 알림 닫기
            }
        });

        Notifications.Bus.notify(notification, project);
    }

    private void createSolvedCodeAndSaveInFile(Project project) {
        // 에러 해결 코드 생성 및 파일에 저장하는 로직
            // 저장할 에러해결된 코드의 파일경로와 코드내용의 각 리스트 생성
        List<String> solvedFilePathList = new ArrayList<>();
        List<String> solvedFileContentList = new ArrayList<>();
        AllFilesStorage storage = AllFilesStorage.getInstance(project);
        if (storage == null) {
            System.out.println("❗ AllFilesStorage 인스턴스를 가져올 수 없습니다.");
            return;
        }
        List<AllFiles> fileList = storage.getAllFiles();
        List<String> filePaths = fileList.get(fileList.size() - 1).filePath;
        List<String> contents = fileList.get(fileList.size() - 1).fileContent;
        for (int i = 0; i < filePaths.size(); i++) {
            String filePath = filePaths.get(i);
            String content = contents.get(i);
            String[] lines = content.split("\n");
//            System.out.println("파일 내용 ----------------: " + content);

            // diff 새 파일 작성 테스트
            String newSolvedCode = "";
            int justBeforeLineNum = 1; // 이전 변경 지점
            try {
                List<String> original = content.lines().toList();
                List<String> revised = Files.readAllLines(Paths.get(filePath));

                Patch<String> patch = DiffUtils.diff(original, revised);

                if(!patch.getDeltas().isEmpty()) {
                    System.out.println("파일 경로 ----------------: " + filePath);
                    for (AbstractDelta<String> delta : patch.getDeltas()) {
                        DeltaType type = delta.getType();
                        int origStart = delta.getSource().getPosition();  // 원본 줄 시작 번호 (0-based)
                        int revStart = delta.getTarget().getPosition();  // 수정본 줄 시작 번호 (0-based)

                        // newSolvedCode에 변경 이전 코드까지 복사해두기
                        for (int j = justBeforeLineNum; j <= revStart; j++) {
                            newSolvedCode += "  " + revised.get(j - 1) + "\n";
                        }
                        justBeforeLineNum = revStart + 1;

                        List<String> deletedLines = delta.getSource().getLines();
                        List<String> addedLines = delta.getTarget().getLines();

//                            System.out.println("🔺 변경 타입: " + type);

                        // 추가된 줄 출력 (수정본 기준)
                        if (!addedLines.isEmpty()) {
//                                System.out.println("➕ 추가된 줄 (수정본 기준):");
                            if (type == DeltaType.CHANGE) {
                                newSolvedCode += "수정된 코드\n";
                            }
                            for (int j = 0; j < addedLines.size(); j++) {
                                int revLineNum = revStart + 1 + j; // 1-based 보정
//                                    System.out.println("   " + revLineNum + ": " + addedLines.get(j));
                                if (type == DeltaType.INSERT) {
                                    newSolvedCode += "+ " + addedLines.get(j) + "\n";
                                }
                                else if (type == DeltaType.CHANGE) {
                                    newSolvedCode += "* " + addedLines.get(j) + "\n";
                                }
                            }
                            justBeforeLineNum += addedLines.size(); // 마지막으로 수정된 줄 번호 업데이트
                        }

                        // 삭제된 줄 출력 (원본 기준 + 수정본 위치 표시)
                        if (!deletedLines.isEmpty()) {
                            if (type == DeltaType.CHANGE) {
                                newSolvedCode += "수정전 코드\n";
                            }
//                                System.out.println("➖ 삭제된 줄 (원본 기준):");
                            for (int j = 0; j < deletedLines.size(); j++) {
                                int origLineNum = origStart + 1 + j; // 1-based 보정
                                int revLineNum = revStart + 1 + j; // 1-based 보정
//                                    System.out.println("   " + origLineNum + ": " + deletedLines.get(j));
                                if (type == DeltaType.DELETE) {
                                    newSolvedCode += "- " + deletedLines.get(j) + "\n"; // 삭제된 줄도 추가
                                }
                                else if (type == DeltaType.CHANGE) {
                                    newSolvedCode += "* " + deletedLines.get(j) + "\n"; // 변경된 줄도 추가
                                }
                            }
                        }

                        System.out.println();
                    }
                    // 변경된 코드 적용 후 나머지 코드 newSolvedCode에 추가
                    for (int j = justBeforeLineNum; j <= revised.size(); j++) {
                        newSolvedCode += "  " + revised.get(j - 1) + "\n";
                    }
                    System.out.println(newSolvedCode);
                    // "SolvedCodeFiles.xml" 파일에 저장할 에러해결코드의 파일경로와 코드내용 각 리스트에 저장
                    solvedFilePathList.add(filePath);
                    solvedFileContentList.add(newSolvedCode);
                }
            } catch (IOException e) {
                System.out.println("[오류] 파일 읽기 실패: " + e.getMessage());
            }
        }
        // 에러 해결된 코드 "SolvedCodeFiles.xml" 파일에 저장
        SolvedCodeFiles solvedCodeFiles = new SolvedCodeFiles(solvedFilePathList, solvedFileContentList);
        SolvedCodeFilesStorage solvedCodeFilesStorage = SolvedCodeFilesStorage.getInstance(project);
        solvedCodeFilesStorage.addSolvedCodeFiles(solvedCodeFiles);
    }

    private void saveErrorToFile(ProcessEvent event) { // 1. 에러 메세지 2. 에러 발생한 파일경로 리스트 3. 에러 발생한 파일에서 에러 위치 리스트 4. 에러 발생한 파일 내용 리스트 ErrorLogStorage.xml 파일에 저장
        String errorToStoreInFile = "";
        String errorMessageToStoreInFile = "";
        List<String> errorFilePathList = new ArrayList<>();
        List<String> errorLocationInFileList = new ArrayList<>();
        List<String> javaFileWhereErrorOccurredList = new ArrayList<>();
        int checkErrorOrExceptionOnce = 0; // 에러나 예외를 한번 처리하기 위한 변수
        System.out.println("Process finished with exit code: " + event.getExitCode());
        for (String log : logList) { // 모든 로그 출력
            System.out.println(log);
        }
        for (String errorLog : errorLogList) { // 에러 로그 출력
            System.out.println(errorLog);
        }
        // ErrorLogStorage에 ErrorLog 저장 (= ErrorLogStorage.xml 파일에 에러 로그 저장)
            // 에러난 코드 파일에 저장 (일단 하나의 에러만 저장 / 여러 에러 한번에 저장하는 기능 추가 예정)
            // 에러 로그있는지 확인하는 조건 넣어주어야 함 (중지시켰을때 에러가 없을 수도 있으니)
        if (errorLogList.get(0).contains("error")) { // error의 경우
            System.out.println("********************에러 로그 파싱********************");
            for (String log : errorLogList) {
                if (log.contains("error: ")) {
                    System.out.println("!!!!!!!!!: " + log);
                    // 첫번째 띄어쓰기 기준으로 파싱: 앞 - 에러가 발생한 파일 경로, 뒤 - 에러 메세지
                    int firstSpaceIndex = log.indexOf(' ');
                    String errorLogFirstWord = "";
                    String errorLogRest = "";
                    if (firstSpaceIndex != -1) {
                        errorLogFirstWord = log.substring(0, firstSpaceIndex);
                        errorLogRest = log.substring(firstSpaceIndex + 1);
                        // 첫 단어인 파일 경로 추출
                        errorFilePathList.add(errorLogFirstWord.trim().replaceAll(":\\d+:$", ""));
                        // 뒷 단어인 에러 메세지 추출
                        errorToStoreInFile = errorLogRest;
                        System.out.println("첫 단어(파일 경로): " + errorLogFirstWord);
                        System.out.println("나머지(에러 메세지): " + errorLogRest);
                    } else {
                        // 공백이 없을 경우 전체를 첫 단어로 간주
                        System.out.println("첫 단어: " + log);
                        System.out.println("나머지: 없음");
                        System.out.println("에러로그 파싱 실패");
                    }
//                    String[] parts = log.split("error: ");
                    // 에러 메세지 추출
//                    errorMessageToStoreInFile = parts[1];
                    // 에러가 발생한 파일 경로 추출 및 파일 가져오기
//                    errorFilePathList.add(parts[0].trim().replaceAll(":\\d+:$", ""));
                    String[] filePathParts = errorLogFirstWord.trim().split(":");
                    errorLocationInFileList.add(filePathParts[filePathParts.length - 1]);
                    javaFileWhereErrorOccurredList.add(readFileViaVirtualFile(errorFilePathList.get(errorFilePathList.size() - 1)));
                    System.out.println("********************에러 이름 ********************: " + errorToStoreInFile);
                    System.out.println("********************에러가 발생한 파일 경로 ********************: " + errorFilePathList.get(errorFilePathList.size() - 1));
                    System.out.println("********************에러가 발생한 파일에서 에러 위치 ********************: " + errorLocationInFileList.get(errorLocationInFileList.size() - 1));
                    System.out.println("********************에러가 발생한 파일 내용 ********************: " + javaFileWhereErrorOccurredList.get(javaFileWhereErrorOccurredList.size() - 1));
                    break; // 일단 하나의 에러 발생, 한개의 파일에서 에러 발생으로 가정
                }
            }
            // 에러 메세지와 에러가 발생한 파일을 ErrorLogStorage.xml파일에 저장
            ErrorLog errorLog = new ErrorLog("", errorToStoreInFile, "", errorFilePathList, errorLocationInFileList, javaFileWhereErrorOccurredList);
            ErrorLogStorage errorLogStorage = ErrorLogStorage.getInstance(project);
            errorLogStorage.addErrorLog(errorLog);
        } else if (errorLogList.get(0).contains("Exception")) { // exception의 경우
            System.out.println("********************예외 로그 파싱********************");
            for (String log : errorLogList) {
                if (log.contains("Exception:")) {
                    String[] parts = log.split(":");
                    // 예외 이름 추출
                    errorToStoreInFile = parts[0].trim();
                    // 예외 메세지 추출
                    errorMessageToStoreInFile = parts[1].trim();
                    continue;
                }
                if (log.contains("at ")) {
                    // 예외가 발생한 파일 경로 추출 및 파일 가져오기
                    int start = log.indexOf('(');
                    int end = log.indexOf(')', start);
                    String insideParentheses = "";
                    if (start != -1 && end != -1 && end > start) { // 괄호안에 파일명 추출 후 파일명과 줄수로 분리 (예:UserController.java:32)
                        insideParentheses = log.substring(start + 1, end);
                        String[] parts = insideParentheses.split(":");

                        if (parts.length == 2) {
                            String fileName = parts[0];  // "UserController.java"
                            String lineNumber = parts[1];  // "32"

                            System.out.println("파일명: " + fileName);
                            System.out.println("라인 번호: " + lineNumber);

                            errorFilePathList.add(findAbsolutePathByFilename(project, fileName));
                            errorLocationInFileList.add(lineNumber);
                            javaFileWhereErrorOccurredList.add(readFileViaVirtualFile(findAbsolutePathByFilename(project, fileName)));
                        }
                    }
                    System.out.println("********************에러가 발생한 파일 경로 ********************: " + errorFilePathList.get(errorFilePathList.size() - 1));
                    System.out.println("********************에러가 발생한 파일에서 에러 위치 ********************: " + errorLocationInFileList.get(errorLocationInFileList.size() - 1));
                    System.out.println("********************에러가 발생한 파일 내용 ********************: " + javaFileWhereErrorOccurredList.get(javaFileWhereErrorOccurredList.size() - 1));
                    break; // 일단 하나의 예외 발생, 한개의 파일에서 예외 발생으로 가정
                }
            }
            // 에러 메세지와 에러가 발생한 파일을 ErrorLogStorage.xml파일에 저장
            ErrorLog errorLog = new ErrorLog("", errorToStoreInFile, errorMessageToStoreInFile, errorFilePathList, errorLocationInFileList, javaFileWhereErrorOccurredList);
            ErrorLogStorage errorLogStorage = ErrorLogStorage.getInstance(project);
            errorLogStorage.addErrorLog(errorLog);
        }
    }

    public static void saveAllFiles(Project project) {
        List<String> fileNames = new ArrayList<>();
        List<String> fileContents = new ArrayList<>();

        ApplicationManager.getApplication().runReadAction(() -> {
            FileTypeManager fileTypeManager = FileTypeManager.getInstance();

            for (FileType fileType : fileTypeManager.getRegisteredFileTypes()) {
                Collection<VirtualFile> files = FileTypeIndex.getFiles(
                        fileType,
                        GlobalSearchScope.projectScope(project)
                );

                for (VirtualFile file : files) {
                    try {
                        if (!file.isDirectory() && file.isValid() && file.getName().endsWith(".java") && !file.getPath().contains("/.idea/") && !file.getPath().contains("/.gradle/")) {
                            fileNames.add(file.getPath());
                            fileContents.add(new String(file.contentsToByteArray(), StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        fileNames.add(file.getName());
                        fileContents.add("[ERROR] 파일 읽기 실패: " + e.getMessage());
                    }
                }
            }
        });

        // AllFiles 객체 생성 및 저장
        AllFiles allFiles = new AllFiles(fileNames, fileContents);
        AllFilesStorage.getInstance(project).addAllFiles(allFiles);
    }

    public String getCapturedLog() {
        return logBuilder.toString();
    }

    public String readFileViaVirtualFile(String absolutePath) {
        try {
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(absolutePath);
            if (file == null) {
                return "[오류] 파일을 찾을 수 없습니다: " + absolutePath;
            }

            return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);

        } catch (IOException e) {
            return "[오류] 파일 읽기 실패: " + e.getMessage();
        }
    }

    public String readFileByName(Project project, String filename) {
        Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(
                project,
                filename,
                GlobalSearchScope.projectScope(project)
        );

        if (files.isEmpty()) {
            return "[오류] 파일을 찾을 수 없습니다: " + filename;
        }

        VirtualFile file = files.iterator().next(); // 첫 번째 찾은 파일 사용
        try {
            return new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[오류] 파일 읽기 실패: " + e.getMessage();
        }
    }

    public String findAbsolutePathByFilename(Project project, String filename) {
        Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(
                project,
                filename,
                GlobalSearchScope.projectScope(project)
        );

        if (files.isEmpty()) {
            return "[오류] 파일을 찾을 수 없습니다: " + filename;
        }

        // 첫 번째 결과 사용 (중복될 경우 필터링 로직 필요)
        VirtualFile file = files.iterator().next();
        return file.getPath();  // ← 절대 경로
    }
}
