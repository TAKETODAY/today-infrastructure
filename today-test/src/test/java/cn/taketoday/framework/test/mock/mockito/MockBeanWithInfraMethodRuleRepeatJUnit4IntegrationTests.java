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

package cn.taketoday.framework.test.mock.mockito;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import cn.taketoday.test.annotation.Repeat;
import cn.taketoday.test.context.junit4.rules.InfraMethodRule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockBean} and {@link Repeat}.
 *
 * @author Andy Wilkinson
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/27693">gh-27693</a>
 */
public class MockBeanWithInfraMethodRuleRepeatJUnit4IntegrationTests {

	@Rule
	public final InfraMethodRule InfraMethodRule = new InfraMethodRule();

	@MockBean
	private FirstService first;

	private static int invocations;

	@AfterClass
	public static void afterClass() {
		assertThat(invocations).isEqualTo(2);
	}

	@Test
	@Repeat(2)
	public void repeatedTest() {
		invocations++;
	}

	interface FirstService {

		String greeting();

	}

}
