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

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessageSourceSupportTests {

  static class TestMessageSourceSupport extends MessageSourceSupport {
    @Override
    public String renderDefaultMessage(String defaultMessage, Object[] args, Locale locale) {
      return super.renderDefaultMessage(defaultMessage, args, locale);
    }
  }

  private final TestMessageSourceSupport messageSource = new TestMessageSourceSupport();

  @Test
  void testAlwaysUseMessageFormat() {
    assertThat(messageSource.isAlwaysUseMessageFormat()).isFalse();
    messageSource.setAlwaysUseMessageFormat(true);
    assertThat(messageSource.isAlwaysUseMessageFormat()).isTrue();
  }

  @Test
  void testFormatMessageWithoutArgs() {
    String message = "Hello World";
    String result = messageSource.renderDefaultMessage(message, null, Locale.US);
    assertThat(result).isEqualTo(message);
  }

  @Test
  void testFormatMessageWithArgs() {
    String message = "Hello {0}!";
    Object[] args = new Object[] { "World" };
    String result = messageSource.renderDefaultMessage(message, args, Locale.US);
    assertThat(result).isEqualTo("Hello World!");
  }

  @Test
  void testFormatMessageWithMultipleArgs() {
    String message = "Hello {0}, today is {1}!";
    Object[] args = new Object[] { "World", "Monday" };
    String result = messageSource.renderDefaultMessage(message, args, Locale.US);
    assertThat(result).isEqualTo("Hello World, today is Monday!");
  }

  @Test
  void testFormatMessageWithDifferentLocales() {
    String message = "{0,number,#.##}";
    Object[] args = new Object[] { 3.14159 };

    String usResult = messageSource.renderDefaultMessage(message, args, Locale.US);
    String germanResult = messageSource.renderDefaultMessage(message, args, Locale.GERMAN);

    assertThat(usResult).isEqualTo("3.14");
    assertThat(germanResult).isEqualTo("3,14");
  }

  @Test
  void testFormatMessageWithEmptyArgs() {
    String message = "Hello World";
    String result = messageSource.renderDefaultMessage(message, new Object[0], Locale.US);
    assertThat(result).isEqualTo(message);
  }

  @Test
  void testFormatMessageWithInvalidPattern() {
    String message = "Hello {invalid}";
    messageSource.setAlwaysUseMessageFormat(true);

    assertThatThrownBy(() ->
            messageSource.renderDefaultMessage(message, null, Locale.US)
    ).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void testFormatMessageWithSingleQuotes() {
    String message = "It''s a nice day";
    messageSource.setAlwaysUseMessageFormat(true);
    String result = messageSource.renderDefaultMessage(message, null, Locale.US);
    assertThat(result).isEqualTo("It's a nice day");
  }

  @Test
  void testFormatMessageWithSpecialCharacters() {
    String message = "Special chars: !@#$%^&*()";
    String result = messageSource.renderDefaultMessage(message, null, Locale.US);
    assertThat(result).isEqualTo(message);
  }

  @Test
  void testCacheMessageFormat() {
    String message = "Hello {0}";
    Object[] args = new Object[] { "World" };

    // First call - creates and caches MessageFormat
    String result1 = messageSource.renderDefaultMessage(message, args, Locale.US);
    // Second call - should use cached MessageFormat
    String result2 = messageSource.renderDefaultMessage(message, args, Locale.US);

    assertThat(result1).isEqualTo(result2);
    assertThat(result1).isEqualTo("Hello World");
  }

  @Test
  void testNullMessageArguments() {
    String message = "Test {0} and {1}";
    Object[] args = new Object[] { null, "value" };
    String result = messageSource.renderDefaultMessage(message, args, Locale.US);
    assertThat(result).isEqualTo("Test null and value");
  }

  @Test
  void testMessageWithComplexFormatting() {
    String message = "Date: {0,date,short} | Number: {1,number,#.##}";
    Object[] args = new Object[] { new java.util.Date(0), 123.456 };
    String result = messageSource.renderDefaultMessage(message, args, Locale.US);
    assertThat(result).containsPattern("Date: \\d{1,2}/\\d{1,2}/\\d{2} | Number: 123.46");
  }

  @Test
  void testEmptyMessage() {
    String message = "";
    String result = messageSource.renderDefaultMessage(message, null, Locale.US);
    assertThat(result).isEmpty();
  }

  @Test
  void testMessageWithMissingArguments() {
    String message = "Hello {0}, {1}";
    Object[] args = new Object[] { "World" }; // Missing second argument
    String result = messageSource.renderDefaultMessage(message, args, Locale.US);
    assertThat(result).isEqualTo("Hello World, {1}");
  }

  @Test
  void testWithDifferentLocalesForSameMessage() {
    String message = "{0,date}";
    Object[] args = new Object[] { new java.util.Date(0) };

    String usResult = messageSource.renderDefaultMessage(message, args, Locale.US);
    String ukResult = messageSource.renderDefaultMessage(message, args, Locale.UK);
    String germanResult = messageSource.renderDefaultMessage(message, args, Locale.GERMAN);

    assertThat(usResult).isNotEqualTo(germanResult);
    assertThat(usResult).isNotEqualTo(ukResult);
  }

  @Test
  void testInvalidFormatWithoutAlwaysUseMessageFormat() {
    String message = "Test {invalid}";
    messageSource.setAlwaysUseMessageFormat(false);
    String result = messageSource.renderDefaultMessage(message, null, Locale.US);
    assertThat(result).isEqualTo(message);
  }

}
