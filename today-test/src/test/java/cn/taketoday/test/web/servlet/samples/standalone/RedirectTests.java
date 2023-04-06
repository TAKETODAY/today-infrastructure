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

package cn.taketoday.test.web.servlet.samples.standalone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.ui.Model;
import cn.taketoday.validation.Errors;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.view.RedirectModel;
import jakarta.validation.Valid;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.post;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.flash;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.model;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

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
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("name"))
            .andExpect(flash().attributeCount(1))
            .andExpect(flash().attribute("message", "success!"));
  }

  @Test
  public void saveSpecial() throws Exception {
    this.mockMvc.perform(post("/people").param("name", "Andy"))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/persons/Joe"))
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("name"))
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
    public String save(@Valid Person person, Errors errors, RedirectModel redirectAttrs) {
      if (errors.hasErrors()) {
        return "persons/add";
      }
      redirectAttrs.addAttribute("name", "Joe");
      redirectAttrs.addAttribute("message", "success!");
      return "redirect:/persons/{name}";
    }

    @PostMapping("/people")
    public Object saveSpecial(@Valid Person person, Errors errors, RedirectModel redirectAttrs) {
      if (errors.hasErrors()) {
        return "persons/add";
      }
      redirectAttrs.addAttribute("name", "Joe");
      redirectAttrs.addAttribute("message", "success!");
      return new StringBuilder("redirect:").append("/persons").append("/{name}");
    }
  }

}
