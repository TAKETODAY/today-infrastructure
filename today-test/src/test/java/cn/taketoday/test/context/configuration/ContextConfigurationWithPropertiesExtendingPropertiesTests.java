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

package cn.taketoday.test.context.configuration;

import org.junit.Test;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.junit4.PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which verify that the same custom {@link ContextLoader} can
 * be used at all levels within a test class hierarchy when the
 * {@code loader} is explicitly declared via {@link ContextConfiguration
 * &#064;ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 3.0
 * @see PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests
 * @see ContextConfigurationWithPropertiesExtendingPropertiesAndInheritedLoaderTests
 */
@SuppressWarnings("deprecation")
@ContextConfiguration(loader = cn.taketoday.test.context.support.GenericPropertiesContextLoader.class)
public class ContextConfigurationWithPropertiesExtendingPropertiesTests extends
		PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests {

	@Autowired
	private Pet dog;

	@Autowired
	private String testString2;


	@Test
	public void verifyExtendedAnnotationAutowiredFields() {
		assertThat(this.dog).as("The dog field should have been autowired.").isNotNull();
		assertThat(this.dog.getName()).isEqualTo("Fido");

		assertThat(this.testString2).as("The testString2 field should have been autowired.").isNotNull();
		assertThat(this.testString2).isEqualTo("Test String #2");
	}

}
