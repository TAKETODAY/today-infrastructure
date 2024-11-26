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
