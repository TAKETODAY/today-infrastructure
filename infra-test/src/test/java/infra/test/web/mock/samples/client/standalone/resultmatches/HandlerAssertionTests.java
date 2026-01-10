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

package infra.test.web.mock.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.http.ResponseEntity;
import infra.test.web.mock.ResultActions;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.EntityExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.handler.method.MvcUriComponentsBuilder;

import static infra.test.web.mock.result.MockMvcResultMatchers.handler;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.web.handler.method.MvcUriComponentsBuilder.on;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.HandlerAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class HandlerAssertionTests {

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new SimpleController())
                  .alwaysExpect(status().isOk())
                  .build();

  @Test
  public void handlerType() throws Exception {
    performRequest().andExpect(handler().handlerType(SimpleController.class));
  }

  @Test
  public void methodCallOnNonMock() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> performRequest().andExpect(handler().methodCall("bogus")))
            .withMessageContaining("The supplied object [bogus] is not an instance of")
            .withMessageContaining(MvcUriComponentsBuilder.MethodInvocationInfo.class.getName())
            .withMessageContaining("Ensure that you invoke the handler method via MvcUriComponentsBuilder.on()");
  }

  @Test
  public void methodCall() throws Exception {
    performRequest().andExpect(handler().methodCall(on(SimpleController.class).handle()));
  }

  @Test
  public void methodName() throws Exception {
    performRequest().andExpect(handler().methodName("handle"));
  }

  @Test
  public void methodNameMatchers() throws Exception {
    performRequest()
            .andExpect(handler().methodName(equalTo("handle")))
            .andExpect(handler().methodName(is(not("save"))));
  }

  @Test
  public void method() throws Exception {
    Method method = SimpleController.class.getMethod("handle");
    performRequest().andExpect(handler().method(method));
  }

  private ResultActions performRequest() {
    EntityExchangeResult<Void> result = client.get().uri("/").exchange().expectBody().isEmpty();
    return MockMvcWebTestClient.resultActionsFor(result);
  }

  @RestController
  static class SimpleController {

    @RequestMapping("/")
    public ResponseEntity<Void> handle() {
      return ResponseEntity.ok().build();
    }
  }

}
