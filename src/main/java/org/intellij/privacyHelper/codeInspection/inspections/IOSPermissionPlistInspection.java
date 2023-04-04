package org.intellij.privacyHelper.codeInspection.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.sun.istack.Nullable;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;
import java.util.List;

public class IOSPermissionPlistInspection {
    public @Nullable
    void checkFile(PsiFile file, InspectionManager manager, boolean isOnTheFly) {
        PsiElementVisitor visitor = null;
        if (file.getName().endsWith(".plist")) {
            List<ProblemDescriptor> problems = new ArrayList<>();

             visitor = new PsiElementVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (element instanceof XmlTag) {
                        XmlTag tag = (XmlTag) element;
                        String tagName = tag.getName();
                        if (tagName.equals("key") && tag.getValue() != null) {
                            String keyName = tag.getValue().getText();
                            System.out.println("Key: "+ keyName);
                            }
                        }
                    super.visitElement(element);
                }
            };
        };

            file.accept(visitor);
            return ;
        }
    }
