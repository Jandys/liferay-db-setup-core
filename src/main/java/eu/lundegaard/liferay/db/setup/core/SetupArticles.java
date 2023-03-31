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

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetLinkConstants;
import com.liferay.asset.kernel.service.AssetEntryLocalServiceUtil;
import com.liferay.asset.kernel.service.AssetLinkLocalServiceUtil;
import com.liferay.dynamic.data.lists.model.DDLRecordSet;
import com.liferay.dynamic.data.lists.service.DDLRecordSetLocalServiceUtil;
import com.liferay.dynamic.data.mapping.exception.StructureDuplicateStructureKeyException;
import com.liferay.dynamic.data.mapping.exception.TemplateDuplicateTemplateKeyException;
import com.liferay.dynamic.data.mapping.model.*;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLocalServiceUtil;
import com.liferay.dynamic.data.mapping.util.DDMUtil;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalArticleConstants;
import com.liferay.journal.model.JournalFolder;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.search.DocumentImpl;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.*;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.xml.ElementImpl;
import com.liferay.portlet.display.template.PortletDisplayTemplate;
import eu.lundegaard.liferay.db.setup.LiferaySetup;
import eu.lundegaard.liferay.db.setup.core.support.ClassNameLocalServiceUtilWrapper;
import eu.lundegaard.liferay.db.setup.core.support.PortalUtilFacade;
import eu.lundegaard.liferay.db.setup.core.util.ResolverUtil;
import eu.lundegaard.liferay.db.setup.core.util.ResourcesUtil;
import eu.lundegaard.liferay.db.setup.core.util.StringPool;
import eu.lundegaard.liferay.db.setup.core.util.TaggingUtil;
import eu.lundegaard.liferay.db.setup.core.util.FieldMapUtil;
import eu.lundegaard.liferay.db.setup.core.util.WebFolderUtil;
import eu.lundegaard.liferay.db.setup.domain.*;
import org.dom4j.tree.DefaultText;
import org.dom4j.util.IndexedElement;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by mapa, guno, Updated by: Jakub Jandak, jakub.jandak@lundegaard.eu,
 * 2023
 */
public final class SetupArticles {

    private static final Log LOG = LogFactoryUtil.getLog(SetupArticles.class);
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS;
    private static final HashMap<String, List<String>> DEFAULT_DDM_PERMISSIONS;
    private static final int ARTICLE_PUBLISH_YEAR = 2008;
    private static final int MIN_DISPLAY_ROWS = 10;

    static {
        DEFAULT_PERMISSIONS = new HashMap<String, List<String>>();
        DEFAULT_DDM_PERMISSIONS = new HashMap<String, List<String>>();
        List<String> actionsOwner = new ArrayList<String>();

        actionsOwner.add(ActionKeys.VIEW);
        actionsOwner.add(ActionKeys.ADD_DISCUSSION);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.DELETE_DISCUSSION);
        actionsOwner.add(ActionKeys.EXPIRE);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.UPDATE_DISCUSSION);

        List<String> ddmActionsOwner = new ArrayList<String>();

        ddmActionsOwner.add(ActionKeys.VIEW);
        ddmActionsOwner.add(ActionKeys.DELETE);
        ddmActionsOwner.add(ActionKeys.UPDATE);
        ddmActionsOwner.add(ActionKeys.PERMISSIONS);


        DEFAULT_PERMISSIONS.put(RoleConstants.OWNER, actionsOwner);
        DEFAULT_DDM_PERMISSIONS.put(RoleConstants.OWNER, ddmActionsOwner);

        List<String> actionsUser = new ArrayList<String>();
        actionsUser.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.USER, actionsUser);
        DEFAULT_DDM_PERMISSIONS.put(RoleConstants.USER, actionsUser);

        List<String> actionsGuest = new ArrayList<String>();
        actionsGuest.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS.put(RoleConstants.GUEST, actionsGuest);
        DEFAULT_DDM_PERMISSIONS.put(RoleConstants.GUEST, actionsGuest);
    }

    private SetupArticles() {

    }

    /**
     * This method is used to call setup actions on the {@link Site} object and
     * groupId, companyId. It classifies into:
     * <ul>
     * <li>Article Structure</li>
     * <li>DDL Structure</li>
     * <li>Article Template</li>
     * </ul>
     * <p>
     * If any exception occurs, it logs the error.
     *
     * @param site the Site object containing the structures and templates to be
     *        added
     * @param groupId the group id where the structures and templates should be
     *        added
     * @param companyId the company id of the group
     * @throws PortalException if an error occurs while adding the structures or
     *         templates
     */
    public static void setupSiteStructuresAndTemplates(final Site site, final long groupId, final long companyId)
            throws PortalException {
        List<Structure> articleStructures = site.getArticleStructure();

        if (articleStructures != null) {
            long classNameId = ClassNameLocalServiceUtilWrapper.getClassNameId(JournalArticle.class);
            for (Structure structure : articleStructures) {
                try {
                    addDDMStructure(structure, groupId, classNameId, companyId);

                } catch (StructureDuplicateStructureKeyException | IOException
                        | URISyntaxException e) {
                    LOG.error(e);
                }
            }
        }

        List<Structure> ddlStructures = site.getDdlStructure();

        if (articleStructures != null) {
            long classNameId = ClassNameLocalServiceUtilWrapper.getClassNameId(DDLRecordSet.class);
            for (Structure structure : ddlStructures) {
                LOG.info("Adding DDL structure " + structure.getName());
                try {
                    addDDMStructure(structure, groupId, classNameId, companyId);
                } catch (StructureDuplicateStructureKeyException | IOException
                        | URISyntaxException e) {
                    LOG.error(e);
                }
            }
        }

        List<ArticleTemplate> articleTemplates = site.getArticleTemplate();
        if (articleTemplates != null) {
            for (ArticleTemplate template : articleTemplates) {
                try {
                    addDDMTemplate(template, groupId, companyId);
                } catch (TemplateDuplicateTemplateKeyException | IOException
                        | URISyntaxException e) {
                    LOG.error(e);
                }
            }
        }
    }

    public static void setupSiteArticles(final Site site,
            final long groupId, final long companyId) throws PortalException, SystemException {

        List<Article> articles = site.getArticle();
        if (articles != null) {
            for (Article article : articles) {
                addJournalArticle(article, groupId, companyId);
            }
        }
        List<Adt> adts = site.getAdt();
        if (adts != null) {
            for (Adt template : adts) {
                try {
                    addDDMTemplate(template, groupId, companyId);
                } catch (TemplateDuplicateTemplateKeyException | URISyntaxException
                        | IOException e) {
                    LOG.error("Error in adding ADT: " + template.getName(), e);
                }
            }
        }
        List<DdlRecordset> recordSets = site.getDdlRecordset();
        if (recordSets != null) {
            for (DdlRecordset recordSet : recordSets) {
                try {
                    addDDLRecordSet(recordSet, groupId, companyId);
                } catch (TemplateDuplicateTemplateKeyException e) {
                    LOG.error("Error in adding DDLRecordSet: " + recordSet.getName(), e);
                }
            }
        }
    }

    /**
     * The method addDDMStructure adds a new structure to the Liferay portal based
     * on the provided structure information. The structure must be in JSON format
     * and it is expected to have certain attributes such as name, key, parent, etc.
     * The method will also set up the permissions for the structure based on the
     * rolePermissions provided in the structure.
     *
     * @param structure the structure to be added
     * @param groupId the groupId to which the structure will belong
     * @param classNameId the classNameId of the structure, this is typically the
     *        class name of the object associated with the structure
     * @param companyId the companyId to which the structure will belong
     * @throws SystemException thrown when there is a system error
     * @throws PortalException thrown when there is a portal error
     * @throws IOException thrown when there is an error reading the structure file
     * @throws URISyntaxException thrown when there is an error with the URI syntax
     */
    public static void addDDMStructure(final Structure structure, final long groupId,
            final long classNameId, final long companyId)
            throws SystemException, PortalException, IOException, URISyntaxException {

        LOG.info("Adding Article structure " + structure.getName());
        Map<Locale, String> nameMap = new HashMap<>();
        Locale siteDefaultLocale = PortalUtilFacade.getDefaultLocale(groupId);
        String name = getStructureNameOrKey(structure);
        nameMap.put(siteDefaultLocale, name);
        Map<Locale, String> descMap = new HashMap<>();

        String content = null;
        DDMForm ddmForm = null;
        DDMFormLayout ddmFormLayout = null;
        try {
            content = ResourcesUtil.getFileContent(structure.getPath());
            ddmForm = DDMUtil.getDDMForm(content);
            if (ddmForm == null) {
                LOG.error("Can not parse given structure JSON content into Liferay DDMForm.");
                return;
            }
            ddmFormLayout = DDMUtil.getDefaultDDMFormLayout(ddmForm);
        } catch (IOException e) {
            LOG.error("Error Reading Structure File content for: " + structure.getName());
            return;
        } catch (PortalException e) {
            LOG.error("Can not parse given structure JSON content into Liferay DDMForm.", e);
            return;
        } catch (Exception e) {
            LOG.error("Other error while trying to get content of the structure file. Possibly wrong filesystem path ("
                    + structure.getPath() + ")?", e);
            return;
        }

        Locale contentDefaultLocale = ddmForm.getDefaultLocale();
        if (!contentDefaultLocale.equals(siteDefaultLocale)) {
            nameMap.put(contentDefaultLocale, name);
        }

        DDMStructure ddmStructure = null;
        try {
            ddmStructure = DDMStructureLocalServiceUtil.fetchStructure(groupId, classNameId,
                    structure.getKey());
        } catch (SystemException e) {
            LOG.error("Error while trying to find Structure with key: " + structure.getKey(), e);
        }

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(companyId);

        if (ddmStructure != null) {
            LOG.info("Structure already exists and will be overwritten.");
            if (structure.getParent() != null && !structure.getParent().isEmpty()) {
                LOG.info("Setting up parent structure: " + structure.getName());
                DDMStructure parentStructure = DDMStructureLocalServiceUtil.fetchStructure(groupId,
                        classNameId, structure.getParent(), true);
                if (parentStructure != null) {
                    ddmStructure.setParentStructureId(parentStructure.getStructureId());
                } else {
                    LOG.info("Parent structure not found: " + structure.getName());
                }
            }


            DDMStructure ddmStructureSaved = DDMStructureLocalServiceUtil.updateStructure(LiferaySetup.getRunAsUserId(),
                    ddmStructure.getStructureId(),
                    ddmStructure.getParentStructureId(), nameMap, descMap, ddmForm, ddmFormLayout, serviceContext);
            LOG.info("Template successfully updated: " + structure.getName());

            SetupPermissions.updatePermission("Structure " + structure.getKey(), groupId, companyId,
                    ddmStructureSaved.getStructureId(),
                    DDMStructure.class.getName() + "-" + JournalArticle.class.getName(), structure.getRolePermissions(),
                    DEFAULT_DDM_PERMISSIONS);

            return;
        }

        DDMStructure newStructure = DDMStructureLocalServiceUtil.addStructure(
                LiferaySetup.getRunAsUserId(), groupId, structure.getParent(), classNameId,
                structure.getKey(), nameMap, descMap, ddmForm, ddmFormLayout, "json", 0, serviceContext);

        SetupPermissions.updatePermission("Structure " + structure.getKey(), groupId, companyId,
                newStructure.getStructureId(), DDMStructure.class.getName() + "-" + JournalArticle.class.getName(),
                structure.getRolePermissions(), DEFAULT_DDM_PERMISSIONS);
        LOG.info("Added Article structure: " + newStructure.getName());
    }

    private static String getStructureNameOrKey(final Structure structure) {
        if (structure.getName() == null) {
            return structure.getName();
        }
        return structure.getKey();
    }

    /**
     * This method adds a DDMTemplate to the Liferay portal
     *
     * @param template The article template to be added
     * @param groupId The groupId where the template will be added
     * @throws SystemException if a system exception occurred
     * @throws PortalException if a portal exception occurred
     * @throws IOException if an IO exception occurred
     * @throws URISyntaxException if URI syntax exception occurred
     */
    public static void addDDMTemplate(final ArticleTemplate template, final long groupId, long companyId)
            throws SystemException, PortalException, IOException, URISyntaxException {

        LOG.info("Adding Article template " + template.getName());
        long classNameId = ClassNameLocalServiceUtilWrapper.getClassNameId(DDMStructure.class);
        long resourceClassnameId = ClassNameLocalServiceUtilWrapper.getClassNameId(JournalArticle.class);
        Map<Locale, String> nameMap = new HashMap<>();
        Locale siteDefaultLocale = PortalUtilFacade.getDefaultLocale(groupId);
        String name = template.getName();
        if (name == null) {
            name = template.getKey();
        }
        nameMap.put(siteDefaultLocale, name);
        Map<Locale, String> descMap = new HashMap<>();

        String script;
        try {
            script = ResourcesUtil.getFileContent(template.getPath());
        } catch (Exception e) {
            LOG.error("Error Reading Template File content for: " + template.getName());
            return;
        }

        long classPK = 0;
        if (template.getArticleStructureKey() != null) {
            try {
                classPK = ResolverUtil
                        .getStructureId(template.getArticleStructureKey(), groupId, JournalArticle.class, false);
            } catch (Exception e) {
                LOG.error("Given article structure with ID: " + template.getArticleStructureKey()
                        + " can not be found. Therefore, article template can not be added/changed.", e);
                return;
            }
        }

        DDMTemplate ddmTemplate = null;
        try {
            ddmTemplate = DDMTemplateLocalServiceUtil.fetchTemplate(groupId, classNameId, template.getKey());
        } catch (SystemException e) {
            LOG.error("Error while trying to find template with key: " + template.getKey(), e);
        }

        if (ddmTemplate != null) {
            LOG.info("Template already exists and will be overwritten.");
            ddmTemplate.setNameMap(nameMap);
            ddmTemplate.setLanguage(template.getLanguage());
            ddmTemplate.setScript(script);
            ddmTemplate.setClassPK(classPK);
            ddmTemplate.setCacheable(template.isCacheable());

            DDMTemplateLocalServiceUtil.updateDDMTemplate(ddmTemplate);
            LOG.info("Template successfully updated: " + ddmTemplate.getName());
            return;
        }

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(companyId);

        DDMTemplate newTemplate = DDMTemplateLocalServiceUtil.addTemplate(
                LiferaySetup.getRunAsUserId(), groupId, classNameId, classPK, resourceClassnameId, template.getKey(),
                nameMap, descMap, DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY, null, template.getLanguage(), script,
                template.isCacheable(), false,
                null, null, serviceContext);
        LOG.info("Added Article template: " + newTemplate.getName());
    }

    /**
     * This method adds an ADT (Asset Display Template) to Liferay portal.
     *
     * @param template an {@link Adt} object representing the ADT to be added
     * @param groupId the ID of the group to which the ADT should be added
     * @throws SystemException if there is a problem accessing Liferay's template
     *         services
     * @throws PortalException if there is a problem with the group or template
     *         information
     * @throws IOException if there is a problem reading the ADT's script file
     * @throws URISyntaxException if there is a problem with the file path of the
     *         ADT's script
     */
    public static void addDDMTemplate(final Adt template, final long groupId, long companyId)
            throws SystemException, PortalException, IOException, URISyntaxException {

        LOG.info("Adding ADT " + template.getName());
        long classNameId = PortalUtil.getClassNameId(template.getClassName());

        long resourceClassnameId = Validator.isBlank(template.getResourceClassName())
                ? ClassNameLocalServiceUtilWrapper.getClassNameId(PortletDisplayTemplate.class)
                : ClassNameLocalServiceUtilWrapper.getClassNameId(template.getResourceClassName());

        Map<Locale, String> nameMap = new HashMap<Locale, String>();

        Locale siteDefaultLocale = PortalUtilFacade.getDefaultLocale(groupId);
        String name = template.getName();
        if (name == null) {
            name = template.getTemplateKey();
        }
        nameMap.put(siteDefaultLocale, name);

        Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
        descriptionMap.put(siteDefaultLocale, template.getDescription());

        DDMTemplate ddmTemplate = null;
        try {
            ddmTemplate = DDMTemplateLocalServiceUtil.fetchTemplate(groupId, classNameId,
                    template.getTemplateKey(), true);
        } catch (SystemException e) {
            LOG.error("Error while trying to find ADT with key: " + template.getTemplateKey());
        }

        String script = ResourcesUtil.getFileContent(template.getPath());

        if (ddmTemplate != null) {
            LOG.info("Template already exists and will be overwritten.");
            ddmTemplate.setLanguage(template.getLanguage());
            ddmTemplate.setNameMap(nameMap);
            ddmTemplate.setDescriptionMap(descriptionMap);
            ddmTemplate.setClassName(template.getClassName());
            ddmTemplate.setCacheable(template.isCacheable());
            ddmTemplate.setScript(script);

            DDMTemplateLocalServiceUtil.updateDDMTemplate(ddmTemplate);
            LOG.info("ADT successfully updated: " + ddmTemplate.getName());
            return;
        }


        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(companyId);


        DDMTemplate newTemplate = DDMTemplateLocalServiceUtil.addTemplate(
                LiferaySetup.getRunAsUserId(), groupId, classNameId, 0, resourceClassnameId, template.getTemplateKey(),
                nameMap, descriptionMap, DDMTemplateConstants.TEMPLATE_TYPE_DISPLAY, null, template.getLanguage(),
                script, true, false,
                null, null, serviceContext);
        LOG.info("Added ADT: " + newTemplate.getName());
    }

    /**
     * This method creates a journal article in Liferay. The method reads the
     * content of the article from the file specified in the path attribute of the
     * Article. And creates article with such content.
     *
     * @param article object that contains information about the article such as its
     *        title, path, and structure key.
     * @param groupId specifies the group the article belongs to.
     * @param companyId that specifies the company the article belongs to.
     */
    public static void addJournalArticle(final Article article, final long groupId,
            final long companyId) {
        LOG.info("Adding Journal Article " + article.getTitle());

        String content = null;
        long folderId = 0L;
        if (article.getArticleFolderPath() != null && !article.getArticleFolderPath().equals("")) {
            JournalFolder jf = WebFolderUtil.findWebFolder(companyId, groupId,
                    LiferaySetup.getRunAsUserId(), article.getArticleFolderPath(), "", true);
            if (jf == null) {
                LOG.warn("Specified webfolder " + article.getArticleFolderPath() + " of article "
                        + article.getTitle()
                        + " not found! Will put article into web content root folder!");
            } else {
                folderId = jf.getFolderId();
            }
        }
        try {
            content = ResourcesUtil.getFileContent(article.getPath());
            content = ResolverUtil.lookupAll(LiferaySetup.getRunAsUserId(), groupId, companyId,
                    content, article.getPath());
        } catch (IOException e) {
            LOG.error(
                    "Error Reading Article File content for article ID: " + article.getArticleId());
        }
        Map<Locale, String> titleMap = FieldMapUtil.getTitleMap(article.getTitleTranslation(),
                groupId, article.getTitle(), " Article with title " + article.getArticleId());

        Locale articleDefaultLocale = LocaleUtil.fromLanguageId(
                LocalizationUtil.getDefaultLanguageId(content));
        if (!titleMap.containsKey(articleDefaultLocale)) {
            titleMap.put(articleDefaultLocale, article.getTitle());
        }


        Map<Locale, String> descriptionMap = null;
        if (article.getArticleDescription() != null && !article.getArticleDescription().isEmpty()) {
            descriptionMap = FieldMapUtil.getDescriptionMap(article.getDescriptionTranslation(), groupId,
                    article.getArticleDescription(), " Article with description " +
                            article.getArticleId());
            if (!descriptionMap.containsKey(articleDefaultLocale)) {
                descriptionMap.put(articleDefaultLocale, article.getArticleDescription());
            }
        }
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(companyId);
        serviceContext.setScopeGroupId(groupId);

        JournalArticle journalArticle = null;

        boolean generatedId = (article.getArticleId().isEmpty());
        if (generatedId) {
            LOG.info("Article " + article.getTitle() + " will have autogenerated ID.");
        } else {
            try {
                journalArticle = JournalArticleLocalServiceUtil.fetchLatestArticle(groupId,
                        article.getArticleId(), WorkflowConstants.STATUS_APPROVED);
            } catch (SystemException e) {
                LOG.error("Error while trying to find article with ID: " + article.getArticleId(),
                        e);
            }
        }

        try {
            if (journalArticle == null) {
                journalArticle = JournalArticleLocalServiceUtil.addArticle(
                        LiferaySetup.getRunAsUserId(), groupId, folderId, 0, 0,
                        article.getArticleId(), generatedId,
                        JournalArticleConstants.VERSION_DEFAULT, titleMap, descriptionMap, content,
                        article.getArticleStructureKey(), article.getArticleTemplateKey(),
                        StringPool.BLANK, 1, 1, ARTICLE_PUBLISH_YEAR, 0, 0, 0, 0, 0, 0, 0, true, 0,
                        0, 0, 0, 0, true, true, false, StringPool.BLANK, null, null,
                        StringPool.BLANK, serviceContext);

                LOG.info("Added JournalArticle " + journalArticle.getTitle() + " with ID: "
                        + journalArticle.getArticleId());
                Indexer bi = IndexerRegistryUtil.getIndexer(JournalArticle.class);
                if (bi != null) {
                    bi.reindex(journalArticle);
                }
            } else {
                LOG.info("Article " + article.getTitle() + " with article ID: "
                        + article.getArticleId() + " already exists. Will be overwritten.");
                journalArticle.setTitleMap(titleMap);
                Document document = (Document) new DocumentImpl();
                org.dom4j.Element domElement = new IndexedElement("content");
                domElement.add(new DefaultText(content));
                Element element = new ElementImpl(domElement);
                document.add(element);
                journalArticle.setDocument(document);
                journalArticle.setDescriptionMap(descriptionMap);

                JournalArticleLocalServiceUtil.updateJournalArticle(journalArticle);

                // if the folder changed, move it...
                if (journalArticle.getFolderId() != folderId) {
                    JournalArticleLocalServiceUtil.moveArticle(groupId,
                            journalArticle.getArticleId(), folderId, serviceContext);
                }
                LOG.info("Updated JournalArticle: " + journalArticle.getTitle());
            }
            TaggingUtil.associateTagsAndCategories(groupId, article, journalArticle);
            processRelatedAssets(article, journalArticle, LiferaySetup.getRunAsUserId(), groupId,
                    companyId);

            SetupPermissions.updatePermission("Article " + journalArticle.getArticleId(), groupId,
                    companyId, journalArticle.getResourcePrimKey(), JournalArticle.class,
                    article.getRolePermissions(), DEFAULT_PERMISSIONS);
        } catch (PortalException | SystemException e) {
            LOG.error("Error while trying to add/update Article with Title: " + article.getTitle(),
                    e);
        }
    }

    /**
     * This method adds a DDL Record Set to the specified groupId. If a DDL Record
     * Set with the same key already exists, it will be overwritten.
     *
     * @param recordSet The DDL Record Set to add
     * @param groupId The groupId to add the DDL Record Set to
     * @throws SystemException if a system exception occurred
     * @throws PortalException if a portal exception occurred
     */
    private static void addDDLRecordSet(final DdlRecordset recordSet, final long groupId, final long companyId)
            throws SystemException, PortalException {
        LOG.info("Adding DDLRecordSet " + recordSet.getName());
        Map<Locale, String> nameMap = new HashMap<>();
        Locale siteDefaultLocale = PortalUtilFacade.getDefaultLocale(groupId);
        nameMap.put(siteDefaultLocale, recordSet.getName());
        Map<Locale, String> descMap = new HashMap<>();
        descMap.put(siteDefaultLocale, recordSet.getDescription());
        DDLRecordSet ddlRecordSet = null;
        try {
            ddlRecordSet = DDLRecordSetLocalServiceUtil.fetchRecordSet(groupId, recordSet.getKey());
        } catch (SystemException e) {
            LOG.error("Error while trying to find DDLRecordSet with key: " + recordSet.getKey(), e);
        }

        if (ddlRecordSet != null) {
            LOG.info("DDLRecordSet already exists and will be overwritten.");
            ddlRecordSet.setNameMap(nameMap);
            ddlRecordSet.setDescriptionMap(descMap);
            ddlRecordSet.setDDMStructureId(ResolverUtil
                    .getStructureId(recordSet.getDdlStructureKey(), groupId, DDLRecordSet.class, false));
            DDLRecordSetLocalServiceUtil.updateDDLRecordSet(ddlRecordSet);
            LOG.info("DDLRecordSet successfully updated: " + recordSet.getName());
            return;
        }


        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setCompanyId(companyId);


        DDLRecordSet newDDLRecordSet = DDLRecordSetLocalServiceUtil.addRecordSet(
                LiferaySetup.getRunAsUserId(), groupId,
                ResolverUtil.getStructureId(recordSet.getDdlStructureKey(), groupId,
                        DDLRecordSet.class, false),
                recordSet.getDdlStructureKey(), nameMap, descMap, MIN_DISPLAY_ROWS, 0,
                serviceContext);
        LOG.info("Added DDLRecordSet: " + newDDLRecordSet.getName());
    }

    /**
     * This method process the related assets for a Journal Article. It adds or
     * clears the related assets specified in the Article object.
     *
     * @param article the Article object that contains the related assets
     *        information
     * @param ja the JournalArticle object that the related assets will be added or
     *        cleared from
     * @param runAsUserId the user id used to perform the action
     * @param groupId the id of the group the assets belong to
     * @param companyId the id of the company the assets belong to
     */
    public static void processRelatedAssets(final Article article, final JournalArticle ja,
            final long runAsUserId, final long groupId, final long companyId) {
        if (article.getRelatedAssets() != null) {
            RelatedAssets ras = article.getRelatedAssets();
            AssetEntry ae = null;
            if (ras.isClearAllAssets()) {

                try {
                    ae = AssetEntryLocalServiceUtil.getEntry(JournalArticle.class.getName(),
                            ja.getResourcePrimKey());
                    AssetLinkLocalServiceUtil.deleteLinks(ae.getEntryId());
                } catch (PortalException | SystemException e) {
                    LOG.error("Problem clearing related assets of article " + ja.getArticleId(), e);
                }
            }
            if (ras.getRelatedAsset() != null && !ras.getRelatedAsset().isEmpty()) {
                List<RelatedAsset> ra = ras.getRelatedAsset();
                for (RelatedAsset r : ra) {
                    String clazz = r.getAssetClass();
                    String clazzPrimKey = r.getAssetClassPrimaryKey();
                    String resolverHint = "Related asset for article " + ja.getArticleId() + " "
                            + "clazz " + clazz + ", " + "primary key " + clazzPrimKey;
                    clazzPrimKey = ResolverUtil.lookupAll(runAsUserId, groupId, companyId,
                            clazzPrimKey, resolverHint);

                    long id = 0;
                    try {
                        id = Long.parseLong(clazzPrimKey);
                    } catch (Exception ex) {
                        LOG.error("Class primary key is not parseable as long value.", ex);
                    }

                    try {

                        AssetEntry ae2 = AssetEntryLocalServiceUtil.getEntry(clazz, id);
                        AssetLinkLocalServiceUtil.addLink(runAsUserId, ae.getEntryId(),
                                ae2.getEntryId(), AssetLinkConstants.TYPE_RELATED, 1);
                    } catch (PortalException | SystemException e) {
                        LOG.error(
                                "Problem resolving related asset of article " + ja.getArticleId()
                                        + " with clazz " + clazz + " primary key " + clazzPrimKey,
                                e);
                    }

                }
            }

        }
    }

}
