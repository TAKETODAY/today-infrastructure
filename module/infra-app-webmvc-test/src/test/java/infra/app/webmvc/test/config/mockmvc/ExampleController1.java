/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.webmvc.test.config.mockmvc;

import infra.app.webmvc.test.config.WebMvcTest;
import infra.session.Session;
import infra.session.config.EnableSession;
import infra.stereotype.Controller;
import infra.web.RequestContext;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;

/**
 * Example {@link Controller @Controller} used with {@link WebMvcTest @WebMvcTest} tests.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@EnableSession
@RestController
public class ExampleController1 {

  @GetMapping("/one")
  public String one() {
    return "one";
  }

  @GetMapping("/error")
  public String error() {
    throw new ExampleException();
  }

  @GetMapping(path = "/html", produces = "text/html")
  public String html() {
    return "<html><body>Hello</body></html>";
  }

  @GetMapping("/formatting")
  public String formatting(RequestContext request, Session session) {
    Object formattingFails = new Object() {
      @Override
      public String toString() {
        throw new IllegalStateException("Formatting failed");
      }
    };
    session.setAttribute("attribute-1", formattingFails);
    return "formatting";
  }

}
