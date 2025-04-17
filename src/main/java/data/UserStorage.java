package data;

public class UserStorage {
    private static String nickname;
    private static String accessToken;

    public static void setAccessToken(String token) {
        accessToken = token;
    }

    public static void setNickname(String nickname) {
        nickname = nickname;
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static String getNickname() {
        return nickname;
    }

    public static void clearAccessToken() {
        accessToken = null;
    }
}
