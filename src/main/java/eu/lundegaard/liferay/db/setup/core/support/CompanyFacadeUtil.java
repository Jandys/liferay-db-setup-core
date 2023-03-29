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

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import eu.lundegaard.liferay.db.setup.LiferaySetup;

/**
 * @author Jakub Jandák, jakub.jandak@lundegaard.eu, 2023
 */

public class CompanyFacadeUtil {

    private static final Log LOG = LogFactoryUtil.getLog(LiferaySetup.class);

    public CompanyFacadeUtil() {}

    /**
     * Method is used to get value of companyId from configured company You can get
     * such value by configuring:
     * <ul>
     * <li>companyId</li>
     * <li>useCompanyMx</li>
     * <li>companyWebId</li>
     * <li>virtualHost</li>
     * <li>logoId</li>
     * </ul>
     *
     * @param company structure defined inside XSD
     * @return company ID
     * @throws PortalException is thrown if while fetching company error occures
     */
    public static long getCompanyId(eu.lundegaard.liferay.db.setup.domain.Company company) throws PortalException {
        if (company == null) {
            return PortalUtilFacade.getDefaultCompanyId();
        }

        String configuredValue = company.getValue();
        if (company.isUseCompanyWebId()) {
            Company companyByWebId = CompanyLocalServiceUtil.getCompanyByWebId(configuredValue);
            if (companyByWebId != null) {
                return companyByWebId.getCompanyId();
            }
        }

        if (company.isUseCompanyMx()) {
            Company companyByMx = CompanyLocalServiceUtil.getCompanyByMx(configuredValue);
            if (companyByMx != null) {
                return companyByMx.getCompanyId();
            }
        }

        if (company.isUseVirtualHost()) {
            Company companyByVirtualHost = CompanyLocalServiceUtil.getCompanyByVirtualHost(configuredValue);
            if (companyByVirtualHost != null) {
                return companyByVirtualHost.getCompanyId();
            }
        }

        try {
            long parsedConfiguredValue = Long.parseLong(configuredValue);

            if (company.isUseLogoId()) {
                Company companyByLogoId = CompanyLocalServiceUtil.getCompanyByLogoId(parsedConfiguredValue);
                if (companyByLogoId != null) {
                    return companyByLogoId.getCompanyId();
                }
            }

            Company companyById = CompanyLocalServiceUtil.getCompanyById(parsedConfiguredValue);

            if (companyById != null) {
                return companyById.getCompanyId();
            }
        } catch (NumberFormatException e) {
            LOG.error("Configured value for company could not be parsed.");
        }

        // if everything fails return default company
        return PortalUtilFacade.getDefaultCompanyId();
    }
}
