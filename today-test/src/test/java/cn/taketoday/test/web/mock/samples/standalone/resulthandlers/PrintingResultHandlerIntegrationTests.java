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

package cn.taketoday.test.web.mock.samples.standalone.resulthandlers;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.web.mock.result.PrintingResultHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.RestController;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultHandlers.print;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests for {@link PrintingResultHandler}.
 *
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @see PrintingResultHandlerSmokeTests
 * @see cn.taketoday.test.web.mock.result.PrintingResultHandlerTests
 */
class PrintingResultHandlerIntegrationTests {

  @Test
  void printMvcResultsToWriter() throws Exception {
    StringWriter writer = new StringWriter();

    standaloneSetup(new SimpleController())
            .alwaysDo(print(writer))
            .build()
            .perform(get("/").content("Hello Request".getBytes()).characterEncoding("ISO-8859-1"))
            .andExpect(content().string("Hello Response"));

    assertThat(writer).asString()
            .contains("Hello Request")
            .contains("Hello Response")
            .contains("Headers = [Set-Cookie:\"enigma=42\", Content-Type:\"text/plain;charset=UTF-8\", Content-Length:\"14\"]");
  }

  @Test
  void printMvcResultsToWriterWithJsonResponseBodyInterpretedAsUtf8() throws Exception {
    StringWriter writer = new StringWriter();

    standaloneSetup(new SimpleController()).build()
            // "Hallöchen" is German slang for "hello".
            .perform(get("/utf8").accept(MediaType.APPLICATION_JSON).content("Hallöchen, Welt!".getBytes(UTF_8)).characterEncoding(UTF_8))
            .andDo(print(writer))
            // "Grüß dich!" is German for "greetings to you".
            .andExpect(content().bytes("Grüß dich!".getBytes(UTF_8)));

    assertThat(writer).asString()
            .contains("Body = Hallöchen, Welt!")
            .contains("Body = Grüß dich!");
  }

  @Test
  void printMvcResultsToWriterWithFailingGlobalResultMatcher() throws Exception {
    StringWriter writer = new StringWriter();

    try {
      standaloneSetup(new SimpleController())
              .alwaysDo(print(writer))
              .alwaysExpect(content().string("Boom!"))
              .build()
              .perform(get("/").content("Hello Request".getBytes()).characterEncoding("ISO-8859-1"));
      fail("AssertionError is expected to be thrown.");
    }
    catch (AssertionError error) {
      assertThat(error).hasMessageContaining("Boom!");
    }

    assertThat(writer).asString()
            .contains("Hello Request")
            .contains("Hello Response")
            .contains("Headers = [Set-Cookie:\"enigma=42\", Content-Type:\"text/plain;charset=UTF-8\", Content-Length:\"14\"]");
  }

  @RestController
  private static class SimpleController {

    @GetMapping("/")
    String hello(RequestContext response) {
      response.addCookie(new HttpCookie("enigma", "42"));
      return "Hello Response";
    }

    @GetMapping("/utf8")
    String utf8(RequestContext response) {
      return "Grüß dich!";
    }
  }

}
