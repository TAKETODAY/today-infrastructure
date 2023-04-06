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

package cn.taketoday.test.web.servlet.samples.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.web.resource.DefaultServletHttpRequestHandler;
import cn.taketoday.web.servlet.WebApplicationContext;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.handler;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests dependent on access to resources under the web application root directory.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration("src/test/resources/META-INF/web-resources")
@ContextHierarchy({
        @ContextConfiguration("root-context.xml"),
        @ContextConfiguration("servlet-context.xml")
})
public class WebAppResourceTests {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).alwaysExpect(status().isOk()).build();
  }

  // Resources served via <mvc:resources/>

  @Test
  public void resourceRequest() throws Exception {
    this.mockMvc.perform(get("/resources/Spring.js"))
            .andExpect(content().contentType("application/javascript"))
            .andExpect(content().string(containsString("Spring={};")));
  }

  // Forwarded to the "default" servlet via <mvc:default-servlet-handler/>

  @Test
  public void resourcesViaDefaultServlet() throws Exception {
    this.mockMvc.perform(get("/unknown/resource"))
            .andExpect(handler().handlerType(DefaultServletHttpRequestHandler.class))
            .andExpect(forwardedUrl("default"));
  }

}
