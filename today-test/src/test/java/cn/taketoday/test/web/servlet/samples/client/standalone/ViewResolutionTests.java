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

package cn.taketoday.test.web.servlet.samples.client.standalone;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.http.MediaType;
import cn.taketoday.oxm.jaxb.Jaxb2Marshaller;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.test.web.servlet.setup.InternalResourceViewResolver;
import cn.taketoday.ui.Model;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.FixedContentNegotiationStrategy;
import cn.taketoday.web.accept.HeaderContentNegotiationStrategy;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.view.ContentNegotiatingViewResolver;
import cn.taketoday.web.view.UrlBasedViewResolver;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.json.MappingJackson2JsonView;
import cn.taketoday.web.view.xml.MarshallingView;

import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.RequestParameterTests}.
 *
 * @author Rossen Stoyanchev
 */
class ViewResolutionTests {

  @Test
  void jsonOnly() {
    WebTestClient testClient =
            MockMvcWebTestClient.bindToController(new PersonController())
                    .singleView(new MappingJackson2JsonView())
                    .build();

    testClient.get().uri("/person/Corea")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("$.person.name", "Corea");
  }

  @Test
  void xmlOnly() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(Person.class);

    WebTestClient testClient =
            MockMvcWebTestClient.bindToController(new PersonController())
                    .singleView(new MarshallingView(marshaller))
                    .build();

    testClient.get().uri("/person/Corea")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_XML)
            .expectBody().xpath("/person/name/text()").isEqualTo("Corea");
  }

  @Test
  void contentNegotiation() throws Exception {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(Person.class);

    List<View> viewList = new ArrayList<>();
    viewList.add(new MappingJackson2JsonView());
    viewList.add(new MarshallingView(marshaller));

    ContentNegotiationManager manager = new ContentNegotiationManager(
            new HeaderContentNegotiationStrategy(), new FixedContentNegotiationStrategy(MediaType.TEXT_HTML));

    ContentNegotiatingViewResolver cnViewResolver = new ContentNegotiatingViewResolver();
    cnViewResolver.setDefaultViews(viewList);
    cnViewResolver.setContentNegotiationManager(manager);
    cnViewResolver.afterPropertiesSet();

    WebTestClient testClient =
            MockMvcWebTestClient.bindToController(new PersonController())
                    .viewResolvers(cnViewResolver, new InternalResourceViewResolver())
                    .build();

    EntityExchangeResult<Void> result = testClient.get().uri("/person/Corea")
            .exchange()
            .expectStatus().isOk()
            .expectBody().isEmpty();

    // Further assertions on the server response
    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("person"))
            .andExpect(forwardedUrl("person/show"));

    testClient.get().uri("/person/Corea")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody().jsonPath("$.person.name", "Corea");

    testClient.get().uri("/person/Corea")
            .accept(MediaType.APPLICATION_XML)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_XML)
            .expectBody().xpath("/person/name/text()").isEqualTo("Corea");
  }

  @Test
  void defaultViewResolver() throws Exception {
    WebTestClient client = MockMvcWebTestClient.bindToController(new PersonController()).build();

    EntityExchangeResult<Void> result = client.get().uri("/person/Corea")
            .exchange()
            .expectStatus().isOk()
            .expectBody().isEmpty();

    // Further assertions on the server response
    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(model().attribute("person", hasProperty("name", equalTo("Corea"))))
            .andExpect(forwardedUrl("person/show"));  // InternalResourceViewResolver
  }

  @Controller
  private static class PersonController {

    @GetMapping("/person/{name}")
    String show(@PathVariable String name, Model model) {
      Person person = new Person(name);
      model.addAttribute(person);
      return "person/show";
    }
  }

}
