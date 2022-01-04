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

package com.liferay.info.item.provider;

import com.liferay.info.field.InfoField;
import com.liferay.info.field.InfoFieldValue;
import com.liferay.info.field.type.ImageInfoFieldType;
import com.liferay.info.field.type.TextInfoFieldType;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;

import java.util.List;
import java.util.Locale;

/**
 * @author Balázs Sáfrány-Kovalik
 */
public interface InfoItemFieldValuesProviderHelper {

	public void setUserPortraitInfoFields(
			List<InfoFieldValue<Object>> fieldValues,
			InfoField<TextInfoFieldType> nameInfoField,
			InfoField<ImageInfoFieldType> profileImageInfoField, Locale locale,
			String pathImage, User user)
		throws PortalException;

}