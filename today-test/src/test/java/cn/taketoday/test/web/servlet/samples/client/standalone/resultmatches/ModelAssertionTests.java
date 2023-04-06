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

package cn.taketoday.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.ResultActions;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.ui.Model;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;

import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.model;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.resultmatchers.ModelAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelAssertionTests {

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new SampleController("a string value", 3, new Person("a name")))
                  .controllerAdvice(new ModelAttributeAdvice())
                  .alwaysExpect(status().isOk())
                  .build();

  @Test
  void attributeEqualTo() throws Exception {
    performRequest(HttpMethod.GET, "/")
            .andExpect(model().attribute("integer", 3))
            .andExpect(model().attribute("string", "a string value"))
            .andExpect(model().attribute("integer", equalTo(3))) // Hamcrest...
            .andExpect(model().attribute("string", equalTo("a string value")))
            .andExpect(model().attribute("globalAttrName", equalTo("Global Attribute Value")));
  }

  @Test
  void attributeExists() throws Exception {
    performRequest(HttpMethod.GET, "/")
            .andExpect(model().attributeExists("integer", "string", "person"))
            .andExpect(model().attribute("integer", notNullValue()))  // Hamcrest...
            .andExpect(model().attribute("INTEGER", nullValue()));
  }

  @Test
  void attributeHamcrestMatchers() throws Exception {
    performRequest(HttpMethod.GET, "/")
            .andExpect(model().attribute("integer", equalTo(3)))
            .andExpect(model().attribute("string", allOf(startsWith("a string"), endsWith("value"))))
            .andExpect(model().attribute("person", hasProperty("name", equalTo("a name"))));
  }

  @Test
  void hasErrors() throws Exception {
    performRequest(HttpMethod.POST, "/persons").andExpect(model().attributeHasErrors("person"));
  }

  @Test
  void hasNoErrors() throws Exception {
    performRequest(HttpMethod.GET, "/").andExpect(model().hasNoErrors());
  }

  private ResultActions performRequest(HttpMethod method, String uri) {
    EntityExchangeResult<Void> result = client.method(method).uri(uri).exchange().expectBody().isEmpty();
    return MockMvcWebTestClient.resultActionsFor(result);
  }

  @Controller
  private static class SampleController {

    private final Object[] values;

    SampleController(Object... values) {
      this.values = values;
    }

    @RequestMapping("/")
    String handle(Model model) {
      for (Object value : this.values) {
        model.addAttribute(value);
      }
      return "view";
    }

    @PostMapping("/persons")
    String create(@Valid Person person, BindingResult result, Model model) {
      return "view";
    }
  }

  @ControllerAdvice
  private static class ModelAttributeAdvice {

    @ModelAttribute("globalAttrName")
    String getAttribute() {
      return "Global Attribute Value";
    }
  }

}
