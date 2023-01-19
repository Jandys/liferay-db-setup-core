/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Lundegaard a.s.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.lundegaard.liferay.db.setup.core;

import com.liferay.dynamic.data.mapping.exception.NoSuchStructureException;
import com.liferay.dynamic.data.mapping.model.DDMFormInstance;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceLocalServiceUtil;
import com.liferay.dynamic.data.mapping.service.DDMStructureLayoutLocalServiceUtil;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import eu.lundegaard.liferay.db.setup.core.support.ClassNameLocalServiceUtilWrapper;
import eu.lundegaard.liferay.db.setup.domain.Form;
import eu.lundegaard.liferay.db.setup.domain.FormName;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static java.util.stream.Collectors.toMap;

/**
 * @author michal.volf@lundegaard.eu
 */
public final class SetupForms {

    private static final Log LOG = LogFactoryUtil.getLog(SetupForms.class);
    public static final String LAYOUT = "_LAYOUT";

    private SetupForms() {}


    /**
     * This method handles the setup of forms. It iterates through a list of Form
     * objects and performs the setup action specified for each form. The possible
     * actions are 'create', 'update' and 'delete'. If the form already exists, the
     * method will perform the specified action on it. If the form does not exist,
     * the method will only create or delete if specified.
     *
     * @param formList List of forms to be handled
     * @param userId ID of the user performing the action
     * @param groupId ID of the group the forms belong to
     * @param companyId ID of the company the forms belong to
     */
    public static void setupForms(List<Form> formList, long userId, long groupId, long companyId) {
        for (Form form : formList) {

            String formName = getFirstFormName(form.getFormName());
            LOG.info("Executing " + form.getSetupAction().toString() + " on form " + formName);

            long structureClassNameId = ClassNameLocalServiceUtilWrapper.getClassNameId(DDMFormInstance.class);

            switch (form.getSetupAction()) {
                case CREATE:
                case UPDATE:
                    saveForm(userId, groupId, form, structureClassNameId, companyId);
                    break;
                case DELETE:
                    deleteForm(groupId, form, structureClassNameId);
                    break;
                default:
                    throw new IllegalArgumentException("Illegal setup action " + form.getSetupAction());
            }
        }
    }


    private static String getFirstFormName(List<FormName> formName) {
        if (formName.size() == 0) {
            return "no-name-set";
        }
        return formName.get(0).getValue();
    }

    private static void saveForm(long userId, long groupId, Form form, long structureClassNameId, long companyId) {
        DDMStructure fetchStructure =
                DDMStructureLocalServiceUtil.fetchStructure(groupId, structureClassNameId,
                        form.getStructureKey());

        if (fetchStructure != null) {
            updateForm(userId, groupId, form, fetchStructure.getStructureId(), companyId);
            return;
        }

        createForm(userId, groupId, form, structureClassNameId, companyId);
    }

    private static void createForm(long userId, long groupId, Form form, long structureClassNameId, long companyId) {
        try {
            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setCompanyId(companyId);

            Map<Locale, String> nameMap = namesListToMap(form.getFormName());
            Map<Locale, String> descriptionMap = descriptionsListToMap(form.getFormDescription().getDescription());
            String structureKey = form.getStructureKey();
            String structureLayoutKey = structureKey + LAYOUT;

            DDMStructure ddmStructure = DDMStructureLocalServiceUtil.addStructure(
                    userId,
                    groupId,
                    0,
                    structureClassNameId,
                    structureKey,
                    nameMap,
                    descriptionMap,
                    form.getFormData(),
                    "json",
                    serviceContext);

            DDMFormInstanceLocalServiceUtil.addFormInstance(
                    userId,
                    groupId,
                    ddmStructure.getStructureId(),
                    nameMap,
                    descriptionMap,
                    form.getFormSettings(),
                    serviceContext);

            DDMStructureLayoutLocalServiceUtil.addStructureLayout(
                    userId,
                    groupId,
                    0,
                    structureLayoutKey,
                    ddmStructure.getStructureVersion().getStructureVersionId(),
                    nameMap,
                    descriptionMap,
                    form.getFormLayout(),
                    serviceContext);

            LOG.info("Form created successfully");
        } catch (PortalException e) {
            LOG.error("Creating the form threw an error", e);
        }
    }

    private static void updateForm(long userId, long groupId, Form form, long oldStructureId, long companyId) {
        try {
            Map<Locale, String> nameMap = namesListToMap(form.getFormName());
            Map<Locale, String> descriptionMap = descriptionsListToMap(form.getFormDescription().getDescription());
            ServiceContext serviceContext = new ServiceContext();
            serviceContext.setCompanyId(companyId);


            //            DDMFORM ddmForm = DDMFormInstanceLocalServiceUtil
            //
            //              DDMStructure structure =
            //              DDMStructureLocalServiceUtil.addStructure(userId, groupId, 0, formName,
            //                         ddmForm, ddmFormLayout, null, null, 0, null);
            //
            //              // Create the form instance
            //              DDMFormInstanceLocalServiceUtil.addFormInstance(userId, groupId,
            //                         structure.getStructureId(), formName, formName, null, null, 0, 0, null,
            //              null);
            //
            //              // Create the structure layout
            //              DDMStructureLayoutLocalServiceUtil.addStructureLayout(userId, groupId,
            //                         structure.getStructureId(), ddmFormLayout, null); } catch (PortalException e)
            //              { e.printStackTrace(); }



            DDMStructure ddmStructure = DDMStructureLocalServiceUtil.updateStructure(
                    userId,
                    oldStructureId,
                    0,
                    nameMap,
                    descriptionMap,
                    form.getFormData(),
                    serviceContext);

            String oldStructureLayoutKey = ddmStructure.getStructureKey() + LAYOUT;
            long oldStructureLayoutId = DDMStructureLayoutLocalServiceUtil
                    .getStructureLayout(groupId, 0, oldStructureLayoutKey).getStructureLayoutId();
            DDMStructureLayoutLocalServiceUtil.updateStructureLayout(
                    oldStructureLayoutId,
                    ddmStructure.getStructureVersion().getStructureVersionId(),
                    nameMap,
                    descriptionMap,
                    form.getFormLayout(),
                    serviceContext);

            long oldFormInstanceId = DDMFormInstanceLocalServiceUtil.getFormInstances(groupId).stream()
                    .filter(ddmFormInstance -> ddmFormInstance.getStructureId() == oldStructureId).findFirst()
                    .orElseThrow(IllegalStateException::new).getFormInstanceId();
            DDMFormInstanceLocalServiceUtil.updateFormInstance(
                    oldFormInstanceId,
                    ddmStructure.getStructureId(),
                    nameMap,
                    descriptionMap,
                    new DDMFormValues(ddmStructure.getDDMForm()),
                    serviceContext);


            /**
             *
             * test this
             *
             *
             * import com.liferay.dynamic.data.mapping.model.DDMForm; import
             * com.liferay.dynamic.data.mapping.model.DDMFormLayout; import
             * com.liferay.dynamic.data.mapping.model.DDMStructure; import
             * com.liferay.dynamic.data.mapping.model.DDMStructureLayout; import
             * com.liferay.dynamic.data.mapping.service.DDMFormInstanceLocalServiceUtil;
             * import
             * com.liferay.dynamic.data.mapping.service.DDMStructureLayoutLocalServiceUtil;
             * import com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil;
             * import com.liferay.portal.kernel.exception.PortalException;
             *
             * public class LiferayForms { public void createForm(long groupId, long userId,
             * String formName, DDMForm ddmForm, DDMFormLayout ddmFormLayout) { try { //
             * Create the structure DDMStructure structure =
             * DDMStructureLocalServiceUtil.addStructure(userId, groupId, 0, formName,
             * ddmForm, ddmFormLayout, null, null, 0, null);
             *
             * // Create the form instance
             * DDMFormInstanceLocalServiceUtil.addFormInstance(userId, groupId,
             * structure.getStructureId(), formName, formName, null, null, 0, 0, null,
             * null);
             *
             * // Create the structure layout
             * DDMStructureLayoutLocalServiceUtil.addStructureLayout(userId, groupId,
             * structure.getStructureId(), ddmFormLayout, null); } catch (PortalException e)
             * { e.printStackTrace(); } } }
             *
             *
             *
             */



            LOG.info("Form updated successfully");
        } catch (PortalException e) {
            LOG.error("Updating the form threw an error", e);
        }
    }

    private static void deleteForm(long groupId, Form form, long structureId) {
        try {
            long formInstanceId = DDMFormInstanceLocalServiceUtil.getFormInstances(groupId).stream()
                    .filter(ddmFormInstance -> ddmFormInstance.getStructureId() == structureId).findFirst()
                    .orElseThrow(IllegalStateException::new).getFormInstanceId();

            String structureLayoutKey = form.getStructureKey() + LAYOUT;
            long oldStructureLayoutId = DDMStructureLayoutLocalServiceUtil
                    .getStructureLayout(groupId, 0, structureLayoutKey).getStructureLayoutId();

            DDMStructureLocalServiceUtil.deleteDDMStructure(structureId);
            DDMFormInstanceLocalServiceUtil.deleteDDMFormInstance(formInstanceId);
            DDMStructureLayoutLocalServiceUtil.deleteStructureLayout(oldStructureLayoutId);

            LOG.info("Form deleted successfully");
        } catch (PortalException e) {
            LOG.error("Deleting the form threw an error", e);
        }
    }

    private static Map<Locale, String> namesListToMap(List<FormName> list) {
        return list.stream()
                .filter(name -> name.getValue() != null)
                .collect(
                        toMap(
                                name -> getLocaleFromTag(name.getLocale()),
                                FormName::getValue));
    }

    private static Map<Locale, String> descriptionsListToMap(List<Form.FormDescription.Description> list) {
        return list.stream().collect(
                toMap(
                        description -> getLocaleFromTag(description.getLanguageId()),
                        Form.FormDescription.Description::getValue));
    }

    private static Locale getLocaleFromTag(String s) {
        return Locale.forLanguageTag(fixLocaleString(s));
    }

    private static String fixLocaleString(String localeString) {
        return localeString.replace('_', '-');
    }

}
