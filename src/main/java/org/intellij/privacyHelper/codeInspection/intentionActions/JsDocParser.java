package org.intellij.privacyHelper.codeInspection.intentionActions;

import com.intellij.lang.javascript.JSDocTokenTypes;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTag;
import com.intellij.lang.javascript.psi.jsdoc.JSDocTagValue;
import com.intellij.lang.javascript.refactoring.changeSignature.JSParameterInfo;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;

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

    public void parseJSDoc(PsiComment psiComment) {
        if (!(psiComment instanceof JSDocComment)) {
            return;
        }

        JSDocComment comment = (JSDocComment) psiComment;
        if (comment != null && comment instanceof JSDocComment) {
            JSDocTag[] tags = comment.getTags();
            for (JSDocTag jsDocTag : tags) {
//
                if ("dataAccess".equals(jsDocTag.getName())) {
//                    System.out.println("Key: "+ jsDocTag.getName());
                    PsiElement nextSibling = jsDocTag.getNextSibling();
                    int idx = 0;

//                    while (nextSibling != null && nextSibling.getNode().getElementType() == JSDocTokenTypes.DOC_COMMENT_DATA) {
                    while(nextSibling != null){
                        String annotationKey = "";
                        String annotationValue = nextSibling.getText();
                        if (annotationValue.matches("^\\s*$") || USELESS_STRINGS.contains(annotationValue)) {
                            nextSibling = nextSibling.getNextSibling();
                            continue;
                        }

                        else  if (annotationValue.startsWith(ANNOTATION_START) && annotationValue.endsWith(ANNOTATION_END)) {
                            annotationKey = annotationValue.substring(1, annotationValue.length() - 1);
//                            System.out.println("Annotation hai ye toh: " + annotationKey);
                            annotationKeys.add(annotationKey);
                            nextSibling = nextSibling.getNextSibling();
                            continue;
                        }
                        System.out.println("Value:  " + annotationValue);
                        hashMap.put(annotationKeys.get(idx), annotationValue);
//                        System.out.println("Added " + annotationKeys.get(idx) + " and uski value: " + annotationValue);

                        idx++;
                        nextSibling = nextSibling.getNextSibling();
                    }
                    Set set=hashMap.entrySet();
                    Iterator iterator=set.iterator();

                    //Print the HashMap elements using iterator.
//                    System.out.println("HashMap elements using iterator:");
//                    while(iterator.hasNext()){
//                        Map.Entry mapEntry=(Map.Entry)iterator.next();
//                        System.out.println("Key: " + mapEntry.getKey() + ", " +
//                                "Value: " + mapEntry.getValue());
//                    }

//                    String eol = System.getProperty("line.separator");

//                    try (Writer writer = new FileWriter("/Users/preritpathak/Downloads/test.csv")) {
//                        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
//                            writer.append(entry.getKey())
//                                    .append(',');
//                        }
//                        writer.append(eol);
//
//                        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
//                            writer.append(entry.getValue())
//                                    .append(',');
//                        }
//                        writer.append(eol);
//
//                    } catch (IOException ex) {
//                        ex.printStackTrace(System.err);
//                    }
                    String csvFilePath = "/Users/preritpathak/thesis/sample.csv";
                    String delimiter = ",";

                    try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
                        String line = "";
                        String[] headers = reader.readLine().split(delimiter); // Read the headers

                        // Find the index of the columns we need
                        int questionIdIndex = -1;
                        int responseValueIndex = -1;

                        for (int i = 0; i < headers.length; i++) {
                            if (headers[i].equals("Question ID (machine readable)")) {
                                questionIdIndex = i;
                            }
                            if (headers[i].equals("Response value")) {
                                responseValueIndex = i;
                            }
                        }
                        System.out.println("Qid: " + questionIdIndex);
                        // Read the data rows and update the desired cell
                        StringBuilder modifiedData = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            String[] data = line.split(delimiter);

                            // Check if this is the row we need to modify
                            if (data[questionIdIndex].equals("PSL_DATA_COLLECTION_COLLECTS_PERSONAL_DATA")
                                || data[questionIdIndex].equals("PSL_DATA_USAGE_RESPONSES:PSL_PRECISE_LOCATION:PSL_DATA_USAGE_COLLECTION_AND_SHARING")
                                || data[questionIdIndex].equals("PSL_DATA_USAGE_RESPONSES:PSL_PRECISE_LOCATION:PSL_DATA_USAGE_EPHEMERAL")
                                || data[questionIdIndex].equals("PSL_DATA_USAGE_RESPONSES:PSL_PRECISE_LOCATION:DATA_USAGE_USER_CONTROL")) {
                                data[responseValueIndex] = "TRUE"; // Set the response value to TRUE
                            }
                            if (data[questionIdIndex].equals("PSL_DATA_COLLECTION_ENCRYPTED_IN_TRANSIT")) {
                                data[responseValueIndex] = "FALSE";
                            }
                            if (data[questionIdIndex].equals("PSL_DATA_COLLECTION_USER_REQUEST_DELETE")) {
                                data[responseValueIndex] = "TRUE";
                            }
                            if (data[questionIdIndex].equals(hashMap.get("ID"))) {
                                data[responseValueIndex] = hashMap.get("ResponseValue");
                            }
                            modifiedData.append(String.join(delimiter, data)).append("\n");
                        }
                            // Write the modified data back to the file
                                try (FileWriter writer = new FileWriter(csvFilePath)) {
                                    writer.write(String.join(delimiter, headers) + "\n"); // write the headers first
                                    writer.write(modifiedData.toString()); // write the modified data
                                }
                                // We're done modifying the file, so we can exit the loop
                                System.exit(0);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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


