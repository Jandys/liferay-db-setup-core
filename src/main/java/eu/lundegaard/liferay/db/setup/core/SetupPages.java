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

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.NoSuchLayoutException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.*;
import com.liferay.portal.kernel.portlet.PortletIdCodec;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.*;
import com.liferay.portal.kernel.service.permission.PortletPermissionUtil;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.PortletKeys;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import eu.lundegaard.liferay.db.setup.LiferaySetup;
import eu.lundegaard.liferay.db.setup.core.util.CustomFieldSettingUtil;
import eu.lundegaard.liferay.db.setup.core.util.ResolverUtil;
import eu.lundegaard.liferay.db.setup.core.util.StringPool;
import eu.lundegaard.liferay.db.setup.core.util.FieldMapUtil;
import eu.lundegaard.liferay.db.setup.domain.*;
import eu.lundegaard.liferay.db.setup.domain.Theme;
import javax.portlet.ReadOnlyException;
import javax.portlet.ValidatorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class SetupPages {

    private static final Log LOG = LogFactoryUtil.getLog(SetupPages.class);
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS_PUBLIC;
    private static final HashMap<String, List<String>> DEFAULT_PERMISSIONS_PRIVATE;

    static {
        DEFAULT_PERMISSIONS_PUBLIC = new HashMap<>();
        DEFAULT_PERMISSIONS_PRIVATE = new HashMap<>();
        List<String> actionsOwner = new ArrayList<>();
        actionsOwner.add(ActionKeys.ADD_DISCUSSION);
        actionsOwner.add(ActionKeys.ADD_LAYOUT);
        actionsOwner.add(ActionKeys.CONFIGURE_PORTLETS);
        actionsOwner.add(ActionKeys.CUSTOMIZE);
        actionsOwner.add(ActionKeys.DELETE);
        actionsOwner.add(ActionKeys.DELETE_DISCUSSION);
        actionsOwner.add(ActionKeys.PERMISSIONS);
        actionsOwner.add(ActionKeys.UPDATE);
        actionsOwner.add(ActionKeys.UPDATE_DISCUSSION);
        actionsOwner.add(ActionKeys.VIEW);

        actionsOwner.add(ActionKeys.UPDATE_DISCUSSION);

        DEFAULT_PERMISSIONS_PUBLIC.put(RoleConstants.OWNER, actionsOwner);

        List<String> actionsUser = new ArrayList<>();
        actionsUser.add(ActionKeys.ADD_DISCUSSION);
        actionsUser.add(ActionKeys.CUSTOMIZE);
        actionsUser.add(ActionKeys.VIEW);

        DEFAULT_PERMISSIONS_PUBLIC.put(RoleConstants.SITE_MEMBER, actionsUser);

        DEFAULT_PERMISSIONS_PRIVATE.putAll(DEFAULT_PERMISSIONS_PUBLIC);
        List<String> actionsGuest = new ArrayList<>();
        actionsGuest.add(ActionKeys.VIEW);
        DEFAULT_PERMISSIONS_PRIVATE.put(RoleConstants.GUEST, actionsGuest);
    }

    private SetupPages() {

    }

    /**
     * Setups the pages for the site with the specified information.
     * <p>
     * This method sets up the public and private pages of the site based on the
     * information passed in. If a theme is specified, it is set up for the pages.
     * If a flag to delete existing pages is set, the existing pages are deleted.
     * The pages are then added, and if a virtual host is specified, it is updated
     * for the pages.
     *
     * @param site the site for which pages are to be set up
     * @param groupId the group ID of the site
     * @param company the company ID of the site
     * @param userid the user ID of the person setting up the site pages
     * @throws SystemException if an error occurs while updating the virtual host
     * @throws PortalException if an error occurs while deleting pages or setting up
     *         the theme
     */
    public static void setupSitePages(final Site site, final long groupId,
            final long company, final long userid) throws SystemException, PortalException {

        PublicPages publicPages = site.getPublicPages();
        if (publicPages != null) {
            if (publicPages.getTheme() != null) {
                setupTheme(groupId, publicPages.getTheme(), false);
            }
            if (publicPages.isDeleteExistingPages()) {
                LOG.info("Setup: Deleting pages from site " + site.getName());
                deletePages(groupId, false);
            }
            addPages(publicPages.getPage(), publicPages.getDefaultLayout(),
                    publicPages.getDefaultLayoutContainedInThemeWithId(),
                    groupId, false, 0, company, userid);
            if (publicPages.getVirtualHost() != null) {
                TreeMap<String, String> hosts = new TreeMap<>();
                hosts.put("host", publicPages.getVirtualHost());
                LayoutSetLocalServiceUtil.updateVirtualHosts(groupId, false, hosts);
            }
        }

        PrivatePages privatePages = site.getPrivatePages();
        if (privatePages != null) {
            if (privatePages.getTheme() != null) {
                setupTheme(groupId, privatePages.getTheme(), true);
            }
            if (privatePages.isDeleteExistingPages()) {
                LOG.info("Setup: Deleting pages from site " + site.getName());
                deletePages(groupId, true);
            }
            addPages(privatePages.getPage(), privatePages.getDefaultLayout(),
                    privatePages.getDefaultLayoutContainedInThemeWithId(),
                    groupId, true, 0, company, userid);
            if (privatePages.getVirtualHost() != null) {
                TreeMap<String, String> hosts = new TreeMap<>();
                hosts.put("host", privatePages.getVirtualHost());
                LayoutSetLocalServiceUtil.updateVirtualHosts(groupId, false, hosts);
            }
        }
    }

    /**
     * Set the page templates up. As this is heavily based on page (layout).
     *
     * @param pageTemplates The page template definitions that are imported.
     * @param groupId The group id of the site where to import the
     * @param company The id of the company to which the templates are imported.
     * @param userid The user id of the importing user.
     */
    public static void setupPageTemplates(final PageTemplates pageTemplates, final long groupId,
            final long company, final long userid) {
        try {
            for (PageTemplate pageTemplate : pageTemplates.getPageTemplate()) {
                String name = pageTemplate.getName();
                if (name != null) {

                    LayoutPrototype lp;
                    DynamicQuery dq = LayoutPrototypeLocalServiceUtil.dynamicQuery()
                            .add(PropertyFactoryUtil.forName("name").like("%" + name + "%"));
                    List<LayoutPrototype> listLayoutPrototype = LayoutPrototypeLocalServiceUtil
                            .dynamicQuery(dq);
                    if (listLayoutPrototype != null && !listLayoutPrototype.isEmpty()) {
                        lp = listLayoutPrototype.get(0);
                    } else {
                        Map<Locale, String> titleMap = FieldMapUtil.getTitleMap(
                                pageTemplate.getTitleTranslation(), groupId, name,
                                " Page template  " + name);
                        lp = LayoutPrototypeLocalServiceUtil.addLayoutPrototype(userid, company,
                                titleMap, FieldMapUtil.getLocalizationMap(name), true, new ServiceContext());
                    }
                    if (lp != null) {
                        Layout layout = lp.getLayout();
                        if (pageTemplate.getPage() != null) {
                            Page page = pageTemplate.getPage();
                            if (page.getFriendlyURL() != null
                                    && !page.getFriendlyURL().equals("")) {
                                LOG.error("The page of page template " + name + " may not have a "
                                        + "friendly URL! Will ignore it!");
                            }
                            setupLiferayPage(layout, page, null, null,
                                    groupId, false, company, userid, name);
                        }
                    } else {
                        LOG.error("Could not create or find the page template " + name);
                    }
                }
            }
        } catch (PortalException | SystemException e) {
            LOG.error("Problem during creating page templates ", e);
        }
    }

    /**
     * Sets up the theme for a given group and its corresponding layout set, either
     * private or public.
     *
     * @param groupId the ID of the group
     * @param theme the theme to be set up
     * @param isPrivate a flag indicating whether the layout set is private or
     *        public
     * @throws SystemException if a system-level error occurred
     */
    private static void setupTheme(final long groupId, final Theme theme, final boolean isPrivate)
            throws SystemException, PortalException {

        Group group = GroupLocalServiceUtil.getGroup(groupId);
        LayoutSet set;
        if (isPrivate) {
            set = group.getPrivateLayoutSet();
        } else {
            set = group.getPublicLayoutSet();
        }
        set.setThemeId(theme.getName());
        LayoutSetLocalServiceUtil.updateLayoutSet(set);
    }

    /**
     * Adds a list of pages to a group with a specified parent layout and sets the
     * default layout for the page.
     *
     * @param pages List of pages to be added
     * @param defaultLayout Default layout to be set if the page doesn't have a
     *        specific layout
     * @param defaultLayoutContainedInThemeWithId ID of the theme that contains the
     *        default layout
     * @param groupId ID of the group the pages will be added to
     * @param isPrivate Specifies if the pages should be added to the private or
     *        public layout set
     * @param parentLayoutId ID of the parent layout for the pages
     * @param company ID of the company
     * @param userId ID of the user creating the pages
     * @throws SystemException If a system-level error occurs
     * @throws PortalException If a portal-level error occurs
     */
    private static void addPages(final List<Page> pages, String defaultLayout,
            String defaultLayoutContainedInThemeWithId,
            final long groupId, final boolean isPrivate, final long parentLayoutId, final long company,
            final long userId) throws SystemException, PortalException {

        for (Page page : pages) {

            Layout layout = null;
            try {
                layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, isPrivate,
                        page.getFriendlyURL());
                LOG.info("Setup: Page " + page.getName() + " already exist, not creating...");
                if (layout != null && page.isDeleteExistingPages()) {
                    LayoutLocalServiceUtil.deleteLayout(layout);
                    if (page.getLinkToURL() == null || page.getLinkToURL().equals("")) {
                        layout = createPage(groupId, page, parentLayoutId, isPrivate);
                    } else {
                        layout = createLinkPage(page, groupId, parentLayoutId, userId);
                    }
                } else if (layout != null
                        && (page.getLinkToURL() != null && !page.getLinkToURL().equals(""))) {
                    updateLinkPage(page, groupId);
                }
            } catch (NoSuchLayoutException e) {
                if (page.getLinkToURL() == null || page.getLinkToURL().equals("")) {
                    layout = createPage(groupId, page, parentLayoutId, isPrivate);
                } else {
                    layout = createLinkPage(page, groupId, parentLayoutId, userId);
                }
                LOG.info("Setup: Page " + page.getName() + " created...");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            // If the page has not a layout set, set the default one. Otherwise set that layout as the default for the subtree
            if (page.getLayout() == null) {
                page.setLayout(defaultLayout);
                page.setLayoutContainedInThemeWithId(defaultLayoutContainedInThemeWithId);
            } else {
                defaultLayout = page.getLayout();
                defaultLayoutContainedInThemeWithId = page.getLayoutContainedInThemeWithId();
            }
            setupLiferayPage(layout, page, defaultLayout, defaultLayoutContainedInThemeWithId, groupId, isPrivate,
                    company, userId, null);
        }
    }

    /**
     * The {@code setupLiferayPage} method sets up a Liferay page by performing the
     * following operations:
     * <ul>
     * <li>Setting the page theme, if the page has a theme specified.</li>
     * <li>Setting the page layout template, if the page has a layout
     * specified.</li>
     * <li>Setting the page target.</li>
     * <li>Adding portlets to the page, if the page has any associated
     * portlets.</li>
     * <li>Adding sub-pages to the page, if the page has any sub-pages.</li>
     * <li>Setting custom fields for the page, if the page has any custom fields
     * specified.</li>
     * <li>Updating the permissions for the page, using the specified role
     * permissions and the default permissions for private or public pages.</li>
     * </ul>
     *
     * @param layout The layout to which the page belongs.
     * @param page The page to set up.
     * @param defaultLayout The default layout to use for the page and its
     *        sub-pages.
     * @param defaultLayoutContainedInThemeWithId The theme that contains the
     *        default layout.
     * @param groupId The group ID of the page.
     * @param isPrivate A flag indicating whether the page is private or public.
     * @param company The company ID of the page.
     * @param userId The user ID of the page.
     * @param pageTemplateName The name of the page template.
     * @throws SystemException Thrown if there is a system error.
     * @throws PortalException Thrown if there is a portal error.
     */
    private static void setupLiferayPage(final Layout layout, final Page page, final String defaultLayout,
            final String defaultLayoutContainedInThemeWithId, final long groupId,
            final boolean isPrivate, final long company, final long userId,
            final String pageTemplateName) throws SystemException, PortalException {
        if (page.getTheme() != null) {
            setPageTheme(layout, page);
        }
        if (page.getLayout() != null) {
            setLayoutTemplate(layout, page, userId);
        }

        setPageTarget(page, layout);

        List<Pageportlet> portlets = page.getPageportlet();
        if (portlets != null && !portlets.isEmpty()) {
            for (Pageportlet portlet : portlets) {
                try {
                    addPortletIntoPage(page, layout, portlet, company, groupId);

                } catch (ValidatorException | IOException e) {
                    LOG.error(e);
                }
            }
        }

        List<Page> subPages = page.getPage();
        if (subPages != null && !subPages.isEmpty()) {
            if (pageTemplateName != null && !pageTemplateName.equals("")) {
                LOG.error("Page template " + pageTemplateName + " may not have any sub-pages! "
                        + "Will ignore them!");
            } else {
                addPages(subPages, defaultLayout, defaultLayoutContainedInThemeWithId, groupId, isPrivate,
                        layout.getLayoutId(), company, userId);
            }
        }

        if (page.getCustomFieldSetting() != null && !page.getCustomFieldSetting().isEmpty()) {
            setCustomFields(userId, groupId, company, page, layout);
        }

        SetupPermissions.updatePermission("Page " + page.getFriendlyURL(), groupId, company,
                layout.getPlid(), Layout.class, page.getRolePermissions(),
                getDefaultPermissions(isPrivate));
    }

    private static HashMap<String, List<String>> getDefaultPermissions(final boolean isPrivate) {
        if (isPrivate) {
            return DEFAULT_PERMISSIONS_PRIVATE;
        }
        return DEFAULT_PERMISSIONS_PUBLIC;
    }

    /**
     * Creates a new link page based on the values provided in the {@link Page}
     * object.
     *
     * @param p the {@link Page} object containing the information needed to create
     *        a new link page
     * @param groupId the ID of the group that the new link page belongs to
     * @param parentLayoutId the ID of the parent layout for the new link page
     * @param userId the ID of the user who is creating the new link page
     * @return the newly created {@link Layout} object representing the link page
     */
    private static Layout createLinkPage(final Page p, final long groupId,
            final long parentLayoutId, final long userId) {
        // all values are usually retrieved via special methods from our code
        // for better readability I have added the real values here

        String title = "my title";
        ServiceContext serviceContext = new ServiceContext();
        String layoutType = LayoutConstants.TYPE_URL;
        boolean hidden = p.isHidden();
        String friendlyURL = p.getFriendlyURL();
        // add the layout
        Layout layout = null;
        try {
            layout = LayoutLocalServiceUtil.addLayout(userId, groupId, false, parentLayoutId, title,
                    title, StringPool.BLANK, layoutType, hidden, friendlyURL, serviceContext);

            String linkToPageUrl = p.getLinkToURL();
            // set the value of the "link to page"
            UnicodeProperties props = layout.getTypeSettingsProperties();
            props.put("url", linkToPageUrl);
            layout.setTypeSettingsProperties(props);
            LayoutLocalServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(),
                    layout.getLayoutId(), layout.getTypeSettings());
        } catch (PortalException | SystemException e) {
            LOG.error("Could not create link page " + p.getFriendlyURL() + " with link to url "
                    + p.getLinkToURL(), e);
        }
        return layout;
    }

    /**
     * Updates the given link page.
     *
     * @param page the link page to be updated
     * @param groupId the ID of the group the page belongs to
     * @throws SystemException in case of an error during updating the link page
     * @throws PortalException in case of an error during retrieving the layout of
     *         the link page
     */
    private static void updateLinkPage(final Page page, final long groupId) {
        try {
            Layout layout = LayoutLocalServiceUtil.getFriendlyURLLayout(groupId, false,
                    page.getFriendlyURL());
            if (layout.getLayoutType().getTypeSettingsProperties().get("url") == null) {
                LOG.error("Could not update link page " + page.getFriendlyURL()
                        + " with link to url" + " " + page.getLinkToURL()
                        + " because page is not a link type page! "
                        + " Maybe it has been imported before as non link type page. Please "
                        + "delete it and rerun!");
            } else {
                UnicodeProperties props = layout.getTypeSettingsProperties();
                props.put("url", page.getLinkToURL());
                layout.setTypeSettingsProperties(props);
                layout.setHidden(page.isHidden());
                LayoutLocalServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(),
                        layout.getLayoutId(), layout.getTypeSettings());
            }
        } catch (PortalException | SystemException e) {
            LOG.error("Could not update link page " + page.getFriendlyURL() + " with link to url "
                    + page.getLinkToURL(), e);
        }
    }

    /**
     * Creates a new page in the database with the provided information.
     *
     * @param groupId The identifier of the group to which the page belongs.
     * @param currentPage The {@link Page} object with the details of the page to be
     *        created.
     * @param parentLayoutId The identifier of the parent layout, or {@code 0} if it
     *        is a top-level page.
     * @param isPrivate A flag indicating whether the page is private or not.
     * @return The newly created {@link Layout} object.
     * @throws SystemException If a system exception occurs.
     * @throws PortalException If a portal exception occurs.
     */
    private static Layout createPage(final long groupId, final Page currentPage,
            final long parentLayoutId, final boolean isPrivate)
            throws SystemException, PortalException {

        Map<Locale, String> titleMap = FieldMapUtil.getTitleMap(currentPage.getTitleTranslation(), groupId,
                currentPage.getName(), " Page with title " + currentPage.getFriendlyURL());

        Locale locale = LocaleUtil.getSiteDefault();

        Map<Locale, String> descriptionMap = new HashMap<>();
        descriptionMap.put(locale, StringPool.BLANK);

        Map<Locale, String> friendlyURLMap = new HashMap<>();
        friendlyURLMap.put(locale, currentPage.getFriendlyURL());

        return LayoutLocalServiceUtil.addLayout(LiferaySetup.getRunAsUserId(), groupId, isPrivate, parentLayoutId,
                titleMap, titleMap, null,
                null, null, currentPage.getType(), StringPool.BLANK, currentPage.isHidden(), friendlyURLMap,
                new ServiceContext());
    }

    /**
     * This method is used to set the custom fields for a layout.
     *
     * @param runAsUserId The user ID to be used when setting the custom fields.
     * @param groupId The group ID associated with the layout.
     * @param company The company ID associated with the layout.
     * @param page The page for which the custom fields are being set.
     * @param layout The layout for which the custom fields are being set.
     */
    private static void setCustomFields(final long runAsUserId, final long groupId,
            final long company, final Page page, final Layout layout) {
        Class clazz = Layout.class;
        String resolverHint = "Resolving customized value for page " + page.getFriendlyURL() + " "
                + "failed for key " + "%%key%% and value %%value%%";
        for (CustomFieldSetting cfs : page.getCustomFieldSetting()) {
            String key = cfs.getKey();
            String value = cfs.getValue();
            CustomFieldSettingUtil.setExpandoValue(
                    resolverHint.replace("%%key%%", key).replace("%%value%%", value), runAsUserId,
                    groupId, company, clazz, layout.getPlid(), key, value);
        }
    }

    /**
     * This method sets the target attribute for the given page. The target
     * attribute is used to specify the target window for the page's link.
     *
     * @param page The page for which the target attribute is to be set.
     * @param layout The layout associated with the page.
     */
    private static void setPageTarget(final Page page, final Layout layout) {
        UnicodeProperties props = layout.getTypeSettingsProperties();
        props.put("target", page.getTarget());
        layout.setTypeSettingsProperties(props);
        try {
            LayoutLocalServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(),
                    layout.getLayoutId(), layout.getTypeSettings());
        } catch (PortalException e) {
            LOG.error("Can not set target attribute value '" + page.getTarget()
                    + "' to page with layoutId:" + layout.getLayoutId() + ".", e);
        }
    }

    /**
     * Sets the theme for a given layout.
     *
     * @param layout the layout to set the theme for
     * @param page the page object containing the theme information
     * @throws SystemException if an error occurs while setting the theme
     */
    private static void setPageTheme(final Layout layout, final Page page) throws SystemException {

        Theme theme = page.getTheme();
        if (theme != null) {
            layout.setThemeId(theme.getName());
            try {
                LayoutLocalServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(),
                        layout.getLayoutId(), layout.getTypeSettings());
            } catch (PortalException e) {
                e.printStackTrace();
            }
            LOG.info("setting theme on page: " + page.getName() + " : " + theme.getName());
        }
    }

    /**
     * Adds a portlet to the given page and sets its preferences.
     *
     * @param page The {@link Page} to which the portlet should be added.
     * @param layout The {@link Layout} of the page.
     * @param portlet The {@link Pageportlet} to be added to the page.
     * @param companyId The ID of the company.
     * @param groupId The ID of the group.
     * @throws SystemException If a system error occurs.
     * @throws ValidatorException If validation fails.
     * @throws IOException If an I/O error occurs.
     * @throws PortalException If a portal error occurs.
     */
    private static void addPortletIntoPage(final Page page, final Layout layout,
            final Pageportlet portlet, final long companyId, final long groupId)
            throws SystemException, ValidatorException, IOException, PortalException {
        if (page.getLinkToURL() != null && !page.getLinkToURL().equals("")) {
            LOG.error("This is a link page! It cannot be cleared. If you intend to use this page "
                    + "for portlets, please"
                    + " delete this page, or remove the link from the page!");
        } else {
            long plid = layout.getPlid();
            long ownerId = PortletKeys.PREFS_OWNER_ID_DEFAULT;
            int ownerType = PortletKeys.PREFS_OWNER_TYPE_LAYOUT;
            long runAsUserId = LiferaySetup.getRunAsUserId();

            LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();

            String portletId = portlet.getPortletId();
            String column = portlet.getColumn();

            String portletIdInc = "";
            try {
                int columnPos = portlet.getColumnPosition();
                portletIdInc = layoutTypePortlet.addPortletId(runAsUserId, portletId, column, columnPos, false);
                if (portletIdInc == null) {
                    portletIdInc = portletId;
                }
            } catch (SystemException e) {
                LOG.error("Add portlet error ", e);
            }

            javax.portlet.PortletPreferences preferences = PortletPreferencesLocalServiceUtil.getPreferences(companyId,
                    ownerId, ownerType, plid, portletIdInc);
            List<PortletPreference> prefsList = portlet.getPortletPreference();
            for (PortletPreference p : prefsList) {
                try {
                    preferences.setValue(p.getKey(), resolvePortletPrefValue(p.getKey(), p.getValue(),
                            portlet, companyId, groupId, runAsUserId));
                } catch (ReadOnlyException e) {
                    LOG.error("Portlet preferences (" + p.getKey() + ", " + p.getValue() + ") of "
                            + "portlet " + portlet.getPortletId() + " caused an excpetion! ");
                }
            }
            PortletPreferencesLocalServiceUtil.updatePreferences(ownerId, ownerType, plid, portletIdInc, preferences);

            if (Validator.isNotNull(column) && Validator.isNotNull(portletIdInc)) {
                layoutTypePortlet.movePortletId(runAsUserId, portletIdInc, column, portlet.getColumnPosition());
            }
            LayoutLocalServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(),
                    layout.getLayoutId(), layout.getTypeSettings());
        }
    }

    /**
     * Substitutes parameters in porlet preferences. Possible values are:
     * <ul>
     * <li>{{$ID_OF_SITE_WITH_NAME= &lt; name of the site/group &gt;}}</li>
     * <li>{{$ART-TEMPLATE-UUID-BY-KEY= &lt; value of article template key &gt;
     * }}</li>
     * <li>{{$ART-STRUCTURE-UUID-BY-KEY=&lt; value of article structure key &gt;
     * }}</li>
     * <li>{{$ART-TEMPLATE-ID-BY-KEY= &lt; value of article template key &gt; }}
     * </li>
     * <li>{{$ART-STRUCTURE-ID-BY-KEY=&lt; value of article structure key &gt;
     * }}</li>
     * <li>{{$ADT-TEMPLATE-ID-BY-KEY= &lt; value of ADT template key &gt; }}</li>
     * <li>{{$ADT-TEMPLATE-UUID-BY-KEY= &lt; value of ADT template key &gt; }}</li>
     * <li>{{$FILE= [value of the site scope in for of ::SITENAME::] &lt; value of
     * path and title of the refered document &gt; }}</li>
     * </ul>
     *
     * @param key The portlet key.
     * @param value The defined value which should be parametrized.
     * @param portlet The pageportlet definition.
     * @param company Id of the company.
     * @param groupId The group id.
     * @param runAsUserId The user id which import the data.
     */
    private static String resolvePortletPrefValue(final String key, final String value,
            final Pageportlet portlet, final long company, final long groupId,
            final long runAsUserId) {
        String locationHint = "Key: " + key + " of portlet " + portlet.getPortletId();
        return ResolverUtil.lookupAll(runAsUserId, groupId, company, value, locationHint);
    }

    public static void setLayoutTemplate(final Layout layout, final Page page, final long userid) {
        if (layout.getLayoutType() instanceof LayoutTypePortlet) {
            LayoutTypePortlet portletLayout = (LayoutTypePortlet) layout.getLayoutType();

            if (page.isClearPage()) {
                if (page.getPageportlet() != null && page.getPageportlet().size() > 0
                        && page.getLinkToURL() != null && !page.getLinkToURL().equals("")) {
                    LOG.error("This is a link page! It cannot be cleared. If you intend to use "
                            + "this page for " + "portlets, please"
                            + " delete this page, or remove the link from the page!");
                } else {
                    removeAllPortlets(userid, portletLayout, layout);
                }
            }
            String themeId = null;
            try {

                if (page.getLayoutContainedInThemeWithId() != null
                        && !page.getLayoutContainedInThemeWithId().equals("")) {
                    themeId = page.getLayoutContainedInThemeWithId();
                }
                LayoutTemplate layoutTemplate = LayoutTemplateLocalServiceUtil
                        .getLayoutTemplate(page.getLayout(), false, themeId);

                if (layoutTemplate != null) {
                    LOG.info("Setting layout to " + page.getLayout() + " for page "
                            + page.getName());
                    if (themeId != null) {
                        LOG.info("Layout was looked up in theme " + themeId);
                    }
                    portletLayout.setLayoutTemplateId(
                            UserLocalServiceUtil.getDefaultUserId(layout.getCompanyId()),
                            layoutTemplate.getLayoutTemplateId());
                    LayoutLocalServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(),
                            layout.getLayoutId(), layout.getTypeSettings());
                } else {
                    LOG.error("Layout template " + page.getLayout() + " not found !");
                    if (themeId != null) {
                        LOG.error("Layout was looked up in theme " + themeId);
                    }
                }
            } catch (Exception e) {
                LOG.error("Error by setting layout template : " + page.getLayout(), e);
                if (themeId != null) {
                    LOG.error("Layout was looked up in theme " + themeId);
                }
            }

        }
    }

    /**
     * Removes all portlets from the given {@code layout}.
     *
     * @param runasUser the user ID to run as
     * @param layoutTypePortlet the layout type portlet
     * @param layout the layout to remove portlets from
     */
    private static void removeAllPortlets(final long runasUser,
            final LayoutTypePortlet layoutTypePortlet, final Layout layout) {
        List<Portlet> portlets = null;
        try {
            portlets = layoutTypePortlet.getAllPortlets();
        } catch (SystemException e1) {
            e1.printStackTrace();
        }
        if (portlets != null) {
            for (Portlet portlet : portlets) {

                String portletId = portlet.getPortletId();

                try {
                    if (layoutTypePortlet.hasPortletId(portletId)) {
                        LOG.debug("Removing portlet " + portletId);
                        layoutTypePortlet.removePortletId(runasUser, portletId);
                        String rootPortletId = PortletIdCodec.decodePortletName(portletId);
                        LOG.debug("Root portletId: " + rootPortletId);
                        ResourceLocalServiceUtil.deleteResource(layout.getCompanyId(),
                                rootPortletId, ResourceConstants.SCOPE_INDIVIDUAL,
                                PortletPermissionUtil.getPrimaryKey(layout.getPlid(), portletId));
                        LayoutLocalServiceUtil.updateLayout(layout.getGroupId(), layout.isPrivateLayout(),
                                layout.getLayoutId(), layout.getTypeSettings());
                        List<PortletPreferences> list = PortletPreferencesLocalServiceUtil
                                .getPortletPreferences(PortletKeys.PREFS_OWNER_TYPE_LAYOUT,
                                        layout.getPlid(), portletId);
                        for (PortletPreferences p : list) {
                            PortletPreferencesLocalServiceUtil.deletePortletPreferences(p);
                        }
                    }
                } catch (PortalException | SystemException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Deletes all the pages of the given group.
     *
     * @param groupId The ID of the group.
     * @param privatePages Indicates whether to delete private pages or not. If
     *        `true`, private pages will be deleted, if `false` public pages will be
     *        deleted.
     */
    private static void deletePages(final long groupId, boolean privatePages) {

        ServiceContext serviceContext = new ServiceContext();
        try {
            LayoutLocalServiceUtil.deleteLayouts(groupId, privatePages, serviceContext);
            LOG.info("Setup: Pages removed.");
        } catch (PortalException | SystemException e) {
            LOG.error("cannot remove pages: " + e);
        }
    }

}
