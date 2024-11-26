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

package infra.test.web.mock.result;

import org.hamcrest.Matcher;

import java.lang.reflect.Method;

import infra.test.web.mock.MvcResult;
import infra.test.web.mock.ResultMatcher;
import infra.util.ClassUtils;
import infra.web.handler.method.HandlerMethod;
import infra.web.handler.method.MvcUriComponentsBuilder;
import infra.web.handler.method.MvcUriComponentsBuilder.MethodInvocationInfo;
import infra.web.handler.method.RequestMappingHandlerAdapter;
import infra.web.handler.method.RequestMappingHandlerMapping;

import static infra.test.util.AssertionErrors.assertEquals;
import static infra.test.util.AssertionErrors.assertNotNull;
import static infra.test.util.AssertionErrors.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Factory for assertions on the selected handler or handler method.
 *
 * <p>An instance of this class is typically accessed via
 * {@link MockMvcResultMatchers#handler}.
 *
 * <p><strong>Note:</strong> Expectations that assert the controller method
 * used to process the request work only for requests processed with
 * {@link RequestMappingHandlerMapping} and {@link RequestMappingHandlerAdapter}
 * which is used by default with the Web MVC Java config and XML namespace.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public class HandlerResultMatchers {

  /**
   * Protected constructor.
   * Use {@link MockMvcResultMatchers#handler()}.
   */
  protected HandlerResultMatchers() {
  }

  /**
   * Assert the type of the handler that processed the request.
   */
  public ResultMatcher handlerType(Class<?> type) {
    return result -> {
      Object handler = result.getHandler();
      assertNotNull("No handler", handler);
      Class<?> actual = handler.getClass();
      if (handler instanceof HandlerMethod handlerMethod) {
        actual = handlerMethod.getBeanType();
      }
      assertEquals("Handler type", type, ClassUtils.getUserClass(actual));
    };
  }

  /**
   * Assert the controller method used to process the request.
   * <p>The expected method is specified through a "mock" controller method
   * invocation similar to {@link MvcUriComponentsBuilder#fromMethodCall(Object)}.
   * <p>For example, given this controller:
   * <pre class="code">
   * &#064;RestController
   * public class SimpleController {
   *
   *     &#064;RequestMapping("/")
   *     public ResponseEntity&lt;Void&gt; handle() {
   *         return ResponseEntity.ok().build();
   *     }
   * }
   * </pre>
   * <p>A test that has statically imported {@link MvcUriComponentsBuilder#on}
   * can be performed as follows:
   * <pre class="code">
   * mockMvc.perform(get("/"))
   *     .andExpect(handler().methodCall(on(SimpleController.class).handle()));
   * </pre>
   *
   * @param obj either the value returned from a "mock" controller invocation
   * or the "mock" controller itself after an invocation
   */
  public ResultMatcher methodCall(Object obj) {
    return result -> {
      if (!(obj instanceof MethodInvocationInfo invocationInfo)) {
        throw new AssertionError("""
                The supplied object [%s] is not an instance of %s. Ensure \
                that you invoke the handler method via MvcUriComponentsBuilder.on()."""
                .formatted(obj, MethodInvocationInfo.class.getName()));
      }
      Method expected = invocationInfo.getControllerMethod();
      Method actual = getHandlerMethod(result).getMethod();
      assertEquals("Handler method", expected, actual);
    };
  }

  /**
   * Assert the name of the controller method used to process the request
   * using the given Hamcrest {@link Matcher}.
   */
  public ResultMatcher methodName(Matcher<? super String> matcher) {
    return result -> {
      HandlerMethod handlerMethod = getHandlerMethod(result);
      assertThat("Handler method", handlerMethod.getMethod().getName(), matcher);
    };
  }

  /**
   * Assert the name of the controller method used to process the request.
   */
  public ResultMatcher methodName(String name) {
    return result -> {
      HandlerMethod handlerMethod = getHandlerMethod(result);
      assertEquals("Handler method", name, handlerMethod.getMethod().getName());
    };
  }

  /**
   * Assert the controller method used to process the request.
   */
  public ResultMatcher method(Method method) {
    return result -> {
      HandlerMethod handlerMethod = getHandlerMethod(result);
      assertEquals("Handler method", method, handlerMethod.getMethod());
    };
  }

  private static HandlerMethod getHandlerMethod(MvcResult result) {
    Object handler = result.getHandler();
    assertTrue("Not a HandlerMethod: " + handler, handler instanceof HandlerMethod);
    return (HandlerMethod) handler;
  }

}
