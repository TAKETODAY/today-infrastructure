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

package infra.http.codec.json;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.io.buffer.DataBuffer;
import infra.core.testfixture.codec.AbstractEncoderTests;
import infra.http.MediaType;
import infra.http.codec.Pojo;
import infra.http.codec.json.JacksonViewBean.MyJacksonView1;
import infra.http.codec.json.JacksonViewBean.MyJacksonView3;
import infra.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

import static infra.http.MediaType.APPLICATION_JSON;
import static infra.http.MediaType.APPLICATION_NDJSON;
import static infra.http.MediaType.APPLICATION_OCTET_STREAM;
import static infra.http.MediaType.APPLICATION_XML;
import static infra.http.codec.JacksonCodecSupport.FILTER_PROVIDER_HINT;
import static infra.http.codec.JacksonCodecSupport.JSON_VIEW_HINT;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JacksonJsonEncoder}.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
class JacksonJsonEncoderTests extends AbstractEncoderTests<JacksonJsonEncoder> {

  public JacksonJsonEncoderTests() {
    super(new JacksonJsonEncoder());
  }

  @Test
  @Override
  public void canEncode() {
    ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
    assertThat(this.encoder.canEncode(pojoType, APPLICATION_JSON)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, APPLICATION_NDJSON)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, null)).isTrue();

    assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
            new MediaType("application", "json", StandardCharsets.UTF_8))).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
            new MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
            new MediaType("application", "json", StandardCharsets.ISO_8859_1))).isFalse();

    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Object.class), APPLICATION_OCTET_STREAM)).isFalse();
  }

  @Test
  @Override
  public void encode() throws Exception {
    Flux<Object> input = Flux.just(new Pojo("foo", "bar"),
            new Pojo("foofoo", "barbar"),
            new Pojo("foofoofoo", "barbarbar"));

    testEncodeAll(input, ResolvableType.forClass(Pojo.class), APPLICATION_NDJSON, null, step -> step
            .consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
            .consumeNextWith(expectString("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
            .consumeNextWith(expectString("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
            .verifyComplete()
    );
  }

  @Test
  public void canEncodeWithCustomMimeType() {
    MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
    JacksonJsonEncoder encoder = new JacksonJsonEncoder(new JsonMapper(), textJavascript);

    assertThat(encoder.getEncodableMimeTypes()).isEqualTo(Collections.singletonList(textJavascript));
  }

  @Test
  void encodableMimeTypesIsImmutable() {
    MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
    JacksonJsonEncoder encoder = new JacksonJsonEncoder(new JsonMapper(), textJavascript);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            encoder.getEncodableMimeTypes().add(new MimeType("text", "ecmascript")));
  }

  @Test
  void canNotEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();
  }

  @Test
  void encodeNonStream() {
    Flux<Pojo> input = Flux.just(
            new Pojo("foo", "bar"),
            new Pojo("foofoo", "barbar"),
            new Pojo("foofoofoo", "barbarbar")
    );

    testEncode(input, Pojo.class, step -> step
            .consumeNextWith(expectString("[{\"foo\":\"foo\",\"bar\":\"bar\"}"))
            .consumeNextWith(expectString(",{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"))
            .consumeNextWith(expectString(",{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"))
            .consumeNextWith(expectString("]"))
            .verifyComplete());
  }

  @Test
  void encodeNonStreamEmpty() {
    testEncode(Flux.empty(), Pojo.class, step -> step
            .consumeNextWith(expectString("["))
            .consumeNextWith(expectString("]"))
            .verifyComplete());
  }

  @Test
  void encodeNonStreamWithErrorAsFirstSignal() {
    String message = "I'm a teapot";
    Flux<Object> input = Flux.error(new IllegalStateException(message));

    Flux<DataBuffer> output = this.encoder.encode(
            input, this.bufferFactory, ResolvableType.forClass(Pojo.class), null, null);

    StepVerifier.create(output).expectErrorMessage(message).verify();
  }

  @Test
  void encodeWithType() {
    Flux<ParentClass> input = Flux.just(new Foo(), new Bar());

    testEncode(input, ParentClass.class, step -> step
            .consumeNextWith(expectString("[{\"type\":\"foo\"}"))
            .consumeNextWith(expectString(",{\"type\":\"bar\"}"))
            .consumeNextWith(expectString("]"))
            .verifyComplete());
  }

  @Test
  public void encodeStreamWithCustomStreamingType() {
    MediaType fooMediaType = new MediaType("application", "foo");
    MediaType barMediaType = new MediaType("application", "bar");
    this.encoder.setStreamingMediaTypes(Arrays.asList(fooMediaType, barMediaType));
    Flux<Pojo> input = Flux.just(
            new Pojo("foo", "bar"),
            new Pojo("foofoo", "barbar"),
            new Pojo("foofoofoo", "barbarbar")
    );

    testEncode(input, ResolvableType.forClass(Pojo.class), barMediaType, null, step -> step
            .consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
            .consumeNextWith(expectString("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
            .consumeNextWith(expectString("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
            .verifyComplete()
    );
  }

  @Test
  void fieldLevelJsonView() {
    JacksonViewBean bean = new JacksonViewBean();
    bean.setWithView1("with");
    bean.setWithView2("with");
    bean.setWithoutView("without");
    Mono<JacksonViewBean> input = Mono.just(bean);

    ResolvableType type = ResolvableType.forClass(JacksonViewBean.class);
    Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView1.class);

    testEncode(input, type, null, hints, step -> step
            .consumeNextWith(expectString("{\"withView1\":\"with\"}"))
            .verifyComplete()
    );
  }

  @Test
  void fieldLevelJsonViewStream() {
    JacksonViewBean bean = new JacksonViewBean();
    bean.setWithView1("with");
    bean.setWithView2("with");
    bean.setWithoutView("without");
    Flux<JacksonViewBean> input = Flux.just(bean, bean);

    ResolvableType type = ResolvableType.forClass(JacksonViewBean.class);
    Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView1.class);

    testEncodeAll(input, type, APPLICATION_NDJSON, hints, step -> step
            .consumeNextWith(expectString("{\"withView1\":\"with\"}\n"))
            .consumeNextWith(expectString("{\"withView1\":\"with\"}\n"))
            .verifyComplete()
    );
  }

  @Test
  void classLevelJsonView() {
    JacksonViewBean bean = new JacksonViewBean();
    bean.setWithView1("with");
    bean.setWithView2("with");
    bean.setWithoutView("without");
    Mono<JacksonViewBean> input = Mono.just(bean);

    ResolvableType type = ResolvableType.forClass(JacksonViewBean.class);
    Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView3.class);

    testEncode(input, type, null, hints, step -> step
            .consumeNextWith(expectString("{\"withoutView\":\"without\"}"))
            .verifyComplete()
    );
  }

  @Test
  void filterProvider() {
    JacksonFilteredBean filteredBean = new JacksonFilteredBean("foo", "bar");
    FilterProvider filters = new SimpleFilterProvider().addFilter("myJacksonFilter",
            SimpleBeanPropertyFilter.serializeAllExcept("property2"));
    Mono<JacksonFilteredBean> input = Mono.just(filteredBean);
    Map<String, Object> hints = singletonMap(FILTER_PROVIDER_HINT, filters);
    testEncode(input, ResolvableType.forClass(Pojo.class), APPLICATION_JSON, hints, step -> step
            .consumeNextWith(expectString("{\"property1\":\"foo\"}"))
            .verifyComplete()
    );
  }

  @Test
  void filterProviderStream() {
    JacksonFilteredBean filteredBean = new JacksonFilteredBean("foo", "bar");
    FilterProvider filters = new SimpleFilterProvider().addFilter("myJacksonFilter",
            SimpleBeanPropertyFilter.serializeAllExcept("property2"));
    Flux<JacksonFilteredBean> input = Flux.just(filteredBean, filteredBean);
    Map<String, Object> hints = singletonMap(FILTER_PROVIDER_HINT, filters);
    testEncodeAll(input, ResolvableType.forClass(Pojo.class), APPLICATION_NDJSON, hints, step -> step
            .consumeNextWith(expectString("{\"property1\":\"foo\"}\n"))
            .consumeNextWith(expectString("{\"property1\":\"foo\"}\n"))
            .verifyComplete()
    );
  }

  @Test
  public void encodeWithFlushAfterWriteOff() {
    JsonMapper mapper = JsonMapper.builder().configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false).build();
    JacksonJsonEncoder encoder = new JacksonJsonEncoder(mapper);

    Flux<DataBuffer> result = encoder.encode(Flux.just(new Pojo("foo", "bar")), this.bufferFactory,
            ResolvableType.forClass(Pojo.class), MimeType.APPLICATION_JSON, Collections.emptyMap());

    StepVerifier.create(result)
            .consumeNextWith(expectString("[{\"foo\":\"foo\",\"bar\":\"bar\"}"))
            .consumeNextWith(expectString("]"))
            .expectComplete()
            .verify(Duration.ofSeconds(5));
  }

  @Test
  void encodeAscii() {
    Mono<Object> input = Mono.just(new Pojo("foo", "bar"));
    MimeType mimeType = new MimeType("application", "json", StandardCharsets.US_ASCII);

    testEncode(input, ResolvableType.forClass(Pojo.class), mimeType, null, step -> step
            .consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}"))
            .verifyComplete()
    );
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  private static class ParentClass {
  }

  @JsonTypeName("foo")
  private static class Foo extends ParentClass {
  }

  @JsonTypeName("bar")
  private static class Bar extends ParentClass {
  }

  @JsonFilter("myJacksonFilter")
  record JacksonFilteredBean(String property1, String property2) {
  }

}
