package actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import data.UserStorage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton LoginButton;
    private JLabel DevLog;
    private JPanel DevLogPanel;
    private JPanel LoginPanel;
    private JPanel UsernamePanel;
    private JPanel PasswordPanel;
    private JPanel LoginButtonPanel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JPanel usernameLabelPanel;
    private JPanel passwordLabelPanel;
    private UserStorage userStorage;

    public LoginForm() {
        setContentPane(LoginPanel);
        setTitle("Login");
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocation(600, 300);
        setVisible(true);

        // ✅ 여기부터 UI 꾸미기
//        UsernamePanel.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0)); // top만 50px
//        PasswordPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0)); // top만 50px
//        LoginButton.setBorder(BorderFactory.createEmptyBorder(0, 0, 50, 0));
        DevLog.setFont(new Font("맑은 고딕", Font.BOLD, 30)); // 글꼴, 스타일, 크기

        // ✅ 버튼 클릭 시 이벤트
        LoginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                // 여기서 HttpClient로 아이디 비밀번호 전달
                try {
                    String url = "http://localhost:8080/users/signin"; // ✅ 수정된 URL

                    String jsonBody = """
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                            """.formatted(username, password); // ✅ 실제 입력값 반영

                    HttpClient client = HttpClient.newHttpClient();

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI(url))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    // ✅ 응답 파싱 (Jackson 필요)
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.body());

                    // ✅ result 객체 내부 접근
                    JsonNode result = root.get("result");
                    String accessToken = result.get("accessToken").asText();
                    String refreshToken = result.get("refreshToken").asText();
                    String nickname = result.get("nickname").asText();

                    // nickname, accessToken UserStorage에 저장
                    userStorage.setNickname(nickname);
                    userStorage.setAccessToken(accessToken);

                    // 응답코드 확인, 응답 성공이면 다음 스텝 실행
                    System.out.println("📦 응답 코드: " + response.statusCode());
                    if(response.statusCode() == 200) {
                        // ✅ 1. 로그인 성공 알림창
                        JOptionPane.showMessageDialog(LoginForm.this,
                                "✅ 로그인 성공!\n환영합니다, " + nickname + "님!",
                                "로그인 완료",
                                JOptionPane.INFORMATION_MESSAGE);

                        // ✅ 2. 로그인 창 닫기
                        dispose();
                    }


                } catch (Exception ex) {
                    ex.printStackTrace(); // ✅ 오류 로그 출력

                    JOptionPane.showMessageDialog(LoginForm.this,
                            "❌ 로그인 실패!\n아이디 또는 비밀번호를 확인하세요.",
                            "로그인 오류",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }
}
