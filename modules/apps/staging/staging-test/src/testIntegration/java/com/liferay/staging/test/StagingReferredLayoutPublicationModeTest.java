/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.staging.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.data.engine.rest.resource.v2_0.DataDefinitionResource;
import com.liferay.fragment.constants.FragmentConstants;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.layout.page.template.constants.LayoutPageTemplateEntryTypeConstants;
import com.liferay.layout.page.template.model.LayoutPageTemplateEntry;
import com.liferay.layout.page.template.service.LayoutPageTemplateEntryLocalService;
import com.liferay.layout.test.util.LayoutTestUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.background.task.model.BackgroundTask;
import com.liferay.portal.background.task.service.BackgroundTaskLocalService;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.kernel.backgroundtask.constants.BackgroundTaskConstants;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.segments.service.SegmentsExperienceLocalService;
import com.liferay.staging.configuration.StagingConfiguration;
import com.liferay.staging.constants.StagingReferredLayoutPublicationModeKeys;

import java.util.Date;
import java.util.List;

import org.junit.AfterClass;
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
@Sync(cleanTransaction = true)
public class StagingReferredLayoutPublicationModeTest
	extends BaseLocalStagingTestCase {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@AfterClass
	public static void tearDownClass() throws Exception {
		ConfigurationTestUtil.deleteConfiguration(
			StagingConfiguration.class.getName());
	}

	@Before
	public void setUp() throws Exception {
		UserTestUtil.setUser(TestPropsValues.getUser());

		_setStagingReferredLayoutPublicationMode(
			StagingReferredLayoutPublicationModeKeys.ALL);

		liveGroup = GroupTestUtil.addGroup();

		enableLocalStaging();

		stagingGroup = liveGroup.getStagingGroup();

		LayoutPageTemplateEntry masterLayoutPageTemplateEntry =
			_layoutPageTemplateEntryLocalService.addLayoutPageTemplateEntry(
				null, TestPropsValues.getUserId(), stagingGroup.getGroupId(), 0,
				RandomTestUtil.randomString(),
				LayoutPageTemplateEntryTypeConstants.MASTER_LAYOUT, 0,
				WorkflowConstants.STATUS_APPROVED,
				ServiceContextTestUtil.getServiceContext(
					stagingGroup.getGroupId()));

		_masterLayout1 = _layoutLocalService.fetchLayout(
			masterLayoutPageTemplateEntry.getPlid());

		masterLayoutPageTemplateEntry =
			_layoutPageTemplateEntryLocalService.addLayoutPageTemplateEntry(
				null, TestPropsValues.getUserId(), stagingGroup.getGroupId(), 0,
				RandomTestUtil.randomString(),
				LayoutPageTemplateEntryTypeConstants.MASTER_LAYOUT, 0,
				WorkflowConstants.STATUS_APPROVED,
				ServiceContextTestUtil.getServiceContext(
					stagingGroup.getGroupId()));

		_masterLayout2 = _layoutLocalService.fetchLayout(
			masterLayoutPageTemplateEntry.getPlid());

		_parentLayout1 = LayoutTestUtil.addTypeContentLayout(
			stagingGroup, false, false, _masterLayout1.getPlid());
		_parentLayout2 = LayoutTestUtil.addTypeContentLayout(
			stagingGroup, false, false, _masterLayout2.getPlid());

		_childLayout1 = LayoutTestUtil.addTypeContentChildLayout(
			_parentLayout1, _parentLayout1.getMasterLayoutPlid());

		_childLayout2 = LayoutTestUtil.addTypeContentChildLayout(
			_parentLayout2, _parentLayout2.getMasterLayoutPlid());

		_addButtonLinkFragment(_parentLayout1, _childLayout2);

		publishLayouts();
	}

	@Test
	public void testAllPagesNewPages() throws Exception {
		_setStagingReferredLayoutPublicationMode(
			StagingReferredLayoutPublicationModeKeys.ALL);

		Layout parentLayout = LayoutTestUtil.addTypeContentLayout(
			stagingGroup, false, false, _masterLayout1.getPlid());

		Layout childLayout = LayoutTestUtil.addTypeContentChildLayout(
			parentLayout, parentLayout.getMasterLayoutPlid());

		_addButtonLinkFragment(childLayout, _childLayout2);

		publishLayouts(new long[] {childLayout.getLayoutId()});

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A new parent layout", parentLayout);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A new child layout", childLayout);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A referred layout", _childLayout2);
	}

	@Test
	public void testAllPagesPublished() throws Exception {
		_setStagingReferredLayoutPublicationMode(
			StagingReferredLayoutPublicationModeKeys.ALL);

		publishLayouts(new long[] {_parentLayout1.getLayoutId()});

		// Order : parent > draft > master > reference

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A selected layout", _parentLayout1);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A draft layout of a selected layout",
			_parentLayout1.fetchDraftLayout());

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A referred master layout", _masterLayout1);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A referred layout", _childLayout2);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A parent layout of a referred layout", _parentLayout2);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A draft layout of a parent layout of a referred layout",
			_parentLayout2.fetchDraftLayout());

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A referred master layout of a referred layout", _masterLayout2);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A draft layout of a referred layout",
			_childLayout2.fetchDraftLayout());

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"An unselected child layout", _childLayout1);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of an unselected child layout",
			_childLayout1.fetchDraftLayout());
	}

	@Test
	public void testSelectedAndModifiedPagesNewPages() throws Exception {
		_setStagingReferredLayoutPublicationMode(
			StagingReferredLayoutPublicationModeKeys.SELECTED_AND_MODIFIED);

		Layout parentLayout = LayoutTestUtil.addTypeContentLayout(
			stagingGroup, false, false, _masterLayout1.getPlid());

		Layout childLayout = LayoutTestUtil.addTypeContentChildLayout(
			parentLayout, parentLayout.getMasterLayoutPlid());

		_addButtonLinkFragment(childLayout, _childLayout2);

		publishLayouts(new long[] {childLayout.getLayoutId()});

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A new parent layout", parentLayout);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A new child layout", childLayout);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A referred layout", _childLayout2);
	}

	@Test
	public void testSelectedAndModifiedPagesPublished() throws Exception {
		_setStagingReferredLayoutPublicationMode(
			StagingReferredLayoutPublicationModeKeys.SELECTED_AND_MODIFIED);

		_childLayout2 = _modifyLayout(_childLayout2);

		publishLayouts(new long[] {_parentLayout1.getLayoutId()});

		// Order : parent > draft > master > reference

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A selected layout", _parentLayout1);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of a selected layout",
			_parentLayout1.fetchDraftLayout());

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A referred master layout", _masterLayout1);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A referred layout", _childLayout2);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A parent layout of a referred layout", _parentLayout2);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of a parent layout of a referred layout",
			_parentLayout2.fetchDraftLayout());

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A referred master layout of a referred layout", _masterLayout2);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of a referred layout",
			_childLayout2.fetchDraftLayout());

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"An unselected child layout", _childLayout1);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of an unselected child layout",
			_childLayout1.fetchDraftLayout());
	}

	@Test
	public void testSelectedOnlyPagesNewMasterPage() throws Exception {
		_setStagingReferredLayoutPublicationMode(
			StagingReferredLayoutPublicationModeKeys.SELECTED_ONLY);

		LayoutPageTemplateEntry masterLayoutPageTemplateEntry =
			_layoutPageTemplateEntryLocalService.addLayoutPageTemplateEntry(
				null, TestPropsValues.getUserId(), stagingGroup.getGroupId(), 0,
				RandomTestUtil.randomString(),
				LayoutPageTemplateEntryTypeConstants.MASTER_LAYOUT, 0,
				WorkflowConstants.STATUS_APPROVED,
				ServiceContextTestUtil.getServiceContext(
					stagingGroup.getGroupId()));

		Layout newMasterLayout = _layoutLocalService.fetchLayout(
			masterLayoutPageTemplateEntry.getPlid());

		Layout newLayout = LayoutTestUtil.addTypeContentLayout(
			stagingGroup, false, false, newMasterLayout.getPlid());

		publishLayouts(new long[] {newLayout.getLayoutId()});

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A new layout", newLayout);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A master page of a new layout", newMasterLayout);
	}

	@Test
	public void testSelectedOnlyPagesNewPages() throws Exception {
		_setStagingReferredLayoutPublicationMode(
			StagingReferredLayoutPublicationModeKeys.SELECTED_ONLY);

		Layout parentLayout = LayoutTestUtil.addTypeContentLayout(
			stagingGroup, false, false, _masterLayout1.getPlid());

		Layout childLayout = LayoutTestUtil.addTypeContentChildLayout(
			parentLayout, parentLayout.getMasterLayoutPlid());

		_addButtonLinkFragment(childLayout, _childLayout2);

		publishLayouts(new long[] {childLayout.getLayoutId()});

		_assertPublicationFailed();

		publishLayouts(
			new long[] {childLayout.getLayoutId(), parentLayout.getLayoutId()});

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A new parent layout", parentLayout);

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A new child layout", childLayout);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A referred layout", _childLayout2);
	}

	@Test
	public void testSelectedOnlyPagesPublished() throws Exception {
		_setStagingReferredLayoutPublicationMode(
			StagingReferredLayoutPublicationModeKeys.SELECTED_ONLY);

		publishLayouts(new long[] {_parentLayout1.getLayoutId()});

		// Order : parent > draft > master > reference

		_assertLayoutWasModifiedOnLiveAfterPublication(
			"A selected layout", _parentLayout1);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of a selected layout",
			_parentLayout1.fetchDraftLayout());

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A referred master layout", _masterLayout1);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A referred layout", _childLayout2);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A parent layout of a referred layout", _parentLayout2);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of a parent layout of a referred layout",
			_parentLayout2.fetchDraftLayout());

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A referred master layout of a referred layout", _masterLayout2);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of a referred layout",
			_childLayout2.fetchDraftLayout());

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"An unselected child layout", _childLayout1);

		_assertLayoutWasNotModifiedOnLiveAfterPublication(
			"A draft layout of an unselected child layout",
			_childLayout1.fetchDraftLayout());
	}

	protected void publishLayouts() throws PortalException {
		_publicationDate = new Date();

		super.publishLayouts();
	}

	protected void publishLayouts(long[] layoutIds) throws PortalException {
		_publicationDate = new Date();

		super.publishLayouts(layoutIds);
	}

	private void _addButtonLinkFragment(Layout source, Layout target)
		throws Exception {

		FragmentEntryLink fragmentEntryLink =
			_fragmentEntryLinkLocalService.addFragmentEntryLink(
				TestPropsValues.getUserId(), stagingGroup.getGroupId(), 0,
				RandomTestUtil.randomLong(),
				_segmentsExperienceLocalService.
					fetchDefaultSegmentsExperienceId(source.getPlid()),
				source.getPlid(), StringPool.BLANK, StringPool.BLANK,
				StringPool.BLANK, StringPool.BLANK, StringPool.BLANK,
				StringPool.BLANK, 0, "BASIC_COMPONENT-button",
				FragmentConstants.TYPE_COMPONENT,
				ServiceContextTestUtil.getServiceContext());

		String editableValues = read("fragment_button_editable_values.json");

		editableValues = StringUtil.replace(
			editableValues, "[@GROUP_ID@]",
			String.valueOf(stagingGroup.getGroupId()));
		editableValues = StringUtil.replace(
			editableValues, "[@LAYOUT_ID@]",
			String.valueOf(target.getLayoutId()));
		editableValues = StringUtil.replace(
			editableValues, "[@UUID@]", target.getUuid());

		_fragmentEntryLinkLocalService.updateFragmentEntryLink(
			TestPropsValues.getUserId(),
			fragmentEntryLink.getFragmentEntryLinkId(), editableValues);
	}

	private void _assertLayoutWasModifiedOnLiveAfterPublication(
			String message, Layout layout)
		throws Exception {

		Layout liveLayout = _layoutLocalService.getLayoutByUuidAndGroupId(
			layout.getUuid(), liveGroup.getGroupId(), layout.isPrivateLayout());

		Assert.assertTrue(
			message + " should have been modified on the Live site",
			_publicationDate.before(liveLayout.getModifiedDate()));
	}

	private void _assertLayoutWasNotModifiedOnLiveAfterPublication(
			String message, Layout layout)
		throws Exception {

		Layout liveLayout = _layoutLocalService.getLayoutByUuidAndGroupId(
			layout.getUuid(), liveGroup.getGroupId(), layout.isPrivateLayout());

		Assert.assertFalse(
			message + " should NOT have been modified on the Live site.",
			_publicationDate.before(liveLayout.getModifiedDate()));
	}

	private void _assertPublicationFailed() throws Exception {
		List<BackgroundTask> failedBackgroundTasks =
			_backgroundTaskLocalService.getBackgroundTasks(
				stagingGroup.getGroupId(),
				"com.liferay.exportimport.internal.background.task." +
					"LayoutStagingBackgroundTaskExecutor",
				BackgroundTaskConstants.STATUS_FAILED);

		Assert.assertFalse(
			"There should be a failed staging publication",
			failedBackgroundTasks.isEmpty());

		_backgroundTaskLocalService.deleteGroupBackgroundTasks(
			stagingGroup.getGroupId());
	}

	private Layout _modifyLayout(Layout layout) {
		layout = _layoutLocalService.fetchLayout(layout.getPlid());

		layout.setModifiedDate(new Date());

		return _layoutLocalService.updateLayout(layout);
	}

	private void _setStagingReferredLayoutPublicationMode(String mode)
		throws Exception {

		_configurationProvider.saveCompanyConfiguration(
			StagingConfiguration.class, CompanyThreadLocal.getCompanyId(),
			HashMapDictionaryBuilder.<String, Object>put(
				"publishReferredLayoutsMode", mode
			).build());
	}

	@Inject
	private static ConfigurationProvider _configurationProvider;

	@Inject
	private BackgroundTaskLocalService _backgroundTaskLocalService;

	private Layout _childLayout1;
	private Layout _childLayout2;

	@Inject
	private DataDefinitionResource.Factory _dataDefinitionResourceFactory;

	@Inject
	private FragmentEntryLinkLocalService _fragmentEntryLinkLocalService;

	@Inject
	private LayoutLocalService _layoutLocalService;

	@Inject
	private LayoutPageTemplateEntryLocalService
		_layoutPageTemplateEntryLocalService;

	private Layout _masterLayout1;
	private Layout _masterLayout2;
	private Layout _parentLayout1;
	private Layout _parentLayout2;
	private Date _publicationDate;

	@Inject
	private SegmentsExperienceLocalService _segmentsExperienceLocalService;

}