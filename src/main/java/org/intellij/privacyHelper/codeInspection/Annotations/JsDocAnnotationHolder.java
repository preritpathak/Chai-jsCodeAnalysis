package org.intellij.privacyHelper.codeInspection.Annotations;

import com.intellij.lang.javascript.psi.JSType;
import org.intellij.privacyHelper.codeInspection.utils.ChaiAnnotationDataType;
import org.intellij.privacyHelper.codeInspection.utils.CodeInspectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.intellij.privacyHelper.codeInspection.utils.ChaiAnnotationDataType.DataAccess;
import static org.intellij.privacyHelper.codeInspection.utils.CodeInspectionUtil.ANNOTATION_TYPE_FIELDS_INIT_VALUE_MAPPING;
import static org.intellij.privacyHelper.codeInspection.utils.CodeInspectionUtil.NO_VALUE_ANNOTATION;

/**
 * Created by Prerit on 4/2/23.
 */
public class JsDocAnnotationHolder {
    public ChaiAnnotationDataType annotationDataType;
    public Map<String, ArrayList<String>> plainValueFieldPairs = new HashMap<>();

    public JsDocAnnotationHolder(ChaiAnnotationDataType annotationType) {
        annotationDataType = annotationType;
        initAllFields();
    }
    public void initAllFields() {
        String typename = annotationDataType.toString();
        try {assert CodeInspectionUtil.ANNOTATION_TYPE_FIELDS_INIT_VALUE_MAPPING.containsKey(typename) || NO_VALUE_ANNOTATION.contains(typename);}
        catch (Exception e) {
            System.out.println("hmm... working");}
        if (ANNOTATION_TYPE_FIELDS_INIT_VALUE_MAPPING.containsKey(typename)) {
            Map<String, String> field_init_value_mapping = CodeInspectionUtil.ANNOTATION_TYPE_FIELDS_INIT_VALUE_MAPPING.get(typename);
        }
    }

    public static String getJSDocString(List<String> parameters, String indentation, Object[] privacyLabels) {
        StringBuilder builder = new StringBuilder("/** \n").append(indentation).append("* Data Access Annotated by Chai:").append("\n");
        builder.append("  * @dataAccess [ID] %s [ResponseID] %s").append("\n");
        builder.append("  * [ResponseValue] TRUE [Answer Requirement] %s [Human-friendly question label] %s").append("\n");

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

        return String.format(builder.append(indentation).append(" */\n").toString(), privacyLabels);
    }

    public void put(String key, ArrayList<String> values) {
        plainValueFieldPairs.put(key, values);
    }
    public void put(String key, String value) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(value);
        plainValueFieldPairs.put(key, arrayList);
    }
    public void clear(String fieldNAme) {
        if (plainValueFieldPairs.containsKey(fieldNAme)) {
            plainValueFieldPairs.get(fieldNAme).clear();
        }
    }
    public void add(String key, String value) {
        ArrayList<String> list;
        if (plainValueFieldPairs.containsKey(key)) {
            list = plainValueFieldPairs.get(key);
        } else {
            list = new ArrayList<>();
        }
        list.add(value);
        plainValueFieldPairs.put(key, list);
    }
    public void add(String key, ArrayList<String> values) {
        plainValueFieldPairs.put(key, values);
    }

    public static String generateJSDocComment(JsDocAnnotationHolder jsDocAnnotationHolder) {
        Map<String, ArrayList<String>> paramMap = jsDocAnnotationHolder.plainValueFieldPairs;
        ChaiAnnotationDataType type = jsDocAnnotationHolder.annotationDataType;
        StringBuilder jsDocComment = new StringBuilder();

        // Start the JSDoc comment
        jsDocComment.append("/**\n");
        jsDocComment.append(" * Data Transmission Annotated by Chai:\n");
        jsDocComment.append(type == DataAccess ? " * @" + type + " [ID]: " : " * @" + type);

        // Iterate through the paramMap and add each key-value pair as a JSDoc tag
        for (Map.Entry<String, ArrayList<String>> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            ArrayList<String> values = entry.getValue();

            // Combine all values into a single line
            StringBuilder valuesLine = new StringBuilder();
            valuesLine.append("[");
            for (int i = 0; i < values.size(); i++) {
                valuesLine.append(values.get(i));
                if (i < values.size() - 1) {
                    valuesLine.append(", ");
                }
            }
            valuesLine.append("]");

            // Append the key and the combined values
            jsDocComment.append("\n * @").append(key).append(" ").append(valuesLine).append("\n");
        }

        // Close the JSDoc comment
        jsDocComment.append(" */");

        return jsDocComment.toString();
    }

}
