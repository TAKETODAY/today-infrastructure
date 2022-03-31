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

package cn.taketoday.test.context.junit.jupiter.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.junit.SpringJUnitJupiterTestSuite;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests which demonstrate use of the Spring MVC Test Framework and
 * the Spring TestContext Framework with JUnit Jupiter and the
 * {@link ApplicationExtension} (via a custom
 * {@link JUnitWebConfig @ApplicationJUnitWebConfig} composed annotation).
 *
 * <p>Note how the {@link #springMvcTest(WebApplicationContext)} test method
 * has the {@link WebApplicationContext} injected as a method parameter.
 * This allows the {@link MockMvc} instance to be configured local to the
 * test method without any fields in the test class.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see ApplicationExtension
 * @see JUnitWebConfig
 * @see MultipleWebRequestsSpringExtensionTests
 * @see cn.taketoday.test.context.junit.jupiter.SpringExtensionTests
 * @see cn.taketoday.test.context.junit.jupiter.ComposedSpringExtensionTests
 */
@JUnitWebConfig(WebConfig.class)
@DisplayName("Web ApplicationExtension Tests")
class WebSpringExtensionTests {

	@Test
	void springMvcTest(WebApplicationContext wac) throws Exception {
		webAppContextSetup(wac).build()
			.perform(get("/person/42").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name", is("Dilbert")));
	}

}
