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

package infra.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.web.handler.result.SmartReturnValueHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/6 15:00
 */
class ReturnValueHandlerTests {

  @Test
  void noneReturnValueConstantIsNotNull() {
    assertThat(ReturnValueHandler.NONE_RETURN_VALUE).isNotNull();
  }

  @Test
  void selectWithNullReturnValueAndNullHandlerReturnsNull() {
    Iterable<ReturnValueHandler> handlers = List.of();

    ReturnValueHandler result = ReturnValueHandler.select(handlers, null, null);

    assertThat(result).isNull();
  }

  @Test
  void selectWithNoneReturnValueAndNonNullHandler() {
    ReturnValueHandler mockHandler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler mockHandler2 = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(mockHandler1, mockHandler2);

    when(mockHandler1.supportsHandler("testHandler")).thenReturn(false);
    when(mockHandler2.supportsHandler("testHandler")).thenReturn(true);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, "testHandler", ReturnValueHandler.NONE_RETURN_VALUE);

    assertThat(result).isSameAs(mockHandler2);
    verify(mockHandler1).supportsHandler("testHandler");
    verify(mockHandler2).supportsHandler("testHandler");
  }

  @Test
  void selectWithNoneReturnValueAndNoMatchingHandlerReturnsNull() {
    ReturnValueHandler mockHandler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler mockHandler2 = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(mockHandler1, mockHandler2);

    when(mockHandler1.supportsHandler("testHandler")).thenReturn(false);
    when(mockHandler2.supportsHandler("testHandler")).thenReturn(false);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, "testHandler", ReturnValueHandler.NONE_RETURN_VALUE);

    assertThat(result).isNull();
    verify(mockHandler1).supportsHandler("testHandler");
    verify(mockHandler2).supportsHandler("testHandler");
  }

  @Test
  void selectWithNonNullHandlerAndReturnValuePrefersSmartHandler() {
    SmartReturnValueHandler smartHandler = mock(SmartReturnValueHandler.class);
    ReturnValueHandler regularHandler = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(regularHandler, smartHandler);

    when(smartHandler.supportsHandler("testHandler", "testValue")).thenReturn(true);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, "testHandler", "testValue");

    assertThat(result).isSameAs(smartHandler);
    verify(smartHandler).supportsHandler("testHandler", "testValue");
  }

  @Test
  void selectWithNonNullHandlerAndReturnValueFallsBackToRegularHandlerSupportsHandler() {
    SmartReturnValueHandler smartHandler = mock(SmartReturnValueHandler.class);
    ReturnValueHandler regularHandler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler regularHandler2 = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(regularHandler1, smartHandler, regularHandler2);

    when(smartHandler.supportsHandler("testHandler", "testValue")).thenReturn(false);
    when(regularHandler1.supportsHandler("testHandler")).thenReturn(true);
    when(regularHandler1.supportsReturnValue("testValue")).thenReturn(false);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, "testHandler", "testValue");

    assertThat(result).isSameAs(regularHandler1);
//    verify(smartHandler).supportsHandler("testHandler", "testValue");
    verify(regularHandler1).supportsHandler("testHandler");
  }

  @Test
  void selectWithNonNullHandlerAndReturnValueFallsBackToRegularHandlerSupportsReturnValue() {
    SmartReturnValueHandler smartHandler = mock(SmartReturnValueHandler.class);
    ReturnValueHandler regularHandler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler regularHandler2 = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(regularHandler1, smartHandler, regularHandler2);

    when(smartHandler.supportsHandler("testHandler", "testValue")).thenReturn(false);
    when(regularHandler1.supportsHandler("testHandler")).thenReturn(false);
    when(regularHandler1.supportsReturnValue("testValue")).thenReturn(false);
    when(regularHandler2.supportsHandler("testHandler")).thenReturn(false);
    when(regularHandler2.supportsReturnValue("testValue")).thenReturn(true);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, "testHandler", "testValue");

    assertThat(result).isSameAs(regularHandler2);
    verify(smartHandler).supportsHandler("testHandler", "testValue");
    verify(regularHandler1).supportsHandler("testHandler");
    verify(regularHandler1).supportsReturnValue("testValue");
    verify(regularHandler2).supportsHandler("testHandler");
    verify(regularHandler2).supportsReturnValue("testValue");
  }

  @Test
  void selectWithNullHandlerAndNonNullReturnValue() {
    ReturnValueHandler mockHandler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler mockHandler2 = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(mockHandler1, mockHandler2);

    when(mockHandler1.supportsReturnValue("testValue")).thenReturn(false);
    when(mockHandler2.supportsReturnValue("testValue")).thenReturn(true);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, null, "testValue");

    assertThat(result).isSameAs(mockHandler2);
    verify(mockHandler1).supportsReturnValue("testValue");
    verify(mockHandler2).supportsReturnValue("testValue");
  }

  @Test
  void selectWithNullHandlerAndNonNullReturnValueNoMatchReturnsNull() {
    ReturnValueHandler mockHandler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler mockHandler2 = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(mockHandler1, mockHandler2);

    when(mockHandler1.supportsReturnValue("testValue")).thenReturn(false);
    when(mockHandler2.supportsReturnValue("testValue")).thenReturn(false);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, null, "testValue");

    assertThat(result).isNull();
    verify(mockHandler1).supportsReturnValue("testValue");
    verify(mockHandler2).supportsReturnValue("testValue");
  }

  @Test
  void selectWithNonNullHandlerAndNoneReturnValue() {
    ReturnValueHandler mockHandler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler mockHandler2 = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(mockHandler1, mockHandler2);

    when(mockHandler1.supportsHandler("testHandler")).thenReturn(false);
    when(mockHandler2.supportsHandler("testHandler")).thenReturn(true);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, "testHandler", ReturnValueHandler.NONE_RETURN_VALUE);

    assertThat(result).isSameAs(mockHandler2);
    verify(mockHandler1).supportsHandler("testHandler");
    verify(mockHandler2).supportsHandler("testHandler");
  }

  @Test
  void selectWithNonNullHandlerAndNoneReturnValueNoMatchReturnsNull() {
    ReturnValueHandler mockHandler1 = mock(ReturnValueHandler.class);
    ReturnValueHandler mockHandler2 = mock(ReturnValueHandler.class);
    Iterable<ReturnValueHandler> handlers = List.of(mockHandler1, mockHandler2);

    when(mockHandler1.supportsHandler("testHandler")).thenReturn(false);
    when(mockHandler2.supportsHandler("testHandler")).thenReturn(false);

    ReturnValueHandler result = ReturnValueHandler.select(handlers, "testHandler", ReturnValueHandler.NONE_RETURN_VALUE);

    assertThat(result).isNull();
    verify(mockHandler1).supportsHandler("testHandler");
    verify(mockHandler2).supportsHandler("testHandler");
  }

}