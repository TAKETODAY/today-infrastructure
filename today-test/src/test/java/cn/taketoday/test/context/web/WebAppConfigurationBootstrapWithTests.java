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
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.io.Resource;
import cn.taketoday.test.context.BootstrapWith;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.web.context.WebApplicationContext;

/**
 * JUnit-based integration tests that verify support for loading a
 * {@link WebApplicationContext} with a custom {@link WebTestContextBootstrapper}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.3
 */
@JUnitWebConfig
@BootstrapWith(WebAppConfigurationBootstrapWithTests.CustomWebTestContextBootstrapper.class)
class WebAppConfigurationBootstrapWithTests {

	@Autowired
	WebApplicationContext wac;


	@Test
	void webApplicationContextIsLoaded() {
		// from: src/test/webapp/resources/Spring.js
		Resource resource = wac.getResource("/resources/Spring.js");
		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();
	}


	@Configuration
	static class Config {
	}

	/**
	 * Custom {@link WebTestContextBootstrapper} that requires {@code @WebAppConfiguration}
	 * but hard codes the resource base path.
	 */
	static class CustomWebTestContextBootstrapper extends WebTestContextBootstrapper {

		@Override
		protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
			return new WebMergedContextConfiguration(mergedConfig, "src/test/webapp");
		}
	}

}
