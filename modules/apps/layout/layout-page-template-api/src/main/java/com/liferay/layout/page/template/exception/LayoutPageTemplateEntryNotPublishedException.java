/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.page.template.exception;

import com.liferay.portal.kernel.exception.PortalException;

/**
 * @author Balázs Sáfrány-Kovalik
 */
public class LayoutPageTemplateEntryNotPublishedException
	extends PortalException {

	public LayoutPageTemplateEntryNotPublishedException(
		String externalReferenceCode, int type) {

		super(
			"Layout page template entry " + externalReferenceCode +
				" is not published");

		_externalReferenceCode = externalReferenceCode;
		_type = type;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	public int getType() {
		return _type;
	}

	private final String _externalReferenceCode;
	private final int _type;

}