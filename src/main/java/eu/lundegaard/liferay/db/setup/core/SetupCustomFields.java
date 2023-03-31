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

import com.liferay.expando.kernel.model.*;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.expando.kernel.util.ExpandoBridgeFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.*;
import eu.lundegaard.liferay.db.setup.LiferaySetup;
import eu.lundegaard.liferay.db.setup.domain.CustomFields;
import eu.lundegaard.liferay.db.setup.domain.RolePermission;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.*;

public final class SetupCustomFields {

    private static final Log LOG = LogFactoryUtil.getLog(SetupCustomFields.class);

    private SetupCustomFields() {

    }

    /**
     * Sets up custom expando fields for a site.
     *
     * @param fields the list of custom fields to set up
     * @param companyId the company ID of the site
     */
    public static void setupExpandoFields(final List<CustomFields.Field> fields, long companyId) {

        for (CustomFields.Field field : fields) {
            String className = field.getClassName();
            LOG.info("Add field " + field.getName() + "(" + className + ") to expando bridge");

            ExpandoBridge bridge = ExpandoBridgeFactoryUtil.getExpandoBridge(companyId, className);
            addAttributeToExpandoBridge(bridge, field, companyId);
        }
    }

    /**
     * @return all expandos with types specified in the "excludeListed" List to
     *         avoid deleting all expandos in the portal!
     */
    private static List<ExpandoColumn> getAllExpandoColumns(
            final List<CustomFields.Field> customFields, long companyId) {

        List<ExpandoColumn> all = new ArrayList<>();
        SortedSet<String> tables = new TreeSet<>();
        for (CustomFields.Field field : customFields) {
            ExpandoTable table;
            try {
                table = ExpandoTableLocalServiceUtil.getDefaultTable(companyId,
                        field.getClassName());
                if (table != null && !tables.contains(table.getName())) {
                    tables.add(table.getName());
                    List<ExpandoColumn> columns = ExpandoColumnLocalServiceUtil
                            .getColumns(companyId, field.getClassName(), table.getName());
                    all.addAll(columns);
                }
            } catch (PortalException | SystemException e) {
                LOG.error("Error in getAllExpandoColumns()." + e.getMessage());
            }
        }
        return all;
    }

    /**
     * Adds a custom attribute to an expando bridge.
     *
     * @param bridge the expando bridge to add the attribute to
     * @param field the custom field to add as an attribute
     * @param companyId the company ID of the site
     */
    private static void addAttributeToExpandoBridge(final ExpandoBridge bridge,
            final CustomFields.Field field, long companyId) {

        String name = field.getName();
        try {
            int fieldTypeKey = getFieldTypeKey(field.getType());
            if (bridge.hasAttribute(name)) {
                ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(companyId,
                        bridge.getClassName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, name);
                ExpandoColumnLocalServiceUtil.updateColumn(column.getColumnId(), name, fieldTypeKey,
                        getAttributeFromString(fieldTypeKey, field.getDefaultData()));
            } else {
                bridge.addAttribute(name, fieldTypeKey,
                        getAttributeFromString(fieldTypeKey, field.getDefaultData()));
            }
            UnicodeProperties properties = bridge.getAttributeProperties(name);
            properties.setProperty(ExpandoColumnConstants.INDEX_TYPE,
                    Integer.toString(getIndexedType(field.getIndexed())));
            properties.setProperty(ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE,
                    getDisplayType(field.getDisplayType()));
            bridge.setAttributeProperties(name, properties);
            setCustomFieldPermission(field.getRolePermission(), bridge, name, companyId);
        } catch (PortalException | SystemException e) {
            LOG.error("Could not set custom attribute: " + name, e);
        }
    }

    /**
     * This method sets custom field permissions on an ExpandoBridge for a specific
     * field name.
     *
     * @param rolePermissions a list of role permissions to be set on the field
     * @param bridge the expando bridge on which the field is defined
     * @param fieldName the name of the field on which to set the permissions
     * @param companyId the id of the company for which the permissions are being
     *        set
     * @throws SystemException if an error occurs while trying to set the
     *         permissions
     */
    private static void setCustomFieldPermission(final List<RolePermission> rolePermissions,
            final ExpandoBridge bridge, final String fieldName, long companyId) throws SystemException {

        LOG.info("Set read permissions on  field " + fieldName + " for " + rolePermissions.size()
                + " rolePermissions");
        ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(companyId,
                bridge.getClassName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, fieldName);
        for (RolePermission rolePermission : rolePermissions) {
            String roleName = rolePermission.getRoleName();
            String permission = rolePermission.getPermission();
            try {
                switch (permission) {
                    case "update":
                        SetupPermissions.addReadWrightRight(roleName, ExpandoColumn.class.getName(),
                                String.valueOf(column.getColumnId()), companyId);
                        LOG.info("Added update permission on field " + fieldName + " for role "
                                + roleName);
                        break;
                    case "view":
                        SetupPermissions.addReadRight(roleName, ExpandoColumn.class.getName(),
                                String.valueOf(column.getColumnId()), companyId);
                        LOG.info("Added read permission on field " + fieldName + " for role "
                                + roleName);
                        break;
                    default:
                        LOG.info("Unknown permission:" + permission + ". No permission added on "
                                + "field " + fieldName + " for role " + roleName);
                        break;
                }

            } catch (PortalException e) {
                LOG.error("Could not set permission to " + roleName + " on " + fieldName, e);
            }
        }
    }

    public static void deleteCustomField(final CustomFields.Field customField,
            final String deleteMethod, long companyId) {
        deleteCustomFields(Arrays.asList(customField), deleteMethod, companyId);
    }


    /**
     * Method to delete the custom fields based on the deleteMethod.
     *
     * <p>
     * </p>
     * <p>
     * If deleteMethod is "excludeListed", the method will delete all custom fields
     * of the specified className in the list but the listed fields.
     * </p>
     * <p>
     * If deleteMethod is "onlyListed", the method will delete only the custom
     * fields that are listed in the list.
     * </p>
     *
     * @param customFields List of custom fields to be deleted
     * @param deleteMethod String value that specifies how to delete the fields. it
     *        can take two values: "excludeListed" or "onlyListed"
     * @param companyId id of the company
     * @throws SystemException
     * @throws PortalException
     */
    public static void deleteCustomFields(final List<CustomFields.Field> customFields,
            final String deleteMethod, long companyId) {

        if ("excludeListed".equals(deleteMethod)) {
            // delete all (from types in the list) but listed
            List<String> skipFields = attributeNamesList(customFields);
            List<ExpandoColumn> expandoColumns = getAllExpandoColumns(customFields, companyId);
            if (expandoColumns != null) {
                for (ExpandoColumn expandoColumn : expandoColumns) {
                    if (!skipFields.contains(expandoColumn.getName())) {
                        try {
                            ExpandoColumnLocalServiceUtil.deleteColumn(expandoColumn.getColumnId());
                        } catch (PortalException | SystemException e) {
                            LOG.error("Could not delete CustomField " + expandoColumn.getName(), e);
                        }
                    }
                }
            }
        } else if (deleteMethod.equals("onlyListed")) {
            for (CustomFields.Field field : customFields) {
                try {
                    ExpandoTable table = ExpandoTableLocalServiceUtil.getDefaultTable(companyId,
                            field.getClassName());
                    ExpandoColumnLocalServiceUtil.deleteColumn(companyId, field.getClassName(),
                            table.getName(), field.getName());
                } catch (PortalException | SystemException e) {
                    LOG.error("Could not delete Custom Field " + field.getName(), e);
                    continue;
                }
                LOG.info("custom field " + field.getName() + " deleted ");
            }
        }
    }

    /**
     * This method maps Expando name to corresponding int value.
     *
     * @param name String name (eq. string, boolean, int, date, long)
     * @return corresponding int value.
     */
    private static int getFieldTypeKey(final String name) {

        if ("stringArray".equals(name)) {
            return ExpandoColumnConstants.STRING_ARRAY;
        }
        if ("string".equals(name)) {
            return ExpandoColumnConstants.STRING;
        }
        if ("int".equals(name)) {
            return ExpandoColumnConstants.INTEGER;
        }
        if ("boolean".equals(name)) {
            return ExpandoColumnConstants.BOOLEAN;
        }
        if ("date".equals(name)) {
            return ExpandoColumnConstants.DATE;
        }
        if ("long".equals(name)) {
            return ExpandoColumnConstants.LONG;
        }
        if ("double".equals(name)) {
            return ExpandoColumnConstants.DOUBLE;
        }
        if ("float".equals(name)) {
            return ExpandoColumnConstants.FLOAT;
        }
        LOG.error("bad setup name: " + name);
        return -1;
    }

    /**
     * Method maps List of {@link CustomFields.Field} to List of its names
     *
     * @param customFields {@link List} of {@link CustomFields.Field} custom fields
     * @return List of mapped names
     */
    private static List<String> attributeNamesList(final List<CustomFields.Field> customFields) {

        List<String> names = new ArrayList<>();
        for (CustomFields.Field f : customFields) {
            if (f.getName() != null) {
                names.add(f.getName());
            }
        }
        return names;
    }

    /**
     * This method maps Expando Index Type to corresponding int value.
     *
     * @param indexed String type (none, text, keyword)
     * @return corresponding int value.
     */
    private static int getIndexedType(final String indexed) {

        if ("none".equals(indexed)) {
            return ExpandoColumnConstants.INDEX_TYPE_NONE;
        } else if ("text".equals(indexed)) {
            return ExpandoColumnConstants.INDEX_TYPE_TEXT;
        } else if ("keyword".equals(indexed)) {
            return ExpandoColumnConstants.INDEX_TYPE_KEYWORD;
        } else {
            LOG.error("cannot get unknown index type: " + indexed);
            return 0;
        }
    }

    /**
     * This method maps Expando display Type to corresponding constant value.
     *
     * @param displayType String type (checkbox, radio, selection-list, text-box)
     * @return corresponding constant.
     */
    private static String getDisplayType(final String displayType) {
        if ("checkbox".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_CHECKBOX;
        } else if ("radio".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_RADIO;
        } else if ("selection-list".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_SELECTION_LIST;
        } else if ("text-box".equals(displayType)) {
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX;
        } else {
            LOG.error("cannot get unknown display type: " + displayType);
            return ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX;
        }
    }

    /**
     * This method converts List of {@link CustomFields.Field} to hashMap
     *
     * @param objects List that should be remapped
     * @return Hash map where key is name of the {@link CustomFields.Field} and
     *         value is Custom Field itself.
     */
    private static Map<String, CustomFields.Field> convertCustomFieldListToHashMap(
            final List<CustomFields.Field> objects) {

        HashMap<String, CustomFields.Field> map = new HashMap<>();
        for (CustomFields.Field field : objects) {
            map.put(field.getName(), field);
        }
        return map;
    }

    public static Serializable getAttributeFromString(final int type, final String attribute) {

        if (attribute == null) {
            return null;
        }

        if (type == ExpandoColumnConstants.BOOLEAN) {
            return GetterUtil.getBoolean(attribute);
        } else if (type == ExpandoColumnConstants.BOOLEAN_ARRAY) {
            return GetterUtil.getBooleanValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.DATE) {
            return GetterUtil.getDate(attribute, getDateFormat());
        } else if (type == ExpandoColumnConstants.DATE_ARRAY) {
            return GetterUtil.getDateValues(StringUtil.split(attribute), getDateFormat());
        } else if (type == ExpandoColumnConstants.DOUBLE) {
            return GetterUtil.getDouble(attribute);
        } else if (type == ExpandoColumnConstants.DOUBLE_ARRAY) {
            return GetterUtil.getDoubleValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.FLOAT) {
            return GetterUtil.getFloat(attribute);
        } else if (type == ExpandoColumnConstants.FLOAT_ARRAY) {
            return GetterUtil.getFloatValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.INTEGER) {
            return GetterUtil.getInteger(attribute);
        } else if (type == ExpandoColumnConstants.INTEGER_ARRAY) {
            return GetterUtil.getIntegerValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.LONG) {
            return GetterUtil.getLong(attribute);
        } else if (type == ExpandoColumnConstants.LONG_ARRAY) {
            return GetterUtil.getLongValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.SHORT) {
            return GetterUtil.getShort(attribute);
        } else if (type == ExpandoColumnConstants.SHORT_ARRAY) {
            return GetterUtil.getShortValues(StringUtil.split(attribute));
        } else if (type == ExpandoColumnConstants.STRING_ARRAY) {
            return StringUtil.split(attribute);
        } else {
            return attribute;
        }
    }

    private static DateFormat getDateFormat() {
        return DateUtil.getISO8601Format();
    }
}
