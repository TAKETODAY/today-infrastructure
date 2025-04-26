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

package infra.validation.beanvalidation;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import infra.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MessageSourceResourceBundleLocator}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/27 22:54
 */
class MessageSourceResourceBundleLocatorTests {

  @Test
  void constructorRequiresMessageSource() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MessageSourceResourceBundleLocator(null))
            .withMessage("MessageSource is required");
  }

  @Test
  void getResourceBundleWithDefaultLocale() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage("test.message", Locale.getDefault(), "test message");

    MessageSourceResourceBundleLocator locator = new MessageSourceResourceBundleLocator(messageSource);
    ResourceBundle bundle = locator.getResourceBundle(Locale.getDefault());

    assertThat(bundle).isNotNull();
    assertThat(bundle.getString("test.message")).isEqualTo("test message");
  }

  @Test
  void getResourceBundleWithChineseLocale() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage("test.message", Locale.CHINESE, "测试消息");

    MessageSourceResourceBundleLocator locator = new MessageSourceResourceBundleLocator(messageSource);
    ResourceBundle bundle = locator.getResourceBundle(Locale.CHINESE);

    assertThat(bundle).isNotNull();
    assertThat(bundle.getString("test.message")).isEqualTo("测试消息");
  }

  @Test
  void getResourceBundleWithMultipleMessages() {
    StaticMessageSource messageSource = new StaticMessageSource();
    messageSource.addMessage("message.one", Locale.ENGLISH, "First Message");
    messageSource.addMessage("message.two", Locale.ENGLISH, "Second Message");

    MessageSourceResourceBundleLocator locator = new MessageSourceResourceBundleLocator(messageSource);
    ResourceBundle bundle = locator.getResourceBundle(Locale.ENGLISH);

    assertThat(bundle).isNotNull();
    assertThat(bundle.getString("message.one")).isEqualTo("First Message");
    assertThat(bundle.getString("message.two")).isEqualTo("Second Message");
  }

  @Test
  void getResourceBundleWithNonExistentMessage() {
    StaticMessageSource messageSource = new StaticMessageSource();
    MessageSourceResourceBundleLocator locator = new MessageSourceResourceBundleLocator(messageSource);
    ResourceBundle bundle = locator.getResourceBundle(Locale.getDefault());

    assertThatExceptionOfType(java.util.MissingResourceException.class)
            .isThrownBy(() -> bundle.getString("nonexistent.message"));
  }
}
