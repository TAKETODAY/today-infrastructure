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

import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.Person;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

/**
 * Examples of defining expectations on JSON response content with
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expressions.
 *
 * @author Rossen Stoyanchev
 * @see ContentAssertionTests
 */
public class JsonPathAssertionTests {

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = standaloneSetup(new MusicController())
            .defaultRequest(get("/").accept(MediaType.APPLICATION_JSON))
            .alwaysExpect(status().isOk())
            .alwaysExpect(content().contentType("application/json"))
            .build();
  }

  @Test
  public void exists() throws Exception {
    String composerByName = "$.composers[?(@.name == '%s')]";
    String performerByName = "$.performers[?(@.name == '%s')]";

    this.mockMvc.perform(get("/music/people"))
            .andExpect(jsonPath(composerByName, "Johann Sebastian Bach").exists())
            .andExpect(jsonPath(composerByName, "Johannes Brahms").exists())
            .andExpect(jsonPath(composerByName, "Edvard Grieg").exists())
            .andExpect(jsonPath(composerByName, "Robert Schumann").exists())
            .andExpect(jsonPath(performerByName, "Vladimir Ashkenazy").exists())
            .andExpect(jsonPath(performerByName, "Yehudi Menuhin").exists())
            .andExpect(jsonPath("$.composers[0]").exists())
            .andExpect(jsonPath("$.composers[1]").exists())
            .andExpect(jsonPath("$.composers[2]").exists())
            .andExpect(jsonPath("$.composers[3]").exists());
  }

  @Test
  public void doesNotExist() throws Exception {
    this.mockMvc.perform(get("/music/people"))
            .andExpect(jsonPath("$.composers[?(@.name == 'Edvard Grieeeeeeg')]").doesNotExist())
            .andExpect(jsonPath("$.composers[?(@.name == 'Robert Schuuuuuuman')]").doesNotExist())
            .andExpect(jsonPath("$.composers[4]").doesNotExist());
  }

  @Test
  public void equality() throws Exception {
    this.mockMvc.perform(get("/music/people"))
            .andExpect(jsonPath("$.composers[0].name").value("Johann Sebastian Bach"))
            .andExpect(jsonPath("$.performers[1].name").value("Yehudi Menuhin"));

    // Hamcrest matchers...
    this.mockMvc.perform(get("/music/people"))
            .andExpect(jsonPath("$.composers[0].name").value(equalTo("Johann Sebastian Bach")))
            .andExpect(jsonPath("$.performers[1].name").value(equalTo("Yehudi Menuhin")));
  }

  @Test
  public void hamcrestMatcher() throws Exception {
    this.mockMvc.perform(get("/music/people"))
            .andExpect(jsonPath("$.composers[0].name", startsWith("Johann")))
            .andExpect(jsonPath("$.performers[0].name", endsWith("Ashkenazy")))
            .andExpect(jsonPath("$.performers[1].name", containsString("di Me")))
            .andExpect(jsonPath("$.composers[1].name", is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms")))));
  }

  @Test
  public void hamcrestMatcherWithParameterizedJsonPath() throws Exception {
    String composerName = "$.composers[%s].name";
    String performerName = "$.performers[%s].name";

    this.mockMvc.perform(get("/music/people"))
            .andExpect(jsonPath(composerName, 0).value(startsWith("Johann")))
            .andExpect(jsonPath(performerName, 0).value(endsWith("Ashkenazy")))
            .andExpect(jsonPath(performerName, 1).value(containsString("di Me")))
            .andExpect(jsonPath(composerName, 1).value(is(in(Arrays.asList("Johann Sebastian Bach", "Johannes Brahms")))));
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
