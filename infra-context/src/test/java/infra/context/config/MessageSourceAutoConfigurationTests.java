/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.config;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.context.MessageSource;
import infra.context.MessageSourceResolvable;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.PropertySource;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.support.ReloadableResourceBundleMessageSource;
import infra.context.support.ResourceBundleMessageSource;
import infra.test.context.assertj.AssertableApplicationContext;
import infra.test.context.runner.ApplicationContextRunner;
import infra.test.context.runner.ContextConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link infra.context.config.MessageSourceAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Kedar Joshi
 * @author Henrique (henriquejsza)
 */
class MessageSourceAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(infra.context.config.MessageSourceAutoConfiguration.class));

  @Test
  void testDefaultMessageSource() {
    this.contextRunner.run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
            .isEqualTo("Foo message"));
  }

  @Test
  void resourceBasedMessageSourceConfigurerIsAvailableWithoutResourceBundle() {
    this.contextRunner
            .run((context) -> assertThat(context).hasSingleBean(ResourceBasedMessageSourceConfigurer.class)
                    .doesNotHaveBean(ResourceBundleMessageSource.class));
  }

  @Test
  void resourceBasedMessageSourceConfigurerCanConfigureUserDefinedMessageSource() {
    this.contextRunner.withUserConfiguration(CustomResourceBasedMessageSourceConfiguration.class)
            .withPropertyValues("messages.cache-duration=10s",
                    "messages.fallback-to-system-locale=false",
                    "messages.always-use-message-format=true",
                    "messages.use-code-as-default-message=true")
            .run((context) -> {
              assertThat(context).hasSingleBean(ResourceBasedMessageSourceConfigurer.class)
                      .hasSingleBean(ReloadableResourceBundleMessageSource.class);
              assertThat(context.getBean(ReloadableResourceBundleMessageSource.class))
                      .hasFieldOrPropertyWithValue("cacheMillis", 10_000L)
                      .hasFieldOrPropertyWithValue("fallbackToSystemLocale", false)
                      .hasFieldOrPropertyWithValue("alwaysUseMessageFormat", true)
                      .hasFieldOrPropertyWithValue("useCodeAsDefaultMessage", true);
            });
  }

  @Test
  void autoConfiguredMessageSourceUsesUserDefinedConfigurer() {
    ResourceBasedMessageSourceConfigurer configurer = mock(ResourceBasedMessageSourceConfigurer.class);
    this.contextRunner.withBean(ResourceBasedMessageSourceConfigurer.class, () -> configurer)
            .withPropertyValues("messages.basename=test/messages")
            .run((context) -> {
              assertThat(context).hasSingleBean(ResourceBasedMessageSourceConfigurer.class)
                      .hasSingleBean(ResourceBundleMessageSource.class);
              assertThat(context.getBean(ResourceBasedMessageSourceConfigurer.class)).isSameAs(configurer);
              then(configurer).should().configure(context.getBean(ResourceBundleMessageSource.class));
            });
  }

  @Test
  void propertiesBundleWithSlashIsDetected() {
    this.contextRunner.withPropertyValues("messages.basename:test/messages").run((context) -> {
      assertThat(context).hasSingleBean(MessageSource.class);
      assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
    });
  }

  @Test
  void propertiesBundleWithDotIsDetected() {
    this.contextRunner.withPropertyValues("messages.basename:test.messages").run((context) -> {
      assertThat(context).hasSingleBean(MessageSource.class);
      assertThat(context.getMessage("foo", null, "Foo message", Locale.UK)).isEqualTo("bar");
    });
  }

  @Test
  void testEncodingWorks() {
    this.contextRunner.withPropertyValues("messages.basename:test/swedish")
            .run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
                    .isEqualTo("Some text with some swedish öäå!"));
  }

  @Test
  void testCacheDurationNoUnit() {
    this.contextRunner
            .withPropertyValues("messages.basename:test/messages", "messages.cache-duration=10")
            .run(assertCache(10 * 1000));
  }

  @Test
  void testCacheDurationWithUnit() {
    this.contextRunner
            .withPropertyValues("messages.basename:test/messages", "messages.cache-duration=1m")
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
    this.contextRunner.withPropertyValues("messages.basename:test/messages,test/messages2")
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
    this.contextRunner.withPropertyValues("messages.basename=test/messages",
                    "messages.common-messages=classpath:test/common-messages.properties")
            .run((context) -> assertThat(context.getMessage("hello", null, "Hello!", Locale.UK)).isEqualTo("world"));
  }

  @Test
  void testFallbackDefault() {
    this.contextRunner.withPropertyValues("messages.basename:test/messages")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("fallbackToSystemLocale", true));
  }

  @Test
  void testFallbackTurnOff() {
    this.contextRunner
            .withPropertyValues("messages.basename:test/messages",
                    "messages.fallback-to-system-locale:false")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("fallbackToSystemLocale", false));
  }

  @Test
  void testFormatMessageDefault() {
    this.contextRunner.withPropertyValues("messages.basename:test/messages")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("alwaysUseMessageFormat", false));
  }

  @Test
  void testFormatMessageOn() {
    this.contextRunner
            .withPropertyValues("messages.basename:test/messages",
                    "messages.always-use-message-format:true")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("alwaysUseMessageFormat", true));
  }

  @Test
  void testUseCodeAsDefaultMessageDefault() {
    this.contextRunner.withPropertyValues("messages.basename:test/messages")
            .run((context) -> assertThat(context.getBean(MessageSource.class))
                    .hasFieldOrPropertyWithValue("useCodeAsDefaultMessage", false));
  }

  @Test
  void testUseCodeAsDefaultMessageOn() {
    this.contextRunner
            .withPropertyValues("messages.basename:test/messages",
                    "messages.use-code-as-default-message:true")
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
            .withPropertyValues("messages.basename:test/messages")
            .run((context) -> assertThat(context.getMessage("foo", null, "Foo message", Locale.UK))
                    .isEqualTo("bar")));
  }

  @Test
  void messageSourceWithNonStandardBeanNameIsIgnored() {
    this.contextRunner.withPropertyValues("messages.basename:test/messages")
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
  static class CustomResourceBasedMessageSourceConfiguration {

    @Bean
    ReloadableResourceBundleMessageSource messageSource(ResourceBasedMessageSourceConfigurer configurer) {
      ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
      configurer.configure(messageSource);
      return messageSource;
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
