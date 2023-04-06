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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.RestController;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Examples of defining expectations on the response content, content type, and
 * the character encoding.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see JsonPathAssertionTests
 * @see XmlContentAssertionTests
 * @see XpathAssertionTests
 */
public class ContentAssertionTests {

  private final MockMvc mockMvc = standaloneSetup(new SimpleController()).alwaysExpect(status().isOk()).build();

  @Test
  void contentType() throws Exception {
    this.mockMvc.perform(get("/handle").accept(MediaType.TEXT_PLAIN))
            .andExpect(content().contentType(MediaType.valueOf("text/plain;charset=UTF-8")))
            .andExpect(content().contentType("text/plain;charset=UTF-8"))
            .andExpect(content().contentTypeCompatibleWith("text/plain"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));

    this.mockMvc.perform(get("/handleUtf8"))
            .andExpect(content().contentType(MediaType.valueOf("text/plain;charset=UTF-8")))
            .andExpect(content().contentType("text/plain;charset=UTF-8"))
            .andExpect(content().contentTypeCompatibleWith("text/plain"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
  }

  @Test
  void contentAsString() throws Exception {
    this.mockMvc.perform(get("/handle").accept(MediaType.TEXT_PLAIN))
            .andExpect(content().string("Hello world!"));

    this.mockMvc.perform(get("/handleUtf8"))
            .andExpect(content().string("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01"));

    // Hamcrest matchers...
    this.mockMvc.perform(get("/handle").accept(MediaType.TEXT_PLAIN)).andExpect(content().string(equalTo("Hello world!")));
    this.mockMvc.perform(get("/handleUtf8")).andExpect(content().string(equalTo("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01")));
  }

  @Test
  void contentAsBytes() throws Exception {
    this.mockMvc.perform(get("/handle").accept(MediaType.TEXT_PLAIN))
            .andExpect(content().bytes("Hello world!".getBytes(StandardCharsets.ISO_8859_1)));

    this.mockMvc.perform(get("/handleUtf8"))
            .andExpect(content().bytes("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void contentStringMatcher() throws Exception {
    this.mockMvc.perform(get("/handle").accept(MediaType.TEXT_PLAIN))
            .andExpect(content().string(containsString("world")));
  }

  @Test
  void characterEncoding() throws Exception {
    this.mockMvc.perform(get("/handle").accept(MediaType.TEXT_PLAIN))
            .andExpect(content().encoding("UTF-8"))
            .andExpect(content().string(containsString("world")));

    this.mockMvc.perform(get("/handle").accept(MediaType.TEXT_PLAIN))
            .andExpect(content().encoding(StandardCharsets.UTF_8))
            .andExpect(content().string(containsString("world")));

    this.mockMvc.perform(get("/handleUtf8"))
            .andExpect(content().encoding("UTF-8"))
            .andExpect(content().bytes("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(StandardCharsets.UTF_8)));

    this.mockMvc.perform(get("/handleUtf8"))
            .andExpect(content().encoding(StandardCharsets.UTF_8))
            .andExpect(content().bytes("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01".getBytes(StandardCharsets.UTF_8)));
  }

  @RestController
  private static class SimpleController {

    @GetMapping(path = "/handle", produces = "text/plain")
    String handle() {
      return "Hello world!";
    }

    @GetMapping(path = "/handleUtf8", produces = "text/plain;charset=UTF-8")
    String handleWithCharset() {
      return "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c\uff01";  // "Hello world! (Japanese)
    }
  }

}
