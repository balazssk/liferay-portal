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

package com.liferay.info.internal.item.provider;

import com.liferay.info.field.InfoField;
import com.liferay.info.field.InfoFieldValue;
import com.liferay.info.field.type.ImageInfoFieldType;
import com.liferay.info.field.type.TextInfoFieldType;
import com.liferay.info.item.provider.InfoItemFieldValuesProviderHelper;
import com.liferay.info.type.WebImage;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserConstants;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;
import java.util.Locale;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 * @author Balázs Sáfrány-Kovalik
 */
@Component(
	immediate = true, property = Constants.SERVICE_RANKING + ":Integer=10",
	service = InfoItemFieldValuesProviderHelper.class
)
public class InfoItemFieldValuesProviderHelperImpl
	implements InfoItemFieldValuesProviderHelper {

	@Override
	public void setUserPortraitInfoFields(
			List<InfoFieldValue<Object>> fieldValues,
			InfoField<TextInfoFieldType> nameInfoField,
			InfoField<ImageInfoFieldType> profileImageInfoField, Locale locale,
			String pathImage, User user)
		throws PortalException {

		String fullName = LanguageUtil.get(
			LocaleUtil.fromLanguageId("en_US"), "deleted-user");
		String portraitURL = StringPool.BLANK;

		if (locale != null) {
			fullName = LanguageUtil.get(locale, "deleted-user");
		}

		if (Validator.isNotNull(pathImage)) {
			portraitURL = pathImage + "/clay/icons.svg#user";
		}

		if (user != null) {
			fullName = user.getFullName();

			if (Validator.isNotNull(pathImage)) {
				portraitURL = UserConstants.getPortraitURL(
					pathImage, user.isMale(), user.getPortraitId(),
					user.getUserUuid());
			}
		}

		fieldValues.add(new InfoFieldValue<>(nameInfoField, fullName));

		WebImage webImage = new WebImage(portraitURL);

		webImage.setAlt(fullName);

		fieldValues.add(new InfoFieldValue<>(profileImageInfoField, webImage));
	}

}