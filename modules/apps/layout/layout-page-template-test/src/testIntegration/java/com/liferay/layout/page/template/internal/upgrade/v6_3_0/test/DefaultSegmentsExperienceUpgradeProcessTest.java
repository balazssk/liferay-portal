/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.page.template.internal.upgrade.v6_3_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.counter.kernel.service.CounterLocalService;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.layout.test.util.LayoutTestUtil;
import com.liferay.layout.utility.page.model.LayoutUtilityPageEntry;
import com.liferay.layout.utility.page.service.LayoutUtilityPageEntryLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.dao.orm.EntityCache;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.service.ResourceLocalService;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.upgrade.test.util.UpgradeTestUtil;
import com.liferay.segments.model.SegmentsExperience;
import com.liferay.segments.service.SegmentsExperienceLocalService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Balázs Sáfrány-Kovalik
 */
@RunWith(Arquillian.class)
public class DefaultSegmentsExperienceUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
	}

	@Test
	@TestInfo("LPD-103969")
	public void testUpgradeAddsMissingDefaultSegmentsExperience()
		throws Exception {

		Layout layout = LayoutTestUtil.addTypeContentLayout(_group);

		_deleteDefaultSegmentsExperience(layout);

		Assert.assertNull(
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid()));

		_runUpgrade();

		SegmentsExperience segmentsExperience =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid());

		Assert.assertEquals(
			layout.getExternalReferenceCode() +
				LayoutConstants.EXTERNAL_REFERENCE_CODE_SUFFIX_DEFAULT,
			segmentsExperience.getExternalReferenceCode());
	}

	@Test
	@TestInfo("LPD-103969")
	public void testUpgradeAddsMissingDefaultSegmentsExperienceForUtilityPage()
		throws Exception {

		LayoutUtilityPageEntry layoutUtilityPageEntry =
			_layoutUtilityPageEntryLocalService.addLayoutUtilityPageEntry(
				null, TestPropsValues.getUserId(), _group.getGroupId(), 0, 0,
				false, RandomTestUtil.randomString(),
				RandomTestUtil.randomString(), null,
				ServiceContextTestUtil.getServiceContext(_group.getGroupId()));

		Layout layout = _layoutLocalService.getLayout(
			layoutUtilityPageEntry.getPlid());

		_deleteDefaultSegmentsExperience(layout);

		Assert.assertNull(
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid()));

		_runUpgrade();

		SegmentsExperience segmentsExperience =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid());

		Assert.assertEquals(
			layout.getExternalReferenceCode() +
				LayoutConstants.EXTERNAL_REFERENCE_CODE_SUFFIX_DEFAULT,
			segmentsExperience.getExternalReferenceCode());
	}

	@Test
	@TestInfo("LPD-103969")
	public void testUpgradeKeepsExistingDefaultSegmentsExperience()
		throws Exception {

		Layout layout = LayoutTestUtil.addTypeContentLayout(_group);

		SegmentsExperience segmentsExperience =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid());

		_runUpgrade();

		SegmentsExperience upgradedSegmentsExperience =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid());

		Assert.assertEquals(
			segmentsExperience.getSegmentsExperienceId(),
			upgradedSegmentsExperience.getSegmentsExperienceId());
	}

	@Test
	@TestInfo("LPD-103969")
	public void testUpgradeSkipsLayoutWithMultipleMissingSegmentsExperiences()
		throws Exception {

		Layout layout = LayoutTestUtil.addTypeContentLayout(_group);

		_addFragmentEntryLink(layout, RandomTestUtil.randomLong());
		_addFragmentEntryLink(layout, RandomTestUtil.randomLong());

		_deleteDefaultSegmentsExperience(layout);

		_runUpgrade();

		Assert.assertNull(
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid()));
	}

	@Test
	@TestInfo("LPD-103969")
	public void testUpgradeUpdatesFragmentEntryLink() throws Exception {
		Layout layout = LayoutTestUtil.addTypeContentLayout(_group);

		SegmentsExperience segmentsExperience =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid());

		FragmentEntryLink fragmentEntryLink = _addFragmentEntryLink(
			layout, segmentsExperience.getSegmentsExperienceId());

		_deleteDefaultSegmentsExperience(layout);

		_runUpgrade();

		SegmentsExperience upgradedSegmentsExperience =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid());

		Assert.assertEquals(
			upgradedSegmentsExperience.getSegmentsExperienceId(),
			_getFragmentEntryLinkSegmentsExperienceId(
				fragmentEntryLink.getFragmentEntryLinkId()));
	}

	@Test
	@TestInfo("LPD-103969")
	public void testUpgradeUpdatesLayoutPageTemplateStructureRel()
		throws Exception {

		Layout layout = LayoutTestUtil.addTypeContentLayout(_group);

		_deleteDefaultSegmentsExperience(layout);

		_runUpgrade();

		SegmentsExperience segmentsExperience =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid());

		Assert.assertEquals(
			segmentsExperience.getSegmentsExperienceId(),
			_getLayoutPageTemplateStructureRelSegmentsExperienceId(
				layout.getPlid()));
	}

	private FragmentEntryLink _addFragmentEntryLink(
			Layout layout, long segmentsExperienceId)
		throws Exception {

		FragmentEntryLink fragmentEntryLink =
			_fragmentEntryLinkLocalService.createFragmentEntryLink(
				_counterLocalService.increment(
					FragmentEntryLink.class.getName()));

		fragmentEntryLink.setExternalReferenceCode(
			RandomTestUtil.randomString());
		fragmentEntryLink.setGroupId(layout.getGroupId());
		fragmentEntryLink.setCompanyId(layout.getCompanyId());
		fragmentEntryLink.setUserId(TestPropsValues.getUserId());
		fragmentEntryLink.setCreateDate(new Date());
		fragmentEntryLink.setModifiedDate(new Date());
		fragmentEntryLink.setSegmentsExperienceId(segmentsExperienceId);
		fragmentEntryLink.setPlid(layout.getPlid());

		return _fragmentEntryLinkLocalService.addFragmentEntryLink(
			fragmentEntryLink);
	}

	private void _deleteDefaultSegmentsExperience(Layout layout)
		throws Exception {

		SegmentsExperience segmentsExperience =
			_segmentsExperienceLocalService.fetchDefaultSegmentsExperience(
				layout.getPlid());

		_resourceLocalService.deleteResource(
			segmentsExperience.getCompanyId(),
			SegmentsExperience.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL,
			segmentsExperience.getSegmentsExperienceId());

		DB db = DBManagerUtil.getDB();

		db.runSQL(
			"delete from SegmentsExperience where segmentsExperienceId = " +
				segmentsExperience.getSegmentsExperienceId());

		_entityCache.clearCache();
		_multiVMPool.clear();
	}

	private long _getFragmentEntryLinkSegmentsExperienceId(
			long fragmentEntryLinkId)
		throws Exception {

		try (Connection connection = DataAccess.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement(
				"select segmentsExperienceId from FragmentEntryLink where " +
					"fragmentEntryLinkId = ?")) {

			preparedStatement.setLong(1, fragmentEntryLinkId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("segmentsExperienceId");
				}
			}
		}

		return 0;
	}

	private long _getLayoutPageTemplateStructureRelSegmentsExperienceId(
			long plid)
		throws Exception {

		try (Connection connection = DataAccess.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select segmentsExperienceId from ",
					"LayoutPageTemplateStructureRel where ",
					"layoutPageTemplateStructureId = (select ",
					"layoutPageTemplateStructureId from ",
					"LayoutPageTemplateStructure where plid = ?)"))) {

			preparedStatement.setLong(1, plid);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong("segmentsExperienceId");
				}
			}
		}

		return 0;
	}

	private void _runUpgrade() throws Exception {
		UpgradeProcess upgradeProcess = UpgradeTestUtil.getUpgradeStep(
			_upgradeStepRegistrator, _CLASS_NAME);

		upgradeProcess.upgrade();
	}

	private static final String _CLASS_NAME =
		"com.liferay.layout.page.template.internal.upgrade.v6_3_0." +
			"DefaultSegmentsExperienceUpgradeProcess";

	@Inject(
		filter = "(&(component.name=com.liferay.layout.page.template.internal.upgrade.registry.LayoutPageTemplateServiceUpgradeStepRegistrator))"
	)
	private static UpgradeStepRegistrator _upgradeStepRegistrator;

	@Inject
	private CounterLocalService _counterLocalService;

	@Inject
	private EntityCache _entityCache;

	@Inject
	private FragmentEntryLinkLocalService _fragmentEntryLinkLocalService;

	@DeleteAfterTestRun
	private Group _group;

	@Inject
	private LayoutUtilityPageEntryLocalService
		_layoutUtilityPageEntryLocalService;

	@Inject
	private MultiVMPool _multiVMPool;

	@Inject
	private ResourceLocalService _resourceLocalService;

	@Inject
	private SegmentsExperienceLocalService _segmentsExperienceLocalService;

}