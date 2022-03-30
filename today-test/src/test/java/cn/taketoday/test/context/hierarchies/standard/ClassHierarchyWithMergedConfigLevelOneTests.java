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

package cn.taketoday.test.context.hierarchies.standard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(ApplicationExtension.class)
@ContextHierarchy({
//
	@ContextConfiguration(name = "parent", classes = ClassHierarchyWithMergedConfigLevelOneTests.AppConfig.class),//
	@ContextConfiguration(name = "child", classes = ClassHierarchyWithMergedConfigLevelOneTests.UserConfig.class) //
})
class ClassHierarchyWithMergedConfigLevelOneTests {

	@Configuration
	static class AppConfig {

		@Bean
		String parent() {
			return "parent";
		}
	}

	@Configuration
	static class UserConfig {

		@Autowired
		private AppConfig appConfig;


		@Bean
		String user() {
			return appConfig.parent() + " + user";
		}

		@Bean
		String beanFromUserConfig() {
			return "from UserConfig";
		}
	}


	@Autowired
	protected String parent;

	@Autowired
	protected String user;

	@Autowired(required = false)
	@Qualifier("beanFromUserConfig")
	protected String beanFromUserConfig;

	@Autowired
	protected ApplicationContext context;


	@Test
	void loadContextHierarchy() {
		assertThat(context).as("child ApplicationContext").isNotNull();
		assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
		assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();
		assertThat(parent).isEqualTo("parent");
		assertThat(user).isEqualTo("parent + user");
		assertThat(beanFromUserConfig).isEqualTo("from UserConfig");
	}

}
