/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.page.template.internal.upgrade.v6_2_0;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.segments.constants.SegmentsExperienceConstants;
import com.liferay.segments.model.SegmentsExperience;
import com.liferay.segments.service.SegmentsExperienceLocalService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Balázs Sáfrány-Kovalik
 */
public class LayoutPageTemplateStructureRelUpgradeProcess
	extends UpgradeProcess {

	public LayoutPageTemplateStructureRelUpgradeProcess(
		LayoutLocalService layoutLocalService,
		SegmentsExperienceLocalService segmentsExperienceLocalService,
		UserLocalService userLocalService) {

		_layoutLocalService = layoutLocalService;
		_segmentsExperienceLocalService = segmentsExperienceLocalService;
		_userLocalService = userLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select distinct LayoutPageTemplateStructure.",
					"layoutPageTemplateStructureId, ",
					"LayoutPageTemplateStructure.companyId, ",
					"LayoutPageTemplateStructure.plid, ",
					"LayoutPageTemplateStructure.userId from ",
					"LayoutPageTemplateStructure inner join ",
					"LayoutPageTemplateStructureRel on ",
					"LayoutPageTemplateStructureRel.",
					"layoutPageTemplateStructureId = ",
					"LayoutPageTemplateStructure.",
					"layoutPageTemplateStructureId where ",
					"LayoutPageTemplateStructureRel.segmentsExperienceId = 0"));

			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				_repairLayoutPageTemplateStructureRels(
					resultSet.getLong("layoutPageTemplateStructureId"),
					resultSet.getLong("companyId"), resultSet.getLong("plid"),
					resultSet.getLong("userId"));
			}
		}
	}

	private void _deleteLayoutPageTemplateStructureRel(
			long layoutPageTemplateStructureId)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"delete from LayoutPageTemplateStructureRel where ",
					"layoutPageTemplateStructureId = ? and ",
					"segmentsExperienceId = 0"))) {

			preparedStatement.setLong(1, layoutPageTemplateStructureId);

			preparedStatement.executeUpdate();
		}
	}

	private long _getDefaultSegmentsExperienceId(
			long companyId, long plid, long userId)
		throws Exception {

		long segmentsExperienceId =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperienceId(
				plid);

		if (segmentsExperienceId != SegmentsExperienceConstants.ID_DEFAULT) {
			return segmentsExperienceId;
		}

		User user = _userLocalService.fetchUser(userId);

		if (user == null) {
			userId = _userLocalService.getGuestUserId(companyId);
		}

		SegmentsExperience segmentsExperience =
			_segmentsExperienceLocalService.addDefaultSegmentsExperience(
				null, userId, plid, new ServiceContext());

		return segmentsExperience.getSegmentsExperienceId();
	}

	private boolean _hasLayoutPageTemplateStructureRel(
			long layoutPageTemplateStructureId, long segmentsExperienceId)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select layoutPageTemplateStructureRelId from ",
					"LayoutPageTemplateStructureRel where ",
					"layoutPageTemplateStructureId = ? and ",
					"segmentsExperienceId = ?"))) {

			preparedStatement.setLong(1, layoutPageTemplateStructureId);
			preparedStatement.setLong(2, segmentsExperienceId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	private void _repairLayoutPageTemplateStructureRels(
			long layoutPageTemplateStructureId, long companyId, long plid,
			long userId)
		throws Exception {

		Layout layout = _layoutLocalService.fetchLayout(plid);

		if (layout == null) {
			_deleteLayoutPageTemplateStructureRel(
				layoutPageTemplateStructureId);

			return;
		}

		long segmentsExperienceId = _getDefaultSegmentsExperienceId(
			companyId, plid, userId);

		if (_hasLayoutPageTemplateStructureRel(
				layoutPageTemplateStructureId, segmentsExperienceId)) {

			_deleteLayoutPageTemplateStructureRel(
				layoutPageTemplateStructureId);
		}
		else {
			_updateLayoutPageTemplateStructureRel(
				layoutPageTemplateStructureId, segmentsExperienceId);
		}
	}

	private void _updateLayoutPageTemplateStructureRel(
			long layoutPageTemplateStructureId, long segmentsExperienceId)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"update LayoutPageTemplateStructureRel set ",
					"segmentsExperienceId = ? where ",
					"layoutPageTemplateStructureId = ? and ",
					"segmentsExperienceId = 0"))) {

			preparedStatement.setLong(1, segmentsExperienceId);
			preparedStatement.setLong(2, layoutPageTemplateStructureId);

			preparedStatement.executeUpdate();
		}
	}

	private final LayoutLocalService _layoutLocalService;
	private final SegmentsExperienceLocalService
		_segmentsExperienceLocalService;
	private final UserLocalService _userLocalService;

}