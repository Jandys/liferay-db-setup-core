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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.liferay.portal.kernel.dao.orm.ObjectNotFoundException;
import com.liferay.portal.kernel.exception.NoSuchRoleException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.RequiredRoleException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import eu.lundegaard.liferay.db.setup.LiferaySetup;
import eu.lundegaard.liferay.db.setup.core.util.ResolverUtil;
import eu.lundegaard.liferay.db.setup.domain.DefinePermission;
import eu.lundegaard.liferay.db.setup.domain.DefinePermissions;
import eu.lundegaard.liferay.db.setup.domain.PermissionAction;

public final class SetupRoles {

    private static final Log LOG = LogFactoryUtil.getLog(SetupRoles.class);
    private static final String SCOPE_INDIVIDUAL = "individual";
    private static final String SCOPE_SITE = "site";
    private static final String SCOPE_SITE_TEMPLATE = "site template";
    private static final String SCOPE_PORTAL = "portal";

    private SetupRoles() {

    }

    public static void setupRoles(final List<eu.lundegaard.liferay.db.setup.domain.Role> roles, long runAsUserId,
            long groupId, long company) {

        for (eu.lundegaard.liferay.db.setup.domain.Role role : roles) {
            try {
                RoleLocalServiceUtil.getRole(LiferaySetup.getCompanyId(), role.getName());
                LOG.info("Setup: Role " + role.getName() + " already exist, not creating...");
            } catch (NoSuchRoleException | ObjectNotFoundException e) {
                addRole(role);

            } catch (SystemException | PortalException e) {
                LOG.error("error while setting up roles", e);
            }
            addRolePermissions(role, runAsUserId, groupId, company);
        }
    }

    /**
     * Adds a role with the specified details.
     *
     * @param role the role to be added
     */
    private static void addRole(final eu.lundegaard.liferay.db.setup.domain.Role role) {

        Map<Locale, String> localeTitleMap = new HashMap<>();
        localeTitleMap.put(Locale.ENGLISH, role.getName());

        try {
            int roleType = RoleConstants.TYPE_REGULAR;
            if (role.getType() != null) {
                if (role.getType().equals("site")) {
                    roleType = RoleConstants.TYPE_SITE;
                } else if (role.getType().equals("organization")) {
                    roleType = RoleConstants.TYPE_ORGANIZATION;
                }
            }

            long defaultUserId = UserLocalServiceUtil.getDefaultUserId(LiferaySetup.getCompanyId());
            RoleLocalServiceUtil.addRole(defaultUserId, null, 0, role.getName(), localeTitleMap, null, roleType, null,
                    null);

            LOG.info("Setup: Role " + role.getName() + " does not exist, adding...");

        } catch (PortalException | SystemException e) {
            LOG.error("error while adding up roles", e);
        }

    }

    /**
     * Deletes roles based on the delete method provided. The roles can be deleted
     * either by excluding the listed roles or
     * <p>
     * including only the listed roles.
     *
     * @param roles List of {@link eu.lundegaard.liferay.db.setup.domain.Role}
     *        objects to be deleted.
     * @param deleteMethod Specifies the method of deletion. Possible values are
     *        "excludeListed" or "onlyListed".
     * @param companyId The identifier of the company that the roles belong to.
     */
    public static void deleteRoles(final List<eu.lundegaard.liferay.db.setup.domain.Role> roles,
            final String deleteMethod, long companyId) {

        switch (deleteMethod) {
            case "excludeListed":
                Map<String, eu.lundegaard.liferay.db.setup.domain.Role> toBeDeletedRoles =
                        convertRoleListToHashMap(roles);
                try {
                    for (Role role : RoleLocalServiceUtil.getRoles(-1, -1)) {
                        String name = role.getName();
                        if (!toBeDeletedRoles.containsKey(name)) {
                            try {
                                RoleLocalServiceUtil.deleteRole(
                                        RoleLocalServiceUtil.getRole(companyId, name));
                                LOG.info("Deleting Role " + name);

                            } catch (Exception e) {
                                LOG.info("Skipping deletion fo system role " + name);
                            }
                        }
                    }
                } catch (SystemException e) {
                    LOG.error("problem with deleting roles", e);
                }
                break;

            case "onlyListed":
                for (eu.lundegaard.liferay.db.setup.domain.Role role : roles) {
                    String name = role.getName();
                    try {
                        RoleLocalServiceUtil.deleteRole(
                                RoleLocalServiceUtil.getRole(LiferaySetup.getCompanyId(), name));
                        LOG.info("Deleting Role " + name);

                    } catch (RequiredRoleException e) {
                        LOG.info("Skipping deletion fo system role " + name);

                    } catch (PortalException | SystemException e) {
                        LOG.error("Unable to delete role.", e);
                    }
                }
                break;

            default:
                LOG.error("Unknown delete method : " + deleteMethod);
                break;
        }

    }

    /**
     * This method adds role permissions to a Liferay database setup.
     *
     * @param role The role for which permissions are to be added.
     * @param runAsUserId The user ID to run the setup as.
     * @param groupId The ID of the group.
     * @param companyId The ID of the company.
     */
    private static void addRolePermissions(eu.lundegaard.liferay.db.setup.domain.Role role, long runAsUserId,
            long groupId, long companyId) {
        if (role.getDefinePermissions() != null) {
            String siteName = role.getSite();
            if (siteName != null && !siteName.equals("")) {
                LOG.warn("Note, refering a site inside a role definition makes no sense and will be ignored! This "
                        + "attribute is intended to be used for refering assigning a site role to an Liferay object, such as a user!"
                        + " When doing so, it is necessary to refer a site!");
            }
            DefinePermissions permissions = role.getDefinePermissions();
            if (permissions.getDefinePermission() != null && !permissions.getDefinePermission().isEmpty()) {
                for (DefinePermission permission : permissions.getDefinePermission()) {
                    String permissionName = permission.getDefinePermissionName();
                    String resourcePrimKey = "0";

                    if (permission.getElementPrimaryKey() != null) {
                        resourcePrimKey = ResolverUtil.lookupAll(runAsUserId, groupId, companyId,
                                permission.getElementPrimaryKey(),
                                "Role " + role.getName() + " permission name " + permissionName);
                    }
                    String type = role.getType();
                    int scope = ResourceConstants.SCOPE_COMPANY;
                    if (type != null && type.equalsIgnoreCase(SCOPE_PORTAL)) {
                        scope = ResourceConstants.SCOPE_COMPANY;
                    }
                    if (type != null && type.equalsIgnoreCase(SCOPE_SITE)) {
                        scope = ResourceConstants.SCOPE_GROUP_TEMPLATE;
                    }

                    if (type != null && type.equalsIgnoreCase("organization")) {
                        scope = ResourceConstants.SCOPE_GROUP_TEMPLATE;
                    }
                    if (scope == ResourceConstants.SCOPE_COMPANY && !resourcePrimKey.equals("0")) {
                        // if we have a regular role which is set for a particular site, set the scope to group
                        scope = ResourceConstants.SCOPE_GROUP;
                    }
                    if (scope == ResourceConstants.SCOPE_COMPANY && resourcePrimKey.equals("0")) {
                        // if we have a regular role which does not have any resource key set, set the company id as resource key
                        resourcePrimKey = Long.toString(companyId);
                    }

                    // if defined override the define permission scope
                    if (permission.getScope() != null && !permission.getScope().equals("")) {
                        if (permission.getScope().equalsIgnoreCase(SCOPE_PORTAL)) {
                            scope = ResourceConstants.SCOPE_COMPANY;
                        } else if (permission.getScope().equalsIgnoreCase(SCOPE_SITE_TEMPLATE)) {
                            scope = ResourceConstants.SCOPE_GROUP_TEMPLATE;
                        } else if (permission.getScope().equalsIgnoreCase(SCOPE_SITE)) {
                            scope = ResourceConstants.SCOPE_GROUP;
                        } else if (permission.getScope().equalsIgnoreCase(SCOPE_INDIVIDUAL)) {
                            scope = ResourceConstants.SCOPE_INDIVIDUAL;
                        }
                    }

                    if (permission.getPermissionAction() != null && !permission.getPermissionAction().isEmpty()) {
                        ArrayList<String> listOfActions = new ArrayList<>();
                        for (PermissionAction pa : permission.getPermissionAction()) {
                            String actionname = pa.getActionName();
                            listOfActions.add(actionname);
                        }
                        String[] loa = new String[listOfActions.size()];
                        loa = listOfActions.toArray(loa);
                        try {
                            SetupPermissions.addPermission(role.getName(), permissionName, resourcePrimKey, scope, loa,
                                    companyId);
                        } catch (SystemException | PortalException e) {
                            LOG.error(
                                    "Error when defining permission " + permissionName + " for role " + role.getName(),
                                    e);
                        }
                    }
                }
            }
        }

    }

    private static Map<String, eu.lundegaard.liferay.db.setup.domain.Role> convertRoleListToHashMap(
            final List<eu.lundegaard.liferay.db.setup.domain.Role> objects) {

        HashMap<String, eu.lundegaard.liferay.db.setup.domain.Role> map = new HashMap<>();
        for (eu.lundegaard.liferay.db.setup.domain.Role role : objects) {
            map.put(role.getName(), role);
        }
        return map;
    }
}
