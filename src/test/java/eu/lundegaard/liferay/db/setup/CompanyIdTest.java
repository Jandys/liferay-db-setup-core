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
package eu.lundegaard.liferay.db.setup;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import eu.lundegaard.liferay.db.setup.core.support.CompanyFacadeUtil;
import eu.lundegaard.liferay.db.setup.core.support.PortalUtilFacade;
import eu.lundegaard.liferay.db.setup.domain.Company;
import eu.lundegaard.liferay.db.setup.util.ModelCreator;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Jakub Jandák, jakub.jandak@lundegaard.eu, 2023
 */

public class CompanyIdTest {

    @Test
    public void nullCompany() {
        long defaultCompany;
        try (MockedStatic<PortalUtilFacade> portalUtilFacade = Mockito.mockStatic(PortalUtilFacade.class)) {
            //prepare
            portalUtilFacade.when(PortalUtilFacade::getDefaultCompanyId).thenReturn(123L);

            //act
            defaultCompany = CompanyFacadeUtil.getCompanyId(null);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(123L, defaultCompany);
    }

    @Test
    public void companyIdById() {
        long defaultCompany;
        Company company = new Company();
        company.setCompanyId("1234");

        com.liferay.portal.kernel.model.Company liferayCompany = ModelCreator.getCompany(0L);

        try (MockedStatic<CompanyLocalServiceUtil> localServiceUtilMockedStatic = Mockito.mockStatic(
                CompanyLocalServiceUtil.class)) {
            //prepare
            localServiceUtilMockedStatic.when((MockedStatic.Verification) CompanyLocalServiceUtil.getCompanyById(1234L))
                    .thenReturn(liferayCompany);

            //act
            defaultCompany = CompanyFacadeUtil.getCompanyId(company);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(0L, defaultCompany);
    }

    @Test
    public void companyIdByWebId() {
        long defaultCompany;
        Company company = new Company();
        company.setCompanyWebId("test.com");

        com.liferay.portal.kernel.model.Company liferayCompany = ModelCreator.getCompany(21L);

        try (MockedStatic<CompanyLocalServiceUtil> localServiceUtilMockedStatic = Mockito.mockStatic(
                CompanyLocalServiceUtil.class)) {
            //prepare
            localServiceUtilMockedStatic.when(
                    (MockedStatic.Verification) CompanyLocalServiceUtil.getCompanyByWebId("test.com"))
                    .thenReturn(liferayCompany);

            //act
            defaultCompany = CompanyFacadeUtil.getCompanyId(company);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(21L, defaultCompany);
    }

    @Test
    public void companyIdByCompanyMx() {
        long defaultCompany;
        Company company = new Company();
        company.setCompanyMx("companyMX");

        com.liferay.portal.kernel.model.Company liferayCompany = ModelCreator.getCompany(21L);

        try (MockedStatic<CompanyLocalServiceUtil> localServiceUtilMockedStatic = Mockito.mockStatic(
                CompanyLocalServiceUtil.class)) {
            //prepare
            localServiceUtilMockedStatic.when(
                    (MockedStatic.Verification) CompanyLocalServiceUtil.getCompanyByMx("companyMX"))
                    .thenReturn(liferayCompany);

            //act
            defaultCompany = CompanyFacadeUtil.getCompanyId(company);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(21L, defaultCompany);
    }

    @Test
    public void companyIdByVirtualHost() {
        long defaultCompany;
        Company company = new Company();
        company.setVirtualHost("virtual-host-name");

        com.liferay.portal.kernel.model.Company liferayCompany = ModelCreator.getCompany(21L);

        try (MockedStatic<CompanyLocalServiceUtil> localServiceUtilMockedStatic = Mockito.mockStatic(
                CompanyLocalServiceUtil.class)) {
            //prepare
            localServiceUtilMockedStatic.when(
                    (MockedStatic.Verification) CompanyLocalServiceUtil.getCompanyByVirtualHost("virtual-host-name"))
                    .thenReturn(liferayCompany);

            //act
            defaultCompany = CompanyFacadeUtil.getCompanyId(company);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(21L, defaultCompany);
    }

    @Test
    public void companyIdByLogoId() {
        long defaultCompany;
        Company company = new Company();
        company.setLogoId("1234");

        com.liferay.portal.kernel.model.Company liferayCompany = ModelCreator.getCompany(21L);

        try (MockedStatic<CompanyLocalServiceUtil> localServiceUtilMockedStatic = Mockito.mockStatic(
                CompanyLocalServiceUtil.class)) {
            //prepare
            localServiceUtilMockedStatic.when(
                    (MockedStatic.Verification) CompanyLocalServiceUtil.getCompanyByLogoId(1234L))
                    .thenReturn(liferayCompany);

            //act
            defaultCompany = CompanyFacadeUtil.getCompanyId(company);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(21L, defaultCompany);
    }


    @Test
    public void companyIdWhenNumberParsingFails() {
        long defaultCompany;
        Company company = new Company();
        company.setValue("test");

        try (MockedStatic<PortalUtilFacade> portalUtilFacade = Mockito.mockStatic(PortalUtilFacade.class)) {
            //prepare
            portalUtilFacade.when(PortalUtilFacade::getDefaultCompanyId).thenReturn(123L);

            //act
            defaultCompany = CompanyFacadeUtil.getCompanyId(company);
        } catch (PortalException e) {
            throw new RuntimeException(e);
        }

        //assert
        Assertions.assertEquals(123L, defaultCompany);
    }
}
