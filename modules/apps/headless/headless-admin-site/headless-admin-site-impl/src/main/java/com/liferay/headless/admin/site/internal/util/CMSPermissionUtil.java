/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.admin.site.internal.util;

import com.liferay.depot.constants.DepotRolesConstants;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.UserGroupRole;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;
import com.liferay.portal.kernel.service.UserGroupRoleLocalServiceUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Balázs Sáfrány-Kovalik
 */
public class CMSPermissionUtil {

	public static boolean isSpaceAdmin(long companyId, long userId)
		throws PortalException {

		if (RoleLocalServiceUtil.hasUserRole(
				userId, companyId, RoleConstants.CMS_ADMINISTRATOR, true)) {

			return true;
		}

		Set<Long> siteScopedRoleIds = new HashSet<>();

		for (String roleName : _CMS_ADMIN_SPACE_ROLE_NAMES) {
			Role role = RoleLocalServiceUtil.fetchRole(companyId, roleName);

			if (role != null) {
				siteScopedRoleIds.add(role.getRoleId());
			}
		}

		if (siteScopedRoleIds.isEmpty()) {
			return false;
		}

		for (UserGroupRole userGroupRole :
				UserGroupRoleLocalServiceUtil.getUserGroupRoles(userId)) {

			if (siteScopedRoleIds.contains(userGroupRole.getRoleId())) {
				return true;
			}
		}

		return false;
	}

	private static final String[] _CMS_ADMIN_SPACE_ROLE_NAMES = {
		DepotRolesConstants.ASSET_LIBRARY_ADMINISTRATOR,
		DepotRolesConstants.ASSET_LIBRARY_OWNER
	};

}