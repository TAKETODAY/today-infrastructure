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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Locale;

import infra.context.MessageSource;
import infra.context.MessageSourceResolvable;
import infra.context.NoSuchMessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DelegatingMessageSourceTests {

  private DelegatingMessageSource messageSource;

  @Mock
  private MessageSource parentMessageSource;

  private static final String CODE = "test.code";
  private static final String DEFAULT_MESSAGE = "default";
  private static final Object[] ARGS = new Object[] { "arg" };
  private static final Locale LOCALE = Locale.ENGLISH;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    messageSource = new DelegatingMessageSource();
  }

  @Test
  void getMessageWithoutParentAndDefaultMessage() {
    String result = messageSource.getMessage(CODE, ARGS, DEFAULT_MESSAGE, LOCALE);
    assertThat(result).isEqualTo(DEFAULT_MESSAGE);
  }

  @Test
  void getMessageWithoutParentAndNoDefaultMessage() {
    String result = messageSource.getMessage(CODE, ARGS, null, LOCALE);
    assertThat(result).isNull();
  }

  @Test
  void getMessageWithoutParentShouldThrowException() {
    assertThatThrownBy(() -> messageSource.getMessage(CODE, ARGS, LOCALE))
            .isInstanceOf(NoSuchMessageException.class)
            .hasMessageContaining(CODE);
  }

  @Test
  void getMessageWithParent() {
    messageSource.setParentMessageSource(parentMessageSource);
    when(parentMessageSource.getMessage(CODE, ARGS, LOCALE))
            .thenReturn("parent message");

    String result = messageSource.getMessage(CODE, ARGS, LOCALE);
    assertThat(result).isEqualTo("parent message");
    verify(parentMessageSource).getMessage(CODE, ARGS, LOCALE);
  }

  @Test
  void setAndGetParentMessageSource() {
    assertThat(messageSource.getParentMessageSource()).isNull();

    messageSource.setParentMessageSource(parentMessageSource);
    assertThat(messageSource.getParentMessageSource()).isSameAs(parentMessageSource);
  }

  @Test
  void getMessageWithResolvableAndParent() {
    messageSource.setParentMessageSource(parentMessageSource);
    MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
    when(parentMessageSource.getMessage(resolvable, LOCALE))
            .thenReturn("resolved message");

    String result = messageSource.getMessage(resolvable, LOCALE);
    assertThat(result).isEqualTo("resolved message");
    verify(parentMessageSource).getMessage(resolvable, LOCALE);
  }

  @Test
  void getMessageWithResolvableNoParentShouldUseDefault() {
    MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
    when(resolvable.getDefaultMessage()).thenReturn(DEFAULT_MESSAGE);
    when(resolvable.getArguments()).thenReturn(ARGS);

    String result = messageSource.getMessage(resolvable, LOCALE);
    assertThat(result).isEqualTo(DEFAULT_MESSAGE);
  }

  @Test
  void getMessageWithResolvableNoParentNoDefaultShouldThrow() {
    MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
    when(resolvable.getCodes()).thenReturn(new String[] { CODE });
    when(resolvable.getDefaultMessage()).thenReturn(null);

    assertThatThrownBy(() -> messageSource.getMessage(resolvable, LOCALE))
            .isInstanceOf(NoSuchMessageException.class)
            .hasMessageContaining(CODE);
  }

  @Test
  void toStringWithoutParent() {
    assertThat(messageSource.toString()).isEqualTo("Empty MessageSource");
  }

  @Test
  void toStringWithParent() {
    messageSource.setParentMessageSource(parentMessageSource);
    when(parentMessageSource.toString()).thenReturn("Parent MessageSource");

    assertThat(messageSource.toString()).isEqualTo("Parent MessageSource");
  }

  @Test
  void getMessageWithEmptyArgs() {
    messageSource.setParentMessageSource(parentMessageSource);
    Object[] emptyArgs = new Object[0];
    when(parentMessageSource.getMessage(CODE, emptyArgs, LOCALE))
            .thenReturn("message without args");

    String result = messageSource.getMessage(CODE, emptyArgs, LOCALE);
    assertThat(result).isEqualTo("message without args");
    verify(parentMessageSource).getMessage(CODE, emptyArgs, LOCALE);
  }

  @Test
  void getMessageWithSpecialLocale() {
    messageSource.setParentMessageSource(parentMessageSource);
    Locale specialLocale = new Locale("zh", "CN");
    when(parentMessageSource.getMessage(CODE, ARGS, specialLocale))
            .thenReturn("中文消息");

    String result = messageSource.getMessage(CODE, ARGS, specialLocale);
    assertThat(result).isEqualTo("中文消息");
    verify(parentMessageSource).getMessage(CODE, ARGS, specialLocale);
  }

  @Test
  void getMessageWithNestedResolvable() {
    messageSource.setParentMessageSource(parentMessageSource);
    MessageSourceResolvable nestedResolvable = mock(MessageSourceResolvable.class);
    MessageSourceResolvable parentResolvable = mock(MessageSourceResolvable.class);

    when(parentMessageSource.getMessage(nestedResolvable, LOCALE))
            .thenReturn("nested message");
    when(parentResolvable.getArguments())
            .thenReturn(new Object[] { nestedResolvable });
    when(parentMessageSource.getMessage(parentResolvable, LOCALE))
            .thenReturn("parent with nested: nested message");

    String result = messageSource.getMessage(parentResolvable, LOCALE);
    assertThat(result).isEqualTo("parent with nested: nested message");
    verify(parentMessageSource).getMessage(parentResolvable, LOCALE);
  }

  @Test
  void getMessageWithEscapeCharacters() {
    messageSource.setParentMessageSource(parentMessageSource);
    String messageWithEscape = "Message with escape: \n\t\"quoted\"";
    when(parentMessageSource.getMessage(CODE, ARGS, LOCALE))
            .thenReturn(messageWithEscape);

    String result = messageSource.getMessage(CODE, ARGS, LOCALE);
    assertThat(result).isEqualTo(messageWithEscape);
    verify(parentMessageSource).getMessage(CODE, ARGS, LOCALE);
  }

  @Test
  void getMessageWithMultipleResolvableCodes() {
    MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
    String[] codes = new String[] { "code1", "code2", "code3" };
    when(resolvable.getCodes()).thenReturn(codes);
    when(resolvable.getDefaultMessage()).thenReturn(null);

    messageSource.setParentMessageSource(parentMessageSource);
    when(parentMessageSource.getMessage(resolvable, LOCALE))
            .thenReturn("resolved from multiple codes");

    String result = messageSource.getMessage(resolvable, LOCALE);
    assertThat(result).isEqualTo("resolved from multiple codes");
    verify(parentMessageSource).getMessage(resolvable, LOCALE);
  }

}
