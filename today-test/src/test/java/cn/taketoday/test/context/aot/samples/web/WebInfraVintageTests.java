/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.aot.samples.web;

import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit4.InfraRunner;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.servlet.WebApplicationContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
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
