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

package infra.test.web.mock.samples.client.standalone;

import org.junit.jupiter.api.Test;

import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.mock.result.MockMvcResultHandlers;
import infra.test.web.reactive.server.EntityExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.ui.Model;
import infra.validation.Errors;
import infra.web.RedirectModel;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.annotation.PostMapping;
import jakarta.validation.Valid;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.flash;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.model;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.RedirectTests}.
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
