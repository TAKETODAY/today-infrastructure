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

package infra.annotation.config.context;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ContextConsumer;
import infra.context.MessageSource;
import infra.context.MessageSourceResolvable;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.PropertySource;
import infra.context.annotation.config.AutoConfigurations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MessageSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Kedar Joshi
 */
class MessageSourceAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(MessageSourceAutoConfiguration.class));

  @Test
  void testDefaultMessageSource() {
    this.contextRunner.run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
            .isEqualTo("Foo message"));
  }

  @Test
  void propertiesBundleWithSlashIsDetected() {
    this.contextRunner.withPropertyValues("infra.messages.basename:test/messages").run((context) -> {
      assertThat(context).hasSingleBean(MessageSource.class);
      assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
    });
  }

  @Test
  void propertiesBundleWithDotIsDetected() {
    this.contextRunner.withPropertyValues("infra.messages.basename:test.messages").run((context) -> {
      assertThat(context).hasSingleBean(MessageSource.class);
      assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
    });
  }

  @Test
  void testEncodingWorks() {
    this.contextRunner.withPropertyValues("infra.messages.basename:test/swedish")
            .run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
                    .isEqualTo("Some text with some swedish öäå!"));
  }

  @Test
  void testCacheDurationNoUnit() {
    this.contextRunner
            .withPropertyValues("infra.messages.basename:test/messages", "infra.messages.cache-duration=10")
            .run(assertCache(10 * 1000));
  }

  @Test
  void testCacheDurationWithUnit() {
    this.contextRunner
            .withPropertyValues("infra.messages.basename:test/messages", "infra.messages.cache-duration=1m")
            .run(assertCache(60 * 1000));
  }

  private ContextConsumer<AssertableApplicationContext> assertCache(long expected) {
    return (context) -> {
      assertThat(context).hasSingleBean(MessageSource.class);
      assertThat(context.getBean(MessageSource.class)).hasFieldOrPropertyWithValue("cacheMillis", expected);
    };
  }

  @Test
  void testMultipleMessageSourceCreated() {
    this.contextRunner.withPropertyValues("infra.messages.basename:test/messages,test/messages2")
            .run((context) -> {
              assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
              assertThat(context.getMessage("foo-foo", null, "Foo-Foo message", Locale.UK)).isEqualTo("bar-bar");
            });
  }

  @Test
  @Disabled("Expected to fail per gh-1075")
  void testMessageSourceFromPropertySourceAnnotation() {
    this.contextRunner.withUserConfiguration(Config.class)
            .run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar"));
  }

  @Test
  void testCommonMessages() {
    this.contextRunner.withPropertyValues("infra.messages.basename=test/messages",
                    "infra.messages.common-messages=classpath:test/common-messages.properties")
            .run((context) -> assertThat(context.getMessage("hello", null, "Hello!", Locale.UK)).isEqualTo("world"));
  }

  @Test
  void testFallbackDefault() {
    this.contextRunner.withPropertyValues("infra.messages.basename:test/messages")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("fallbackToSystemLocale", true));
  }

  @Test
  void testFallbackTurnOff() {
    this.contextRunner
            .withPropertyValues("infra.messages.basename:test/messages",
                    "infra.messages.fallback-to-system-locale:false")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("fallbackToSystemLocale", false));
  }

  @Test
  void testFormatMessageDefault() {
    this.contextRunner.withPropertyValues("infra.messages.basename:test/messages")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("alwaysUseMessageFormat", false));
  }

  @Test
  void testFormatMessageOn() {
    this.contextRunner
            .withPropertyValues("infra.messages.basename:test/messages",
                    "infra.messages.always-use-message-format:true")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("alwaysUseMessageFormat", true));
  }

  @Test
  void testUseCodeAsDefaultMessageDefault() {
    this.contextRunner.withPropertyValues("infra.messages.basename:test/messages")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("useCodeAsDefaultMessage", false));
  }

  @Test
  void testUseCodeAsDefaultMessageOn() {
    this.contextRunner
            .withPropertyValues("infra.messages.basename:test/messages",
                    "infra.messages.use-code-as-default-message:true")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("useCodeAsDefaultMessage", true));
  }

  @Test
  void existingMessageSourceIsPreferred() {
    this.contextRunner.withUserConfiguration(CustomMessageSourceConfiguration.class)
            .run((context) -> assertThat(context.getMessage("foo", null, null, null)).isEqualTo("foo"));
  }

  @Test
  void existingMessageSourceInParentIsIgnored() {
    this.contextRunner.run((parent) -> this.contextRunner.withParent(parent)
            .withPropertyValues("infra.messages.basename:test/messages")
            .run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
                    .isEqualTo("bar")));
  }

  @Test
  void messageSourceWithNonStandardBeanNameIsIgnored() {
    this.contextRunner.withPropertyValues("infra.messages.basename:test/messages")
            .withUserConfiguration(CustomBeanNameMessageSourceConfiguration.class)
            .run((context) -> assertThat(context.getMessage("foo", null, Locale.US)).isEqualTo("bar"));
  }

  @Test
  void shouldRegisterDefaultHints() {
    RuntimeHints hints = new RuntimeHints();
    new MessageSourceAutoConfiguration.Hints().registerHints(hints, getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.resource().forResource("messages.properties")).accepts(hints);
    assertThat(RuntimeHintsPredicates.resource().forResource("messages_de.properties")).accepts(hints);
    assertThat(RuntimeHintsPredicates.resource().forResource("messages_zh-CN.properties")).accepts(hints);
  }

  @Configuration(proxyBeanMethods = false)
  @PropertySource("classpath:/switch-messages.properties")
  static class Config {

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomMessageSourceConfiguration {

    @Bean
    MessageSource messageSource() {
      return new TestMessageSource();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomBeanNameMessageSourceConfiguration {

    @Bean
    MessageSource codeReturningMessageSource() {
      return new TestMessageSource();
    }

  }

  static class TestMessageSource implements MessageSource {

    @Override
    public String getMessage(String code, Object @Nullable [] args, String defaultMessage, Locale locale) {
      return code;
    }

    @Override
    public String getMessage(String code, Object @Nullable [] args, Locale locale) {
      return code;
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) {
      return resolvable.getCodes()[0];
    }

  }

}
