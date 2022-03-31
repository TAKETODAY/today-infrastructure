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

package cn.taketoday.test.context.web;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.context.web.GenericXmlWebContextLoader;
import cn.taketoday.test.context.web.WebMergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link GenericXmlWebContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class GenericXmlWebContextLoaderTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];


	@Test
	void configMustNotContainAnnotatedClasses() throws Exception {
		GenericXmlWebContextLoader loader = new GenericXmlWebContextLoader();
		WebMergedContextConfiguration mergedConfig = new WebMergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
				new Class<?>[] { getClass() }, null, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY,
				"resource/path", loader, null, null);
		assertThatIllegalStateException()
			.isThrownBy(() -> loader.loadContext(mergedConfig))
			.withMessageContaining("does not support annotated classes");
	}

}
