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

package cn.taketoday.web.reactive.function;

import com.fasterxml.jackson.annotation.JsonView;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.core.codec.ByteBufferEncoder;
import cn.taketoday.core.codec.CharSequenceEncoder;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpOutputMessage;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.http.codec.EncoderHttpMessageWriter;
import cn.taketoday.http.codec.FormHttpMessageWriter;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.http.codec.ResourceHttpMessageWriter;
import cn.taketoday.http.codec.ServerSentEvent;
import cn.taketoday.http.codec.ServerSentEventHttpMessageWriter;
import cn.taketoday.http.codec.json.Jackson2JsonEncoder;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageWriter;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.testfixture.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpRequest;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpResponse;
import io.reactivex.rxjava3.core.Single;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static cn.taketoday.http.codec.json.Jackson2CodecSupport.JSON_VIEW_HINT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
public class BodyInsertersTests {

  private BodyInserter.Context context;

  private Map<String, Object> hints;

  @BeforeEach
  public void createContext() {
    final List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
    messageWriters.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
    messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
    messageWriters.add(new ResourceHttpMessageWriter());
//    messageWriters.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
    Jackson2JsonEncoder jsonEncoder = new Jackson2JsonEncoder();
    messageWriters.add(new EncoderHttpMessageWriter<>(jsonEncoder));
    messageWriters.add(new ServerSentEventHttpMessageWriter(jsonEncoder));
    messageWriters.add(new FormHttpMessageWriter());
    messageWriters.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
    messageWriters.add(new MultipartHttpMessageWriter(messageWriters));

    this.context = new BodyInserter.Context() {
      @Override
      public List<HttpMessageWriter<?>> messageWriters() {
        return messageWriters;
      }

      @Override
      public Optional<ServerHttpRequest> serverRequest() {
        return Optional.empty();
      }

      @Override
      public Map<String, Object> hints() {
        return hints;
      }
    };
    this.hints = new HashMap<>();
  }

  @Test
  public void ofString() {
    String body = "foo";
    BodyInserter<String, ReactiveHttpOutputMessage> inserter = BodyInserters.fromValue(body);

    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();
    StepVerifier.create(response.getBody())
            .consumeNextWith(buf -> {
              String actual = buf.toString(UTF_8);
              assertThat(actual).isEqualTo("foo");
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void ofObject() {
    User body = new User("foo", "bar");
    BodyInserter<User, ReactiveHttpOutputMessage> inserter = BodyInserters.fromValue(body);
    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();

    StepVerifier.create(response.getBodyAsString())
            .expectNext("{\"username\":\"foo\",\"password\":\"bar\"}")
            .expectComplete()
            .verify();
  }

  @Test
  public void ofObjectWithHints() {
    User body = new User("foo", "bar");
    BodyInserter<User, ReactiveHttpOutputMessage> inserter = BodyInserters.fromValue(body);
    this.hints.put(JSON_VIEW_HINT, SafeToSerialize.class);
    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();

    StepVerifier.create(response.getBodyAsString())
            .expectNext("{\"username\":\"foo\"}")
            .expectComplete()
            .verify();
  }

  @Test
  public void ofProducerWithMono() {
    Mono<User> body = Mono.just(new User("foo", "bar"));
    BodyInserter<?, ReactiveHttpOutputMessage> inserter = BodyInserters.fromProducer(body, User.class);

    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();
    StepVerifier.create(response.getBodyAsString())
            .expectNext("{\"username\":\"foo\",\"password\":\"bar\"}")
            .expectComplete()
            .verify();
  }

  @Test
  public void ofProducerWithFlux() {
    Flux<String> body = Flux.just("foo");
    BodyInserter<?, ReactiveHttpOutputMessage> inserter = BodyInserters.fromProducer(body, String.class);

    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();
    StepVerifier.create(response.getBody())
            .consumeNextWith(buf -> {
              String actual = buf.toString(UTF_8);
              assertThat(actual).isEqualTo("foo");
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void ofProducerWithSingle() {
    Single<User> body = Single.just(new User("foo", "bar"));
    BodyInserter<?, ReactiveHttpOutputMessage> inserter = BodyInserters.fromProducer(body, User.class);

    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();
    StepVerifier.create(response.getBodyAsString())
            .expectNext("{\"username\":\"foo\",\"password\":\"bar\"}")
            .expectComplete()
            .verify();
  }

  @Test
  public void ofPublisher() {
    Flux<String> body = Flux.just("foo");
    BodyInserter<Flux<String>, ReactiveHttpOutputMessage> inserter = BodyInserters.fromPublisher(body, String.class);

    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();
    StepVerifier.create(response.getBody())
            .consumeNextWith(buf -> {
              String actual = buf.toString(UTF_8);
              assertThat(actual).isEqualTo("foo");
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void ofResource() throws IOException {
    Resource resource = new ClassPathResource("response.txt", getClass());

    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = BodyInserters.fromResource(resource).insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();

    byte[] expectedBytes = Files.readAllBytes(resource.getFile().toPath());

    StepVerifier.create(response.getBody())
            .consumeNextWith(dataBuffer -> {
              byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(resultBytes);
              DataBufferUtils.release(dataBuffer);
              assertThat(resultBytes).isEqualTo(expectedBytes);
            })
            .expectComplete()
            .verify();
  }

  @Test // gh-24366
  public void ofResourceWithExplicitMediaType() throws IOException {
    Resource resource = new ClassPathResource("response.txt", getClass());

    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, "/");
    request.getHeaders().setContentType(MediaType.TEXT_MARKDOWN);
    Mono<Void> result = BodyInserters.fromResource(resource).insert(request, this.context);
    StepVerifier.create(result).expectComplete().verify();

    byte[] expectedBytes = Files.readAllBytes(resource.getFile().toPath());

    assertThat(request.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_MARKDOWN);
    StepVerifier.create(request.getBody())
            .consumeNextWith(dataBuffer -> {
              byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(resultBytes);
              DataBufferUtils.release(dataBuffer);
              assertThat(resultBytes).isEqualTo(expectedBytes);
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void ofResourceRange() throws IOException {
    final int rangeStart = 10;
    Resource body = new ClassPathResource("response.txt", getClass());
    BodyInserter<Resource, ReactiveHttpOutputMessage> inserter = BodyInserters.fromResource(body);

    MockServerHttpRequest request = MockServerHttpRequest.get("/foo")
            .range(HttpRange.createByteRange(rangeStart))
            .build();
    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, new BodyInserter.Context() {
      @Override
      public List<HttpMessageWriter<?>> messageWriters() {
        return Collections.singletonList(new ResourceHttpMessageWriter());
      }

      @Override
      public Optional<ServerHttpRequest> serverRequest() {
        return Optional.of(request);
      }

      @Override
      public Map<String, Object> hints() {
        return hints;
      }
    });
    StepVerifier.create(result).expectComplete().verify();

    byte[] allBytes = Files.readAllBytes(body.getFile().toPath());
    byte[] expectedBytes = new byte[allBytes.length - rangeStart];
    System.arraycopy(allBytes, rangeStart, expectedBytes, 0, expectedBytes.length);

    StepVerifier.create(response.getBody())
            .consumeNextWith(dataBuffer -> {
              byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(resultBytes);
              DataBufferUtils.release(dataBuffer);
              assertThat(resultBytes).isEqualTo(expectedBytes);
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void ofServerSentEventFlux() {
    ServerSentEvent<String> event = ServerSentEvent.builder("foo").build();
    Flux<ServerSentEvent<String>> body = Flux.just(event);
    BodyInserter<Flux<ServerSentEvent<String>>, ServerHttpResponse> inserter =
            BodyInserters.fromServerSentEvents(body);

    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectNextCount(0).expectComplete().verify();
  }

  @Test
  public void fromFormDataMap() {
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.setOrRemove("name 1", "value 1");
    body.add("name 2", "value 2+1");
    body.add("name 2", "value 2+2");
    body.add("name 3", null);

    BodyInserter<MultiValueMap<String, String>, ClientHttpRequest>
            inserter = BodyInserters.fromFormData(body);

    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com"));
    Mono<Void> result = inserter.insert(request, this.context);
    StepVerifier.create(result).expectComplete().verify();

    StepVerifier.create(request.getBody())
            .consumeNextWith(dataBuffer -> {
              byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(resultBytes);
              DataBufferUtils.release(dataBuffer);
              assertThat(resultBytes).isEqualTo("name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3".getBytes(StandardCharsets.UTF_8));
            })
            .expectComplete()
            .verify();

  }

  @Test
  public void fromFormDataWith() {
    BodyInserter<MultiValueMap<String, String>, ClientHttpRequest>
            inserter = BodyInserters.fromFormData("name 1", "value 1")
            .with("name 2", "value 2+1")
            .with("name 2", "value 2+2")
            .with("name 3", null);

    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com"));
    Mono<Void> result = inserter.insert(request, this.context);
    StepVerifier.create(result).expectComplete().verify();

    StepVerifier.create(request.getBody())
            .consumeNextWith(dataBuffer -> {
              byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(resultBytes);
              DataBufferUtils.release(dataBuffer);
              assertThat(resultBytes).isEqualTo("name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3".getBytes(StandardCharsets.UTF_8));
            })
            .expectComplete()
            .verify();

  }

  @Test
  public void fromMultipartData() {
    MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
    map.setOrRemove("name 3", "value 3");

    BodyInserters.FormInserter<Object> inserter =
            BodyInserters.fromMultipartData("name 1", "value 1")
                    .withPublisher("name 2", Flux.just("foo", "bar", "baz"), String.class)
                    .with(map);

    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com"));
    Mono<Void> result = inserter.insert(request, this.context);
    StepVerifier.create(result).expectComplete().verify();

  }

  @Test  // SPR-16350
  public void fromMultipartDataWithMultipleValues() {
    MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
    map.put("name", Arrays.asList("value1", "value2"));
    BodyInserters.FormInserter<Object> inserter = BodyInserters.fromMultipartData(map);

    MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com"));
    Mono<Void> result = inserter.insert(request, this.context);
    StepVerifier.create(result).expectComplete().verify();

    StepVerifier.create(DataBufferUtils.join(request.getBody()))
            .consumeNextWith(dataBuffer -> {
              byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(resultBytes);
              DataBufferUtils.release(dataBuffer);
              String content = new String(resultBytes, StandardCharsets.UTF_8);
              assertThat(content).contains("Content-Disposition: form-data; name=\"name\"\r\n" +
                      "Content-Type: text/plain;charset=UTF-8\r\n" +
                      "Content-Length: 6\r\n" +
                      "\r\n" +
                      "value1");
              assertThat(content).contains("Content-Disposition: form-data; name=\"name\"\r\n" +
                      "Content-Type: text/plain;charset=UTF-8\r\n" +
                      "Content-Length: 6\r\n" +
                      "\r\n" +
                      "value2");
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void ofDataBuffers() {
    byte[] bytes = "foo".getBytes(UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    BodyInserter<Flux<DataBuffer>, ReactiveHttpOutputMessage> inserter = BodyInserters.fromDataBuffers(body);

    MockServerHttpResponse response = new MockServerHttpResponse();
    Mono<Void> result = inserter.insert(response, this.context);
    StepVerifier.create(result).expectComplete().verify();

    StepVerifier.create(response.getBody())
            .expectNext(dataBuffer)
            .expectComplete()
            .verify();
  }

  interface SafeToSerialize { }

  @SuppressWarnings("unused")
  private static class User {

    @JsonView(SafeToSerialize.class)
    private String username;

    private String password;

    public User() {
    }

    public User(String username, String password) {
      this.username = username;
      this.password = password;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

}
