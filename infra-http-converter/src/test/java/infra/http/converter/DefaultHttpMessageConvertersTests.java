/*
 * Copyright 2002-present the original author or authors.
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

package infra.http.converter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import infra.http.HttpInputMessage;
import infra.http.HttpOutputMessage;
import infra.http.converter.cbor.JacksonCborHttpMessageConverter;
import infra.http.converter.feed.AtomFeedHttpMessageConverter;
import infra.http.converter.feed.RssChannelHttpMessageConverter;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.http.converter.json.GsonHttpMessageConverter;
import infra.http.converter.json.JsonbHttpMessageConverter;
import infra.http.converter.smile.JacksonSmileHttpMessageConverter;
import infra.http.converter.xml.JacksonXmlHttpMessageConverter;
import infra.http.converter.yaml.JacksonYamlHttpMessageConverter;
import infra.util.Feature;
import infra.util.FeatureDetector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mockStatic;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/1 13:32
 */
class DefaultHttpMessageConvertersTests {

  @ParameterizedTest
  @MethodSource("emptyMessageConverters")
  void emptyConverters(Iterable<HttpMessageConverter<?>> converters) {
    assertThat(converters).isEmpty();
  }

  static Stream<Iterable<HttpMessageConverter<?>>> emptyMessageConverters() {
    return Stream.of(
            HttpMessageConverters.forClient().build(),
            HttpMessageConverters.forServer().build()
    );
  }

  @Test
  void selectsTheNextAvailableJsonLibraryWhenJacksonIsUnavailable() {
    var client = HttpMessageConverters.forClient();
    try (var features = mockStatic(FeatureDetector.class)) {
      features.when(() -> FeatureDetector.isPresent(Feature.GSON)).thenReturn(true);
      assertThat(client.registerDefaults().build()).hasAtLeastOneElementOfType(GsonHttpMessageConverter.class);
      features.when(() -> FeatureDetector.isPresent(Feature.GSON)).thenReturn(false);
      features.when(() -> FeatureDetector.isPresent(Feature.JSONB)).thenReturn(true);
      assertThat(HttpMessageConverters.forServer().registerDefaults().build()).hasAtLeastOneElementOfType(JsonbHttpMessageConverter.class);
      features.when(() -> FeatureDetector.isPresent(Feature.JSONB)).thenReturn(false);
      assertThat(HttpMessageConverters.forServer().registerDefaults().build().asList())
              .noneMatch(converter -> converter.canWrite(Object.class, infra.http.MediaType.APPLICATION_JSON));
    }
  }

  @Test
  void configuresEveryDetectedConverterAndInvokesChainedConfigurer() {
    var seen = new java.util.ArrayList<HttpMessageConverter<?>>();
    var builder = HttpMessageConverters.forClient().registerDefaults()
            .configureMessageConverters(seen::add)
            .configureMessageConverters(converter -> assertThat(seen).contains(converter));
    var converters = builder.build();
    assertThat(seen).containsExactlyElementsOf(converters.asList());
    assertThat(converters).hasAtLeastOneElementOfType(ByteArrayHttpMessageConverter.class)
            .hasAtLeastOneElementOfType(StringHttpMessageConverter.class);
  }

  @Test
  void keepsOnlyCustomConvertersWhenDefaultsAreDisabled() {
    var custom = new CustomHttpMessageConverter();
    var converters = HttpMessageConverters.forServer().disableDefaults()
            .addCustomConverter(custom).build();
    assertThat(converters.asList()).hasExactlyElementsOfTypes(CustomHttpMessageConverter.class,
            AllEncompassingFormHttpMessageConverter.class);
    assertThat(converters.iterator()).toIterable().containsExactlyElementsOf(converters.asList());
  }

  @Test
  void clientKeepsExplicitFormatConvertersAndIgnoresNullBulkRegistration() {
    var xml = new JacksonXmlHttpMessageConverter();
    var smile = new JacksonSmileHttpMessageConverter();
    var cbor = new JacksonCborHttpMessageConverter();
    var yaml = new JacksonYamlHttpMessageConverter();
    var custom = new CustomHttpMessageConverter();
    var builder = HttpMessageConverters.forClient().registerDefaults()
            .withXmlConverter(xml).withSmileConverter(smile).withCborConverter(cbor).withYamlConverter(yaml);

    var converters = builder.addCustomConverters(null).addCustomConverters(List.of(custom)).build();
    assertThat(converters.isEmpty()).isFalse();
    assertThat(converters.asList()).startsWith(custom).contains(xml, smile, cbor, yaml);
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> converters.asList().clear());
    assertThat(builder.disableDefaults().build().asList()).hasExactlyElementsOfTypes(
            CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
    assertThat(HttpMessageConverters.forClient().registerDefaults().disableDefaults().build().isEmpty()).isTrue();
  }

  @Test
  void serverKeepsExplicitFormatConvertersInTheMainListAndMultipart() {
    var xml = new JacksonXmlHttpMessageConverter();
    var smile = new JacksonSmileHttpMessageConverter();
    var cbor = new JacksonCborHttpMessageConverter();
    var yaml = new JacksonYamlHttpMessageConverter();
    var custom = new CustomHttpMessageConverter();
    var builder = HttpMessageConverters.forServer().registerDefaults()
            .withXmlConverter(xml).withSmileConverter(smile).withCborConverter(cbor).withYamlConverter(yaml);

    var converters = builder.addCustomConverters(null).addCustomConverters(List.of(custom)).build();
    assertThat(converters.asList()).startsWith(custom).contains(xml, smile, cbor, yaml);
    assertThat(findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters).getPartConverters())
            .contains(xml, smile, cbor, yaml, custom);
    assertThat(builder.disableDefaults().build().asList()).hasExactlyElementsOfTypes(
            CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
  }

  @Test
  void failsWhenStringConverterDoesNotSupportMediaType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpMessageConverters.forClient().withStringConverter(new CustomHttpMessageConverter()).build())
            .withMessage("converter should support 'text/plain'");
  }

  @Test
  void failsWhenJsonConverterDoesNotSupportMediaType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpMessageConverters.forClient().withJsonConverter(new CustomHttpMessageConverter()).build())
            .withMessage("converter should support 'application/json'");
  }

  @Test
  void canConfigureXmlConverterWithCharset() {
    HttpMessageConverters.forClient().withXmlConverter(new JacksonXmlHttpMessageConverter()).build();
  }

  @Test
  void failsWhenXmlConverterDoesNotSupportMediaType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpMessageConverters.forClient().withXmlConverter(new CustomHttpMessageConverter()).build())
            .withMessage("converter should support 'text/xml'");
  }

  @Test
  void failsWhenSmileConverterDoesNotSupportMediaType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpMessageConverters.forClient().withSmileConverter(new CustomHttpMessageConverter()).build())
            .withMessage("converter should support 'application/x-jackson-smile'");
  }

  @Test
  void failsWhenCborConverterDoesNotSupportMediaType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpMessageConverters.forClient().withCborConverter(new CustomHttpMessageConverter()).build())
            .withMessage("converter should support 'application/cbor'");
  }

  @Test
  void failsWhenYamlConverterDoesNotSupportMediaType() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> HttpMessageConverters.forClient().withYamlConverter(new CustomHttpMessageConverter()).build())
            .withMessage("converter should support 'application/yaml'");
  }

  @Nested
  class ClientConvertersTests {

    @Test
    void defaultConverters() {
      var converters = HttpMessageConverters.forClient().registerDefaults().build();
      assertThat(converters).hasExactlyElementsOfTypes(ByteArrayHttpMessageConverter.class,
              StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
              AllEncompassingFormHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
              JacksonCborHttpMessageConverter.class, JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
              AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
    }

    @Test
    void multipartConverterContainsOtherConverters() {
      var converters = HttpMessageConverters.forClient().registerDefaults().build();
      var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);

      assertThat(multipartConverter.getPartConverters()).hasExactlyElementsOfTypes(
              ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
              ResourceHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
              JacksonCborHttpMessageConverter.class, JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
              AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
    }

    @Test
    void registerCustomMessageConverter() {
      var converters = HttpMessageConverters.forClient()
              .addCustomConverter(new CustomHttpMessageConverter()).build();
      assertThat(converters).hasExactlyElementsOfTypes(CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
    }

    @Test
    void registerCustomMessageConverterAheadOfDefaults() {
      var converters = HttpMessageConverters.forClient().registerDefaults()
              .addCustomConverter(new CustomHttpMessageConverter()).build();
      assertThat(converters).hasExactlyElementsOfTypes(
              CustomHttpMessageConverter.class, ByteArrayHttpMessageConverter.class,
              StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
              AllEncompassingFormHttpMessageConverter.class,
              JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
              JacksonCborHttpMessageConverter.class,
              JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
              AtomFeedHttpMessageConverter.class,
              RssChannelHttpMessageConverter.class);
    }

    @Test
    void registerCustomConverterInMultipartConverter() {
      var converters = HttpMessageConverters.forClient().registerDefaults()
              .addCustomConverter(new CustomHttpMessageConverter()).build();
      var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);
      assertThat(multipartConverter.getPartConverters()).hasAtLeastOneElementOfType(CustomHttpMessageConverter.class);
    }

    @Test
    void shouldNotConfigureOverridesWhenDefaultOff() {
      var stringConverter = new StringHttpMessageConverter();
      var converters = HttpMessageConverters.forClient().withStringConverter(stringConverter).build();
      assertThat(converters).isEmpty();
    }

    @Test
    void shouldUseSpecificConverter() {
      var jacksonConverter = new JacksonJsonHttpMessageConverter();
      var converters = HttpMessageConverters.forClient().registerDefaults()
              .withJsonConverter(jacksonConverter).build();

      var customConverter = findMessageConverter(JacksonJsonHttpMessageConverter.class, converters);
      assertThat(customConverter).isEqualTo(jacksonConverter);
    }

    @Test
    void shouldOverrideStringConverters() {
      var stringConverter = new StringHttpMessageConverter();
      var converters = HttpMessageConverters.forClient().registerDefaults()
              .withStringConverter(stringConverter).build();

      var actualConverter = findMessageConverter(StringHttpMessageConverter.class, converters);
      assertThat(actualConverter).isEqualTo(stringConverter);
    }

    @Test
    void shouldConfigureConverter() {
      var customConverter = new CustomHttpMessageConverter();
      HttpMessageConverters.forClient()
              .addCustomConverter(customConverter)
              .configureMessageConverters(converter -> {
                if (converter instanceof CustomHttpMessageConverter custom) {
                  assertThat(custom.processCount).isZero();
                  custom.processCount++;
                }
              })
              .configureMessageConverters(converter -> {
                if (converter instanceof CustomHttpMessageConverter custom) {
                  assertThat(custom.processCount).isEqualTo(1);
                  custom.processCount++;
                }
              }).build();

      assertThat(customConverter.processCount).isEqualTo(2);
    }

  }

  @Nested
  class ServerConvertersTests {

    @Test
    void defaultConverters() {
      var converters = HttpMessageConverters.forServer().registerDefaults().build();
      assertThat(converters).hasExactlyElementsOfTypes(
              ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
              ResourceHttpMessageConverter.class, ResourceRegionHttpMessageConverter.class,
              AllEncompassingFormHttpMessageConverter.class,
              JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
              JacksonCborHttpMessageConverter.class,
              JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
              AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
    }

    @Test
    void multipartConverterContainsOtherConverters() {
      var converters = HttpMessageConverters.forServer().registerDefaults().build();
      var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);

      assertThat(multipartConverter.getPartConverters()).hasExactlyElementsOfTypes(
              ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
              ResourceHttpMessageConverter.class,
              JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
              JacksonCborHttpMessageConverter.class,
              JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
              AtomFeedHttpMessageConverter.class,
              RssChannelHttpMessageConverter.class);
    }

    @Test
    void registerCustomMessageConverter() {
      var converters = HttpMessageConverters.forServer()
              .addCustomConverter(new CustomHttpMessageConverter()).build();
      assertThat(converters).hasExactlyElementsOfTypes(CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
    }

    @Test
    void registerCustomMessageConverterAheadOfDefaults() {
      var converters = HttpMessageConverters.forServer().registerDefaults()
              .addCustomConverter(new CustomHttpMessageConverter()).build();
      assertThat(converters).hasExactlyElementsOfTypes(
              CustomHttpMessageConverter.class,
              ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
              ResourceHttpMessageConverter.class, ResourceRegionHttpMessageConverter.class,
              AllEncompassingFormHttpMessageConverter.class,
              JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
              JacksonCborHttpMessageConverter.class,
              JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
              AtomFeedHttpMessageConverter.class,
              RssChannelHttpMessageConverter.class);
    }

    @Test
    void registerCustomConverterInMultipartConverter() {
      var converters = HttpMessageConverters.forServer().registerDefaults()
              .addCustomConverter(new CustomHttpMessageConverter()).build();
      var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);
      assertThat(multipartConverter.getPartConverters()).hasAtLeastOneElementOfType(CustomHttpMessageConverter.class);
    }

    @Test
    void shouldNotConfigureOverridesWhenDefaultOff() {
      var stringConverter = new StringHttpMessageConverter();
      var converters = HttpMessageConverters.forServer().withStringConverter(stringConverter).build();
      assertThat(converters).isEmpty();
    }

    @Test
    void shouldUseSpecificConverter() {
      var jacksonConverter = new JacksonJsonHttpMessageConverter();
      var converters = HttpMessageConverters.forServer().registerDefaults()
              .withJsonConverter(jacksonConverter).build();

      var customConverter = findMessageConverter(JacksonJsonHttpMessageConverter.class, converters);
      assertThat(customConverter).isEqualTo(jacksonConverter);
    }

    @Test
    void shouldOverrideStringConverters() {
      var stringConverter = new StringHttpMessageConverter();
      var converters = HttpMessageConverters.forServer().registerDefaults()
              .withStringConverter(stringConverter).build();

      var actualConverter = findMessageConverter(StringHttpMessageConverter.class, converters);
      assertThat(actualConverter).isEqualTo(stringConverter);
    }

    @Test
    void shouldConfigureConverter() {
      var customConverter = new CustomHttpMessageConverter();
      HttpMessageConverters.forServer().registerDefaults()
              .addCustomConverter(customConverter)
              .configureMessageConverters(converter -> {
                if (converter instanceof CustomHttpMessageConverter custom) {
                  assertThat(custom.processCount).isZero();
                  custom.processCount++;
                }
              })
              .configureMessageConverters(converter -> {
                if (converter instanceof CustomHttpMessageConverter custom) {
                  assertThat(custom.processCount).isEqualTo(1);
                  custom.processCount++;
                }
              }).build();

      assertThat(customConverter.processCount).isEqualTo(2);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T findMessageConverter(Class<T> converterType, Iterable<HttpMessageConverter<?>> converters) {
    return (T) StreamSupport
            .stream(converters.spliterator(), false)
            .filter(converter -> converter.getClass().equals(converterType))
            .findFirst().orElseThrow();
  }

  static class CustomHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    int processCount;

    @Override
    protected boolean supports(Class<?> clazz) {
      return false;
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
      return null;
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

    }
  }

}
