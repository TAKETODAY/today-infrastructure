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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.web.servlet.WebApplicationContext;

/**
 * Integration tests which demonstrate use of the Web MVC Test Framework and
 * the TestContext Framework with JUnit Jupiter and the
 * {@link InfraExtension} (via a custom
 * {@link JUnitWebConfig @ApplicationJUnitWebConfig} composed annotation).
 *
 * <p>Note how the {@link #springMvcTest(WebApplicationContext)} test method
 * has the {@link WebApplicationContext} injected as a method parameter.
 * This allows the {@link MockMvc} instance to be configured local to the
 * test method without any fields in the test class.
 *
 * @author Sam Brannen
 * @see InfraExtension
 * @see JUnitWebConfig
 * @see MultipleWebRequestsSpringExtensionTests
 * @see cn.taketoday.test.context.junit.jupiter.SpringExtensionTests
 * @see cn.taketoday.test.context.junit.jupiter.ComposedSpringExtensionTests
 * @since 4.0
 */
@Disabled
@JUnitWebConfig(WebConfig.class)
@DisplayName("Web ApplicationExtension Tests")
class WebSpringExtensionTests {

  @Test
  @Disabled("TODO-web")
  void springMvcTest(WebApplicationContext wac) throws Exception {
//    webAppContextSetup(wac).build()
//            .perform(get("/person/42").accept(MediaType.APPLICATION_JSON))
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.name", is("Dilbert")));
  }

}
