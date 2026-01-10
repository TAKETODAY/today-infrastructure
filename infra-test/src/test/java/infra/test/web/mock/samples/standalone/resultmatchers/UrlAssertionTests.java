/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.web.mock.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.stereotype.Controller;
import infra.test.web.mock.MockMvc;
import infra.web.annotation.RequestMapping;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrlPattern;
import static infra.test.web.mock.result.MockMvcResultMatchers.redirectedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.redirectedUrlPattern;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

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
      return "/home";
    }
  }
}
