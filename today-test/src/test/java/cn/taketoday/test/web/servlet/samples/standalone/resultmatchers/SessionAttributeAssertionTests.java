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

import java.util.Locale;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.ui.Model;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import cn.taketoday.web.bind.annotation.SessionAttributes;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.request;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Examples of expectations on created session attributes.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class SessionAttributeAssertionTests {

  private final MockMvc mockMvc = standaloneSetup(new SimpleController())
          .defaultRequest(get("/"))
          .alwaysExpect(status().isOk())
          .build();

  @Test
  void sessionAttributeEqualTo() throws Exception {
    this.mockMvc.perform(get("/"))
            .andExpect(request().sessionAttribute("locale", Locale.UK));

    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() ->
                    this.mockMvc.perform(get("/"))
                            .andExpect(request().sessionAttribute("locale", Locale.US)))
            .withMessage("Session attribute 'locale' expected:<en_US> but was:<en_GB>");
  }

  @Test
  void sessionAttributeMatcher() throws Exception {
    this.mockMvc.perform(get("/"))
            .andExpect(request().sessionAttribute("bogus", is(nullValue())))
            .andExpect(request().sessionAttribute("locale", is(notNullValue())))
            .andExpect(request().sessionAttribute("locale", equalTo(Locale.UK)));

    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() ->
                    this.mockMvc.perform(get("/"))
                            .andExpect(request().sessionAttribute("bogus", is(notNullValue()))))
            .withMessageContaining("null");
  }

  @Test
  void sessionAttributeDoesNotExist() throws Exception {
    this.mockMvc.perform(get("/"))
            .andExpect(request().sessionAttributeDoesNotExist("bogus", "enigma"));

    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() ->
                    this.mockMvc.perform(get("/"))
                            .andExpect(request().sessionAttributeDoesNotExist("locale")))
            .withMessage("Session attribute 'locale' exists");
  }

  @Controller
  @SessionAttributes("locale")
  private static class SimpleController {

    @ModelAttribute
    void populate(Model model) {
      model.addAttribute("locale", Locale.UK);
    }

    @RequestMapping("/")
    String handle() {
      return "view";
    }
  }

}
