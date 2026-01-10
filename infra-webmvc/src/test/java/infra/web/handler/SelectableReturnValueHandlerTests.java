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

import java.util.List;

import infra.web.RequestContext;
import infra.web.ReturnValueHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 22:41
 */
class SelectableReturnValueHandlerTests {

  @Test
  void constructorWithNullHandlersThrowsException() {
    assertThatThrownBy(() -> new SelectableReturnValueHandler(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("internalHandlers is required");
  }

  @Test
  void constructorWithValidHandlers() {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    assertThat(selectableHandler.getInternalHandlers()).isSameAs(handlers);
  }

  @Test
  void supportsHandlerReturnsTrueWhenHandlerSupported() {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsHandler("testHandler")).willReturn(false);
    given(handler2.supportsHandler("testHandler")).willReturn(true);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    boolean result = selectableHandler.supportsHandler("testHandler");

    assertThat(result).isTrue();
  }

  @Test
  void supportsHandlerReturnsFalseWhenNoHandlerSupported() {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsHandler("testHandler")).willReturn(false);
    given(handler2.supportsHandler("testHandler")).willReturn(false);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    boolean result = selectableHandler.supportsHandler("testHandler");

    assertThat(result).isFalse();
  }

  @Test
  void supportsReturnValueReturnsTrueWhenHandlerSupported() {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsReturnValue("testReturnValue")).willReturn(false);
    given(handler2.supportsReturnValue("testReturnValue")).willReturn(true);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    boolean result = selectableHandler.supportsReturnValue("testReturnValue");

    assertThat(result).isTrue();
  }

  @Test
  void supportsReturnValueReturnsFalseWhenNoHandlerSupported() {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsReturnValue("testReturnValue")).willReturn(false);
    given(handler2.supportsReturnValue("testReturnValue")).willReturn(false);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    boolean result = selectableHandler.supportsReturnValue("testReturnValue");

    assertThat(result).isFalse();
  }

  @Test
  void selectHandlerReturnsMatchingHandlerForHandler() {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsHandler("testHandler")).willReturn(false);
    given(handler2.supportsHandler("testHandler")).willReturn(true);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    ReturnValueHandler result = selectableHandler.selectHandler("testHandler", SelectableReturnValueHandler.NONE_RETURN_VALUE);

    assertThat(result).isSameAs(handler2);
  }

  @Test
  void selectHandlerReturnsMatchingHandlerForReturnValue() {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsReturnValue("testReturnValue")).willReturn(false);
    given(handler2.supportsReturnValue("testReturnValue")).willReturn(true);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    ReturnValueHandler result = selectableHandler.selectHandler(null, "testReturnValue");

    assertThat(result).isSameAs(handler2);
  }

  @Test
  void selectHandlerReturnsNullWhenNoMatch() {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsHandler("testHandler")).willReturn(false);
    given(handler2.supportsHandler("testHandler")).willReturn(false);
    given(handler1.supportsReturnValue("testReturnValue")).willReturn(false);
    given(handler2.supportsReturnValue("testReturnValue")).willReturn(false);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    ReturnValueHandler result1 = selectableHandler.selectHandler("testHandler", SelectableReturnValueHandler.NONE_RETURN_VALUE);
    ReturnValueHandler result2 = selectableHandler.selectHandler(null, "testReturnValue");

    assertThat(result1).isNull();
    assertThat(result2).isNull();
  }

  @Test
  void handleReturnValueSuccessfullyHandlesWithSelectedHandler() throws Exception {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsReturnValue("testReturnValue")).willReturn(false);
    given(handler2.supportsReturnValue("testReturnValue")).willReturn(true);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);
    RequestContext context = mock(RequestContext.class);

    selectableHandler.handleReturnValue(context, "testHandler", "testReturnValue");

    verify(handler2).handleReturnValue(context, "testHandler", "testReturnValue");
  }

  @Test
  void handleReturnValueThrowsExceptionWhenNoHandlerFound() throws Exception {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsReturnValue("testReturnValue")).willReturn(false);
    given(handler2.supportsReturnValue("testReturnValue")).willReturn(false);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);
    RequestContext context = mock(RequestContext.class);

    assertThatThrownBy(() -> selectableHandler.handleReturnValue(context, "testHandler", "testReturnValue"))
            .isInstanceOf(ReturnValueHandlerNotFoundException.class);
  }

  @Test
  void handleSelectivelyReturnsSelectedHandler() throws Exception {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsReturnValue("testReturnValue")).willReturn(false);
    given(handler2.supportsReturnValue("testReturnValue")).willReturn(true);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);
    RequestContext context = mock(RequestContext.class);

    ReturnValueHandler result = selectableHandler.handleSelectively(context, "testHandler", "testReturnValue");

    assertThat(result).isSameAs(handler2);
    verify(handler2).handleReturnValue(context, "testHandler", "testReturnValue");
  }

  @Test
  void handleSelectivelyReturnsNullWhenNoHandlerFound() throws Exception {
    ReturnValueHandler handler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler handler2 = mock(ReturnValueHandler.class);
    given(handler1.supportsReturnValue("testReturnValue")).willReturn(false);
    given(handler2.supportsReturnValue("testReturnValue")).willReturn(false);
    List<ReturnValueHandler> handlers = List.of(handler1, handler2);

    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);
    RequestContext context = mock(RequestContext.class);

    ReturnValueHandler result = selectableHandler.handleSelectively(context, "testHandler", "testReturnValue");

    assertThat(result).isNull();
  }

  @Test
  void trimToSizeTrimsInternalHandlersList() {
    List<ReturnValueHandler> handlers = new java.util.ArrayList<>(List.of(mock(ReturnValueHandler.class)));
    handlers.add(null); // Add null to make it modifiable
    SelectableReturnValueHandler selectableHandler = new SelectableReturnValueHandler(handlers);

    selectableHandler.trimToSize();

    // Verification that trimToSize was called - this mainly tests that the method doesn't throw an exception
    assertThatCode(selectableHandler::trimToSize).doesNotThrowAnyException();
  }

}