/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 Lundegaard a.s.
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



import java.util.ArrayList;
import java.util.List;
import com.liferay.dynamic.data.lists.model.DDLRecordSet;
import com.liferay.journal.model.JournalArticle;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import eu.lundegaard.liferay.db.setup.domain.ArticleTemplate;
import eu.lundegaard.liferay.db.setup.domain.Site;
import eu.lundegaard.liferay.db.setup.domain.Structure;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

class SetupArticlesTest {

    private Site site;
    private long groupId = 12345;
    private long companyId = 54321;

    @Before
    public void setUp() {
        // Create mock instances of the required classes
        site = Mockito.mock(Site.class);
        JournalArticle journalArticle = Mockito.mock(JournalArticle.class);
        DDLRecordSet ddlRecordSet = Mockito.mock(DDLRecordSet.class);

        // Set up the mock article structures
        Structure articleStructure1 = new Structure();
        articleStructure1.setName("Article Structure 1");
        Structure articleStructure2 = new Structure();
        articleStructure2.setName("Article Structure 2");
        List<Structure> articleStructures = new ArrayList<>();
        articleStructures.add(articleStructure1);
        articleStructures.add(articleStructure2);
        Mockito.when(site.getArticleStructure()).thenReturn(articleStructures);

        // Set up the mock DDL structures
        Structure ddlStructure1 = new Structure();
        ddlStructure1.setName("DDL Structure 1");
        Structure ddlStructure2 = new Structure();
        ddlStructure2.setName("DDL Structure 2");
        List<Structure> ddlStructures = new ArrayList<>();
        ddlStructures.add(ddlStructure1);
        ddlStructures.add(ddlStructure2);
        Mockito.when(site.getDdlStructure()).thenReturn(ddlStructures);

        // Set up the mock article templates
        ArticleTemplate articleTemplate1 = new ArticleTemplate();
        articleTemplate1.setName("Article Template 1");
        ArticleTemplate articleTemplate2 = new ArticleTemplate();
        articleTemplate2.setName("Article Template 2");
        List<ArticleTemplate> articleTemplates = new ArrayList<>();
        articleTemplates.add(articleTemplate1);
        articleTemplates.add(articleTemplate2);
        Mockito.when(site.getArticleTemplate()).thenReturn(articleTemplates);

        // Set up the mock class name ids
        Mockito.when(ClassNameLocalServiceUtil.getClassNameId(JournalArticle.class)).thenReturn(1L);
        Mockito.when(ClassNameLocalServiceUtil.getClassNameId(DDLRecordSet.class)).thenReturn(2L);
    }

    @Test
    public void testSetupSiteStructuresAndTemplates() throws PortalException {
        SetupArticles.setupSiteStructuresAndTemplates(site,groupId,companyId);

//        Mockito.verify();
    }

    public void testSetupSiteArticles() {
    }

    public void testAddDDMStructure() {
    }

    public void testAddDDMTemplate() {
    }

    public void testTestAddDDMTemplate() {
    }

    public void testAddJournalArticle() {
    }

    public void testProcessRelatedAssets() {
    }
}
