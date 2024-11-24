/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.web.mock.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.stereotype.Controller;
import infra.test.web.mock.MockMvc;
import infra.web.annotation.RequestMapping;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.result.MockMvcResultMatchers.view;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Examples of expectations on the view name selected by the controller.
 *
 * @author Rossen Stoyanchev
 */
public class ViewNameAssertionTests {

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = standaloneSetup(new SimpleController())
            .alwaysExpect(status().isOk())
            .build();
  }

  @Test
  public void testEqualTo() throws Exception {
    this.mockMvc.perform(get("/"))
            .andExpect(view().name("mySpecialView"))
            .andExpect(view().name(equalTo("mySpecialView")));
  }

  @Test
  public void testHamcrestMatcher() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(view().name(containsString("Special")));
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/")
    public String handle() {
      return "mySpecialView";
    }
  }
}
