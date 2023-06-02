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

package cn.taketoday.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.hasXPath;

/**
 * Examples of defining expectations on XML response content with XMLUnit.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see ContentAssertionTests
 * @see XpathAssertionTests
 */
public class XmlContentAssertionTests {

  private static final String PEOPLE_XML =
          "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                  "<people><composers>" +
                  "<composer><name>Johann Sebastian Bach</name><someBoolean>false</someBoolean><someDouble>21.0</someDouble></composer>" +
                  "<composer><name>Johannes Brahms</name><someBoolean>false</someBoolean><someDouble>0.0025</someDouble></composer>" +
                  "<composer><name>Edvard Grieg</name><someBoolean>false</someBoolean><someDouble>1.6035</someDouble></composer>" +
                  "<composer><name>Robert Schumann</name><someBoolean>false</someBoolean><someDouble>NaN</someDouble></composer>" +
                  "</composers></people>";

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = standaloneSetup(new MusicController())
            .defaultRequest(get("/").accept(MediaType.APPLICATION_XML, MediaType.parseMediaType("application/xml;charset=UTF-8")))
            .alwaysExpect(status().isOk())
            .alwaysExpect(content().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8")))
            .build();
  }

  @Test
  public void testXmlEqualTo() throws Exception {
    this.mockMvc.perform(get("/music/people")).andExpect(content().xml(PEOPLE_XML));
  }

  @Test
  public void testNodeHamcrestMatcher() throws Exception {
    this.mockMvc.perform(get("/music/people"))
            .andExpect(content().node(hasXPath("/people/composers/composer[1]")));
  }

  @Controller
  private static class MusicController {

    @RequestMapping(value = "/music/people")
    public @ResponseBody PeopleWrapper getPeople() {

      List<Person> composers = Arrays.asList(
              new Person("Johann Sebastian Bach").setSomeDouble(21),
              new Person("Johannes Brahms").setSomeDouble(.0025),
              new Person("Edvard Grieg").setSomeDouble(1.6035),
              new Person("Robert Schumann").setSomeDouble(Double.NaN));

      return new PeopleWrapper(composers);
    }
  }

  @SuppressWarnings("unused")
  @XmlRootElement(name = "people")
  @XmlAccessorType(XmlAccessType.FIELD)
  private static class PeopleWrapper {

    @XmlElementWrapper(name = "composers")
    @XmlElement(name = "composer")
    private List<Person> composers;

    public PeopleWrapper() {
    }

    public PeopleWrapper(List<Person> composers) {
      this.composers = composers;
    }

    public List<Person> getComposers() {
      return this.composers;
    }
  }
}
