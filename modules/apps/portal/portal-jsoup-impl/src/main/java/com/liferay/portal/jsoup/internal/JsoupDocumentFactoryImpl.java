/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.jsoup.internal;

import com.liferay.portal.jsoup.JsoupDocumentFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.osgi.service.component.annotations.Component;

/**
 * @author Balázs Sáfrány-Kovalik
 */
@Component(service = JsoupDocumentFactory.class)
public class JsoupDocumentFactoryImpl implements JsoupDocumentFactory {

	@Override
	public Document parse(String html) {
		return _disablePrettyPrint(Jsoup.parse(html));
	}

	@Override
	public Document parseBodyFragment(String html) {
		return _disablePrettyPrint(Jsoup.parseBodyFragment(html));
	}

	private Document _disablePrettyPrint(Document document) {
		Document.OutputSettings outputSettings = document.outputSettings();

		outputSettings.prettyPrint(false);

		return document;
	}

}