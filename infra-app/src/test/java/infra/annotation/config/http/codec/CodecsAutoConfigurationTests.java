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

package infra.annotation.config.http.codec;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import infra.annotation.config.http.CodecProperties;
import infra.annotation.config.http.CodecsAutoConfiguration;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.http.codec.CodecConfigurer;
import infra.http.codec.CodecCustomizer;
import infra.http.codec.support.DefaultClientCodecConfigurer;
import infra.util.ReflectionUtils;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CodecsAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class CodecsAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(CodecsAutoConfiguration.class));

  @Test
  void autoConfigShouldProvideALoggingRequestDetailsCustomizer() {
    this.contextRunner.run((context) -> {
      CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
      CodecConfigurer configurer = new DefaultClientCodecConfigurer();
      customizer.customize(configurer);
      assertThat(configurer.defaultCodecs()).hasFieldOrPropertyWithValue("enableLoggingRequestDetails", false);
    });

  }

  @Test
  void loggingRequestDetailsCustomizerShouldUseHttpProperties() {
    this.contextRunner.withPropertyValues("http.codec.log-request-details=true").run((context) -> {
      CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
      CodecConfigurer configurer = new DefaultClientCodecConfigurer();
      customizer.customize(configurer);
      assertThat(configurer.defaultCodecs()).hasFieldOrPropertyWithValue("enableLoggingRequestDetails", true);
    });
  }

  @Test
  void defaultCodecCustomizerBeanShouldHaveOrderZero() {
    this.contextRunner.run((context) -> {
      Method customizerMethod = ReflectionUtils.findMethod(
              CodecsAutoConfiguration.class, "defaultCodecCustomizer",
              CodecProperties.class);
      Integer order = new TestAnnotationAwareOrderComparator().findOrder(customizerMethod);
      assertThat(order).isEqualTo(0);
    });
  }

  @Test
  void jacksonCodecCustomizerBacksOffWhenThereIsNoObjectMapper() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("jacksonCodecCustomizer"));
  }

  @Test
  void jacksonCodecCustomizerIsAutoConfiguredWhenObjectMapperIsPresent() {
    this.contextRunner.withUserConfiguration(ObjectMapperConfiguration.class)
            .run((context) -> assertThat(context).hasBean("jacksonCodecCustomizer"));
  }

  @Test
  void userProvidedCustomizerCanOverrideJacksonCodecCustomizer() {
    this.contextRunner.withUserConfiguration(ObjectMapperConfiguration.class, CodecCustomizerConfiguration.class)
            .run((context) -> {
              List<CodecCustomizer> codecCustomizers = context.getBean(CodecCustomizers.class).codecCustomizers;
              assertThat(codecCustomizers).hasSize(3);
              assertThat(codecCustomizers.get(2)).isInstanceOf(TestCodecCustomizer.class);
            });
  }

  @Test
  void maxInMemorySizeEnforcedInDefaultCodecs() {
    this.contextRunner.withPropertyValues("http.codec.max-in-memory-size=1MB").run((context) -> {
      CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
      CodecConfigurer configurer = new DefaultClientCodecConfigurer();
      customizer.customize(configurer);
      assertThat(configurer.defaultCodecs()).hasFieldOrPropertyWithValue("maxInMemorySize", 1048576);
    });
  }

  static class TestAnnotationAwareOrderComparator extends AnnotationAwareOrderComparator {

    @Override
    public Integer findOrder(Object obj) {
      return super.findOrder(obj);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ObjectMapperConfiguration {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CodecCustomizerConfiguration {

    @Bean
    CodecCustomizer codecCustomizer() {
      return new TestCodecCustomizer();
    }

    @Bean
    CodecCustomizers codecCustomizers(List<CodecCustomizer> customizers) {
      return new CodecCustomizers(customizers);
    }

  }

  private static final class TestCodecCustomizer implements CodecCustomizer {

    @Override
    public void customize(CodecConfigurer configurer) {
    }

  }

  private static final class CodecCustomizers {

    private final List<CodecCustomizer> codecCustomizers;

    private CodecCustomizers(List<CodecCustomizer> codecCustomizers) {
      this.codecCustomizers = codecCustomizers;
    }

  }

}
