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

import org.junit.jupiter.api.Test;

import infra.app.webmvc.test.config.WebMvcTest;
import infra.beans.factory.annotation.Autowired;
import infra.http.HttpStatus;
import infra.test.context.TestPropertySource;
import infra.test.web.mock.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Test {@link infra.web.DispatcherHandler} customizations.
 *
 * @author Stephane Nicoll
 */
@WebMvcTest
@TestPropertySource(properties = { "web.mvc.throw-exception-if-no-handler-found=true",
        "web.resources.static-path-pattern=/static/**" })
class WebMvcTestCustomDispatcherIntegrationTests {

  @Autowired
  private MockMvcTester mvc;

  @Test
  void dispatcherServletIsCustomized() {
    assertThat(this.mvc.get().uri("/does-not-exist")).hasStatus(HttpStatus.BAD_REQUEST)
            .hasBodyTextEqualTo("Invalid request: /does-not-exist");
  }

}
