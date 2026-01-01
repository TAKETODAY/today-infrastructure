/*
 * Copyright 2017 - 2025 the original author or authors.
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