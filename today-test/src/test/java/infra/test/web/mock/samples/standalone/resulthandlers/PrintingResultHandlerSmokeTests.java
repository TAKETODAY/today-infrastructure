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

package infra.test.web.mock.samples.standalone.resulthandlers;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import infra.stereotype.Controller;
import infra.test.web.mock.result.PrintingResultHandler;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.ResponseBody;
import infra.mock.api.http.Cookie;
import infra.mock.api.http.HttpMockResponse;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultHandlers.log;
import static infra.test.web.mock.result.MockMvcResultHandlers.print;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

/**
 * Smoke test for {@link PrintingResultHandler}.
 *
 * <p>Prints debugging information about the executed request and response to
 * various output streams.
 *
 * <p><strong>NOTE</strong>: this <em>smoke test</em> is not intended to be
 * executed with the build. To run this test, comment out the {@code @Disabled}
 * declaration and inspect the output manually.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see infra.test.web.mock.result.PrintingResultHandlerTests
 */
@Disabled
public class PrintingResultHandlerSmokeTests {

  // Not intended to be executed with the build.
  // Comment out class-level @Disabled to see the output.

  @Test
  public void testPrint() throws Exception {
    StringWriter writer = new StringWriter();

    standaloneSetup(new SimpleController())
            .build()
            .perform(get("/").content("Hello Request".getBytes()))
            .andDo(log())
            .andDo(print())
            .andDo(print(System.err))
            .andDo(print(writer))
    ;

    System.out.println();
    System.out.println("===============================================================");
    System.out.println(writer);
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/")
    @ResponseBody
    public String hello(HttpMockResponse response) {
      response.addCookie(new Cookie("enigma", "42"));
      return "Hello Response";
    }
  }
}
