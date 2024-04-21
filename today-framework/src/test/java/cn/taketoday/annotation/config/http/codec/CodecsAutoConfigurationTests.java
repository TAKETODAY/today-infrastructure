/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.annotation.config.http.codec;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.annotation.config.http.CodecProperties;
import cn.taketoday.annotation.config.http.CodecsAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.http.codec.CodecConfigurer;
import cn.taketoday.http.codec.CodecCustomizer;
import cn.taketoday.http.codec.support.DefaultClientCodecConfigurer;
import cn.taketoday.util.ReflectionUtils;

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
