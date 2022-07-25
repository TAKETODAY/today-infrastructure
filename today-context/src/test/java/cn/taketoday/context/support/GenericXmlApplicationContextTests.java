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

package cn.taketoday.context.support;

import org.junit.jupiter.api.Test;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for {@link GenericXmlApplicationContext}.
 *
 * See SPR-7530.
 *
 * @author Chris Beams
 */
public class GenericXmlApplicationContextTests {

	private static final Class<?> RELATIVE_CLASS = GenericXmlApplicationContextTests.class;
	private static final String RESOURCE_BASE_PATH = ClassUtils.classPackageAsResourcePath(RELATIVE_CLASS);
	private static final String RESOURCE_NAME = GenericXmlApplicationContextTests.class.getSimpleName() + "-context.xml";
	private static final String FQ_RESOURCE_PATH = RESOURCE_BASE_PATH + '/' + RESOURCE_NAME;
	private static final String TEST_BEAN_NAME = "testBean";


	@Test
	public void classRelativeResourceLoading_ctor() {
		ApplicationContext ctx = new GenericXmlApplicationContext(RELATIVE_CLASS, RESOURCE_NAME);
		assertThat(ctx.containsBean(TEST_BEAN_NAME)).isTrue();
	}

	@Test
	public void classRelativeResourceLoading_load() {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
		ctx.load(RELATIVE_CLASS, RESOURCE_NAME);
		ctx.refresh();
		assertThat(ctx.containsBean(TEST_BEAN_NAME)).isTrue();
	}

	@Test
	public void fullyQualifiedResourceLoading_ctor() {
		ApplicationContext ctx = new GenericXmlApplicationContext(FQ_RESOURCE_PATH);
		assertThat(ctx.containsBean(TEST_BEAN_NAME)).isTrue();
	}

	@Test
	public void fullyQualifiedResourceLoading_load() {
		GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
		ctx.load(FQ_RESOURCE_PATH);
		ctx.refresh();
		assertThat(ctx.containsBean(TEST_BEAN_NAME)).isTrue();
	}
}
