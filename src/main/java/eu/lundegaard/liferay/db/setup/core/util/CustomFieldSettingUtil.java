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
package eu.lundegaard.liferay.db.setup.core.util;

import com.liferay.expando.kernel.model.ExpandoColumn;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoValueLocalServiceUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import eu.lundegaard.liferay.db.setup.core.support.ClassNameLocalServiceUtilWrapper;

/**
 * Utility for setting a custom field.
 *
 * @author msi
 */
public final class CustomFieldSettingUtil {

    private static final Log LOG = LogFactoryUtil.getLog(CustomFieldSettingUtil.class);

    private CustomFieldSettingUtil() {

    }

    /**
     * Auxiliary method that returns the expando value of a given expando field with
     * a given key.
     *
     * @param resolverHint a hint to be used when resolving the value
     * @param runAsUserId the user id to run as
     * @param groupId the id of the group
     * @param company the company id
     * @param clazz the class of the object to set the expando value for
     * @param id the id of the object to set the expando value for
     * @param key the key of the expando to set
     * @param value the value to set the expando to
     */
    // CHECKSTYLE:OFF
    public static void setExpandoValue(final String resolverHint, final long runAsUserId,
            final long groupId, final long company, final Class clazz, final long id,
            final String key, final String value) {
        String valueCopy = value;
        try {

            ExpandoValue ev = ExpandoValueLocalServiceUtil.getValue(company, clazz.getName(),
                    "CUSTOM_FIELDS", key, id);
            // resolve any values to be substituted
            valueCopy = ResolverUtil.lookupAll(runAsUserId, groupId, company, valueCopy,
                    resolverHint);
            if (ev == null) {
                long classNameId = ClassNameLocalServiceUtilWrapper.getClassNameId(clazz.getName());

                ExpandoTable expandoTable = ExpandoTableLocalServiceUtil.getTable(company,
                        classNameId, "CUSTOM_FIELDS");
                ExpandoColumn expandoColumn = ExpandoColumnLocalServiceUtil.getColumn(company,
                        classNameId, expandoTable.getName(), key);

                // In this we are adding MyUserColumnData for the column
                // MyUserColumn. See the
                // above line
                ev = ExpandoValueLocalServiceUtil.addValue(classNameId, expandoTable.getTableId(),
                        expandoColumn.getColumnId(), id, valueCopy);
            } else {
                ev.setData(valueCopy);
                ExpandoValueLocalServiceUtil.updateExpandoValue(ev);
            }
        } catch (Exception ex) {
            LOG.error("Expando (custom field) not found or problem accessing it: " + key + " for "
                    + "class " + clazz.getName() + " with id " + id, ex);
        }

    }
    // CHECKSTYLE:ON
}
