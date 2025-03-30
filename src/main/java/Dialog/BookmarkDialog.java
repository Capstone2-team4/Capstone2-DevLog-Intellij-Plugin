package Dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class BookmarkDialog extends DialogWrapper {
    private JPanel panel;
    private JTextField titleField;
    private JTextField contentField;
    private JComboBox<String> categoryCombo;
    private JTextArea codeArea;

    private final String selectedCode;
    private final List<String> categoryList;

    public BookmarkDialog(Project project, String selectedCode, List<String> categories) {
        super(project);
        this.selectedCode = selectedCode;
        this.categoryList = categories;
        init();
        setTitle("북마크 추가");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 선택한 코드
        panel.add(createLabeledComponent("선택한 코드:", createCodeScrollPane()));

        // 제목
        panel.add(Box.createVerticalStrut(10));
        titleField = new JTextField();
        panel.add(createLabeledComponent("제목:", titleField));

        // 기록할 내용
        panel.add(Box.createVerticalStrut(10));
        contentField = new JTextField();
        panel.add(createLabeledComponent("기록할 내용:", contentField));

        // 카테고리
        panel.add(Box.createVerticalStrut(10));
        categoryCombo = new ComboBox<>(categoryList.toArray(new String[0]));
        categoryCombo.setEditable(true);
        panel.add(createLabeledComponent("카테고리:", categoryCombo));

        panel.setPreferredSize(new Dimension(450, 320));
        return panel;
    }

    private JScrollPane createCodeScrollPane() {
        codeArea = new JTextArea(selectedCode);
        codeArea.setEditable(false);
        codeArea.setRows(4);
        codeArea.setLineWrap(true);
        codeArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setPreferredSize(new Dimension(400, 80));
        return scrollPane;
    }

    private JPanel createLabeledComponent(String labelText, JComponent inputComponent) {
        JPanel wrapper = new JPanel(new BorderLayout(5, 5));
        JLabel label = new JLabel(labelText);
        wrapper.add(label, BorderLayout.NORTH);
        wrapper.add(inputComponent, BorderLayout.CENTER);
        return wrapper;
    }

    public String getTitleText() {
        return titleField.getText();
    }

    public String getBookmarkContent() {
        return contentField.getText();
    }

    public String getSelectedCategory() {
        return (String) categoryCombo.getSelectedItem();
    }

    public void addCategoryListener(ActionListener listener) {
        categoryCombo.addActionListener(listener);
    }
}

