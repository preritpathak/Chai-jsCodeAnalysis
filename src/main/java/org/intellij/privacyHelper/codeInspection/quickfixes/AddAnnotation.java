package org.intellij.privacyHelper.codeInspection.quickfixes;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.lang.javascript.psi.*;
import com.intellij.openapi.editor.Document;
        import com.intellij.openapi.project.Project;
        import com.intellij.psi.PsiDocumentManager;
        import com.intellij.psi.PsiElement;
        import com.intellij.psi.PsiFile;
        import com.intellij.psi.PsiWhiteSpace;
        import org.jetbrains.annotations.Nls;
        import org.jetbrains.annotations.NotNull;

        import java.util.Arrays;
        import java.util.Collections;
        import java.util.List;
        import java.util.Optional;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;

        import static java.util.stream.Collectors.toList;

/**
 * Quick fix for a missing method comment.
 */
public class AddAnnotation extends LocalQuickFixOnPsiElement {

    public AddAnnotation(JSFunction functionElement) {
        super(functionElement);
    }

    @NotNull
    @Override
    public String getText() {
        return "Annotate this data usage using JsDoc";
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PsiElement targetElement, @NotNull PsiElement psiElement1) {
        JSFunction functionElement = (JSFunction) targetElement;
        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }
        String documentText = document.getText();
        String indentation = "";
        if (functionElement.getPrevSibling() instanceof PsiWhiteSpace) {
            indentation = functionElement.getPrevSibling().getText();
            if (indentation.contains("\n")) {
                // Only consider indentation of same line as the method declaration
                indentation = indentation.substring(indentation.lastIndexOf("\n") + 1);
            }
        }

        List<String> parameters = getMethodParameters(functionElement);
        String newDocumentText = documentText.substring(0, functionElement.getTextRange().getStartOffset())
                + buildJsDoc(parameters, indentation, functionElement.getReturnType())
                + indentation + documentText.substring(functionElement.getTextRange().getStartOffset());
        document.setText(newDocumentText);


    }

    @NotNull
    private String buildJsDoc(List<String> parameters, String indentation, JSType returnType) {
        StringBuilder builder = new StringBuilder("/** \n").append(indentation).append(" * TODO").append("\n");
        builder.append("  * @dataAccess [ID] PSL_DATA_TYPES_LOCATION [ResponseID] PSL_PRECISE_LOCATION").append("\n");
        builder.append("  * [ResponseValue] TRUE [Answer Requirement] MULTIPLE_CHOICE [Human-friendly question label] Location").append("\n");

        if (!parameters.isEmpty()) {
            builder.append(indentation).append(" *").append("\n");
        }
        for (String parameter : parameters) {
            builder.append(indentation).append(" * @param {TODO");
            if (parameter.startsWith("opt_")) {
                builder.append("=");
            }
            builder.append("} ").append(parameter).append("\n");
        }

        return builder.append(indentation).append(" */\n").toString();
    }
    public static List<String> getMethodParameters(PsiElement functionElement) {
        Optional<PsiElement> parameters = Arrays.stream(functionElement.getChildren()).filter(child -> child instanceof JSParameterList).findFirst();
        if (parameters.isPresent()) {
            return Arrays.stream(parameters.get().getChildren()).filter(parameterOrWhitespace -> parameterOrWhitespace instanceof JSParameter)
                    .map(PsiElement::getText).collect(toList());
        }
        return Collections.emptyList();
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return "Type safety";
    }
}
