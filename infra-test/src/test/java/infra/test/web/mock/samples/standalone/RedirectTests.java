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

package infra.test.web.mock.samples.standalone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.mock.MockMvc;
import infra.ui.Model;
import infra.validation.Errors;
import infra.web.RedirectModel;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.PostMapping;
import jakarta.validation.Valid;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.request.MockMvcRequestBuilders.post;
import static infra.test.web.mock.result.MockMvcResultMatchers.flash;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.model;
import static infra.test.web.mock.result.MockMvcResultMatchers.redirectedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

/**
 * Redirect scenarios including saving and retrieving flash attributes.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class RedirectTests {

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = standaloneSetup(new PersonController()).build();
  }

  @Test
  public void save() throws Exception {
    this.mockMvc.perform(post("/persons").param("name", "Andy"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/persons/Joe"))
            .andExpect(model().size(2))
            .andExpect(model().attributeExists("name"))
            .andExpect(flash().attributeCount(1))
            .andExpect(flash().attribute("message", "success!"));
  }

  @Test
  public void saveSpecial() throws Exception {
    this.mockMvc.perform(post("/people").param("name", "Andy"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/persons/Joe"))
            .andExpect(model().size(2))
            .andExpect(model().attributeExists("name", "person"))
            .andExpect(flash().attributeCount(1))
            .andExpect(flash().attribute("message", "success!"));
  }

  @Test
  public void saveWithErrors() throws Exception {
    this.mockMvc.perform(post("/persons"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("persons/add"))
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("person"))
            .andExpect(flash().attributeCount(0));
  }

  @Test
  public void saveSpecialWithErrors() throws Exception {
    this.mockMvc.perform(post("/people"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("persons/add"))
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("person"))
            .andExpect(flash().attributeCount(0));
  }

  @Test
  public void getPerson() throws Exception {
    this.mockMvc.perform(get("/persons/Joe").flashAttr("message", "success!"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("persons/index"))
            .andExpect(model().size(2))
            .andExpect(model().attribute("person", new Person("Joe")))
            .andExpect(model().attribute("message", "success!"))
            .andExpect(flash().attributeCount(0));
  }

  @Controller
  private static class PersonController {

    @GetMapping("/persons/{name}")
    public String getPerson(@PathVariable String name, Model model) {
      model.addAttribute(new Person(name));
      return "persons/index";
    }

    @PostMapping("/persons")
    public String save(@Valid Person person, Errors errors, Model model, RedirectModel redirectAttrs) {
      if (errors.hasErrors()) {
        return "persons/add";
      }
      model.addAttribute("name", "Joe");
      redirectAttrs.addAttribute("message", "success!");
      return "redirect:/persons/{name}";
    }

    @PostMapping("/people")
    public Object saveSpecial(@Valid Person person, Errors errors, Model model, RedirectModel redirectAttrs) {
      if (errors.hasErrors()) {
        return "persons/add";
      }
      model.addAttribute("name", "Joe");
      redirectAttrs.addAttribute("message", "success!");
      return new StringBuilder("redirect:").append("/persons").append("/{name}");
    }
  }

}
