package org.intellij.privacyHelper.codeInspection.intentionActions;

import com.intellij.lang.javascript.psi.jsdoc.JSDocComment;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTag;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTagValue;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;

import java.io.*;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/* Created by Prerit Pathak on 2/2/2023 */

public class JsDocParser {
    private static final HashSet RELEVANT_TAGS = new HashSet(asList("dataAccess", "dataTransmission"));
    private static final HashSet USELESS_STRINGS = new HashSet(asList("{", "}", "*/", "*"));
    ArrayList<String> annotationKeys = new ArrayList<String>();
    Map<String, String> hashMap = new LinkedHashMap<String, String>();

    private static final String ANNOTATION_START = "[";

    private static final String ANNOTATION_END = "]";

    public JsDocParser() {
    }

    public Map<String, String> parseJSDoc(PsiComment psiComment) {
        if (!(psiComment instanceof JSDocComment)) {
            return null;
        }

        JSDocComment comment = (JSDocComment) psiComment;
        if (comment != null && comment instanceof JSDocComment) {
            JSDocTag[] tags = comment.getTags();
            for (JSDocTag jsDocTag : tags) {
                System.out.println(jsDocTag.getName());
                if ("DataAccess".equals(jsDocTag.getName()) || "DataTransmission".equals(jsDocTag.getName())) {
                    PsiElement nextSibling = jsDocTag.getNextSibling();
                    int idx = 0;

                    while(nextSibling != null){
                        String annotationKey = "";
                        String annotationValue = nextSibling.getText();
                        if (annotationValue.matches("^\\s*$") || USELESS_STRINGS.contains(annotationValue)) {
                            nextSibling = nextSibling.getNextSibling();
                            continue;
                        }

                        else  if (annotationValue.startsWith(ANNOTATION_START) && annotationValue.endsWith(ANNOTATION_END)) {
                            annotationKey = annotationValue.substring(1, annotationValue.length() - 1);
                            System.out.println("KEY: " + annotationKey);
                            annotationKeys.add(annotationKey);
                            nextSibling = nextSibling.getNextSibling();
                            continue;
                        }
                        System.out.println("idx: " +idx + " annotationKeys.size(): " + annotationKeys.size());
                        if (idx < annotationKeys.size()) {
                            hashMap.put(annotationKeys.get(idx), annotationValue);
                            System.out.println("Annotation Value" + annotationValue);
                            idx++;
                        }

                        nextSibling = nextSibling.getNextSibling();
                    }
                    return hashMap;
                }
            }
        }
        return null;
    }

    public static List<JSDocTag> collectTagsFromComment(JSDocComment comment, Set<String> relevantTags) {
        return Arrays.stream(comment.getTags()).filter(tag -> relevantTags.contains(tag.getName())).collect(toList());
    }
    private Optional<String> extractClosureTypeReference(JSDocTagValue tagValue) {
        if (tagValue == null || tagValue.getChildren().length == 0 || tagValue.getFirstChild().getText() == null) {
            return Optional.empty();
        }
        String referenceWithBrackets = tagValue.getText();

        // Closure type parameters are always embraced in { }, e.g. @type{string}
        if (!referenceWithBrackets.startsWith(ANNOTATION_START) || !referenceWithBrackets.endsWith(ANNOTATION_END)) {
            // Malformated type reference
            return Optional.empty();
        }

        return Optional.of(referenceWithBrackets.substring(1, referenceWithBrackets.length() - 1).replaceAll("(Array)?(function)?[?!<>()]", ""));
    }
}


