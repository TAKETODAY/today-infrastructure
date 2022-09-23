/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.codec.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.codec.Pojo;
import cn.taketoday.http.codec.ServerSentEvent;
import cn.taketoday.http.codec.json.JacksonViewBean.MyJacksonView1;
import cn.taketoday.http.codec.json.JacksonViewBean.MyJacksonView3;
import cn.taketoday.http.converter.json.MappingJacksonValue;
import cn.taketoday.util.MimeType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static cn.taketoday.http.MediaType.APPLICATION_JSON;
import static cn.taketoday.http.MediaType.APPLICATION_NDJSON;
import static cn.taketoday.http.MediaType.APPLICATION_OCTET_STREAM;
import static cn.taketoday.http.MediaType.APPLICATION_STREAM_JSON;
import static cn.taketoday.http.MediaType.APPLICATION_XML;
import static cn.taketoday.http.codec.json.Jackson2CodecSupport.JSON_VIEW_HINT;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Sebastien Deleuze
 */
public class Jackson2JsonEncoderTests extends AbstractEncoderTests<Jackson2JsonEncoder> {

  public Jackson2JsonEncoderTests() {
    super(new Jackson2JsonEncoder());
  }

  @Override
  @Test
  @SuppressWarnings("deprecation")
  public void canEncode() {
    ResolvableType pojoType = ResolvableType.fromClass(Pojo.class);
    assertThat(this.encoder.canEncode(pojoType, APPLICATION_JSON)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, APPLICATION_NDJSON)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, APPLICATION_STREAM_JSON)).isTrue();
    assertThat(this.encoder.canEncode(pojoType, null)).isTrue();

    assertThat(this.encoder.canEncode(ResolvableType.fromClass(Pojo.class),
            new MediaType("application", "json", StandardCharsets.UTF_8))).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(Pojo.class),
            new MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(Pojo.class),
            new MediaType("application", "json", StandardCharsets.ISO_8859_1))).isFalse();

    // SPR-15464
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();

    // SPR-15910
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(Object.class), APPLICATION_OCTET_STREAM)).isFalse();
  }

  @Override
  @Test
  public void encode() throws Exception {
    Flux<Object> input = Flux.just(new Pojo("foo", "bar"),
            new Pojo("foofoo", "barbar"),
            new Pojo("foofoofoo", "barbarbar"));

    testEncodeAll(input, ResolvableType.fromClass(Pojo.class), APPLICATION_STREAM_JSON, null,
            step -> step.consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
                    .consumeNextWith(expectString("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
                    .consumeNextWith(expectString("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
                    .verifyComplete()
    );
  }

  @Test  // SPR-15866
  public void canEncodeWithCustomMimeType() {
    MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(new ObjectMapper(), textJavascript);

    assertThat(encoder.getEncodableMimeTypes()).isEqualTo(Collections.singletonList(textJavascript));
  }

  @Test
  public void encodableMimeTypesIsImmutable() {
    MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(new ObjectMapper(), textJavascript);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
            encoder.getMimeTypes().add(new MimeType("text", "ecmascript")));
  }

  @Test
  public void canNotEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(String.class), null)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(Pojo.class), APPLICATION_XML)).isFalse();

    ResolvableType sseType = ResolvableType.fromClass(ServerSentEvent.class);
    assertThat(this.encoder.canEncode(sseType, APPLICATION_JSON)).isFalse();
  }

  @Test
  public void encodeNonStream() {
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
  void encodeNonStreamWithErrorAsFirstSignal() {
    String message = "I'm a teapot";
    Flux<Object> input = Flux.error(new IllegalStateException(message));

    Flux<DataBuffer> output = this.encoder.encode(
            input, this.bufferFactory, ResolvableType.fromClass(Pojo.class), null, null);

    StepVerifier.create(output).expectErrorMessage(message).verify();
  }

  @Test
  public void encodeWithType() {
    Flux<ParentClass> input = Flux.just(new Foo(), new Bar());

    testEncode(input, ParentClass.class, step -> step
            .consumeNextWith(expectString("[{\"type\":\"foo\"}"))
            .consumeNextWith(expectString(",{\"type\":\"bar\"}"))
            .consumeNextWith(expectString("]"))
            .verifyComplete());
  }

  @Test  // SPR-15727
  public void encodeStreamWithCustomStreamingType() {
    MediaType fooMediaType = new MediaType("application", "foo");
    MediaType barMediaType = new MediaType("application", "bar");
    this.encoder.setStreamingMediaTypes(Arrays.asList(fooMediaType, barMediaType));
    Flux<Pojo> input = Flux.just(
            new Pojo("foo", "bar"),
            new Pojo("foofoo", "barbar"),
            new Pojo("foofoofoo", "barbarbar")
    );

    testEncode(input, ResolvableType.fromClass(Pojo.class), barMediaType, null, step -> step
            .consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
            .consumeNextWith(expectString("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
            .consumeNextWith(expectString("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
            .verifyComplete()
    );
  }

  @Test
  public void fieldLevelJsonView() {
    JacksonViewBean bean = new JacksonViewBean();
    bean.setWithView1("with");
    bean.setWithView2("with");
    bean.setWithoutView("without");
    Mono<JacksonViewBean> input = Mono.just(bean);

    ResolvableType type = ResolvableType.fromClass(JacksonViewBean.class);
    Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView1.class);

    testEncode(input, type, null, hints, step -> step
            .consumeNextWith(expectString("{\"withView1\":\"with\"}"))
            .verifyComplete()
    );
  }

  @Test
  public void classLevelJsonView() {
    JacksonViewBean bean = new JacksonViewBean();
    bean.setWithView1("with");
    bean.setWithView2("with");
    bean.setWithoutView("without");
    Mono<JacksonViewBean> input = Mono.just(bean);

    ResolvableType type = ResolvableType.fromClass(JacksonViewBean.class);
    Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView3.class);

    testEncode(input, type, null, hints, step -> step
            .consumeNextWith(expectString("{\"withoutView\":\"without\"}"))
            .verifyComplete()
    );
  }

  @Test
  public void jacksonValue() {
    JacksonViewBean bean = new JacksonViewBean();
    bean.setWithView1("with");
    bean.setWithView2("with");
    bean.setWithoutView("without");

    MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
    jacksonValue.setSerializationView(MyJacksonView1.class);

    ResolvableType type = ResolvableType.fromClass(MappingJacksonValue.class);

    testEncode(Mono.just(jacksonValue), type, null, Collections.emptyMap(), step -> step
            .consumeNextWith(expectString("{\"withView1\":\"with\"}"))
            .verifyComplete()
    );
  }

  @Test  // gh-28045
  public void jacksonValueUnwrappedBeforeObjectMapperSelection() {
    JacksonViewBean bean = new JacksonViewBean();
    bean.setWithView1("with");
    bean.setWithView2("with");
    bean.setWithoutView("without");

    MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
    jacksonValue.setSerializationView(MyJacksonView1.class);

    ResolvableType type = ResolvableType.fromClass(MappingJacksonValue.class);

    MediaType halMediaType = MediaType.parseMediaType("application/hal+json");
    ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
    this.encoder.registerObjectMappersForType(JacksonViewBean.class, map -> map.put(halMediaType, mapper));

    String ls = System.lineSeparator();  // output below is different between Unix and Windows
    testEncode(Mono.just(jacksonValue), type, halMediaType, Collections.emptyMap(),
            step -> step.consumeNextWith(expectString("{" + ls + "  \"withView1\" : \"with\"" + ls + "}"))
                    .verifyComplete()
    );
  }

  @Test  // gh-22771
  public void encodeWithFlushAfterWriteOff() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false);
    Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(mapper);

    Flux<DataBuffer> result = encoder.encode(Flux.just(new Pojo("foo", "bar")), this.bufferFactory,
            ResolvableType.fromClass(Pojo.class), MimeTypeUtils.APPLICATION_JSON, Collections.emptyMap());

    StepVerifier.create(result)
            .consumeNextWith(expectString("[{\"foo\":\"foo\",\"bar\":\"bar\"}"))
            .consumeNextWith(expectString("]"))
            .expectComplete()
            .verify(Duration.ofSeconds(5));
  }

  @Test
  public void encodeAscii() {
    Mono<Object> input = Mono.just(new Pojo("foo", "bar"));
    MimeType mimeType = new MimeType("application", "json", StandardCharsets.US_ASCII);

    testEncode(input, ResolvableType.fromClass(Pojo.class), mimeType, null, step -> step
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

}
