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

package infra.context.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.context.MessageSource;
import infra.context.MessageSourceResolvable;
import infra.context.NoSuchMessageException;
import infra.core.i18n.LocaleContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageSourceAccessorTests {

  private MessageSource messageSource;
  private MessageSourceAccessor accessor;
  private static final String CODE = "test.code";
  private static final String MESSAGE = "test message";
  private static final Object[] ARGS = new Object[] { "arg1", "arg2" };
  private static final String DEFAULT_MESSAGE = "default";

  @BeforeEach
  void setUp() {
    messageSource = mock(MessageSource.class);
    accessor = new MessageSourceAccessor(messageSource);
  }

  @Test
  void getMessageWithCodeOnly() {
    when(messageSource.getMessage(CODE, null, LocaleContextHolder.getLocale()))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void getMessageWithCodeAndArgs() {
    when(messageSource.getMessage(CODE, ARGS, LocaleContextHolder.getLocale()))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE, ARGS);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void getMessageWithDefaultMessage() {
    when(messageSource.getMessage(CODE, null, DEFAULT_MESSAGE, LocaleContextHolder.getLocale()))
            .thenReturn(DEFAULT_MESSAGE);

    String result = accessor.getMessage(CODE, DEFAULT_MESSAGE);
    assertThat(result).isEqualTo(DEFAULT_MESSAGE);
  }

  @Test
  void getMessageWithLocale() {
    Locale locale = Locale.CHINESE;
    when(messageSource.getMessage(CODE, null, locale))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE, locale);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void getMessageWithResolvable() {
    MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
    when(messageSource.getMessage(resolvable, LocaleContextHolder.getLocale()))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(resolvable);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void throwsNoSuchMessageException() {
    when(messageSource.getMessage(CODE, null, LocaleContextHolder.getLocale()))
            .thenThrow(new NoSuchMessageException(CODE));

    assertThatThrownBy(() -> accessor.getMessage(CODE))
            .isInstanceOf(NoSuchMessageException.class)
            .hasMessageContaining(CODE);
  }

  @Test
  void constructorWithDefaultLocale() {
    Locale defaultLocale = Locale.GERMAN;
    accessor = new MessageSourceAccessor(messageSource, defaultLocale);

    when(messageSource.getMessage(CODE, null, defaultLocale))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void returnsEmptyStringForNullMessage() {
    when(messageSource.getMessage(CODE, null, DEFAULT_MESSAGE, LocaleContextHolder.getLocale()))
            .thenReturn(null);

    String result = accessor.getMessage(CODE, DEFAULT_MESSAGE);
    assertThat(result).isEmpty();
  }

  @Test
  void getMessageWithArgsAndDefaultMessage() {
    when(messageSource.getMessage(CODE, ARGS, DEFAULT_MESSAGE, LocaleContextHolder.getLocale()))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE, ARGS, DEFAULT_MESSAGE);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void getMessageWithArgsAndLocale() {
    Locale locale = Locale.FRENCH;
    when(messageSource.getMessage(CODE, ARGS, locale))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE, ARGS, locale);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void getMessageWithArgsAndDefaultMessageAndLocale() {
    Locale locale = Locale.ITALIAN;
    when(messageSource.getMessage(CODE, ARGS, DEFAULT_MESSAGE, locale))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE, ARGS, DEFAULT_MESSAGE, locale);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void getMessageWithResolvableAndLocale() {
    MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
    Locale locale = Locale.JAPANESE;
    when(messageSource.getMessage(resolvable, locale))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(resolvable, locale);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void defaultLocaleFallback() {
    Locale defaultLocale = Locale.GERMAN;
    accessor = new MessageSourceAccessor(messageSource, defaultLocale);
    LocaleContextHolder.setLocale(null);

    when(messageSource.getMessage(CODE, null, defaultLocale))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE);
    assertThat(result).isEqualTo(MESSAGE);

    LocaleContextHolder.resetLocaleContext(); // 清理测试状态
  }

  @Test
  void handleNullArguments() {
    when(messageSource.getMessage(CODE, null, LocaleContextHolder.getLocale()))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE, (Object[]) null);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void handleNullDefaultMessage() {
    when(messageSource.getMessage(CODE, ARGS, null, LocaleContextHolder.getLocale()))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(CODE, ARGS, (String) null);
    assertThat(result).isEqualTo(MESSAGE);
  }

  @Test
  void multipleCodesInResolvable() {
    MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
    when(resolvable.getCodes()).thenReturn(new String[] { "code1", "code2" });
    when(messageSource.getMessage(resolvable, LocaleContextHolder.getLocale()))
            .thenReturn(MESSAGE);

    String result = accessor.getMessage(resolvable);
    assertThat(result).isEqualTo(MESSAGE);
  }

}
