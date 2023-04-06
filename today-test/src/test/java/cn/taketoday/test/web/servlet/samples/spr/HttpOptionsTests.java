/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.samples.spr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.servlet.WebApplicationContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.options;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SPR-10093 (support for OPTIONS requests).
 *
 * @author Arnaud Cogoluègnes
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class HttpOptionsTests {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = webAppContextSetup(this.wac).build();
  }

  @Test
  public void test() throws Exception {
    MyController controller = this.wac.getBean(MyController.class);
    int initialCount = controller.counter.get();
    this.mockMvc.perform(options("/myUrl")).andExpect(status().isOk());

    assertThat(controller.counter.get()).isEqualTo((initialCount + 1));
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Bean
    public MyController myController() {
      return new MyController();
    }
  }

  @Controller
  private static class MyController {

    private final AtomicInteger counter = new AtomicInteger();

    @RequestMapping(value = "/myUrl", method = HttpMethod.OPTIONS)
    @ResponseBody
    public void handle() {
      counter.incrementAndGet();
    }
  }

}
