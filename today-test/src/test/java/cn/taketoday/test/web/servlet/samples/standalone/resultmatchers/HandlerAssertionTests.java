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

import java.lang.reflect.Method;

import cn.taketoday.http.ResponseEntity;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.handler.method.MvcUriComponentsBuilder;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.handler;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static cn.taketoday.web.handler.method.MvcUriComponentsBuilder.on;
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
