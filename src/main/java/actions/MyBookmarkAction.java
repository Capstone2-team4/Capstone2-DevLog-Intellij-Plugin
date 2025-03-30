package actions;

import Dialog.BookmarkDialog;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import data.MyBookMark;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MyBookmarkAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

        if (editor == null || project == null || file == null) return;

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();

        if (selectedText != null && !selectedText.isEmpty()) {
            List<String> categories = fetchCategoriesFromServer();

            // "새 카테고리 추가..." 항목을 맨 아래에 추가
            if (!categories.contains("새 카테고리 추가...")) {
                categories.add("새 카테고리 추가...");
            }

            BookmarkDialog dialog = new BookmarkDialog(project, selectedText, categories);

            // 콤보박스 리스너 등록
            dialog.addCategoryListener(e1 -> {
                JComboBox<String> combo = (JComboBox<String>) e1.getSource();
                String selected = (String) combo.getSelectedItem();
                if ("새 카테고리 추가...".equals(selected)) {
                    String newCategory = Messages.showInputDialog(
                            "새 카테고리를 입력하세요:",
                            "카테고리 추가",
                            Messages.getQuestionIcon()
                    );
                    if (newCategory != null && !newCategory.isBlank()) {
                        boolean success = postNewCategoryToServer(newCategory);
                        if (success) {
                            combo.insertItemAt(newCategory, combo.getItemCount() - 1);
                            combo.setSelectedItem(newCategory);
                        } else {
                            Messages.showMessageDialog("카테고리 저장에 실패했습니다.", "서버 오류", Messages.getErrorIcon());
                        }
                    } else {
                        combo.setSelectedIndex(0); // 되돌리기
                    }
                }
            });

            if (dialog.showAndGet()) {
                String title = dialog.getTitleText();
                String content = dialog.getBookmarkContent();
                String category = dialog.getSelectedCategory();

                if ("새 카테고리 추가...".equals(category)) {
                    String newCategory = Messages.showInputDialog(
                            "새 카테고리를 입력하세요:",
                            "카테고리 추가",
                            Messages.getQuestionIcon()
                    );
                    if (newCategory != null && !newCategory.isBlank()) {
                        category = newCategory;
                    } else {
                        Messages.showMessageDialog("카테고리를 입력하지 않았습니다.", "입력 오류", Messages.getErrorIcon());
                        return;
                    }
                }

                if (title.isEmpty() || content.isEmpty() || category.isEmpty()) {
                    Messages.showMessageDialog("모든 입력값을 채워주세요!", "입력 오류", Messages.getErrorIcon());
                    return;
                }

                int lineNumber = editor.getCaretModel().getLogicalPosition().line;

                MyBookMark myBookMark = new MyBookMark();
                myBookMark.filePath = file.getPath();
                myBookMark.line = lineNumber;
                myBookMark.title = title;
                myBookMark.content = content;
                myBookMark.category = category;
                myBookMark.code = selectedText;

                MyBookmarkStorage storage = MyBookmarkStorage.getInstance(project);
                storage.addBookmark(myBookMark);

                Messages.showMessageDialog(
                        "북마크 추가 완료!\n\n제목: " + title + "\n이름: " + content + "\n카테고리: " + category + "\n코드:\n" + selectedText,
                        "성공",
                        Messages.getInformationIcon()
                );
            }
        } else {
            Messages.showMessageDialog("먼저 코드를 드래그해주세요!", "오류", Messages.getErrorIcon());
        }
    }

    private boolean postNewCategoryToServer(String newCategory) {
        return true;
    }

    private List<String> fetchCategoriesFromServer() {
        // 기본 카테고리 우선 제공
        List<String> categories = new ArrayList<>(List.of("리팩토링", "버그 수정", "기능 구현"));
        try {
            URL url = new URL("http://localhost:8080/categories");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                JsonObject jsonResponse = new Gson().fromJson(reader, JsonObject.class);
                reader.close();

                boolean isSuccess = jsonResponse.has("isSuccess") && jsonResponse.get("isSuccess").getAsBoolean();
                if (isSuccess && jsonResponse.has("result") && jsonResponse.get("result").isJsonArray()) {
                    Type listType = new TypeToken<List<String>>() {}.getType();
                    List<String> serverCategories = new Gson().fromJson(jsonResponse.get("result"), listType);
                    categories.addAll(serverCategories);
                } else if (jsonResponse.has("message")) {
                    String message = jsonResponse.get("message").getAsString();
                    Messages.showMessageDialog(message, "카테고리 없음", Messages.getWarningIcon());
                }
            }

            conn.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return categories;
    }
}
