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

package infra.test.web.mock.samples.standalone.resulthandlers;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.test.web.mock.result.PrintingResultHandler;
import infra.web.RequestContext;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultHandlers.print;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration tests for {@link PrintingResultHandler}.
 *
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @see PrintingResultHandlerSmokeTests
 * @see infra.test.web.mock.result.PrintingResultHandlerTests
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
      response.addCookie(ResponseCookie.forSimple("enigma", "42"));
      return "Hello Response";
    }

    @GetMapping("/utf8")
    String utf8(RequestContext response) {
      return "Grüß dich!";
    }
  }

}
