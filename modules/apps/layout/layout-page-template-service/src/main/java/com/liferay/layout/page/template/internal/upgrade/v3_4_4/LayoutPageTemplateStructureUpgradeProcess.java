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

package com.liferay.layout.page.template.internal.upgrade.v3_4_4;

import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Mark Gulacsy
 */
public class LayoutPageTemplateStructureUpgradeProcess extends UpgradeProcess {

	public LayoutPageTemplateStructureUpgradeProcess(
		LayoutLocalService layoutLocalService) {

		_layoutLocalService = layoutLocalService;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
				"select plid,userId from Layout where type_ = '" +
					LayoutConstants.TYPE_PORTLET + "' and status = 2");
			PreparedStatement preparedStatement2 =
				AutoBatchPreparedStatementUtil.autoBatch(
					connection.prepareStatement(
						"delete from LayoutPageTemplateStructure where " +
							"classPk = ?"));
			ResultSet resultSet = preparedStatement1.executeQuery()) {

			ServiceContext serviceContext = new ServiceContext();

			while (resultSet.next()) {
				_layoutLocalService.updateStatus(
					resultSet.getLong("userId"), resultSet.getLong("plid"), 0,
					serviceContext);

				preparedStatement2.setLong(1, resultSet.getLong("plid"));
				preparedStatement2.addBatch();
			}

			preparedStatement2.executeBatch();
		}
	}

	private final LayoutLocalService _layoutLocalService;

}