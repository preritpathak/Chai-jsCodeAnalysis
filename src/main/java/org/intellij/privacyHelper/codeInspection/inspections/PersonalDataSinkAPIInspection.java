package org.intellij.privacyHelper.codeInspection.inspections;

import com.intellij.codeInspection.*;
import com.intellij.lang.ecmascript6.psi.ES6ImportDeclaration;
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment;
import com.intellij.psi.*;
import com.intellij.lang.javascript.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.intellij.privacyHelper.codeInspection.intentionActions.JsDocParser;
import org.intellij.privacyHelper.codeInspection.quickfixes.AddAnnotation;
import org.intellij.privacyHelper.codeInspection.utils.AndroidPermission;
import org.intellij.privacyHelper.codeInspection.utils.ChaiAnnotationDataType;
import org.intellij.privacyHelper.codeInspection.utils.CodeInspectionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.psi.PsiElement;

import java.util.*;

/**
 * Created by Prerit on 11/8/2022.
 */
public class PersonalDataSinkAPIInspection extends LocalInspectionTool {

    private static final HashSet sensitiveUserDataAccessLibrary = new HashSet();
    private static final HashSet userDataTransmissionLibrary = new HashSet();


    public PersonalDataSinkAPIInspection() {
        super();
    }

    @NotNull
    @Override
    public String getShortName() {
        return "PersonalDataSinkAPIInspection";
    }

    public void extractMethodInfo(@Nullable ProblemsHolder holder, JSFunction functionElement) {
        if (methodHasNoComment(functionElement)) {
            if (isUserDataAccessed(functionElement)) {
                holder.registerProblem(functionElement, "Possible user data access detected! Add JSDoc comment", new AddAnnotation(functionElement, CodeInspectionUtil.createEmptyAnnotationHolderByType(
                        ChaiAnnotationDataType.DataAccess)));
                return;
            }
            else if (isDataTransmitted(functionElement)) {
                holder.registerProblem(functionElement, "Possible user data transmission detected! Add JSDoc comment", new AddAnnotation(functionElement, CodeInspectionUtil.createEmptyAnnotationHolderByType(
                        ChaiAnnotationDataType.DataTransmission)));
                return;
            }
        }

        if (!methodHasNoComment(functionElement)) {
            JsDocParser jsDocCommentParser = new JsDocParser();
            PsiComment comment = (PsiComment) functionElement.getFirstChild();
            if (!(comment instanceof JSDocComment)) {
                return;
            }
            jsDocCommentParser.parseJSDoc(comment);
        }
    }

    private boolean methodHasNoComment(PsiElement psiElement) {
        return !(psiElement.getFirstChild() instanceof PsiComment);
    }

    private boolean isUserDataAccessed(PsiElement psiElement) {
        boolean userDataIsAccessed = false;
        String permissionString = null;
        for(Object library: sensitiveUserDataAccessLibrary) {
            String psiText = psiElement.getText();
            if(psiText.contains((library).toString())){
                if(library.toString().equals("PermissionsAndroid")){
                    permissionString = psiText.substring(psiText.indexOf(library.toString()+".PERMISSIONS"), psiText.indexOf(","));
                    String[] parts = permissionString.split("\\.");
                    String permissionName = parts[parts.length - 1];
                    System.out.println(permissionName);
                   try {
                        AndroidPermission.valueOf(permissionName);
                       System.out.println("Works!");
                       userDataIsAccessed = true;
                   } catch (IllegalArgumentException e) {
                       System.out.println("doesnt exist");
                   }
                }else {
                    userDataIsAccessed = true;
                }
            }
        }
        return userDataIsAccessed;
    }

    private boolean isDataTransmitted(PsiElement psiElement) {
        boolean userDataTransmitted = false;
        for (Object library : userDataTransmissionLibrary) {
            String psiText = psiElement.getText();
            if (psiText.contains((library).toString())) {
                userDataTransmitted = true;
            }
        }
        return userDataTransmitted;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        Set<String> alreadyVisited = new HashSet<String>();
        return new JSElementVisitor() {
            @Override
            public void visitJSElement(JSElement element) {
                super.visitJSElement(element);

                if (element instanceof ES6ImportDeclaration) {
                    String importText = element.getText();
                    if (importText.matches(".*react-native-.*-intent.*")) {
                        extractImportName(importText, userDataTransmissionLibrary);
                    }
                    else if (importText.matches(".*react-native-.*") || importText.matches(".*PermissionsAndroid.*")) {
                        extractImportName(importText, sensitiveUserDataAccessLibrary);
                    }
                }

                if (element instanceof JSReferenceExpression) {
                    if (sensitiveUserDataAccessLibrary.contains(element.getText()) || userDataTransmissionLibrary.contains(element.getText())) {
                    JSFunction containingFunction = PsiTreeUtil.getParentOfType(element, JSFunction.class);
                        if (!alreadyVisited.contains(containingFunction.getName())) {
                        extractMethodInfo(holder, containingFunction);
                        alreadyVisited.add(containingFunction.getName());
                    }
                }
                }
            }
        };
    }

    private void extractImportName(String importText, HashSet sensitiveUserDataAccessLibrary) {
        int importStart = importText.indexOf("import ") + "import ".length(); // Get the start position of the import statement
        int importEnd = importText.indexOf(" from "); // Get the end position of the import statement
        String importName = importText.substring(importStart, importEnd).trim(); // Extract the import name and remove any whitespace
        importName = importName.matches("^\\{(.*)\\}$") ? importName.replaceAll("^\\{(.*)\\}$", "$1").trim() : importName;
        sensitiveUserDataAccessLibrary.add(importName);
    }
}

