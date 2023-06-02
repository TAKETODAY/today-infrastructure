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

package cn.taketoday.test.web.servlet.setup;

import org.junit.jupiter.api.Test;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.web.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.SharedHttpSessionConfigurer.sharedHttpSession;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SharedHttpSessionConfigurer}.
 *
 * @author Rossen Stoyanchev
 */
public class SharedHttpSessionTests {

  @Test
  public void httpSession() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .apply(sharedHttpSession())
            .build();

    String url = "/session";

    MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    HttpSession session = result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    assertThat(session.getAttribute("counter")).isEqualTo(1);

    result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    session = result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    assertThat(session.getAttribute("counter")).isEqualTo(2);

    result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    session = result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    assertThat(session.getAttribute("counter")).isEqualTo(3);
  }

  @Test
  public void noHttpSession() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .apply(sharedHttpSession())
            .build();

    String url = "/no-session";

    MvcResult result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    HttpSession session = result.getRequest().getSession(false);
    assertThat(session).isNull();

    result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    session = result.getRequest().getSession(false);
    assertThat(session).isNull();

    url = "/session";

    result = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    session = result.getRequest().getSession(false);
    assertThat(session).isNotNull();
    assertThat(session.getAttribute("counter")).isEqualTo(1);
  }

  @Controller
  private static class TestController {

    @GetMapping("/session")
    public String handle(HttpSession session) {
      Integer counter = (Integer) session.getAttribute("counter");
      session.setAttribute("counter", (counter != null ? counter + 1 : 1));
      return "view";
    }

    @GetMapping("/no-session")
    public String handle() {
      return "view";
    }
  }

}
