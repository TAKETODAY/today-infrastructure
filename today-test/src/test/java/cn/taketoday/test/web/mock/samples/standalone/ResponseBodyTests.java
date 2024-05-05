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

package cn.taketoday.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.MediaType;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RestController;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Response written from {@code @ResponseBody} method.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class ResponseBodyTests {

  @Test
  void json() throws Exception {
    standaloneSetup(new PersonController()).defaultResponseCharacterEncoding(UTF_8).build()
            // We use a name containing an umlaut to test UTF-8 encoding for the request and the response.
            .perform(get("/person/Jürgen").characterEncoding(UTF_8).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(content().encoding(UTF_8))
            .andExpect(content().string(containsString("Jürgen")))
            .andExpect(jsonPath("$.name").value("Jürgen"))
            .andExpect(jsonPath("$.age").value(42))
            .andExpect(jsonPath("$.age").value(42.0f))
            .andExpect(jsonPath("$.age").value(equalTo(42)))
            .andExpect(jsonPath("$.age").value(equalTo(42.0f), Float.class))
            .andExpect(jsonPath("$.age", equalTo(42)))
            .andExpect(jsonPath("$.age", equalTo(42.0f), Float.class));
  }

  @RestController
  private static class PersonController {

    @GetMapping("/person/{name}")
    Person get(@PathVariable String name) {
      Person person = new Person(name);
      person.setAge(42);
      return person;
    }
  }

  @SuppressWarnings("unused")
  private static class Person {

    private final String name;

    private int age;

    public Person(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    public int getAge() {
      return this.age;
    }

    public void setAge(int age) {
      this.age = age;
    }
  }

}
