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

package cn.taketoday.test.web.mock.samples.client.standalone;

import org.junit.jupiter.api.Test;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.mock.client.MockMvcWebTestClient;
import cn.taketoday.test.web.mock.result.MockMvcResultHandlers;
import cn.taketoday.ui.Model;
import cn.taketoday.validation.Errors;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.PostMapping;
import cn.taketoday.web.RedirectModel;
import jakarta.validation.Valid;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.flash;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.model;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.mock.samples.standalone.RedirectTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RedirectTests {

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new PersonController()).build();

  @Test
  public void save() throws Exception {
    EntityExchangeResult<Void> exchangeResult =
            testClient.post().uri("/persons?name=Andy")
                    .exchange()
                    .expectStatus().isFound()
                    .expectHeader().location("/persons/Joe")
                    .expectBody().isEmpty();

    // Further assertions on the server response
    MockMvcWebTestClient.resultActionsFor(exchangeResult)
            .andExpect(model().size(2))
            .andExpect(model().attributeExists("name", "person"))
            .andExpect(flash().attributeCount(1))
            .andExpect(flash().attribute("message", "success!"));
  }

  @Test
  public void saveSpecial() throws Exception {
    EntityExchangeResult<Void> result =
            testClient.post().uri("/people?name=Andy")
                    .exchange()
                    .expectStatus().isFound()
                    .expectHeader().location("/persons/Joe")
                    .expectBody().isEmpty();

    // Further assertions on the server response
    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(model().size(2))
            .andExpect(model().attributeExists("name", "person"))
            .andExpect(flash().attributeCount(1))
            .andExpect(flash().attribute("message", "success!"));
  }

  @Test
  public void saveWithErrors() throws Exception {
    EntityExchangeResult<Void> result =
            testClient.post().uri("/persons").exchange().expectStatus().isOk().expectBody().isEmpty();

    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(forwardedUrl("persons/add"))
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("person"))
            .andExpect(flash().attributeCount(0));
  }

  @Test
  public void saveSpecialWithErrors() throws Exception {
    EntityExchangeResult<Void> result =
            testClient.post().uri("/people").exchange().expectStatus().isOk().expectBody().isEmpty();

    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(forwardedUrl("persons/add"))
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("person"))
            .andExpect(flash().attributeCount(0));
  }

  @Test
  public void getPerson() throws Exception {
    EntityExchangeResult<Void> result =
            MockMvcWebTestClient.bindToController(new PersonController())
                    .defaultRequest(get("/").flashAttr("message", "success!"))
                    .build()
                    .get().uri("/persons/Joe")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody().isEmpty();

    // Further assertions on the server response
    MockMvcWebTestClient.resultActionsFor(result)
            .andDo(MockMvcResultHandlers.print())
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
