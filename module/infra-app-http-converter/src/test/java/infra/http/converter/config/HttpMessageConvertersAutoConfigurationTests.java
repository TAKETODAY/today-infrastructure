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

package infra.http.converter.config;

import com.google.gson.Gson;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import infra.app.test.context.FilteredClassLoader;
import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ReactiveWebApplicationContextRunner;
import infra.app.test.context.runner.WebApplicationContextRunner;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.MediaType;
import infra.http.converter.AbstractHttpMessageConverter;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.HttpMessageConverters;
import infra.http.converter.HttpMessageConverters.ClientBuilder;
import infra.http.converter.HttpMessageConverters.ServerBuilder;
import infra.http.converter.HttpMessageNotReadableException;
import infra.http.converter.HttpMessageNotWritableException;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.config.GsonHttpMessageConvertersConfiguration.GsonHttpConvertersCustomizer;
import infra.http.converter.config.JacksonHttpMessageConvertersConfiguration.JacksonJsonHttpMessageConvertersCustomizer;
import infra.http.converter.config.JacksonHttpMessageConvertersConfiguration.JacksonXmlHttpMessageConvertersCustomizer;
import infra.http.converter.config.JsonbHttpMessageConvertersConfiguration.JsonbHttpMessageConvertersCustomizer;
import infra.http.converter.json.GsonHttpMessageConverter;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.http.converter.json.JsonbHttpMessageConverter;
import infra.http.converter.xml.JacksonXmlHttpMessageConverter;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

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
    this.contextRunner.withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName()))
            .run((context) -> {
              assertThat(context).doesNotHaveBean(JsonMapper.class);
              assertThat(context).doesNotHaveBean(JacksonJsonHttpMessageConvertersCustomizer.class);
              assertThat(context).doesNotHaveBean(JacksonXmlHttpMessageConvertersCustomizer.class);
            });
  }

  @Test
  void jacksonDefaultConverter() {
    this.contextRunner.withUserConfiguration(JacksonJsonMapperConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(JacksonJsonHttpMessageConvertersCustomizer.class);
      assertConverterIsRegistered(context, JacksonJsonHttpMessageConverter.class);
    });
  }

  @Test
  void jacksonServerCustomizer() {
    this.contextRunner.withUserConfiguration(CustomJsonConverterConfig.class).run((context) -> {
      assertConverterIsNotRegistered(context, JacksonJsonHttpMessageConverter.class);
      assertConverterIsRegistered(context, CustomConverter.class);
    });
  }

  @Test
  void jacksonConverterWithBuilder() {
    this.contextRunner.withUserConfiguration(JacksonJsonMapperBuilderConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(JacksonJsonHttpMessageConvertersCustomizer.class);
      assertConverterIsRegistered(context, JacksonJsonHttpMessageConverter.class);
    });
  }

  @Test
  void jacksonXmlConverterWithBuilder() {
    this.contextRunner.withUserConfiguration(JacksonXmlMapperBuilderConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(JacksonXmlHttpMessageConvertersCustomizer.class);
      assertConverterIsRegistered(context, JacksonXmlHttpMessageConverter.class);
    });
  }

  @Test
  void jacksonCustomConverter() {
    this.contextRunner.withUserConfiguration(JacksonJsonMapperConfig.class, JacksonConverterConfig.class)
            .run((context) -> {
              assertThat(context).doesNotHaveBean(JacksonJsonHttpMessageConvertersCustomizer.class);
              HttpMessageConverters serverConverters = getServerConverters(context);
              assertThat(serverConverters)
                      .contains(context.getBean("customJacksonMessageConverter", JacksonJsonHttpMessageConverter.class));
            });
  }

  @Test
  void jacksonServerAndClientConvertersShouldBeDifferent() {
    this.contextRunner.withUserConfiguration(JacksonJsonMapperConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(JacksonJsonHttpMessageConvertersCustomizer.class);
      JacksonJsonHttpMessageConverter serverConverter = findConverter(getServerConverters(context),
              JacksonJsonHttpMessageConverter.class);
      JacksonJsonHttpMessageConverter clientConverter = findConverter(getClientConverters(context),
              JacksonJsonHttpMessageConverter.class);
      assertThat(serverConverter).isNotEqualTo(clientConverter);
    });
  }

  @Test
  void gsonNotAvailable() {
    this.contextRunner.run((context) -> {
      assertThat(context).doesNotHaveBean(Gson.class);
      assertConverterIsNotRegistered(context, GsonHttpMessageConverter.class);
    });
  }

  @Test
  void gsonDefaultConverter() {
    this.contextRunner.withBean(Gson.class)
            .run((context) -> assertConverterIsRegistered(context, GsonHttpMessageConverter.class));
  }

  @Test
  void gsonCustomConverter() {
    this.contextRunner.withUserConfiguration(GsonConverterConfig.class)
            .withBean(Gson.class)
            .run((context) -> assertThat(getServerConverters(context))
                    .contains(context.getBean("customGsonMessageConverter", GsonHttpMessageConverter.class)));
  }

  @Test
  void gsonCanBePreferred() {
    allOptionsRunner().withPropertyValues("http.converters.preferred-json-mapper:gson").run((context) -> {
      assertConverterIsRegistered(context, GsonHttpMessageConverter.class);
      assertConverterIsNotRegistered(context, JsonbHttpMessageConverter.class);
      assertConverterIsNotRegistered(context, JacksonJsonHttpMessageConverter.class);
    });
  }

  @Test
  void jsonbNotAvailable() {
    this.contextRunner.run((context) -> {
      assertThat(context).doesNotHaveBean(Jsonb.class);
      assertConverterIsNotRegistered(context, JsonbHttpMessageConverter.class);
    });
  }

  @Test
  void jsonbDefaultConverter() {
    this.contextRunner.withBean(Jsonb.class, JsonbBuilder::create)
            .run((context) -> assertConverterIsRegistered(context, JsonbHttpMessageConverter.class));
  }

  @Test
  void jsonbCustomConverter() {
    this.contextRunner.withUserConfiguration(JsonbConverterConfig.class)
            .withBean(Jsonb.class, JsonbBuilder::create)
            .run((context) -> assertThat(getServerConverters(context))
                    .contains(context.getBean("customJsonbMessageConverter", JsonbHttpMessageConverter.class)));
  }

  @Test
  void jsonbCanBePreferred() {
    allOptionsRunner().withPropertyValues("http.converters.preferred-json-mapper:jsonb").run((context) -> {
      assertConverterIsRegistered(context, JsonbHttpMessageConverter.class);
      assertConverterIsNotRegistered(context, GsonHttpMessageConverter.class);
      assertConverterIsNotRegistered(context, JacksonJsonHttpMessageConverter.class);
    });
  }

  @Test
  void stringDefaultConverter() {
    this.contextRunner.run((context) -> assertConverterIsRegistered(context, StringHttpMessageConverter.class));
  }

  @Test
  void stringCustomConverter() {
    this.contextRunner.withUserConfiguration(StringConverterConfig.class).run((context) -> {
      assertThat(getClientConverters(context))
              .filteredOn((converter) -> converter instanceof StringHttpMessageConverter)
              .hasSize(2);
      assertThat(getServerConverters(context))
              .filteredOn((converter) -> converter instanceof StringHttpMessageConverter)
              .hasSize(2);
    });
  }

  @Test
  void jacksonIsPreferredByDefault() {
    allOptionsRunner().run((context) -> {
      assertBeanExists(context, JacksonJsonHttpMessageConvertersCustomizer.class,
              "jacksonJsonHttpMessageConvertersCustomizer");
      assertConverterIsRegistered(context, JacksonJsonHttpMessageConverter.class);
      assertThat(context).doesNotHaveBean(GsonHttpConvertersCustomizer.class);
      assertThat(context).doesNotHaveBean(JsonbHttpMessageConvertersCustomizer.class);
    });
  }

  @Test
  void gsonIsPreferredIfJacksonAndJackson2AreNotAvailable() {
    allOptionsRunner()
            .withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName(),
                    ObjectMapper.class.getPackage().getName()))
            .run((context) -> {
              assertConverterIsRegistered(context, GsonHttpMessageConverter.class);
              assertConverterIsNotRegistered(context, JsonbHttpMessageConverter.class);
            });
  }

  @Test
  void jsonbIsPreferredIfJacksonAndGsonAreNotAvailable() {
    allOptionsRunner()
            .withClassLoader(new FilteredClassLoader(JsonMapper.class.getPackage().getName(),
                    ObjectMapper.class.getPackage().getName(), Gson.class.getPackage().getName()))
            .run((context) -> assertConverterIsRegistered(context, JsonbHttpMessageConverter.class));
  }

  @Test
  void whenWebApplicationHttpMessageConvertersIsConfigured() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .run((context) -> assertThat(context).hasSingleBean(DefaultHttpMessageConvertersCustomizer.class)
                    .hasSingleBean(DefaultHttpMessageConvertersCustomizer.class));
  }

  @Test
  void whenReactiveWebApplicationHttpMessageConvertersIsNotConfigured() {
    new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .run((context) -> assertThat(context).doesNotHaveBean(ServerHttpMessageConvertersCustomizer.class)
                    .doesNotHaveBean(ClientHttpMessageConvertersCustomizer.class));
  }

  @Test
  void whenEncodingCharsetIsNotConfiguredThenStringMessageConverterUsesUtf8() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .run((context) -> {
              StringHttpMessageConverter converter = findConverter(getServerConverters(context),
                      StringHttpMessageConverter.class);
              assertThat(converter.getDefaultCharset()).isEqualTo(StandardCharsets.UTF_8);
            });
  }

  @Test
  void whenEncodingCharsetIsConfiguredThenStringMessageConverterUsesSpecificCharset() {
    new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .withPropertyValues("http.converters.string-encoding-charset=UTF-16")
            .run((context) -> {
              StringHttpMessageConverter serverConverter = findConverter(getServerConverters(context),
                      StringHttpMessageConverter.class);
              assertThat(serverConverter.getDefaultCharset()).isEqualTo(StandardCharsets.UTF_16);
            });
  }

  @Test
  void defaultConvertersCustomizerHasOrderZero() {
    defaultConvertersCustomizerHasOrderZero(DefaultHttpMessageConvertersCustomizer.class);
  }

  private <T> void defaultConvertersCustomizerHasOrderZero(Class<T> customizerType) {
    new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
            .run((context) -> {
              Map<String, T> customizers = context.getBeansOfType(customizerType);
              assertThat(customizers).hasSize(1);
              StandardBeanFactory beanFactory = (StandardBeanFactory) context.getBeanFactory();
              customizers.keySet().forEach((beanName) -> assertThat(beanFactory.getOrder(beanName)).isZero());
            });
  }

  private ApplicationContextRunner allOptionsRunner() {
    return this.contextRunner.withBean(Gson.class)
            .withBean(JsonMapper.class)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(Jsonb.class, JsonbBuilder::create);
  }

  private void assertConverterIsRegistered(AssertableApplicationContext context,
          Class<? extends HttpMessageConverter<?>> converterType) {
    assertThat(getClientConverters(context)).filteredOn((c) -> converterType.isAssignableFrom(c.getClass()))
            .hasSize(1);
    assertThat(getServerConverters(context)).filteredOn((c) -> converterType.isAssignableFrom(c.getClass()))
            .hasSize(1);
  }

  private void assertConverterIsNotRegistered(AssertableApplicationContext context,
          Class<? extends HttpMessageConverter<?>> converterType) {
    assertThat(getClientConverters(context)).filteredOn((c) -> converterType.isAssignableFrom(c.getClass()))
            .isEmpty();
    assertThat(getServerConverters(context)).filteredOn((c) -> converterType.isAssignableFrom(c.getClass()))
            .isEmpty();
  }

  private void assertBeanExists(AssertableApplicationContext context, Class<?> type, String beanName) {
    assertThat(context).getBean(beanName).isInstanceOf(type);
    assertThat(context).hasBean(beanName);
  }

  private HttpMessageConverters getClientConverters(ApplicationContext context) {
    ClientBuilder clientBuilder = HttpMessageConverters.forClient().registerDefaults();
    context.getBeanProvider(ClientHttpMessageConvertersCustomizer.class)
            .orderedStream()
            .forEach((customizer) -> customizer.customize(clientBuilder));
    return clientBuilder.build();
  }

  private HttpMessageConverters getServerConverters(ApplicationContext context) {
    ServerBuilder serverBuilder = HttpMessageConverters.forServer().registerDefaults();
    context.getBeanProvider(ServerHttpMessageConvertersCustomizer.class)
            .orderedStream()
            .forEach((customizer) -> customizer.customize(serverBuilder));
    return serverBuilder.build();
  }

  @SuppressWarnings("unchecked")
  private <T extends HttpMessageConverter<?>> T findConverter(HttpMessageConverters converters,
          Class<? extends HttpMessageConverter<?>> type) {
    for (HttpMessageConverter<?> converter : converters) {
      if (type.isAssignableFrom(converter.getClass())) {
        return (T) converter;
      }
    }
    throw new IllegalStateException("Could not find converter of type " + type);
  }

  private void assertConvertersRegisteredWithHttpMessageConverters(AssertableApplicationContext context,
          List<Class<? extends HttpMessageConverter<?>>> types) {
    HttpMessageConverters clientConverters = getClientConverters(context);
    List<Class<?>> clientConverterTypes = new ArrayList<>();
    clientConverters.forEach((converter) -> clientConverterTypes.add(converter.getClass()));
    assertThat(clientConverterTypes).containsSubsequence(types);

    HttpMessageConverters serverConverters = getServerConverters(context);
    List<Class<?>> serverConverterTypes = new ArrayList<>();
    serverConverters.forEach((converter) -> serverConverterTypes.add(converter.getClass()));
    assertThat(serverConverterTypes).containsSubsequence(types);
  }

  @Configuration(proxyBeanMethods = false)
  static class JacksonJsonMapperConfig {

    @Bean
    JsonMapper jsonMapper() {
      return new JsonMapper();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CustomJsonConverterConfig {

    @Bean
    JsonMapper jsonMapper() {
      return new JsonMapper();
    }

    @Bean
    ServerHttpMessageConvertersCustomizer jsonServerCustomizer() {
      return (configurer) -> configurer.withJsonConverter(new CustomConverter(MediaType.APPLICATION_JSON));
    }

    @Bean
    ClientHttpMessageConvertersCustomizer jsonClientCustomizer() {
      return (configurer) -> configurer.withJsonConverter(new CustomConverter(MediaType.APPLICATION_JSON));
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JacksonJsonMapperBuilderConfig {

    @Bean
    JsonMapper jsonMapper() {
      return new JsonMapper();
    }

    @Bean
    JsonMapper.Builder builder() {
      return JsonMapper.builder();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JacksonXmlMapperBuilderConfig {

    @Bean
    XmlMapper xmlMapper() {
      return new XmlMapper();
    }

    @Bean
    XmlMapper.Builder builder() {
      return XmlMapper.builder();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class JacksonConverterConfig {

    @Bean
    JacksonJsonHttpMessageConverter customJacksonMessageConverter(JsonMapper jsonMapperMapper) {
      JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapperMapper);
      return converter;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class Jackson2ObjectMapperConfig {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
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

  @SuppressWarnings("NullAway")
  static class CustomConverter extends AbstractHttpMessageConverter<Object> {

    CustomConverter(MediaType supportedMediaType) {
      super(supportedMediaType);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
      return true;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
      return null;
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

    }

  }

}
