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

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.servlet.filter.CharacterEncodingFilter;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Test for SPR-10277 (multiple method chaining when building MockMvc).
 *
 * @author Wesley Hall
 */
@JUnitWebConfig
class MockMvcBuilderMethodChainTests {

  @Test
  void chainMultiple(WebApplicationContext wac) {
    assertThatNoException().isThrownBy(() ->
            MockMvcBuilders
                    .webAppContextSetup(wac)
                    .addFilter(new CharacterEncodingFilter())
                    .defaultRequest(get("/").contextPath("/mywebapp"))
                    .build()
    );
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig {
  }

}
