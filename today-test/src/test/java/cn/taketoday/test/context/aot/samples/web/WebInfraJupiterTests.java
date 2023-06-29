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

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Value;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.servlet.WebApplicationContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitWebConfig(classes = WebTestConfiguration.class, resourcePath = "classpath:META-INF/web-resources")
@TestPropertySource(properties = "test.engine = jupiter")
public class WebInfraJupiterTests {

  MockMvc mockMvc;

  @Autowired
  WebApplicationContext wac;

  @org.junit.jupiter.api.BeforeEach
  void setUpMockMvc() {
    this.mockMvc = webAppContextSetup(this.wac).build();
  }

  @org.junit.jupiter.api.Test
  void controller(@Value("${test.engine}") String testEngine) throws Exception {
    assertThat(testEngine)
            .as("@Value").isEqualTo("jupiter");
    assertThat(wac.getEnvironment().getProperty("test.engine"))
            .as("Environment").isEqualTo("jupiter");

    mockMvc.perform(get("/hello"))
            .andExpectAll(status().isOk(), content().string("Hello, AOT!"));
  }

  @org.junit.jupiter.api.Test
  void resources() throws Exception {
    this.mockMvc.perform(get("/resources/Spring.js"))
            .andExpectAll(
                    content().contentType("application/javascript"),
                    content().string(containsString("Spring={};"))
            );
  }

}
