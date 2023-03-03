package org.intellij.privacyHelper.codeInspection.inspections;

import com.intellij.codeInspection.*;
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.lang.javascript.psi.*;
//import com.intellij.psi.impl.source.tree.java.PsiEmptyExpressionImpl;
import org.intellij.privacyHelper.codeInspection.intentionActions.JsDocParser;
import org.intellij.privacyHelper.codeInspection.quickfixes.AddAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.psi.PsiElement;


import java.util.*;

import static java.util.Arrays.asList;

/**
 * Created by Prerit on 11/8/2022.
 */
public class PersonalDataSinkAPIInspection extends LocalInspectionTool {

    private static final HashSet geolocationMethods = new HashSet(asList("BackgroundGeolocation", "BackgroundGeolocation"));
    private ArrayList<String> lastElement = new ArrayList<>();
    int i = 0;

    public PersonalDataSinkAPIInspection() {
        super();
    }

    @NotNull
    @Override
    public String getShortName() {
        return "PersonalDataSinkAPIInspection";
    }

    @Override
    public ProblemDescriptor[] checkFile(PsiFile file, InspectionManager manager, boolean isOnTheFly) {
        // Inspection logic here

        Project project = file.getProject();
        MyCustomDialogBox dialogBox = new MyCustomDialogBox(project);
        dialogBox.show();

        // Return problem descriptors
        return new ProblemDescriptor[];
    }


    public void extractMethodInfo(@Nullable ProblemsHolder holder, PsiElement element) {
        if (element != null && !(element instanceof JSElement)) {
            element = element.getParent();
            JSElement classElement = (JSElement) element;
            System.out.println("element " + element + "text: " + ((JSElement) element).getName());
//            System.out.println("classelement --> " + classElement + " TEXT: " + classElement.getText());
            for (int i = 0; i < classElement.getChildren().length; i++) {
                PsiElement child = classElement.getChildren()[i];
                if (!(child instanceof JSFunction)) {
                    continue;
                }

//                System.out.println("Child -> " + child + " and name: " + ((JSFunction) child).getName() + " Text: " + child.getText().contains("PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION"));

                JSFunction functionElement = (JSFunction) child;
                if (methodHasNoComment(functionElement) && functionElement.getText().contains("PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION")) {
//                    System.out.println("fn Element -> " + functionElement + " and text: " + functionElement.getName());
                    holder.registerProblem(functionElement, "Possible location access detected! Add JSDoc comment", new AddAnnotation(functionElement));
                    return;

                }

                if (!methodHasNoComment(functionElement)) {
                JsDocParser jsDocCommentParser = new JsDocParser();
                PsiComment comment = (PsiComment)functionElement.getFirstChild();
                if (!(comment instanceof JSDocComment)) {
                    return;
                }
                jsDocCommentParser.parseJSDoc(comment);
            }
            }
        }
    }

    private boolean methodHasNoComment (PsiElement methodELement){
        return !(methodELement.getFirstChild() instanceof PsiComment);
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
//        return new JSElementVisitor() {
//            @Override
//            public void visitJSReferenceExpression(JSReferenceExpression expression) {
//                super.visitJSReferenceExpression(expression);
//
//                extractMethodInfo(holder, expression);
//            }
//        };
        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
//                super.visitElement(element);
                extractMethodInfo(holder, element);
            }
        };
    }
}
