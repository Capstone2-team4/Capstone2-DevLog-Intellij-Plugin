package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class LoginAction extends AnAction {
    public LoginAction() {
        super("로그인"); // 메뉴에 보이는 이름
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        LoginForm loginForm = new LoginForm();
        // 여기에 로그인 로직 넣기
    }
}
