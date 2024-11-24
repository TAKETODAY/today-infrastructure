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

package infra.test.web.mock.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.reactive.server.WebTestClient;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.XmlContentAssertionTests}.
 *
 * @author Rossen Stoyanchev
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

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new MusicController())
                  .alwaysExpect(status().isOk())
                  .alwaysExpect(content().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8")))
                  .configureClient()
                  .defaultHeader(HttpHeaders.ACCEPT, "application/xml;charset=UTF-8")
                  .build();

  @Test
  public void testXmlEqualTo() {
    testClient.get().uri("/music/people")
            .exchange()
            .expectBody().xml(PEOPLE_XML);
  }

  @Test
  public void testNodeHamcrestMatcher() {
    testClient.get().uri("/music/people")
            .exchange()
            .expectBody().xpath("/people/composers/composer[1]").exists();
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
