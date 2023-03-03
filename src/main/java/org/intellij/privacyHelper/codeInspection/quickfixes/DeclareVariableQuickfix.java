package org.intellij.privacyHelper.codeInspection.quickfixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.lang.javascript.psi.JSElementFactory;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Prerit on 1/18/23.
 */
public class DeclareVariableQuickfix implements LocalQuickFix {
    private String declarationTypeText;
    private String declarationNameText;
    private JSExpression targetExpression;
    final static private String DeclareVariableQuickfixName = "Generate a declaration for this value";
    final static private String DeclareVariableQuickfixFamilyName = "Declare variable quickfixes";

    public DeclareVariableQuickfix(String declarationNameText, JSExpression targetExpression) {
        super();

        this.declarationNameText = declarationNameText;
        this.targetExpression = targetExpression;
    }

    @Nls
    @NotNull
    @Override
    public String getName() {
        return DeclareVariableQuickfixName;
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return DeclareVariableQuickfixFamilyName;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        if (declarationTypeText == null) {
            return;
        }
        problemDescriptor.getPsiElement().getText();
    }
}

