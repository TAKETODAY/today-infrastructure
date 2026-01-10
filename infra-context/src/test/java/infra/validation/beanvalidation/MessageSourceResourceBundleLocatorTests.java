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
