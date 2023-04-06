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

package cn.taketoday.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.jupiter.api.Test;

import java.net.URL;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.view.RedirectModel;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.post;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.flash;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Examples of expectations on flash attributes.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class FlashAttributeAssertionTests {

  private final MockMvc mockMvc = standaloneSetup(new PersonController())
          .alwaysExpect(status().isFound())
          .alwaysExpect(flash().attributeCount(3))
          .build();

  @Test
  void attributeCountWithWrongCount() throws Exception {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> this.mockMvc.perform(post("/persons")).andExpect(flash().attributeCount(1)))
            .withMessage("RedirectModel size expected:<1> but was:<3>");
  }

  @Test
  void attributeExists() throws Exception {
    this.mockMvc.perform(post("/persons"))
            .andExpect(flash().attributeExists("one", "two", "three"));
  }

  @Test
  void attributeEqualTo() throws Exception {
    this.mockMvc.perform(post("/persons"))
            .andExpect(flash().attribute("one", "1"))
            .andExpect(flash().attribute("two", 2.222))
            .andExpect(flash().attribute("three", new URL("https://example.com")));
  }

  @Test
  void attributeMatchers() throws Exception {
    this.mockMvc.perform(post("/persons"))
            .andExpect(flash().attribute("one", containsString("1")))
            .andExpect(flash().attribute("two", closeTo(2, 0.5)))
            .andExpect(flash().attribute("three", notNullValue()))
            .andExpect(flash().attribute("one", equalTo("1")))
            .andExpect(flash().attribute("two", equalTo(2.222)))
            .andExpect(flash().attribute("three", equalTo(new URL("https://example.com"))));
  }

  @Controller
  private static class PersonController {

    @PostMapping("/persons")
    String save(RedirectModel redirectAttrs) throws Exception {
      redirectAttrs.addAttribute("one", "1");
      redirectAttrs.addAttribute("two", 2.222);
      redirectAttrs.addAttribute("three", new URL("https://example.com"));
      return "redirect:/person/1";
    }
  }

}
