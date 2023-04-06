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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.annotation.RequestMapping;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.forwardedUrlPattern;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Examples of expectations on forwarded or redirected URLs.
 *
 * @author Rossen Stoyanchev
 */
public class UrlAssertionTests {

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = standaloneSetup(new SimpleController()).build();
  }

  @Test
  public void testRedirect() throws Exception {
    this.mockMvc.perform(get("/persons")).andExpect(redirectedUrl("/persons/1"));
  }

  @Test
  public void testRedirectPattern() throws Exception {
    this.mockMvc.perform(get("/persons")).andExpect(redirectedUrlPattern("/persons/*"));
  }

  @Test
  public void testForward() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(forwardedUrl("/home"));
  }

  @Test
  public void testForwardPattern() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(forwardedUrlPattern("/ho?e"));
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/persons")
    public String save() {
      return "redirect:/persons/1";
    }

    @RequestMapping("/")
    public String forward() {
      return "forward:/home";
    }
  }
}
