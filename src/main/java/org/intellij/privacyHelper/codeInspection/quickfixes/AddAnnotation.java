package org.intellij.privacyHelper.codeInspection.quickfixes;

import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.lang.javascript.psi.*;

import static org.intellij.privacyHelper.codeInspection.Annotations.JsDocAnnotationHolder.generateJSDocComment;
import static org.intellij.privacyHelper.codeInspection.Annotations.JsDocAnnotationHolder.getJSDocString;
import static org.intellij.privacyHelper.codeInspection.utils.ChaiAnnotationDataType.DataAccess;
import static org.intellij.privacyHelper.codeInspection.utils.ChaiAnnotationDataType.DataTransmission;
import static org.intellij.privacyHelper.codeInspection.utils.CodeInspectionUtil.DATA_TYPE_PRIVACY_LABEL_MAPPING;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.intellij.privacyHelper.codeInspection.Annotations.JsDocAnnotationHolder;
import org.intellij.privacyHelper.codeInspection.utils.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.intellij.privacyHelper.codeInspection.utils.Constants.fieldDataAccessDataType;

/**
 * Quick fix for a missing method comment.
 */
public class AddAnnotation extends LocalQuickFixOnPsiElement {
    private JsDocAnnotationHolder preFilledAnnotation;
    private final String ADD_ANNOTATION_QUICKFIX;

    public AddAnnotation(JSFunction functionElement, JsDocAnnotationHolder preFilledAnnotation) {
        super(functionElement);
        this.preFilledAnnotation = preFilledAnnotation;
        this.ADD_ANNOTATION_QUICKFIX = String.format("Annotate data %s behavior",
                preFilledAnnotation.annotationDataType == DataAccess ? "access" : "transmission");
        preFilledAnnotation.initAllFields();
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
        final String[][][] privacyLabels = new String[1][1][1];
        if (preFilledAnnotation.annotationDataType == DataAccess) {
            PersonalDataGroup[] personalDataGroups = PersonalDataGroup.values();
            String finalIndentation = indentation;
            DataTypeSelectorDialog dialog = new DataTypeSelectorDialog(project, personalDataGroups,
                    new DataTypeSelectorDialog.Callback() {
                        @Override
                        public void onOk(PersonalDataGroup[] selectedDataTypes) {
                            privacyLabels[0] = Arrays.stream(selectedDataTypes)
                                    .map(d -> DATA_TYPE_PRIVACY_LABEL_MAPPING.get(d))
                                    .toArray(String[][]::new);

                            if (selectedDataTypes.length == 0) {
                                preFilledAnnotation =
                                        CodeInspectionUtil.createEmptyAnnotationHolderByType(
                                                ChaiAnnotationDataType.NotPersonalDataAccess);
                            } else {
                                preFilledAnnotation =
                                        CodeInspectionUtil.createEmptyAnnotationHolderByType(
                                                DataAccess);
                                preFilledAnnotation.clear(fieldDataAccessDataType);
                                for (PersonalDataGroup dataType : selectedDataTypes) {
                                    preFilledAnnotation.add(fieldDataAccessDataType, String.format("DataType.%s", dataType));
                                }
                            }

                            String comment = generateJSDocComment(preFilledAnnotation);
                            System.out.println("-------------------");
                            System.out.println("Created comment: " + comment);

                            List<String> parameters = getMethodParameters(functionElement);
                            String newDocumentText = documentText.substring(0, functionElement.getTextRange().getStartOffset())
//                                    + buildJsDoc(parameters, finalIndentation, privacyLabels[0][0])
                                    + comment
                                    + finalIndentation + documentText.substring(functionElement.getTextRange().getStartOffset());
                            document.setText(newDocumentText);
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
            ApplicationManager.getApplication().invokeLater(dialog::showAndGet);
        }   else if (preFilledAnnotation.annotationDataType == DataTransmission) {
            DataTransmissionSelectorDialog dataTransmissionSelectorDialog = new DataTransmissionSelectorDialog(project, preFilledAnnotation,
                    new DataTransmissionSelectorDialog.Callback() {
                        @Override
                        public void onOk(JsDocAnnotationHolder filledAnnotation) {
                            preFilledAnnotation = filledAnnotation;
                    String comment = generateJSDocComment(preFilledAnnotation);
                            System.out.println("-------------------");
                            System.out.println("Created comment: " + comment);
                        }

                        @Override
                        public void onCancel() {
                            return;
                        }
                    });
            ApplicationManager.getApplication().invokeLater(dataTransmissionSelectorDialog::showAndGet);
        }

    }

    @NotNull
    private String buildJsDoc(List<String> parameters, String indentation, String[] privacyLabels) {
        return getJSDocString(parameters, indentation, privacyLabels);
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
        return "Add Annotation";
    }
}
