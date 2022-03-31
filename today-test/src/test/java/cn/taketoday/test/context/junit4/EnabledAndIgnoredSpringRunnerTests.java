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

package cn.taketoday.test.context.junit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import cn.taketoday.test.annotation.IfProfileValue;
import cn.taketoday.test.annotation.ProfileValueSource;
import cn.taketoday.test.annotation.ProfileValueSourceConfiguration;
import cn.taketoday.test.context.TestExecutionListeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Verifies proper handling of JUnit's {@link Ignore &#064;Ignore} and Spring's
 * {@link IfProfileValue &#064;IfProfileValue} and
 * {@link ProfileValueSourceConfiguration &#064;ProfileValueSourceConfiguration}
 * (with the <em>implicit, default {@link ProfileValueSource}</em>) annotations in
 * conjunction with the {@link Runner}.
 * <p>
 * Note that {@link TestExecutionListeners &#064;TestExecutionListeners} is
 * explicitly configured with an empty list, thus disabling all default
 * listeners.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see HardCodedProfileValueSourceSpringRunnerTests
 */
@RunWith(Runner.class)
@TestExecutionListeners( {})
public class EnabledAndIgnoredSpringRunnerTests {

	protected static final String NAME = "EnabledAndIgnoredSpringRunnerTests.profile_value.name";

	protected static final String VALUE = "enigma";

	protected static int numTestsExecuted = 0;


	@BeforeClass
	public static void setProfileValue() {
		numTestsExecuted = 0;
		System.setProperty(NAME, VALUE);
	}

	@AfterClass
	public static void verifyNumTestsExecuted() {
		assertThat(numTestsExecuted).as("Verifying the number of tests executed.").isEqualTo(3);
	}

	@Test
	@IfProfileValue(name = NAME, value = VALUE + "X")
	public void testIfProfileValueDisabled() {
		numTestsExecuted++;
		fail("The body of a disabled test should never be executed!");
	}

	@Test
	@IfProfileValue(name = NAME, value = VALUE)
	public void testIfProfileValueEnabledViaSingleValue() {
		numTestsExecuted++;
	}

	@Test
	@IfProfileValue(name = NAME, values = { "foo", VALUE, "bar" })
	public void testIfProfileValueEnabledViaMultipleValues() {
		numTestsExecuted++;
	}

	@Test
	public void testIfProfileValueNotConfigured() {
		numTestsExecuted++;
	}

	@Test
	@Ignore
	public void testJUnitIgnoreAnnotation() {
		numTestsExecuted++;
		fail("The body of an ignored test should never be executed!");
	}

}
