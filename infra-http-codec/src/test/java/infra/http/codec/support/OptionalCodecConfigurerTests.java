/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.http.codec.support;

import org.junit.jupiter.api.Test;

import infra.core.ResolvableType;
import infra.http.MediaType;
import infra.http.codec.ClientCodecConfigurer;
import infra.http.codec.DecoderHttpMessageReader;
import infra.http.codec.EncoderHttpMessageWriter;
import infra.http.codec.ServerCodecConfigurer;
import infra.http.codec.ServerSentEventHttpMessageReader;
import infra.http.codec.ServerSentEventHttpMessageWriter;
import infra.http.codec.json.GsonDecoder;
import infra.http.codec.json.GsonEncoder;
import infra.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

class OptionalCodecConfigurerTests {

  @Test
  @ClassPathExclusions({ "jackson-databind-*.jar", "protobuf-java-*.jar", "netty-buffer-*.jar" })
  void fallsBackToGsonAndKeepsTextEventsWithoutJackson() {
    var client = ClientCodecConfigurer.create();
    var server = ServerCodecConfigurer.create();
    client.defaultCodecs().maxInMemorySize(512);
    server.defaultCodecs().maxInMemorySize(512);
    var gsonDecoder = new GsonDecoder();
    var gsonEncoder = new GsonEncoder();
    client.defaultCodecs().gsonDecoder(gsonDecoder);
    client.defaultCodecs().gsonEncoder(gsonEncoder);

    assertThat(client.getReaders()).anySatisfy(reader -> assertThat(reader)
            .isInstanceOfSatisfying(DecoderHttpMessageReader.class, adapter -> assertThat(adapter.getDecoder()).isSameAs(gsonDecoder)));
    assertThat(client.getWriters()).anySatisfy(writer -> assertThat(writer)
            .isInstanceOfSatisfying(EncoderHttpMessageWriter.class, adapter -> assertThat(adapter.getEncoder()).isSameAs(gsonEncoder)));
    assertThat(client.getReaders()).anySatisfy(reader -> assertThat(reader)
            .isInstanceOfSatisfying(ServerSentEventHttpMessageReader.class, sse -> assertThat(sse.getDecoder()).isNull()));
    assertThat(server.getWriters()).anySatisfy(writer -> assertThat(writer)
            .isInstanceOfSatisfying(ServerSentEventHttpMessageWriter.class, sse -> assertThat(sse.getEncoder()).isNull()));
    assertThat(client.getReaders()).noneMatch(reader -> reader.canRead(ResolvableType.forClass(Object.class), MediaType.parseMediaType("application/cbor")));
  }

  @Test
  @ClassPathExclusions({ "jackson-databind-*.jar", "gson-*.jar" })
  void hasNoObjectJsonCodecWithoutJacksonOrGson() {
    var configurer = ServerCodecConfigurer.create();
    var type = ResolvableType.forClassWithGenerics(java.util.List.class, Integer.class);

    assertThat(configurer.getReaders()).noneMatch(reader -> reader.canRead(type, MediaType.APPLICATION_JSON));
    assertThat(configurer.getWriters()).noneMatch(writer -> writer.canWrite(type, MediaType.APPLICATION_JSON));
  }
}
