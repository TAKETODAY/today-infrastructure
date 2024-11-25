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
import infra.test.web.Person;
import infra.test.web.mock.MockMvc;
import infra.ui.Model;
import infra.validation.BindingResult;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.PostMapping;
import infra.web.annotation.RequestMapping;
import infra.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.request.MockMvcRequestBuilders.post;
import static infra.test.web.mock.result.MockMvcResultMatchers.model;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Examples of expectations on the content of the model prepared by the controller.
 *
 * @author Rossen Stoyanchev
 */
public class ModelAssertionTests {

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    SampleController controller = new SampleController("a string value", 3, new Person("a name"));

    this.mockMvc = standaloneSetup(controller)
            .defaultRequest(get("/"))
            .alwaysExpect(status().isOk())
            .setControllerAdvice(new ModelAttributeAdvice())
            .build();
  }

  @Test
  void attributeEqualTo() throws Exception {
    mockMvc.perform(get("/"))
            .andExpect(model().attribute("integer", 3))
            .andExpect(model().attribute("string", "a string value"))
            .andExpect(model().attribute("integer", equalTo(3))) // Hamcrest...
            .andExpect(model().attribute("string", equalTo("a string value")))
            .andExpect(model().attribute("globalAttrName", equalTo("Global Attribute Value")));
  }

  @Test
  void attributeExists() throws Exception {
    mockMvc.perform(get("/"))
            .andExpect(model().attributeExists("integer", "string", "person"))
            .andExpect(model().attribute("integer", notNullValue()))  // Hamcrest...
            .andExpect(model().attribute("INTEGER", nullValue()));
  }

  @Test
  void attributeHamcrestMatchers() throws Exception {
    mockMvc.perform(get("/"))
            .andExpect(model().attribute("integer", equalTo(3)))
            .andExpect(model().attribute("string", allOf(startsWith("a string"), endsWith("value"))))
            .andExpect(model().attribute("person", hasProperty("name", equalTo("a name"))));
  }

  @Test
  void hasErrors() throws Exception {
    mockMvc.perform(post("/persons"))
            .andExpect(model().attributeHasErrors("person"));
  }

  @Test
  void hasNoErrors() throws Exception {
    mockMvc.perform(get("/")).andExpect(model().hasNoErrors());
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