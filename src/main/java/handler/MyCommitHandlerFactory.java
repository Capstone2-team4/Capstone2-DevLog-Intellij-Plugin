package handler;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class MyCommitHandlerFactory extends CheckinHandlerFactory {

    public MyCommitHandlerFactory() {
        super();
    }

    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext context) {
        return new MyCommitHandler(panel.getProject());
    }
}