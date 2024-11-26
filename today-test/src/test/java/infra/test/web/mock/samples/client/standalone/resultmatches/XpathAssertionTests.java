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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import static infra.http.HttpMethod.GET;
import static infra.http.HttpMethod.HEAD;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.XpathAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class XpathAssertionTests {

  private static final Map<String, String> musicNamespace =
          Collections.singletonMap("ns", "https://example.org/music/people");

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new MusicController())
                  .alwaysExpect(status().isOk())
                  .alwaysExpect(content().contentType(MediaType.parseMediaType("application/xml;charset=UTF-8")))
                  .configureClient()
                  .defaultHeader(HttpHeaders.ACCEPT, "application/xml;charset=UTF-8")
                  .build();

  @Test
  public void testExists() {
    String composer = "/ns:people/composers/composer[%s]";
    String performer = "/ns:people/performers/performer[%s]";

    testClient.get().uri("/music/people")
            .exchange()
            .expectBody()
            .xpath(composer, musicNamespace, 1).exists()
            .xpath(composer, musicNamespace, 2).exists()
            .xpath(composer, musicNamespace, 3).exists()
            .xpath(composer, musicNamespace, 4).exists()
            .xpath(performer, musicNamespace, 1).exists()
            .xpath(performer, musicNamespace, 2).exists()
            .xpath(composer, musicNamespace, 1).string(notNullValue());
  }

  @Test
  public void testDoesNotExist() {
    String composer = "/ns:people/composers/composer[%s]";
    String performer = "/ns:people/performers/performer[%s]";

    testClient.get().uri("/music/people")
            .exchange()
            .expectBody()
            .xpath(composer, musicNamespace, 0).doesNotExist()
            .xpath(composer, musicNamespace, 5).doesNotExist()
            .xpath(performer, musicNamespace, 0).doesNotExist()
            .xpath(performer, musicNamespace, 3).doesNotExist();
  }

  @Test
  public void testString() {

    String composerName = "/ns:people/composers/composer[%s]/name";
    String performerName = "/ns:people/performers/performer[%s]/name";

    testClient.get().uri("/music/people")
            .exchange()
            .expectBody()
            .xpath(composerName, musicNamespace, 1).isEqualTo("Johann Sebastian Bach")
            .xpath(composerName, musicNamespace, 2).isEqualTo("Johannes Brahms")
            .xpath(composerName, musicNamespace, 3).isEqualTo("Edvard Grieg")
            .xpath(composerName, musicNamespace, 4).isEqualTo("Robert Schumann")
            .xpath(performerName, musicNamespace, 1).isEqualTo("Vladimir Ashkenazy")
            .xpath(performerName, musicNamespace, 2).isEqualTo("Yehudi Menuhin")
            .xpath(composerName, musicNamespace, 1).string(equalTo("Johann Sebastian Bach")) // Hamcrest..
            .xpath(composerName, musicNamespace, 1).string(startsWith("Johann"))
            .xpath(composerName, musicNamespace, 1).string(notNullValue());
  }

  @Test
  public void testNumber() {
    String expression = "/ns:people/composers/composer[%s]/someDouble";

    testClient.get().uri("/music/people")
            .exchange()
            .expectBody()
            .xpath(expression, musicNamespace, 1).isEqualTo(21d)
            .xpath(expression, musicNamespace, 2).isEqualTo(.0025)
            .xpath(expression, musicNamespace, 3).isEqualTo(1.6035)
            .xpath(expression, musicNamespace, 4).isEqualTo(Double.NaN)
            .xpath(expression, musicNamespace, 1).number(equalTo(21d))  // Hamcrest..
            .xpath(expression, musicNamespace, 3).number(closeTo(1.6, .01));
  }

  @Test
  public void testBoolean() {
    String expression = "/ns:people/performers/performer[%s]/someBoolean";

    testClient.get().uri("/music/people")
            .exchange()
            .expectBody()
            .xpath(expression, musicNamespace, 1).isEqualTo(false)
            .xpath(expression, musicNamespace, 2).isEqualTo(true);
  }

  @Test
  public void testNodeCount() {
    testClient.get().uri("/music/people")
            .exchange()
            .expectBody()
            .xpath("/ns:people/composers/composer", musicNamespace).nodeCount(4)
            .xpath("/ns:people/performers/performer", musicNamespace).nodeCount(2)
            .xpath("/ns:people/composers/composer", musicNamespace).nodeCount(equalTo(4)) // Hamcrest..
            .xpath("/ns:people/performers/performer", musicNamespace).nodeCount(equalTo(2));
  }

  @Test
  public void testFeedWithLinefeedChars() {
    MockMvcWebTestClient.bindToController(new BlogFeedController()).build()
            .get().uri("/blog.atom")
            .accept(MediaType.APPLICATION_ATOM_XML)
            .exchange()
            .expectBody()
            .xpath("//feed/title").isEqualTo("Test Feed")
            .xpath("//feed/icon").isEqualTo("https://www.example.com/favicon.ico");
  }

  @Controller
  private static class MusicController {

    @RequestMapping(value = "/music/people")
    public @ResponseBody
    PeopleWrapper getPeople() {

      List<Person> composers = Arrays.asList(
              new Person("Johann Sebastian Bach").setSomeDouble(21),
              new Person("Johannes Brahms").setSomeDouble(.0025),
              new Person("Edvard Grieg").setSomeDouble(1.6035),
              new Person("Robert Schumann").setSomeDouble(Double.NaN));

      List<Person> performers = Arrays.asList(
              new Person("Vladimir Ashkenazy").setSomeBoolean(false),
              new Person("Yehudi Menuhin").setSomeBoolean(true));

      return new PeopleWrapper(composers, performers);
    }
  }

  @SuppressWarnings("unused")
  @XmlRootElement(name = "people", namespace = "https://example.org/music/people")
  @XmlAccessorType(XmlAccessType.FIELD)
  private static class PeopleWrapper {

    @XmlElementWrapper(name = "composers")
    @XmlElement(name = "composer")
    private List<Person> composers;

    @XmlElementWrapper(name = "performers")
    @XmlElement(name = "performer")
    private List<Person> performers;

    public PeopleWrapper() {
    }

    public PeopleWrapper(List<Person> composers, List<Person> performers) {
      this.composers = composers;
      this.performers = performers;
    }

    public List<Person> getComposers() {
      return this.composers;
    }

    public List<Person> getPerformers() {
      return this.performers;
    }
  }

  @Controller
  public static class BlogFeedController {

    @RequestMapping(value = "/blog.atom", method = { GET, HEAD })
    @ResponseBody
    public String listPublishedPosts() {
      return """
              <?xml version="1.0" encoding="UTF-8"?>
              <feed xmlns="http://www.w3.org/2005/Atom">
              	<title>Test Feed</title>
              	<icon>https://www.example.com/favicon.ico</icon>
              </feed>""".replaceAll("\n", "\r\n");
    }
  }

}
