/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.intellij.privacyHelper.codeInspection.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class DataTypeSelectorDialog extends DialogWrapper{
    private ArrayList<JCheckBox> dataTypeCheckBoxes = new ArrayList<>();
    private JTextArea myTextArea;
    private PersonalDataGroup[] dataGroups;
    private HashMap<String, PersonalDataGroup> labelToDataGroup = new HashMap<>();
    private Callback callback;
    private JTextField idField;
    private Set<String> uniqueIDs;
    private boolean idNotUniqueOrNotEntered = true;

    public interface Callback {
        void onOk(PersonalDataGroup[] selectedDataTypes);

        void onCancel();
    }

    public DataTypeSelectorDialog(Project project, PersonalDataGroup [] dataGroupOptions, Callback callback) {
        super(project, false);
        this.callback = callback;
        this.dataGroups = dataGroupOptions;
        setTitle("Chai Data Access Annotation");
        init();
        setOKButtonText("Submit");
        setCancelButtonText("Cancel");
    }

    @Override
    protected Action @NotNull [] createActions(){
        return new Action[]{getOKAction(), getCancelAction()};
    }

    private void fillDataTypeCheckBoxes(JPanel cbPanel, int start, int end){
        JCheckBox checkBox;
        String currentGroup = "";

        // sort dataGroups
        Arrays.sort(dataGroups, Comparator.comparing(Enum::name));
        for (PersonalDataGroup dataGroup : Arrays.copyOfRange(dataGroups, start, end)) {
            checkBox = new JCheckBox(prettifyDataTypeString(dataGroup.name()));
            checkBox.setSelected(false);
            dataTypeCheckBoxes.add(checkBox);
            if (currentGroup.isEmpty() || !currentGroup.equals(dataGroup.name().split("_")[0])) {
                // bold JLabel
                JLabel label = new JLabel(prettifyDataTypeString(dataGroup.name().split("_")[0]));
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                cbPanel.add(label);
                currentGroup = dataGroup.name().split("_")[0];
            }
            cbPanel.add(checkBox);
            labelToDataGroup.put(checkBox.getText(), dataGroup);
        }
    }

    static public String prettifyDataTypeString(String dataGroup) {
        if (dataGroup.split("_").length > 1) {
            return prettifyCamelCaseString(dataGroup.split("_")[1]);
        } else {
            return prettifyCamelCaseString(dataGroup);
        }
    }

    public static String prettifyCamelCaseString(String camelCaseString) {
        return String.join(" ", camelCaseString.split("(?<=[a-z])(?=[A-Z])")).replace(".", " ");
    }

    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new BorderLayout());

        JPanel optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.X_AXIS));

        JPanel cbPanel = new JPanel();
        JCheckBox checkBox;
        cbPanel.setLayout(new BoxLayout(cbPanel, BoxLayout.Y_AXIS));
        cbPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        fillDataTypeCheckBoxes(cbPanel, 0, Math.min(dataGroups.length, 20));
        optionPanel.add(cbPanel);

        if (dataGroups.length > 20) {
            cbPanel = new JPanel();
            cbPanel.setLayout(new BoxLayout(cbPanel, BoxLayout.Y_AXIS));
            cbPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

            fillDataTypeCheckBoxes(cbPanel, 20, dataGroups.length);
        }

        // add vertical space to cbPanel
        cbPanel.add(Box.createVerticalGlue());
        checkBox = new JCheckBox("None of the Above");
        checkBox.setFont(checkBox.getFont().deriveFont(Font.BOLD));
        checkBox.setSelected(false);
        dataTypeCheckBoxes.add(checkBox);
        cbPanel.add(checkBox);
        optionPanel.add(cbPanel);

        panel.add(optionPanel, BorderLayout.SOUTH);

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        panel.add(textPanel, BorderLayout.CENTER);

        myTextArea = new JTextArea("What types of user data do you access here? Select all that apply");
        textPanel.add(myTextArea, BorderLayout.CENTER);
        myTextArea.setEditable(false);
        myTextArea.setBackground(UIUtil.getPanelBackground());
        Font font = dataTypeCheckBoxes.get(0).getFont();
        font = new Font(font.getName(), font.getStyle(), font.getSize() + 1);
        myTextArea.setFont(font);

        uniqueIDs = new HashSet<>();

        // Add ID panel
        JPanel idPanel = new JPanel();
        idPanel.setLayout(new BorderLayout());
        idPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel idLabel = new JLabel("ID:");
        idPanel.add(idLabel, BorderLayout.WEST);

        idField = new JTextField();
        idPanel.add(idField, BorderLayout.CENTER);
        panel.add(idPanel, BorderLayout.NORTH);

        // Add a button to check the uniqueness of the entered ID
        JButton checkUniqueIDButton = new JButton("Check Unique ID");
        panel.add(checkUniqueIDButton);

        // Add an ActionListener to the button
        checkUniqueIDButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String enteredID = idField.getText();
                if (uniqueIDs.add(enteredID)) {
                    JOptionPane.showMessageDialog(null, "The entered ID is unique and has been added to the set.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    idNotUniqueOrNotEntered = false;
                } else {
                    JOptionPane.showMessageDialog(null, "The entered ID is not unique. Please try again.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return panel;
    }

    @Override
    protected void doOKAction() {
        boolean noneOfTheAboveSelected = false;
        ArrayList<PersonalDataGroup> selectedDataTypes = new ArrayList<>();
        for (JCheckBox checkBox : dataTypeCheckBoxes) {
            if (checkBox.isSelected()) {
                if (!labelToDataGroup.containsKey(checkBox.getText())) {
                    noneOfTheAboveSelected = true;
                } else {
                    selectedDataTypes.add(labelToDataGroup.get(checkBox.getText()));
                }
            }
        }

        if (noneOfTheAboveSelected && selectedDataTypes.size() > 0) {
            JOptionPane.showMessageDialog(this.getContentPanel(),
                    "You cannot select 'None of the Above' and other data types at the same time");
            return;
        } else if (!noneOfTheAboveSelected && selectedDataTypes.isEmpty()) {
            JOptionPane.showMessageDialog(this.getContentPanel(),
                    "You must select at least one data type or 'None of the Above'");
            return;
        } else if (idNotUniqueOrNotEntered) {
            JOptionPane.showMessageDialog(this.getContentPanel(),
                    "You must enter an ID and confirm its uniqueness before proceeding");
            return;
        }

        if (noneOfTheAboveSelected) {
            super.doOKAction();
            callback.onOk(new PersonalDataGroup[0]);
        } else {
            super.doOKAction();
            callback.onOk(selectedDataTypes.toArray(new PersonalDataGroup[0]));
        }
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        callback.onCancel();
    }
}
