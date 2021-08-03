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

package com.liferay.layout.type.controller.asset.display.internal.portlet;

import com.liferay.asset.display.page.portlet.AssetDisplayPageFriendlyURLProvider;
import com.liferay.asset.display.page.util.AssetDisplayPageUtil;
import com.liferay.info.item.InfoItemReference;
import com.liferay.layout.display.page.LayoutDisplayPageObjectProvider;
import com.liferay.layout.display.page.LayoutDisplayPageProvider;
import com.liferay.layout.display.page.LayoutDisplayPageProviderTracker;
import com.liferay.layout.page.template.model.LayoutPageTemplateEntry;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.Portal;

import java.util.Locale;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Tardín
 */
@Component(service = AssetDisplayPageFriendlyURLProvider.class)
public class AssetDisplayPageFriendlyURLProviderImpl
	implements AssetDisplayPageFriendlyURLProvider {

	@Override
	public String getFriendlyURL(
			String className, long classPK, Locale locale,
			ThemeDisplay themeDisplay)
		throws PortalException {

		LayoutDisplayPageProvider<?> layoutDisplayPageProvider =
			_layoutDisplayPageProviderTracker.
				getLayoutDisplayPageProviderByClassName(className);

		if (layoutDisplayPageProvider == null) {
			return null;
		}

		InfoItemReference infoItemReference = new InfoItemReference(
			className, classPK);

		LayoutDisplayPageObjectProvider<?> layoutDisplayPageObjectProvider =
			layoutDisplayPageProvider.getLayoutDisplayPageObjectProvider(
				infoItemReference);

		if (layoutDisplayPageObjectProvider == null) {
			return null;
		}

		String friendlyURL = _getFriendlyURL(
			themeDisplay.getScopeGroupId(), layoutDisplayPageProvider,
			layoutDisplayPageObjectProvider, locale, themeDisplay);

		if ((friendlyURL != null) ||
			(themeDisplay.getScopeGroupId() ==
				layoutDisplayPageObjectProvider.getGroupId())) {

			return friendlyURL;
		}

		return _getFriendlyURL(
			layoutDisplayPageObjectProvider.getGroupId(),
			layoutDisplayPageProvider, layoutDisplayPageObjectProvider, locale,
			themeDisplay);
	}

	@Override
	public String getFriendlyURL(
			String className, long classPK, ThemeDisplay themeDisplay)
		throws PortalException {

		return getFriendlyURL(
			className, classPK, themeDisplay.getLocale(), themeDisplay);
	}

	private String _getFriendlyURL(
			long groupId,
			LayoutDisplayPageProvider<?> layoutDisplayPageProvider,
			LayoutDisplayPageObjectProvider<?> layoutDisplayPageObjectProvider,
			Locale locale, ThemeDisplay themeDisplay)
		throws PortalException {

		LayoutPageTemplateEntry layoutPageTemplateEntry =
			AssetDisplayPageUtil.getAssetDisplayPageLayoutPageTemplateEntry(
				groupId, layoutDisplayPageObjectProvider.getClassNameId(),
				layoutDisplayPageObjectProvider.getClassPK(),
				layoutDisplayPageObjectProvider.getClassTypeId());

		if (layoutPageTemplateEntry == null) {
			return null;
		}

		themeDisplay.setLayout(
			_layoutLocalService.getLayout(layoutPageTemplateEntry.getPlid()));

		StringBundler sb = new StringBundler(3);

		sb.append(_portal.getCanonicalURL(null, themeDisplay, null));
		sb.append(layoutDisplayPageProvider.getURLSeparator());
		sb.append(layoutDisplayPageObjectProvider.getURLTitle(locale));

		return sb.toString();
	}

	@Reference
	private LayoutDisplayPageProviderTracker _layoutDisplayPageProviderTracker;

	@Reference
	private LayoutLocalService _layoutLocalService;

	@Reference
	private Portal _portal;

}