package org.intellij.privacyHelper.codeInspection.inspections;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class MyCustomDialogBox extends DialogWrapper {
    private JPanel panel;
    private JTextField textField;

    protected MyCustomDialogBox(@Nullable Project project) {
        super(project, true);
        setTitle("My Custom Dialog Box");
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return panel;
    }

    public String getEnteredText() {
        return textField.getText();
    }
}