/*
 * Copyright (C) Lundegaard a.s. 2023 - All Rights Reserved
 * Proprietary and confidential. Unauthorized copying of this file, via any
 * medium is strictly prohibited.
 */

package eu.lundegaard.liferay.db.setup.util;

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.model.impl.CompanyImpl;

/**
 * @author Jakub Jandák, jakub.jandak@lundegaard.eu, 2023
 */

public class ModelCreator {

    public static Company getCompany(long companyId){
        CompanyImpl company = new CompanyImpl();
        company.setCompanyId(companyId);
        return company;
    }

}
