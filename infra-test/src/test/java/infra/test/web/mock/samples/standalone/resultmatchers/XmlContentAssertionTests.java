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

package infra.test.web.mock.samples.standalone.resultmatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.mock.MockMvc;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
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
