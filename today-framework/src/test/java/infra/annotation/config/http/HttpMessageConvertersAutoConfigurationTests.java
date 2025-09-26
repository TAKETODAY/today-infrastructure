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

package infra.annotation.config.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.annotation.config.gson.GsonAutoConfiguration;
import infra.annotation.config.jackson.JacksonAutoConfiguration;
import infra.annotation.config.jsonb.JsonbAutoConfiguration;
import infra.annotation.config.web.RandomPortWebServerConfig;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.app.test.context.FilteredClassLoader;
import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ContextConsumer;
import infra.app.test.context.runner.ReactiveWebApplicationContextRunner;
import infra.app.test.context.runner.WebApplicationContextRunner;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.support.GenericApplicationContext;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.GsonHttpMessageConverter;
import infra.http.converter.json.Jackson2ObjectMapperBuilder;
import infra.http.converter.json.JsonbHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import infra.web.server.EncodingProperties;
import jakarta.json.bind.Jsonb;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpMessageConvertersAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author David Liu
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 */
class HttpMessageConvertersAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class));

  @Test
  void jacksonNotAvailable() {
    this.contextRunner.run((context) -> {
      assertThat(context).doesNotHaveBean(ObjectMapper.class);
      assertThat(context).doesNotHaveBean(MappingJackson2HttpMessageConverter.class);
      assertThat(context).doesNotHaveBean(MappingJackson2XmlHttpMessageConverter.class);
    });
  }

  @Test
  void jacksonDefaultConverter() {
    this.contextRunner.withUserConfiguration(JacksonObjectMapperConfig.class)
            .run(assertConverter(MappingJackson2HttpMessageConverter.class, "mappingJackson2HttpMessageConverter"));
  }

  @Test
  void jacksonConverterWithBuilder() {
    this.contextRunner.withUserConfiguration(JacksonObjectMapperBuilderConfig.class)
            .run(assertConverter(MappingJackson2HttpMessageConverter.class, "mappingJackson2HttpMessageConverter"));
  }

  @Test
  void jacksonXmlConverterWithBuilder() {
    this.contextRunner.withUserConfiguration(JacksonObjectMapperBuilderConfig.class)
            .run(assertConverter(MappingJackson2XmlHttpMessageConverter.class,
                    "mappingJackson2XmlHttpMessageConverter"));
  }

  @Test
  void jacksonCustomConverter() {
    this.contextRunner.withUserConfiguration(JacksonObjectMapperConfig.class, JacksonConverterConfig.class)
            .run(assertConverter(MappingJackson2HttpMessageConverter.class, "customJacksonMessageConverter"));
  }

  @Test
  void gsonNotAvailable() {
    this.contextRunner.run((context) -> {
      assertThat(context).doesNotHaveBean(Gson.class);
      assertThat(context).doesNotHaveBean(GsonHttpMessageConverter.class);
    });
  }

  @Test
  void gsonDefaultConverter() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(GsonAutoConfiguration.class))
            .run(assertConverter(GsonHttpMessageConverter.class, "gsonHttpMessageConverter"));
  }

  @Test
  void gsonCustomConverter() {
    this.contextRunner.withUserConfiguration(GsonConverterConfig.class)
            .withConfiguration(AutoConfigurations.of(GsonAutoConfiguration.class))
            .run(assertConverter(GsonHttpMessageConverter.class, "customGsonMessageConverter"));
  }

  @Test
  void gsonCanBePreferred() {
    allOptionsRunner().withPropertyValues("web.mvc.converters.preferred-json-mapper:gson").run((context) -> {
      assertConverterBeanExists(context, GsonHttpMessageConverter.class, "gsonHttpMessageConverter");
      assertConverterBeanRegisteredWithHttpMessageConverters(context, GsonHttpMessageConverter.class);
      assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
      assertThat(context).doesNotHaveBean(MappingJackson2HttpMessageConverter.class);
    });
  }

  @Test
  void jsonbNotAvailable() {
    this.contextRunner.run((context) -> {
      assertThat(context).doesNotHaveBean(Jsonb.class);
      assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
    });
  }

  @Test
  void jsonbDefaultConverter() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class))
            .run(assertConverter(JsonbHttpMessageConverter.class, "jsonbHttpMessageConverter"));
  }

  @Test
  void jsonbCustomConverter() {
    this.contextRunner.withUserConfiguration(JsonbConverterConfig.class)
            .withConfiguration(AutoConfigurations.of(JsonbAutoConfiguration.class))
            .run(assertConverter(JsonbHttpMessageConverter.class, "customJsonbMessageConverter"));
  }

  @Test
  void jsonbCanBePreferred() {
    allOptionsRunner().withPropertyValues("web.mvc.converters.preferred-json-mapper:jsonb").run((context) -> {
      assertConverterBeanExists(context, JsonbHttpMessageConverter.class, "jsonbHttpMessageConverter");
      assertConverterBeanRegisteredWithHttpMessageConverters(context, JsonbHttpMessageConverter.class);
      assertThat(context).doesNotHaveBean(GsonHttpMessageConverter.class);
      assertThat(context).doesNotHaveBean(MappingJackson2HttpMessageConverter.class);
    });
  }

  @Test
  void stringDefaultConverter() {
    this.contextRunner.run(assertConverter(StringHttpMessageConverter.class, "stringHttpMessageConverter"));
  }

  @Test
  void stringCustomConverter() {
    this.contextRunner.withUserConfiguration(StringConverterConfig.class)
            .run(assertConverter(StringHttpMessageConverter.class, "customStringMessageConverter"));
  }

  @Test
  void typeConstrainedConverterDoesNotPreventAutoConfigurationOfJacksonConverter() {
    this.contextRunner.withUserConfiguration(JacksonObjectMapperBuilderConfig.class).run((context) -> {
      BeanDefinition beanDefinition = ((GenericApplicationContext) context.getSourceApplicationContext())
              .getBeanDefinition("mappingJackson2HttpMessageConverter");
      assertThat(beanDefinition.getFactoryBeanName())
              .isNull();
    });
  }

  @Test
  void typeConstrainedConverterDataDoesNotPreventAutoConfigurationOfJacksonConverter() {
    this.contextRunner
            .withUserConfiguration(JacksonObjectMapperBuilderConfig.class)
            .run((context) -> {
              BeanDefinition beanDefinition = ((GenericApplicationContext) context.getSourceApplicationContext())
                      .getBeanDefinition("mappingJackson2HttpMessageConverter");
              assertThat(beanDefinition.getFactoryBeanName())
                      .isNull();
            });
  }

  @Test
  void jacksonIsPreferredByDefault() {
    allOptionsRunner().run((context) -> {
      assertConverterBeanExists(context, MappingJackson2HttpMessageConverter.class,
              "mappingJackson2HttpMessageConverter");
      assertConverterBeanRegisteredWithHttpMessageConverters(context, MappingJackson2HttpMessageConverter.class);
      assertThat(context).doesNotHaveBean(GsonHttpMessageConverter.class);
      assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
    });
  }

  @Test
  void gsonIsPreferredIfJacksonIsNotAvailable() {
    allOptionsRunner().withClassLoader(new FilteredClassLoader(ObjectMapper.class.getPackage().getName()))
            .run((context) -> {
              assertConverterBeanExists(context, GsonHttpMessageConverter.class, "gsonHttpMessageConverter");
              assertConverterBeanRegisteredWithHttpMessageConverters(context, GsonHttpMessageConverter.class);
              assertThat(context).doesNotHaveBean(JsonbHttpMessageConverter.class);
            });
  }

  @Test
  void jsonbIsPreferredIfJacksonAndGsonAreNotAvailable() {
    allOptionsRunner()
            .withClassLoader(new FilteredClassLoader(ObjectMapper.class.getPackage().getName(),
                    Gson.class.getPackage().getName()))
            .run(assertConverter(JsonbHttpMessageConverter.class, "jsonbHttpMessageConverter"));
  }

  @Test
  void whenMockWebApplicationHttpMessageConvertersIsConfigured() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RandomPortWebServerConfig.class, HttpMessageConvertersAutoConfiguration.class))
            .run((context) -> assertThat(context).hasSingleBean(HttpMessageConverters.class));
  }

  @Test
  void whenReactiveWebApplicationHttpMessageConvertersIsNotConfigured() {
    new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .run((context) -> assertThat(context).doesNotHaveBean(HttpMessageConverters.class));
  }

  @Test
  void whenEncodingCharsetIsNotConfiguredThenStringMessageConverterUsesUtf8() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RandomPortWebServerConfig.class, HttpMessageConvertersAutoConfiguration.class))
            .run((context) -> {
              assertThat(context).hasSingleBean(StringHttpMessageConverter.class);
              assertThat(context.getBean(StringHttpMessageConverter.class).getDefaultCharset())
                      .isEqualTo(StandardCharsets.UTF_8);
            });
  }

  @Test
  void whenEncodingCharsetIsConfiguredThenStringMessageConverterUsesSpecificCharset() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RandomPortWebServerConfig.class, HttpMessageConvertersAutoConfiguration.class))
            .withPropertyValues("server.encoding.charset=UTF-16").run((context) -> {
              assertThat(context).hasSingleBean(StringHttpMessageConverter.class);
              assertThat(context.getBean(StringHttpMessageConverter.class).getDefaultCharset())
                      .isEqualTo(StandardCharsets.UTF_16);
            });
  }

  @Test
  @SuppressWarnings("removal")
  void shouldRegisterHints() {
    RuntimeHints hints = new RuntimeHints();
    new HttpMessageConvertersAutoConfiguration.Hints().registerHints(hints, getClass().getClassLoader());
    assertThat(RuntimeHintsPredicates.reflection().onType(EncodingProperties.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onMethod(EncodingProperties.class, "getCharset")).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onMethod(EncodingProperties.class, "setCharset")).accepts(hints);
  }

  private ApplicationContextRunner allOptionsRunner() {
    return this.contextRunner.withConfiguration(AutoConfigurations.of(GsonAutoConfiguration.class,
            JacksonAutoConfiguration.class, JsonbAutoConfiguration.class));
  }

  private ContextConsumer<AssertableApplicationContext> assertConverter(
          Class<? extends HttpMessageConverter<?>> converterType, String beanName) {
    return (context) -> {
      assertConverterBeanExists(context, converterType, beanName);
      assertConverterBeanRegisteredWithHttpMessageConverters(context, converterType);
    };
  }

  private void assertConverterBeanExists(AssertableApplicationContext context, Class<?> type, String beanName) {
    assertThat(context).hasSingleBean(type);
    assertThat(context).hasBean(beanName);
  }

  private void assertConverterBeanRegisteredWithHttpMessageConverters(AssertableApplicationContext context,
          Class<? extends HttpMessageConverter<?>> type) {
    HttpMessageConverter<?> converter = context.getBean(type);
    HttpMessageConverters converters = context.getBean(HttpMessageConverters.class);
    assertThat(converters.getConverters()).contains(converter);
  }

  @Configuration(proxyBeanMethods = false)
  static class JacksonObjectMapperConfig {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JacksonObjectMapperBuilderConfig {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    Jackson2ObjectMapperBuilder builder() {
      return new Jackson2ObjectMapperBuilder();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JacksonConverterConfig {

    @Bean
    MappingJackson2HttpMessageConverter customJacksonMessageConverter(ObjectMapper objectMapper) {
      MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
      converter.setObjectMapper(objectMapper);
      return converter;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class GsonConverterConfig {

    @Bean
    GsonHttpMessageConverter customGsonMessageConverter(Gson gson) {
      GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
      converter.setGson(gson);
      return converter;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JsonbConverterConfig {

    @Bean
    JsonbHttpMessageConverter customJsonbMessageConverter(Jsonb jsonb) {
      JsonbHttpMessageConverter converter = new JsonbHttpMessageConverter();
      converter.setJsonb(jsonb);
      return converter;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class StringConverterConfig {

    @Bean
    StringHttpMessageConverter customStringMessageConverter() {
      return new StringHttpMessageConverter();
    }

  }

}
