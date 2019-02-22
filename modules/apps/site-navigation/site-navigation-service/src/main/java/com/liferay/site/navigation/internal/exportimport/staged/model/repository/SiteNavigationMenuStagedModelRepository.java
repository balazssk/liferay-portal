/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.site.navigation.internal.exportimport.staged.model.repository;

import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.exportimport.kernel.lar.StagedModelModifiedDateComparator;
import com.liferay.exportimport.staged.model.repository.StagedModelRepository;
import com.liferay.exportimport.staged.model.repository.StagedModelRepositoryHelper;
import com.liferay.portal.kernel.dao.orm.ExportActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.site.navigation.model.SiteNavigationMenu;
import com.liferay.site.navigation.service.SiteNavigationMenuLocalService;

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Pavel Savinov
 */
@Component(
	immediate = true,
	property = "model.class.name=com.liferay.site.navigation.model.SiteNavigationMenu",
	service = StagedModelRepository.class
)
public class SiteNavigationMenuStagedModelRepository
	implements StagedModelRepository<SiteNavigationMenu> {

	@Override
	public SiteNavigationMenu addStagedModel(
			PortletDataContext portletDataContext,
			SiteNavigationMenu siteNavigationMenu)
		throws PortalException {

		long userId = portletDataContext.getUserId(
			siteNavigationMenu.getUserUuid());

		ServiceContext serviceContext = portletDataContext.createServiceContext(
			siteNavigationMenu);

		if (portletDataContext.isDataStrategyMirror()) {
			serviceContext.setUuid(siteNavigationMenu.getUuid());
		}

		_createUniqueName(siteNavigationMenu);

		return _siteNavigationMenuLocalService.addSiteNavigationMenu(
			userId, siteNavigationMenu.getGroupId(),
			siteNavigationMenu.getName(), siteNavigationMenu.getType(),
			siteNavigationMenu.isAuto(), serviceContext);
	}

	@Override
	public void deleteStagedModel(SiteNavigationMenu siteNavigationMenu)
		throws PortalException {

		_siteNavigationMenuLocalService.deleteSiteNavigationMenu(
			siteNavigationMenu);
	}

	@Override
	public void deleteStagedModel(
			String uuid, long groupId, String className, String extraData)
		throws PortalException {

		SiteNavigationMenu siteNavigationMenu =
			fetchStagedModelByUuidAndGroupId(uuid, groupId);

		if (siteNavigationMenu != null) {
			deleteStagedModel(siteNavigationMenu);
		}
	}

	@Override
	public void deleteStagedModels(PortletDataContext portletDataContext)
		throws PortalException {

		_siteNavigationMenuLocalService.deleteSiteNavigationMenus(
			portletDataContext.getScopeGroupId());
	}

	@Override
	public SiteNavigationMenu fetchMissingReference(String uuid, long groupId) {
		return _stagedModelRepositoryHelper.fetchMissingReference(
			uuid, groupId, this);
	}

	@Override
	public SiteNavigationMenu fetchStagedModelByUuidAndGroupId(
		String uuid, long groupId) {

		return _siteNavigationMenuLocalService.
			fetchSiteNavigationMenuByUuidAndGroupId(uuid, groupId);
	}

	@Override
	public List<SiteNavigationMenu> fetchStagedModelsByUuidAndCompanyId(
		String uuid, long companyId) {

		return _siteNavigationMenuLocalService.
			getSiteNavigationMenusByUuidAndCompanyId(
				uuid, companyId, QueryUtil.ALL_POS, QueryUtil.ALL_POS,
				new StagedModelModifiedDateComparator<>());
	}

	@Override
	public ExportActionableDynamicQuery getExportActionableDynamicQuery(
		PortletDataContext portletDataContext) {

		return _siteNavigationMenuLocalService.getExportActionableDynamicQuery(
			portletDataContext);
	}

	@Override
	public SiteNavigationMenu getStagedModel(long siteNavigationMenuId)
		throws PortalException {

		return _siteNavigationMenuLocalService.getSiteNavigationMenu(
			siteNavigationMenuId);
	}

	@Override
	public SiteNavigationMenu saveStagedModel(
		SiteNavigationMenu siteNavigationMenu) {

		return _siteNavigationMenuLocalService.updateSiteNavigationMenu(
			siteNavigationMenu);
	}

	@Override
	public SiteNavigationMenu updateStagedModel(
			PortletDataContext portletDataContext,
			SiteNavigationMenu siteNavigationMenu)
		throws PortalException {

		long userId = portletDataContext.getUserId(
			siteNavigationMenu.getUserUuid());

		SiteNavigationMenu existingSiteNavigationMenu =
			_siteNavigationMenuLocalService.
				fetchSiteNavigationMenuByUuidAndGroupId(
					siteNavigationMenu.getUuid(),
					siteNavigationMenu.getGroupId());

		if (_wasBaseNameChanged(
				siteNavigationMenu.getName(),
				existingSiteNavigationMenu.getName())) {

			_createUniqueName(siteNavigationMenu);
		}
		else {
			siteNavigationMenu.setName(existingSiteNavigationMenu.getName());
		}

		return _siteNavigationMenuLocalService.updateSiteNavigationMenu(
			userId, siteNavigationMenu.getSiteNavigationMenuId(),
			siteNavigationMenu.getGroupId(), siteNavigationMenu.getName(),
			siteNavigationMenu.getType(), siteNavigationMenu.isAuto());
	}

	private void _createUniqueName(SiteNavigationMenu siteNavigationMenu) {
		String name = siteNavigationMenu.getName();

		while (_siteNavigationMenuLocalService.fetchSiteNavigationMenu(
					siteNavigationMenu.getGroupId(),
					siteNavigationMenu.getName()) != null) {

			StringBundler sb = new StringBundler(3);

			sb.append(name);
			sb.append(_SEPARATOR);
			sb.append(StringUtil.randomString(12));

			siteNavigationMenu.setName(sb.toString());
		}
	}

	private boolean _wasBaseNameChanged(
		String importedName, String existingName) {

		boolean wasNameChanged = false;

		if (!importedName.equals(existingName)) {
			String strippedExistingName =
				StringUtil.split(existingName, _SEPARATOR)[0];

			if (!importedName.equals(strippedExistingName)) {
				wasNameChanged = true;
			}
		}

		return wasNameChanged;
	}

	private static final String _SEPARATOR = "_Imported_";

	@Reference
	private SiteNavigationMenuLocalService _siteNavigationMenuLocalService;

	@Reference
	private StagedModelRepositoryHelper _stagedModelRepositoryHelper;

}