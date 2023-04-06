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

package cn.taketoday.test.web.servlet.samples.client.standalone;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RestController;
import jakarta.validation.constraints.NotNull;

import static org.hamcrest.Matchers.equalTo;

/**
 * MockMvcTestClient equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.ResponseBodyTests}.
 *
 * @author Rossen Stoyanchev
 */
class ResponseBodyTests {

  @Test
  void json() {
    MockMvcWebTestClient.bindToController(new PersonController()).build()
            .get()
            .uri("/person/Lee")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.name").isEqualTo("Lee")
            .jsonPath("$.age").isEqualTo(42)
            .jsonPath("$.age").value(equalTo(42))
            .jsonPath("$.age").value(equalTo(42.0f), Float.class);
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

    @NotNull
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
