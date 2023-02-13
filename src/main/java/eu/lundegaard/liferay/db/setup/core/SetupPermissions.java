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

import com.liferay.portal.kernel.exception.NestableException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import eu.lundegaard.liferay.db.setup.domain.*;
import java.util.*;


public final class SetupPermissions {

    public static final String[] PERMISSION_RO = {ActionKeys.VIEW};
    public static final String[] PERMISSION_RW = {ActionKeys.VIEW, ActionKeys.UPDATE};
    private static final Log LOG = LogFactoryUtil.getLog(SetupPermissions.class);

    private SetupPermissions() {

    }

    /**
     * Sets up permissions for a PortletPermissions object. This method iterates
     * through the Portlet objects in the given PortletPermissions object, deletes
     * any existing permissions for each Portlet, and sets new permissions based on
     * the actions defined for each role in the PortletPermissions object. The new
     * permissions are set using the Liferay
     * ResourcePermissionLocalServiceUtil.setResourcePermissions method. If any
     * exceptions are thrown while setting the permissions, they will be logged
     * using the logger and the method will continue with the next Portlet in the
     * PortletPermissions object.
     *
     * @param portletPermissions The PortletPermissions object to set up permissions
     *        for.
     * @param companyId The ID of the company for which to set up permissions.
     */
    public static void setupPortletPermissions(final PortletPermissions portletPermissions, long companyId) {

        for (PortletPermissions.Portlet portlet : portletPermissions.getPortlet()) {

            deleteAllPortletPermissions(portlet, companyId);

            Map<String, Set<String>> actionsPerRole = getActionsPerRole(portlet);
            for (String roleName : actionsPerRole.keySet()) {
                try {
                    long roleId = RoleLocalServiceUtil.getRole(companyId, roleName).getRoleId();
                    final Set<String> actionStrings = actionsPerRole.get(roleName);
                    final String[] actionIds = actionStrings.toArray(new String[actionStrings.size()]);

                    ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId,
                            portlet.getPortletId(), ResourceConstants.SCOPE_COMPANY,
                            String.valueOf(companyId), roleId, actionIds);
                    LOG.info("Set permission for role: " + roleName + " for action ids: " + actionIds);
                } catch (NestableException e) {
                    LOG.error("Could not set permission to portlet :" + portlet.getPortletId(),
                            e);
                }
            }
        }
    }

    /**
     * @param portlet -
     * @return mapping of role name to action ids for the portlet
     */
    private static Map<String, Set<String>> getActionsPerRole(PortletPermissions.Portlet portlet) {
        Map<String, Set<String>> result = new HashMap<>();

        for (PortletPermissions.Portlet.ActionId actionId : portlet.getActionId()) {
            for (Role role : actionId.getRole()) {
                final String roleName = role.getName();
                Set<String> actions = result.get(roleName);
                if (actions == null) {
                    actions = new HashSet<>();
                    result.put(roleName, actions);
                }
                actions.add(actionId.getName());
            }
        }

        return result;
    }


    public static void addReadRight(final String roleName, final String className,
            final String primaryKey, long companyId) throws SystemException, PortalException {

        addPermission(roleName, className, primaryKey, PERMISSION_RO, companyId);
    }

    public static void addReadWrightRight(final String roleName, final String className,
            final String primaryKey, long companyId) throws SystemException, PortalException {

        addPermission(roleName, className, primaryKey, PERMISSION_RW, companyId);
    }

    public static void removePermission(final long companyId, final String name,
            final String primKey) throws PortalException, SystemException {
        ResourcePermissionLocalServiceUtil.deleteResourcePermissions(companyId, name,
                ResourceConstants.SCOPE_INDIVIDUAL, primKey);
    }

    public static void addPermission(String roleName, String name, String primaryKey, int scope,
            String[] permission, long companyId)
            throws SystemException, PortalException {
        try {
            long roleId = RoleLocalServiceUtil.getRole(companyId, roleName).getRoleId();
            ResourcePermissionLocalServiceUtil
                    .setResourcePermissions(companyId, name, scope, primaryKey, roleId, permission);
        } catch (Exception ex) {
            LOG.error("Error when adding role!", ex);
        }
    }

    public static void addPermission(final String roleName, final String className,
            final String primaryKey, final String[] permission, long companyId)
            throws SystemException, PortalException {
        try {
            long roleId = RoleLocalServiceUtil.getRole(companyId, roleName).getRoleId();
            ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId, className,
                    ResourceConstants.SCOPE_INDIVIDUAL, primaryKey, roleId, permission);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void addPermissionToPage(final Role role,
            final String primaryKey, final String[] actionKeys, long companyId)
            throws PortalException, SystemException {

        long roleId = RoleLocalServiceUtil.getRole(companyId, role.getName()).getRoleId();
        ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId,
                Layout.class.getName(), ResourceConstants.SCOPE_INDIVIDUAL,
                String.valueOf(primaryKey), roleId, actionKeys);
    }

    private static void deleteAllPortletPermissions(final PortletPermissions.Portlet portlet, long companyId) {

        try {
            List<ResourcePermission> resourcePermissions = ResourcePermissionLocalServiceUtil
                    .getResourcePermissions(companyId, portlet.getPortletId(),
                            ResourceConstants.SCOPE_COMPANY, String.valueOf(companyId));
            for (ResourcePermission resourcePermission : resourcePermissions) {
                ResourcePermissionLocalServiceUtil.deleteResourcePermission(resourcePermission);
            }
        } catch (SystemException e) {
            LOG.error("could not delete permissions for portlet :" + portlet.getPortletId(), e);
        }
    }

    public static void clearPagePermissions(final String primaryKey, long companyId)
            throws PortalException, SystemException {

        ResourcePermissionLocalServiceUtil.deleteResourcePermissions(companyId,
                Layout.class.getName(), ResourceConstants.SCOPE_INDIVIDUAL,
                String.valueOf(primaryKey));
    }

    public static void updatePermission(final String locationHint, final long groupId,
            final long companyId, final long elementId, final Class clazz,
            final RolePermissions rolePermissions,
            final HashMap<String, List<String>> defaultPermissions) {

        updatePermission(locationHint, groupId, companyId, elementId, clazz.getName(), rolePermissions,
                defaultPermissions);
    }

    /**
     * Update permissions for an element of a given class.
     * <p>
     * This method updates the permissions for an element of a given class. If the
     * rolePermissions parameter is not null, it will be used to set the permissions
     * for the element. The rolePermissions parameter should be an object containing
     * information about the roles and actions that should have permissions for the
     * element. If the rolePermissions is null or the rolePermissions object does
     * not contain any rolePermission, the defaultPermissions map will be used to
     * set the permissions. The method uses a SetupPermissions helper class to add
     * and remove permissions. If any exceptions are thrown while setting the
     * permissions, they will be logged using the logger and the method will
     * continue with the next role.
     * </p>
     * 
     * @param locationHint A hint about the location of the element (used for
     *        logging)
     * @param groupId The ID of the group the element belongs to
     * @param companyId The ID of the company the element belongs to
     * @param elementId The ID of the element
     * @param className The fully-qualified class name of the element
     * @param rolePermissions The role-based permissions to set for the element, or
     *        null to use default permissions
     * @param defaultPermissions A map of default permissions, where the keys are
     *        role names and the values are lists of action names
     * 
     */
    public static void updatePermission(final String locationHint, final long groupId,
            final long companyId, final long elementId, final String className,
            final RolePermissions rolePermissions,
            final HashMap<String, List<String>> defaultPermissions) {
        boolean useDefaultPermissions = false;
        if (rolePermissions != null) {
            if (rolePermissions.isClearPermissions()) {
                try {
                    SetupPermissions.removePermission(companyId, className,
                            Long.toString(elementId));
                } catch (PortalException e) {
                    LOG.error("Permissions for " + locationHint + " could not be cleared. ", e);
                } catch (SystemException e) {
                    LOG.error("Permissions for " + locationHint + " could not be cleared. ", e);
                }
            }
            List<String> actions = new ArrayList<String>();
            List<RolePermission> rolePermissionList = rolePermissions.getRolePermission();
            if (rolePermissionList != null) {
                for (RolePermission rp : rolePermissionList) {
                    actions.clear();
                    String roleName = rp.getRoleName();
                    List<PermissionAction> roleActions = rp.getPermissionAction();
                    for (PermissionAction pa : roleActions) {
                        String actionName = pa.getActionName();
                        actions.add(actionName);
                    }
                    try {
                        SetupPermissions.addPermission(roleName, className,
                                Long.toString(elementId),
                                actions.toArray(new String[actions.size()]), companyId);
                    } catch (SystemException e) {
                        LOG.error("Permissions for " + roleName + " for " + locationHint + " "
                                + "could not be set. ", e);
                    } catch (PortalException e) {
                        LOG.error("Permissions for " + roleName + " for " + locationHint + " "
                                + "could not be set. ", e);
                    } catch (NullPointerException e) {
                        LOG.error("Permissions for " + roleName + " for " + locationHint + " "
                                + "could not be set. " + "Probably role not found! ", e);
                    }
                }
            } else {
                useDefaultPermissions = true;
            }
        } else {
            useDefaultPermissions = true;
        }
        if (useDefaultPermissions) {
            Set<String> roles = defaultPermissions.keySet();
            List<String> actions;
            for (String r : roles) {
                actions = defaultPermissions.get(r);
                try {
                    SetupPermissions.addPermission(r, className, Long.toString(elementId),
                            actions.toArray(new String[actions.size()]), companyId);
                } catch (SystemException e) {
                    LOG.error("Permissions for " + r + " for " + locationHint + " could not be "
                            + "set. ", e);
                } catch (PortalException e) {
                    LOG.error("Permissions for " + r + " for " + locationHint + " could not be "
                            + "set. ", e);
                }
            }
        }
    }

}
