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

package cn.taketoday.test.context.junit4.hybrid;

import org.junit.Test;
import org.junit.runner.RunWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for hybrid {@link SmartContextLoader} implementations that
 * support path-based and class-based resources simultaneously, as is done in
 * Spring Boot.
 *
 * @author Sam Brannen
 * @since 4.0
 * @see HybridContextLoader
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(loader = HybridContextLoader.class)
public class HybridContextLoaderTests {

	@Configuration
	static class Config {

		@Bean
		public String fooFromJava() {
			return "Java";
		}

		@Bean
		public String enigma() {
			return "enigma from Java";
		}
	}


	@Autowired
	private String fooFromXml;

	@Autowired
	private String fooFromJava;

	@Autowired
	private String enigma;


	@Test
	public void verifyContentsOfHybridApplicationContext() {
		assertThat(fooFromXml).isEqualTo("XML");
		assertThat(fooFromJava).isEqualTo("Java");

		// Note: the XML bean definition for "enigma" always wins since
		// ConfigurationClassBeanDefinitionReader.isOverriddenByExistingDefinition()
		// lets XML bean definitions override those "discovered" later via an
		// @Bean method.
		assertThat(enigma).isEqualTo("enigma from XML");
	}

}
