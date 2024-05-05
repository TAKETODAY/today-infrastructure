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

package cn.taketoday.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.http.MediaType;
import cn.taketoday.oxm.jaxb.Jaxb2Marshaller;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.mock.MockMvc;
import cn.taketoday.test.web.mock.setup.InternalResourceViewResolver;
import cn.taketoday.ui.Model;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.FixedContentNegotiationStrategy;
import cn.taketoday.web.accept.HeaderContentNegotiationStrategy;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.view.ContentNegotiatingViewResolver;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.json.MappingJackson2JsonView;
import cn.taketoday.web.view.xml.MarshallingView;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.model;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.xpath;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
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
    standaloneSetup(new PersonController()).setSingleView(new MappingJackson2JsonView()).build()
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
    viewList.add(new MappingJackson2JsonView());
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
