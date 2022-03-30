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

package cn.taketoday.test.context.support;

import org.junit.jupiter.api.Test;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.test.context.support.AbstractGenericContextLoader;
import cn.taketoday.test.context.support.GenericXmlContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test which verifies that extensions of
 * {@link AbstractGenericContextLoader} are able to <em>customize</em> the
 * newly created {@code ApplicationContext}. Specifically, this test
 * addresses the issues raised in <a
 * href="https://opensource.atlassian.com/projects/spring/browse/SPR-4008"
 * target="_blank">SPR-4008</a>: <em>Supply an opportunity to customize context
 * before calling refresh in ContextLoaders</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 */
class CustomizedGenericXmlContextLoaderTests {

	@Test
	void customizeContext() throws Exception {
		StringBuilder builder = new StringBuilder();
		String expectedContents = "customizeContext() was called";

		new GenericXmlContextLoader() {

			@Override
			protected void customizeContext(GenericApplicationContext context) {
				assertThat(context.isActive()).as("The context should not yet have been refreshed.").isFalse();
				builder.append(expectedContents);
			}
		}.loadContext("classpath:/org/springframework/test/context/support/CustomizedGenericXmlContextLoaderTests-context.xml");

		assertThat(builder.toString()).as("customizeContext() should have been called.").isEqualTo(expectedContents);
	}

}
