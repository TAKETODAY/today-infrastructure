/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.aot.samples.web;

import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit4.InfraRunner;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.mock.MockMvc;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@ContextConfiguration(classes = WebTestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = "test.engine = vintage")
public class WebInfraVintageTests {

  MockMvc mockMvc;

  @Autowired
  WebApplicationContext wac;

  @Value("${test.engine}")
  String testEngine;

  @org.junit.Before
  public void setUpMockMvc() {
    this.mockMvc = webAppContextSetup(this.wac).build();
  }

  @org.junit.Test
  public void test() throws Exception {
    assertThat(testEngine)
            .as("@Value").isEqualTo("vintage");
    assertThat(wac.getEnvironment().getProperty("test.engine"))
            .as("Environment").isEqualTo("vintage");

    mockMvc.perform(get("/hello"))
            .andExpectAll(status().isOk(), content().string("Hello, AOT!"));
  }

}
