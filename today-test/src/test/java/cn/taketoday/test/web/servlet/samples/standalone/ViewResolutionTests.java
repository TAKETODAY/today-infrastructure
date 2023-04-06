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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.ui.Model;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.accept.FixedContentNegotiationStrategy;
import cn.taketoday.web.accept.HeaderContentNegotiationStrategy;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.view.ContentNegotiatingViewResolver;
import cn.taketoday.web.view.View;
import cn.taketoday.web.view.json.MappingJackson2JsonView;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.model;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

/**
 * Tests with view resolution.
 *
 * @author Rossen Stoyanchev
 */
class ViewResolutionTests {

  @Test
  void jspOnly() throws Exception {
    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver("/WEB-INF/", ".jsp");

    standaloneSetup(new PersonController()).setViewResolvers(viewResolver).build()
            .perform(get("/person/Corea"))
            .andExpect(status().isOk())
            .andExpect(model().size(1))
            .andExpect(model().attributeExists("person"))
            .andExpect(forwardedUrl("/WEB-INF/person/show.jsp"));
  }

  @Test
  void jsonOnly() throws Exception {
    standaloneSetup(new PersonController()).setSingleView(new MappingJackson2JsonView()).build()
            .perform(get("/person/Corea"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.person.name").value("Corea"));
  }

//  @Test
//  void xmlOnly() throws Exception {
//    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
//    marshaller.setClassesToBeBound(Person.class);
//
//    standaloneSetup(new PersonController()).setSingleView(new MarshallingView(marshaller)).build()
//            .perform(get("/person/Corea"))
//            .andExpect(status().isOk())
//            .andExpect(content().contentType(MediaType.APPLICATION_XML))
//            .andExpect(xpath("/person/name/text()").string(equalTo("Corea")));
//  }

  @Test
  void contentNegotiation() throws Exception {
//    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
//    marshaller.setClassesToBeBound(Person.class);

    List<View> viewList = new ArrayList<>();
    viewList.add(new MappingJackson2JsonView());
//    viewList.add(new MarshallingView(marshaller));

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
