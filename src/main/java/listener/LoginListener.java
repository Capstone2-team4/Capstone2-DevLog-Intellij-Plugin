package listener;

import com.intellij.util.messages.Topic;

/**
 * 로그인 성공 시점을 통지하기 위한 리스너 인터페이스
 */
public interface LoginListener {
    Topic<LoginListener> TOPIC =
            Topic.create("MyPlugin Login Events", LoginListener.class);

    /**
     * 로그인에 성공했을 때 호출됩니다.
     */
    void loginSucceeded();
}
