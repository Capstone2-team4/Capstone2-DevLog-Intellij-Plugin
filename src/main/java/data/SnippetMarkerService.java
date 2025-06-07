package data;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

// 프로젝트 단위로 하나씩 생성되어, snippetId ↔ RangeMarker를 관리한다.
@Service(Service.Level.PROJECT)
public final class SnippetMarkerService {
    private final Project project;

    // snippetId (예: UUID) ↔ RangeMarker
    private final Map<String, RangeMarker> markers = new HashMap<>();

    // 프로젝트마다 스니펫 복원 여부를 기억할 플래그
    private boolean restored = false;

    private SnippetMarkerService(Project project) {
        this.project = project;
    }

    public static SnippetMarkerService getInstance(@NotNull Project project) {
        return project.getService(SnippetMarkerService.class);
    }

    // 새로운 snippetId로 RangeMarker를 추가
    public void addMarker(@NotNull String snippetId, @NotNull Document document, int startOffset, int endOffset) {
        RangeMarker marker = document.createRangeMarker(startOffset, endOffset);
        marker.setGreedyToLeft(true);
        marker.setGreedyToRight(true);
        markers.put(snippetId, marker);
    }

    // snippetId에 연결된 RangeMarker를 가져옴
    public RangeMarker getMarker(@NotNull String snippetId) {
        return markers.get(snippetId);
    }

    // snippetId 삭제 (예: 스니펫 삭제 시)
    public void removeMarker(@NotNull String snippetId) {
        RangeMarker marker = markers.remove(snippetId);
        if (marker != null) marker.dispose();
    }

    // 전체 마커 Map 반환 (디버깅/루프 탐색용)
    public Map<String, RangeMarker> getAllMarkers() {
        return markers;
    }

    /** 이미 한 번 복원했는지 확인 */
    public boolean isRestored() {
        return restored;
    }

    /** 복원 완료 표시 */
    public void markRestored() {
        this.restored = true;
    }
}
