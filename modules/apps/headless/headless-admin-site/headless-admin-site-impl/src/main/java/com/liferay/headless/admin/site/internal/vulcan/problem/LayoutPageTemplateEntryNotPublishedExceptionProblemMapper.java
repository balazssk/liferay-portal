/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.admin.site.internal.vulcan.problem;

import com.liferay.layout.page.template.constants.LayoutPageTemplateEntryTypeConstants;
import com.liferay.layout.page.template.exception.LayoutPageTemplateEntryNotPublishedException;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.vulcan.problem.Problem;
import com.liferay.portal.vulcan.problem.ProblemMapper;

import org.osgi.service.component.annotations.Component;

/**
 * @author Balázs Sáfrány-Kovalik
 */
@Component(service = ProblemMapper.class)
public class LayoutPageTemplateEntryNotPublishedExceptionProblemMapper
	implements ProblemMapper<LayoutPageTemplateEntryNotPublishedException> {

	@Override
	public Problem getProblem(
		LayoutPageTemplateEntryNotPublishedException
			layoutPageTemplateEntryNotPublishedException) {

		String name = _getName(
			layoutPageTemplateEntryNotPublishedException.getType());

		String externalReferenceCode =
			layoutPageTemplateEntryNotPublishedException.
				getExternalReferenceCode();

		String message = StringBundler.concat(
			"The ", name, " must be published before it can be assigned");

		if (Validator.isNotNull(externalReferenceCode)) {
			message = StringBundler.concat(
				"The ", name, " ", externalReferenceCode,
				" must be published before it can be assigned");
		}

		return ProblemUtil.getProblem(
			message, Problem.Status.CONFLICT,
			layoutPageTemplateEntryNotPublishedException);
	}

	private String _getName(int type) {
		if (type == LayoutPageTemplateEntryTypeConstants.DISPLAY_PAGE) {
			return "display page template";
		}

		if (type == LayoutPageTemplateEntryTypeConstants.MASTER_LAYOUT) {
			return "master page";
		}

		return "page template";
	}

}