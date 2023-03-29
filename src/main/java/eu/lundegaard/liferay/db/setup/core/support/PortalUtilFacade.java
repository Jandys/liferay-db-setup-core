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
package eu.lundegaard.liferay.db.setup.core.support;

import java.util.Locale;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalUtil;

/**
 * @author Jakub Jandák, jakub.jandak@lundegaard.eu, 2023
 */

public class PortalUtilFacade {

    private static final Log LOG = LogFactoryUtil.getLog(PortalUtil.class);


    public static Locale getDefaultLocale(final long groupId) {
        return getDefaultLocale(groupId, null);
    }

    public static Locale getDefaultLocale(final long groupId, final String locationHint) {
        Locale siteDefaultLocale = null;
        try {
            siteDefaultLocale = PortalUtil.getSiteDefaultLocale(groupId);
        } catch (PortalException | SystemException e) {
            if (locationHint != null) {
                LOG.error("Error Reading Locale for " + locationHint);
            } else {
                LOG.error("Error trying to read locale.");
            }
        }
        return siteDefaultLocale;
    }

    public static long getDefaultCompanyId(){
        return PortalUtil.getDefaultCompanyId();
    }


}
