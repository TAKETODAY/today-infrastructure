/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.view.xslt;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @since 2.0
 */
public class XsltViewResolverTests {

	@Test
	public void resolveView() throws Exception {
		StaticApplicationContext ctx = new StaticApplicationContext();

		String prefix = ClassUtils.classPackageAsResourcePath(getClass());
		String suffix = ".xsl";
		String viewName = "products";

		XsltViewResolver viewResolver = new XsltViewResolver();
		viewResolver.setPrefix(prefix);
		viewResolver.setSuffix(suffix);
		viewResolver.setApplicationContext(ctx);

		XsltView view = (XsltView) viewResolver.resolveViewName(viewName, Locale.ENGLISH);
		assertThat(view).as("View should not be null").isNotNull();
		assertThat(view.getUrl()).as("Incorrect URL").isEqualTo((prefix + viewName + suffix));
	}
}
