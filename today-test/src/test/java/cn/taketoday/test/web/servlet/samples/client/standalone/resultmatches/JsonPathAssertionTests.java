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

package cn.taketoday.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;

import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.resultmatchers.JsonPathAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class JsonPathAssertionTests {

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new MusicController())
                  .alwaysExpect(status().isOk())
                  .alwaysExpect(content().contentType(MediaType.APPLICATION_JSON))
                  .configureClient()
                  .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                  .build();

  @Test
  public void exists() {
    String composerByName = "$.composers[?(@.name == '%s')]";
    String performerByName = "$.performers[?(@.name == '%s')]";

    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath(composerByName, "Johann Sebastian Bach").exists()
            .jsonPath(composerByName, "Johannes Brahms").exists()
            .jsonPath(composerByName, "Edvard Grieg").exists()
            .jsonPath(composerByName, "Robert Schumann").exists()
            .jsonPath(performerByName, "Vladimir Ashkenazy").exists()
            .jsonPath(performerByName, "Yehudi Menuhin").exists()
            .jsonPath("$.composers[0]").exists()
            .jsonPath("$.composers[1]").exists()
            .jsonPath("$.composers[2]").exists()
            .jsonPath("$.composers[3]").exists();
  }

  @Test
  public void doesNotExist() {
    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath("$.composers[?(@.name == 'Edvard Grieeeeeeg')]").doesNotExist()
            .jsonPath("$.composers[?(@.name == 'Robert Schuuuuuuman')]").doesNotExist()
            .jsonPath("$.composers[4]").doesNotExist();
  }

  @Test
  public void equality() {
    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath("$.composers[0].name").isEqualTo("Johann Sebastian Bach")
            .jsonPath("$.performers[1].name").isEqualTo("Yehudi Menuhin");

    // Hamcrest matchers...
    client.get().uri("/music/people")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.composers[0].name").value(equalTo("Johann Sebastian Bach"))
            .jsonPath("$.performers[1].name").value(equalTo("Yehudi Menuhin"));
  }

  @Test
  public void hamcrestMatcher() {
    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath("$.composers[0].name").value(startsWith("Johann"))
            .jsonPath("$.performers[0].name").value(endsWith("Ashkenazy"))
            .jsonPath("$.performers[1].name").value(containsString("di Me"))
            .jsonPath("$.composers[1].name").value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))));
  }

  @Test
  public void hamcrestMatcherWithParameterizedJsonPath() {
    String composerName = "$.composers[%s].name";
    String performerName = "$.performers[%s].name";

    client.get().uri("/music/people")
            .exchange()
            .expectBody()
            .jsonPath(composerName, 0).value(startsWith("Johann"))
            .jsonPath(performerName, 0).value(endsWith("Ashkenazy"))
            .jsonPath(performerName, 1).value(containsString("di Me"))
            .jsonPath(composerName, 1).value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms"))));
  }

  @RestController
  private class MusicController {

    @RequestMapping("/music/people")
    public MultiValueMap<String, Person> get() {
      MultiValueMap<String, Person> map = new LinkedMultiValueMap<>();

      map.add("composers", new Person("Johann Sebastian Bach"));
      map.add("composers", new Person("Johannes Brahms"));
      map.add("composers", new Person("Edvard Grieg"));
      map.add("composers", new Person("Robert Schumann"));

      map.add("performers", new Person("Vladimir Ashkenazy"));
      map.add("performers", new Person("Yehudi Menuhin"));

      return map;
    }
  }

}
