/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/2/12 15:18
 */
class HandlerWrapperTests {

  @Test
  void unwrap() {
    assertThat(HandlerWrapper.unwrap(null)).isEqualTo(null);
    Object handler = new Object();
    assertThat(HandlerWrapper.unwrap(handler)).isSameAs(handler);
  }

  @Test
  void unwrapWithNullHandlerReturnsNull() {
    assertThat(HandlerWrapper.unwrap(null)).isNull();
  }

  @Test
  void unwrapWithNonHandlerWrapperReturnsSameObject() {
    Object handler = new Object();

    Object result = HandlerWrapper.unwrap(handler);

    assertThat(result).isSameAs(handler);
  }

  @Test
  void unwrapWithHandlerWrapperReturnsRawHandler() {
    Object rawHandler = new Object();
    HandlerWrapper wrapper = () -> rawHandler;

    Object result = HandlerWrapper.unwrap(wrapper);

    assertThat(result).isSameAs(rawHandler);
  }

  @Test
  void unwrapWithNestedHandlerWrappersReturnsInnerMostHandler() {
    Object rawHandler = new Object();
    HandlerWrapper innerWrapper = () -> rawHandler;
    HandlerWrapper outerWrapper = () -> innerWrapper;

    Object result = HandlerWrapper.unwrap(outerWrapper);

    assertThat(result).isSameAs(innerWrapper);
  }

  @Test
  void getRawHandlerReturnsCorrectHandler() {
    Object rawHandler = new Object();
    HandlerWrapper wrapper = () -> rawHandler;

    Object result = wrapper.getRawHandler();

    assertThat(result).isSameAs(rawHandler);
  }

  @Test
  void unwrapWithHandlerWrapperReturningNull() {
    HandlerWrapper wrapper = () -> null;

    Object result = HandlerWrapper.unwrap(wrapper);

    assertThat(result).isNull();
  }

}