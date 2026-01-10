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

package infra.test.web.mock.samples.standalone.resultmatchers;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.http.ResponseEntity;
import infra.test.web.mock.MockMvc;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.handler.method.MvcUriComponentsBuilder;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.handler;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static infra.web.handler.method.MvcUriComponentsBuilder.on;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Examples of expectations on the controller type and controller method.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class HandlerAssertionTests {

  private final MockMvc mockMvc = standaloneSetup(new SimpleController()).alwaysExpect(status().isOk()).build();

  @Test
  public void handlerType() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(handler().handlerType(SimpleController.class));
  }

  @Test
  public void methodCallOnNonMock() throws Exception {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
                    this.mockMvc.perform(get("/")).andExpect(handler().methodCall("bogus")))
            .withMessageContaining("The supplied object [bogus] is not an instance of")
            .withMessageContaining(MvcUriComponentsBuilder.MethodInvocationInfo.class.getName())
            .withMessageContaining("Ensure that you invoke the handler method via MvcUriComponentsBuilder.on()");
  }

  @Test
  public void methodCall() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(handler().methodCall(on(SimpleController.class).handle()));
  }

  @Test
  public void methodName() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(handler().methodName("handle"));
  }

  @Test
  public void methodNameMatchers() throws Exception {
    this.mockMvc.perform(get("/")).andExpect(handler().methodName(equalTo("handle")));
    this.mockMvc.perform(get("/")).andExpect(handler().methodName(is(not("save"))));
  }

  @Test
  public void method() throws Exception {
    Method method = SimpleController.class.getMethod("handle");
    this.mockMvc.perform(get("/")).andExpect(handler().method(method));
  }

  @RestController
  static class SimpleController {

    @RequestMapping("/")
    public ResponseEntity<Void> handle() {
      return ResponseEntity.ok().build();
    }
  }

}
