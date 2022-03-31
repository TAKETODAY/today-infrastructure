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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.junit.SpringJUnitJupiterTestSuite;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.context.WebApplicationContext;

import static cn.taketoday.http.MediaType.APPLICATION_JSON;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests which demonstrate how to set up a {@link MockMvc}
 * instance in an {@link BeforeEach @BeforeEach} method with the
 * {@link ApplicationExtension} (registered via a custom
 * {@link JUnitWebConfig @ApplicationJUnitWebConfig} composed annotation).
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @see ApplicationExtension
 * @see JUnitWebConfig
 * @see WebSpringExtensionTests
 * @since 4.0
 */
@JUnitWebConfig(WebConfig.class)
class MultipleWebRequestsSpringExtensionTests {

  MockMvc mockMvc;

  @BeforeEach
  void setUpMockMvc(WebApplicationContext wac) {
    this.mockMvc = webAppContextSetup(wac)
            .alwaysExpect(status().isOk())
            .alwaysExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .build();
  }

  @Test
  void getPerson42() throws Exception {
    this.mockMvc.perform(get("/person/42").accept(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Dilbert")));
  }

  @Test
  void getPerson99() throws Exception {
    this.mockMvc.perform(get("/person/99").accept(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name", is("Wally")));
  }

}
