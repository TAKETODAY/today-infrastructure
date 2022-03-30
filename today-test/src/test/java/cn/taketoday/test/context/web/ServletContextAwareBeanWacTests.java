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

import org.junit.Test;

import cn.taketoday.test.context.junit4.JUnitTestingUtils;

import static cn.taketoday.test.context.junit4.JUnitTestingUtils.runTestsAndAssertCounters;

/**
 * Introduced to investigate claims in SPR-11145.
 *
 * <p>Yes, this test class does in fact use JUnit to run JUnit. ;)
 *
 * @author Sam Brannen
 * @since 4.0.2
 */
public class ServletContextAwareBeanWacTests {

	@Test
	public void ensureServletContextAwareBeanIsProcessedProperlyWhenExecutingJUnitManually() throws Exception {
		JUnitTestingUtils.runTestsAndAssertCounters(BasicAnnotationConfigWacTests.class, 3, 0, 3, 0, 0);
	}

}
