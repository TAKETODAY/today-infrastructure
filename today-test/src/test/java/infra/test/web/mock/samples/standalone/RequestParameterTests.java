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

package infra.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;

import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.ResponseBody;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

/**
 * Tests demonstrating the use of request parameters.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParameterTests {

  @Test
  public void queryParameter() throws Exception {

    standaloneSetup(new PersonController()).build()
            .perform(get("/search?name=George").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.name").value("George"));
  }

  @Controller
  private class PersonController {

    @RequestMapping(value = "/search")
    @ResponseBody
    public Person get(@RequestParam String name) {
      Person person = new Person(name);
      return person;
    }
  }

}
