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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.http.MediaType;
import infra.oxm.jaxb.Jaxb2Marshaller;
import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.setup.InternalResourceViewResolver;
import infra.ui.Model;
import infra.web.accept.ContentNegotiationManager;
import infra.web.accept.FixedContentNegotiationStrategy;
import infra.web.accept.HeaderContentNegotiationStrategy;
import infra.web.annotation.GetMapping;
import infra.web.annotation.PathVariable;
import infra.web.view.ContentNegotiatingViewResolver;
import infra.web.view.View;
import infra.web.view.json.JacksonJsonView;
import infra.web.view.xml.MarshallingView;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static infra.test.web.mock.result.MockMvcResultMatchers.model;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.result.MockMvcResultMatchers.xpath;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

/**
 * Tests with view resolution.
 *
 * @author Rossen Stoyanchev
 */
class ViewResolutionTests {

  @Test
  void jsonOnly() throws Exception {
    standaloneSetup(new PersonController()).setSingleView(new JacksonJsonView()).build()
            .perform(get("/person/Corea"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.person.name").value("Corea"));
  }

  @Test
  void xmlOnly() throws Exception {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(Person.class);

    standaloneSetup(new PersonController()).setSingleView(new MarshallingView(marshaller)).build()
            .perform(get("/person/Corea"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_XML))
            .andExpect(xpath("/person/name/text()").string(equalTo("Corea")));
  }

  @Test
  void contentNegotiation() throws Exception {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(Person.class);

    List<View> viewList = new ArrayList<>();
    viewList.add(new JacksonJsonView());
    viewList.add(new MarshallingView(marshaller));

    ContentNegotiationManager manager = new ContentNegotiationManager(
            new HeaderContentNegotiationStrategy(), new FixedContentNegotiationStrategy(MediaType.TEXT_HTML));

    ContentNegotiatingViewResolver cnViewResolver = new ContentNegotiatingViewResolver();
    cnViewResolver.setDefaultViews(viewList);
    cnViewResolver.setContentNegotiationManager(manager);
    cnViewResolver.afterPropertiesSet();

    MockMvc mockMvc =
            standaloneSetup(new PersonController())
                    .setViewResolvers(cnViewResolver, new InternalResourceViewResolver())
                    .build();

    mockMvc.perform(get("/person/Corea"))
            .andExpect(status().isOk())
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("person"))
            .andExpect(forwardedUrl("person/show"));

    mockMvc.perform(get("/person/Corea").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.person.name").value("Corea"));

    mockMvc.perform(get("/person/Corea").accept(MediaType.APPLICATION_XML))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_XML))
            .andExpect(xpath("/person/name/text()").string(equalTo("Corea")));
  }

  @Test
  void defaultViewResolver() throws Exception {
    standaloneSetup(new PersonController()).build()
            .perform(get("/person/Corea"))
            .andExpect(model().attribute("person", hasProperty("name", equalTo("Corea"))))
            .andExpect(status().isOk())
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
