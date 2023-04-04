/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
//import org.intellij.privacyHelper.codeInspection.instances.AnnotationInstance;
//import org.intellij.privacyHelper.codeInspection.state.PrivacyPracticesHolder;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.javascript.JavaScriptFileType;
import com.intellij.lang.javascript.psi.jsdoc.JSDocComment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.intellij.privacyHelper.codeInspection.Annotations.JsDocAnnotationHolder;
import org.intellij.privacyHelper.codeInspection.intentionActions.JsDocParser;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.lang.javascript.parsing.JSDocParsing.parseJSDoc;
import static org.intellij.privacyHelper.codeInspection.utils.ChaiAnnotationDataType.*;
import static org.intellij.privacyHelper.codeInspection.utils.Constants.*;

/**
 * Modified for Chai by Prerit on 3/28/23
 * Created by tianshi on 3/26/17.
 */
public class CodeInspectionUtil {

    private static final String MISSING_FIELDS = "Missing fields: %s";
    private static final String EMPTY_FIELD = "This field is empty. %s";
    private static final String INCOMPLETE_FIELD_VALUE = "This value is incomplete. %s";

    private static final Map<String, String> FIX_GUIDE_MESSAGES = Collections.unmodifiableMap(new HashMap<>() {{
        put(fieldDataAccessId, "You are recommended to choose a meaningful name that represents the data type and " +
                "the purpose for the data accessed here. For example, if the GPS location is accessed for building " +
                "map features for your app, you can create a string resource `R.string.gps_location_for_map`, and then " +
                "set the value of the string resource to the same value of its name or a more extensive explanation");
    }});

    /**
     * Sets the fields and order for annotations of each type. This determines which fields must be completed for each annotation by the user.
     */
    public static Map<String, String []> ANNOTATION_FIELD_ORDER = new HashMap<>();
    public static final List<String> SINGLE_VALUE_FIELDS =
            Arrays.asList(fieldDataAccessId);

    public static Set<String> NO_VALUE_ANNOTATION = new HashSet<>();
    public static Map<String, Map<String, String>> ANNOTATION_TYPE_FIELDS_INIT_VALUE_MAPPING = new HashMap<>();
    public static Map<String, Map<String, String>> ANNOTATION_TYPE_FIELDS_EMPTY_VALUE_PATTERN_MAPPING = new HashMap<>();
    public static Map<String, Set<String>> ANNOTATION_TYPE_REQUIRED_FIELDS_SET = new HashMap<>();
    public static Map<String, String[]> SAFETY_SECTION_ATTRIBUTE_MAPPING =
            Collections.unmodifiableMap(new HashMap<>() {{
                put("isCollected", safetySectionCollected);
                put("isShared", safetySectionShared);
                put("isEphemeral", safetySectionEphemeral);
                put("isOptional", safetySectionOptional);
                put("isRequired", safetySectionRequired);
                put("isCollectedForAppFunctionality", safetySectionCollectionAppFunctionality);
                put("isCollectedForAnalytics", safetySectionCollectionAnalytics);
                put("isCollectedForDevCommunications", safetySectionCollectionDevCommunications);
                put("isCollectedForFraudPrevention", safetySectionCollectionFraudPrevention);
                put("isCollectedForAdvertising", safetySectionCollectionAdvertising);
                put("isCollectedForPersonalization", safetySectionCollectionPersonalization);
                put("isCollectedForAccountManagement", safetySectionCollectionAccountManagement);
                put("isSharedForAppFunctionality", safetySectionSharingAppFunctionality);
                put("isSharedForAnalytics", safetySectionSharingAnalytics);
                put("isSharedForDevCommunications", safetySectionSharingDevCommunications);
                put("isSharedForFraudPrevention", safetySectionSharingFraudPrevention);
                put("isSharedForAdvertising", safetySectionSharingAdvertising);
                put("isSharedForPersonalization", safetySectionSharingPersonalization);
                put("isSharedForAccountManagement", safetySectionSharingAccountManagement);
            }});
    public static Map<String[], String> SAFETY_SECTION_ATTRIBUTE_REVERSE_MAPPING =
            Collections.unmodifiableMap(new HashMap<>() {{
                put(safetySectionCollected, "isCollected");
                put(safetySectionShared, "isShared");
                put(safetySectionEphemeral, "isEphemeral");
                put(safetySectionOptional, "isOptional");
                put(safetySectionRequired, "isRequired");
                put(safetySectionCollectionAppFunctionality, "isCollectedForAppFunctionality");
                put(safetySectionCollectionAnalytics, "isCollectedForAnalytics");
                put(safetySectionCollectionDevCommunications, "isCollectedForDevCommunications");
                put(safetySectionCollectionFraudPrevention, "isCollectedForFraudPrevention");
                put(safetySectionCollectionAdvertising, "isCollectedForAdvertising");
                put(safetySectionCollectionPersonalization, "isCollectedForPersonalization");
                put(safetySectionCollectionAccountManagement, "isCollectedForAccountManagement");
                put(safetySectionSharingAppFunctionality, "isSharedForAppFunctionality");
                put(safetySectionSharingAnalytics, "isSharedForAnalytics");
                put(safetySectionSharingDevCommunications, "isSharedForDevCommunications");
                put(safetySectionSharingFraudPrevention, "isSharedForFraudPrevention");
                put(safetySectionSharingAdvertising, "isSharedForAdvertising");
                put(safetySectionSharingPersonalization, "isSharedForPersonalization");
                put(safetySectionSharingAccountManagement, "isSharedForAccountManagement");
            }});
    public static Map<PersonalDataGroup, String[]> DATA_TYPE_PRIVACY_LABEL_MAPPING =
            Collections.unmodifiableMap(new HashMap<>() {{
                put(PersonalDataGroup.PersonalInfo_Name, safetySectionName);
                put(PersonalDataGroup.PersonalInfo_EmailAddress, safetySectionEmail);
                put(PersonalDataGroup.PersonalInfo_UserIds, safetySectionUserAccount);
                put(PersonalDataGroup.PersonalInfo_Address, safetySectionAddress);
                put(PersonalDataGroup.PersonalInfo_PhoneNumber, safetySectionPhone);
                put(PersonalDataGroup.PersonalInfo_RaceAndEthnicity, safetySectionEthnicity);
                put(PersonalDataGroup.PersonalInfo_PoliticalOrReligiousBeliefs, safetySectionReligious);
                put(PersonalDataGroup.PersonalInfo_SexualOrientation, safetySectionSexGender);
                put(PersonalDataGroup.PersonalInfo_OtherPersonalInfo, safetySectionPersonal);
                put(PersonalDataGroup.FinancialInfo_UserPaymentInfo, safetySectionBankAccount);
                put(PersonalDataGroup.FinancialInfo_PurchaseHistory, safetySectionPurchaseHistory);
                put(PersonalDataGroup.FinancialInfo_CreditScore, safetySectionCreditScore);
                put(PersonalDataGroup.FinancialInfo_OtherFinancialInfo, safetySectionOtherFinancial);
                put(PersonalDataGroup.Location_ApproximateLocation, safetySectionApproxLocation);
                put(PersonalDataGroup.Location_PreciseLocation, safetySectionPreciseLocation);
                put(PersonalDataGroup.WebBrowsing_WebBrowsingHistory, safetySectionWebBrowsingHistory);
                put(PersonalDataGroup.Messages_Emails, safetySectionMessagesEmails);
                put(PersonalDataGroup.Messages_SmsOrMms, safetySectionSmsCallLog);
                put(PersonalDataGroup.Messages_InAppMessages, safetySectionOtherMessages);
                put(PersonalDataGroup.PhotosAndVideos_Photos, safetySectionPhotos);
                put(PersonalDataGroup.PhotosAndVideos_Videos, safetySectionVideos);
                put(PersonalDataGroup.AudioFiles_VoiceOrSoundRecordings, safetySectionAudio);
                put(PersonalDataGroup.AudioFiles_MusicFiles, safetySectionMusic);
                put(PersonalDataGroup.AudioFiles_OtherUserAudioFiles, safetySectionOtherAudio);
                put(PersonalDataGroup.HealthAndFitness_HealthInfo, safetySectionHealth);
                put(PersonalDataGroup.HealthAndFitness_FitnessInfo, safetySectionFitness);
                put(PersonalDataGroup.Contacts_Contacts, safetySectionContacts);
                put(PersonalDataGroup.Calendar_CalendarEvents, safetySectionCalendar);
                put(PersonalDataGroup.AppInfoAndPerformance_CrashLogs, safetySectionCrashLogs);
                put(PersonalDataGroup.AppInfoAndPerformance_Diagnostics, safetySectionPerformanceDiagnostics);
                put(PersonalDataGroup.AppInfoAndPerformance_OtherAppPerformanceData, safetySectionOtherPerformance);
                put(PersonalDataGroup.FilesAndDocs_FilesAndDocs, safetySectionFilesAndDocs);
                put(PersonalDataGroup.AppActivity_AppInteractions, safetySectionUserInteraction);
                put(PersonalDataGroup.AppActivity_InAppSearchHistory, safetySectionInAppSearchHistory);
                put(PersonalDataGroup.AppActivity_InstalledApps, safetySectionAppsOnDevice);
                put(PersonalDataGroup.AppActivity_OtherUserGeneratedContent, safetySectionUserGeneratedContent);
                put(PersonalDataGroup.AppActivity_OtherUserActivities, safetySectionOtherAppActivity);
                put(PersonalDataGroup.DeviceOrOtherIds_DeviceOrOtherIds, safetySectionDeviceId);
            }});

    public static Map<String[], PersonalDataGroup> DATA_TYPE_PRIVACY_LABEL_REVERSE_MAPPING =
            Collections.unmodifiableMap(new HashMap<>() {{
                put(safetySectionName, PersonalDataGroup.PersonalInfo_Name);
                put(safetySectionEmail, PersonalDataGroup.PersonalInfo_EmailAddress);
                put(safetySectionUserAccount, PersonalDataGroup.PersonalInfo_UserIds);
                put(safetySectionAddress, PersonalDataGroup.PersonalInfo_Address);
                put(safetySectionPhone, PersonalDataGroup.PersonalInfo_PhoneNumber);
                put(safetySectionEthnicity, PersonalDataGroup.PersonalInfo_RaceAndEthnicity);
                put(safetySectionReligious, PersonalDataGroup.PersonalInfo_PoliticalOrReligiousBeliefs);
                put(safetySectionSexGender, PersonalDataGroup.PersonalInfo_SexualOrientation);
                put(safetySectionPersonal, PersonalDataGroup.PersonalInfo_OtherPersonalInfo);
                put(safetySectionBankAccount, PersonalDataGroup.FinancialInfo_UserPaymentInfo);
                put(safetySectionPurchaseHistory, PersonalDataGroup.FinancialInfo_PurchaseHistory);
                put(safetySectionCreditScore, PersonalDataGroup.FinancialInfo_CreditScore);
                put(safetySectionOtherFinancial, PersonalDataGroup.FinancialInfo_OtherFinancialInfo);
                put(safetySectionApproxLocation, PersonalDataGroup.Location_ApproximateLocation);
                put(safetySectionPreciseLocation, PersonalDataGroup.Location_PreciseLocation);
                put(safetySectionWebBrowsingHistory, PersonalDataGroup.WebBrowsing_WebBrowsingHistory);
                put(safetySectionMessagesEmails, PersonalDataGroup.Messages_Emails);
                put(safetySectionSmsCallLog, PersonalDataGroup.Messages_SmsOrMms);
                put(safetySectionOtherMessages, PersonalDataGroup.Messages_InAppMessages);
                put(safetySectionPhotos, PersonalDataGroup.PhotosAndVideos_Photos);
                put(safetySectionVideos, PersonalDataGroup.PhotosAndVideos_Videos);
                put(safetySectionAudio, PersonalDataGroup.AudioFiles_VoiceOrSoundRecordings);
                put(safetySectionMusic, PersonalDataGroup.AudioFiles_MusicFiles);
                put(safetySectionOtherAudio, PersonalDataGroup.AudioFiles_OtherUserAudioFiles);
                put(safetySectionHealth, PersonalDataGroup.HealthAndFitness_HealthInfo);
                put(safetySectionFitness, PersonalDataGroup.HealthAndFitness_FitnessInfo);
                put(safetySectionContacts, PersonalDataGroup.Contacts_Contacts);
                put(safetySectionCalendar, PersonalDataGroup.Calendar_CalendarEvents);
                put(safetySectionCrashLogs, PersonalDataGroup.AppInfoAndPerformance_CrashLogs);
                put(safetySectionPerformanceDiagnostics, PersonalDataGroup.AppInfoAndPerformance_Diagnostics);
                put(safetySectionOtherPerformance, PersonalDataGroup.AppInfoAndPerformance_OtherAppPerformanceData);
                put(safetySectionFilesAndDocs, PersonalDataGroup.FilesAndDocs_FilesAndDocs);
                put(safetySectionUserInteraction, PersonalDataGroup.AppActivity_AppInteractions);
                put(safetySectionInAppSearchHistory, PersonalDataGroup.AppActivity_InAppSearchHistory);
                put(safetySectionAppsOnDevice, PersonalDataGroup.AppActivity_InstalledApps);
                put(safetySectionUserGeneratedContent, PersonalDataGroup.AppActivity_OtherUserGeneratedContent);
                put(safetySectionOtherAppActivity, PersonalDataGroup.AppActivity_OtherUserActivities);
                put(safetySectionDeviceId, PersonalDataGroup.DeviceOrOtherIds_DeviceOrOtherIds);
            }});

    public static final String ANNOTATION_PKG = "me.preritpathak.annotationlib";

    public static final List<String> PREDEFINED_VALUE_FIELDS = List.of(fieldDataAccessDataType,
            fieldDataTransmissionSharingAttributeList, fieldDataTransmissionCollectionAttributeList);

    private static final String resourceStringPrefix = "";
    private static final String sourceDataStringPrefix = "DataType.";

    static {
        Collections.addAll(NO_VALUE_ANNOTATION,
                NotPersonalDataAccess.toString(), NotPersonalDataTransmission.toString());
        ANNOTATION_TYPE_REQUIRED_FIELDS_SET.put(DataAccess.toString(),
                new HashSet<>(Arrays.asList(fieldDataAccessId, fieldDataAccessDataType)));
        ANNOTATION_TYPE_FIELDS_INIT_VALUE_MAPPING.put(DataAccess.toString(),
                Collections.unmodifiableMap(new HashMap<>() {{
                    put(fieldDataAccessId, resourceStringPrefix);
                    put(fieldDataAccessDataType, sourceDataStringPrefix);
                }}));
        ANNOTATION_TYPE_FIELDS_EMPTY_VALUE_PATTERN_MAPPING.put(DataAccess.toString(),
                Collections.unmodifiableMap(new HashMap<>() {{
                    put(fieldDataAccessId, resourceStringPrefix);
                    put(fieldDataAccessDataType, sourceDataStringPrefix);
                    put(fieldDataTransmissionAccessIdList, resourceStringPrefix);
                }}));
        ANNOTATION_FIELD_ORDER.put(DataAccess.toString(),
                new String[]{fieldDataAccessId, fieldDataAccessDataType});
        ANNOTATION_TYPE_REQUIRED_FIELDS_SET.put(DataTransmission.toString(),
                new HashSet<>(Arrays.asList(fieldDataTransmissionAccessIdList,
                        fieldDataTransmissionCollectionAttributeList, fieldDataTransmissionSharingAttributeList)));
        ANNOTATION_TYPE_FIELDS_INIT_VALUE_MAPPING.put(DataTransmission.toString(),
                Collections.unmodifiableMap(new HashMap<>() {{
                    put(fieldDataTransmissionAccessIdList, null);
                    put(fieldDataTransmissionCollectionAttributeList, null);
                    put(fieldDataTransmissionSharingAttributeList, null);
                }}));
        ANNOTATION_TYPE_FIELDS_EMPTY_VALUE_PATTERN_MAPPING.put(DataTransmission.toString(),
                Collections.unmodifiableMap(new HashMap<>() {{
                    put(fieldDataTransmissionAccessIdList, resourceStringPrefix);
                    put(fieldDataTransmissionCollectionAttributeList, " *");
                    put(fieldDataTransmissionSharingAttributeList, " *");
                }}));
        ANNOTATION_FIELD_ORDER.put(DataTransmission.toString(),
                new String[]{fieldDataTransmissionAccessIdList,
                        fieldDataTransmissionCollectionAttributeList, fieldDataTransmissionSharingAttributeList});
    }


    public static ArrayList<String> getAllUniqueSourceIds(Project project) {
        ArrayList<String> allSourceIds = new ArrayList<>();
        // Get all JavaScript files in the project
        Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(JavaScriptFileType.INSTANCE, GlobalSearchScope.projectScope(project));
        JsDocParser jsDocCommentParser = new JsDocParser();
        for (VirtualFile virtualFile : virtualFiles) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            // Find all PsiComment elements in the PSI tree
            PsiElement[] psiComments = PsiTreeUtil.collectElements(psiFile, element -> element instanceof JSDocComment);

            // Iterate through all PsiComment elements
            for (PsiElement commentElement : psiComments) {
                PsiComment psiComment = (PsiComment) commentElement;

                Map<String, String> jsDocComment = jsDocCommentParser.parseJSDoc(psiComment);
                Set set=jsDocComment.entrySet();
                Iterator iterator=set.iterator();

                 if (jsDocComment != null){
                    String id = jsDocComment.get("ID");
                    if (id != null) {
                        allSourceIds.add(id);
                    }
                }
            }
        }
        return allSourceIds;
    }

    /**
     * Creates an empty annotation holder based on the given parameters
     *
     * @param type The type of annotation a holder is needed for
     * @return An empty annotation holder for the given type
     */
    @NotNull
    public static JsDocAnnotationHolder createEmptyAnnotationHolderByType(ChaiAnnotationDataType type) {
        return new JsDocAnnotationHolder(type);
    }


}
