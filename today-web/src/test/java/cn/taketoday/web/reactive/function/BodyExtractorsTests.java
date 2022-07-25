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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.codec.ByteBufferDecoder;
import cn.taketoday.core.codec.StringDecoder;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBuffer;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.core.io.buffer.NettyDataBuffer;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ReactiveHttpInputMessage;
import cn.taketoday.http.codec.DecoderHttpMessageReader;
import cn.taketoday.http.codec.FormHttpMessageReader;
import cn.taketoday.http.codec.HttpMessageReader;
import cn.taketoday.http.codec.json.Jackson2JsonDecoder;
import cn.taketoday.http.codec.multipart.DefaultPartHttpMessageReader;
import cn.taketoday.http.codec.multipart.FilePart;
import cn.taketoday.http.codec.multipart.FormFieldPart;
import cn.taketoday.http.codec.multipart.MultipartHttpMessageReader;
import cn.taketoday.http.codec.multipart.Part;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.web.testfixture.http.client.reactive.MockClientHttpResponse;
import cn.taketoday.web.testfixture.http.server.reactive.MockServerHttpRequest;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.IllegalReferenceCountException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static cn.taketoday.http.codec.json.Jackson2CodecSupport.JSON_VIEW_HINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
public class BodyExtractorsTests {

  private BodyExtractor.Context context;

  private Map<String, Object> hints;

  private Optional<ServerHttpResponse> serverResponse = Optional.empty();

  @BeforeEach
  public void createContext() {
    final List<HttpMessageReader<?>> messageReaders = new ArrayList<>();
    messageReaders.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
    messageReaders.add(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
//		messageReaders.add(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
    messageReaders.add(new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));
    messageReaders.add(new FormHttpMessageReader());
    DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
    messageReaders.add(partReader);
    messageReaders.add(new MultipartHttpMessageReader(partReader));

    messageReaders.add(new FormHttpMessageReader());

    this.context = new BodyExtractor.Context() {
      @Override
      public List<HttpMessageReader<?>> messageReaders() {
        return messageReaders;
      }

      @Override
      public Optional<ServerHttpResponse> serverResponse() {
        return serverResponse;
      }

      @Override
      public Map<String, Object> hints() {
        return hints;
      }
    };
    this.hints = new HashMap<>();
  }

  @Test
  public void toMono() {
    BodyExtractor<Mono<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(String.class);

    DefaultDataBufferFactory factory = DefaultDataBufferFactory.sharedInstance;
    DefaultDataBuffer dataBuffer =
            factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);
    Mono<String> result = extractor.extract(request, this.context);

    StepVerifier.create(result)
            .expectNext("foo")
            .expectComplete()
            .verify();
  }

  @Test
  public void toMonoParameterizedTypeReference() {
    BodyExtractor<Mono<Map<String, String>>, ReactiveHttpInputMessage> extractor =
            BodyExtractors.toMono(new TypeReference<Map<String, String>>() { });

    byte[] bytes = "{\"username\":\"foo\",\"password\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON).body(body);
    Mono<Map<String, String>> result = extractor.extract(request, this.context);

    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("username", "foo");
    expected.put("password", "bar");
    StepVerifier.create(result)
            .expectNext(expected)
            .expectComplete()
            .verify();
  }

  @Test
  public void toMonoWithHints() {
    BodyExtractor<Mono<User>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(User.class);
    this.hints.put(JSON_VIEW_HINT, SafeToDeserialize.class);

    byte[] bytes = "{\"username\":\"foo\",\"password\":\"bar\"}".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body);

    Mono<User> result = extractor.extract(request, this.context);

    StepVerifier.create(result)
            .consumeNextWith(user -> {
              assertThat(user.getUsername()).isEqualTo("foo");
              assertThat(user.getPassword()).isNull();
            })
            .expectComplete()
            .verify();
  }

  @Test  // SPR-15758
  public void toMonoWithEmptyBodyAndNoContentType() {
    BodyExtractor<Mono<Map<String, String>>, ReactiveHttpInputMessage> extractor =
            BodyExtractors.toMono(new TypeReference<Map<String, String>>() { });

    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(Flux.empty());
    Mono<Map<String, String>> result = extractor.extract(request, this.context);

    StepVerifier.create(result).expectComplete().verify();
  }

  @Test
  public void toMonoVoidAsClientShouldConsumeAndCancel() {
    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    TestPublisher<DataBuffer> body = TestPublisher.create();

    BodyExtractor<Mono<Void>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(Void.class);
    MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
    response.setBody(body.flux());

    StepVerifier.create(extractor.extract(response, this.context))
            .then(() -> {
              body.assertWasSubscribed();
              body.emit(dataBuffer);
            })
            .verifyComplete();

    body.assertCancelled();
  }

  @Test
  public void toMonoVoidAsClientWithEmptyBody() {
    TestPublisher<DataBuffer> body = TestPublisher.create();

    BodyExtractor<Mono<Void>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(Void.class);
    MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
    response.setBody(body.flux());

    StepVerifier.create(extractor.extract(response, this.context))
            .then(() -> {
              body.assertWasSubscribed();
              body.complete();
            })
            .verifyComplete();
  }

  @Test
  public void toFlux() {
    BodyExtractor<Flux<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toFlux(String.class);

    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);
    Flux<String> result = extractor.extract(request, this.context);

    StepVerifier.create(result)
            .expectNext("foo")
            .expectComplete()
            .verify();
  }

  @Test
  public void toFluxWithHints() {
    BodyExtractor<Flux<User>, ReactiveHttpInputMessage> extractor = BodyExtractors.toFlux(User.class);
    this.hints.put(JSON_VIEW_HINT, SafeToDeserialize.class);

    String text = "[{\"username\":\"foo\",\"password\":\"bar\"},{\"username\":\"bar\",\"password\":\"baz\"}]";
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body);

    Flux<User> result = extractor.extract(request, this.context);

    StepVerifier.create(result)
            .consumeNextWith(user -> {
              assertThat(user.getUsername()).isEqualTo("foo");
              assertThat(user.getPassword()).isNull();
            })
            .consumeNextWith(user -> {
              assertThat(user.getUsername()).isEqualTo("bar");
              assertThat(user.getPassword()).isNull();
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void toFluxUnacceptable() {
    BodyExtractor<Flux<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toFlux(String.class);

    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body);

    BodyExtractor.Context emptyContext = new BodyExtractor.Context() {
      @Override
      public List<HttpMessageReader<?>> messageReaders() {
        return Collections.emptyList();
      }

      @Override
      public Optional<ServerHttpResponse> serverResponse() {
        return Optional.empty();
      }

      @Override
      public Map<String, Object> hints() {
        return Collections.emptyMap();
      }
    };

    Flux<String> result = extractor.extract(request, emptyContext);
    StepVerifier.create(result)
            .expectError(UnsupportedMediaTypeException.class)
            .verify();
  }

  @Test
  public void toFormData() {
    byte[] bytes = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body);

    Mono<MultiValueMap<String, String>> result = BodyExtractors.toFormData().extract(request, this.context);

    StepVerifier.create(result)
            .consumeNextWith(form -> {
              assertThat(form.size()).as("Invalid result").isEqualTo(3);
              assertThat(form.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
              List<String> values = form.get("name 2");
              assertThat(values.size()).as("Invalid result").isEqualTo(2);
              assertThat(values.get(0)).as("Invalid result").isEqualTo("value 2+1");
              assertThat(values.get(1)).as("Invalid result").isEqualTo("value 2+2");
              assertThat(form.getFirst("name 3")).as("Invalid result").isNull();
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void toParts() {
    BodyExtractor<Flux<Part>, ServerHttpRequest> extractor = BodyExtractors.toParts();

    String bodyContents = "-----------------------------9051914041544843365972754266\r\n" +
            "Content-Disposition: form-data; name=\"text\"\r\n" +
            "\r\n" +
            "text default\r\n" +
            "-----------------------------9051914041544843365972754266\r\n" +
            "Content-Disposition: form-data; name=\"file1\"; filename=\"a.txt\"\r\n" +
            "Content-Type: text/plain\r\n" +
            "\r\n" +
            "Content of a.txt.\r\n" +
            "\r\n" +
            "-----------------------------9051914041544843365972754266\r\n" +
            "Content-Disposition: form-data; name=\"file2\"; filename=\"a.html\"\r\n" +
            "Content-Type: text/html\r\n" +
            "\r\n" +
            "<!DOCTYPE html><title>Content of a.html.</title>\r\n" +
            "\r\n" +
            "-----------------------------9051914041544843365972754266--\r\n";

    byte[] bytes = bodyContents.getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .header("Content-Type", "multipart/form-data; boundary=---------------------------9051914041544843365972754266")
            .body(body);

    Flux<Part> result = extractor.extract(request, this.context);

    StepVerifier.create(result)
            .consumeNextWith(part -> {
              assertThat(part.name()).isEqualTo("text");
              boolean condition = part instanceof FormFieldPart;
              assertThat(condition).isTrue();
              FormFieldPart formFieldPart = (FormFieldPart) part;
              assertThat(formFieldPart.value()).isEqualTo("text default");
            })
            .consumeNextWith(part -> {
              assertThat(part.name()).isEqualTo("file1");
              boolean condition = part instanceof FilePart;
              assertThat(condition).isTrue();
              FilePart filePart = (FilePart) part;
              assertThat(filePart.filename()).isEqualTo("a.txt");
              assertThat(filePart.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
            })
            .consumeNextWith(part -> {
              assertThat(part.name()).isEqualTo("file2");
              boolean condition = part instanceof FilePart;
              assertThat(condition).isTrue();
              FilePart filePart = (FilePart) part;
              assertThat(filePart.filename()).isEqualTo("a.html");
              assertThat(filePart.headers().getContentType()).isEqualTo(MediaType.TEXT_HTML);
            })
            .expectComplete()
            .verify();
  }

  @Test
  public void toDataBuffers() {
    BodyExtractor<Flux<DataBuffer>, ReactiveHttpInputMessage> extractor = BodyExtractors.toDataBuffers();

    byte[] bytes = "foo".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(body);
    Flux<DataBuffer> result = extractor.extract(request, this.context);

    StepVerifier.create(result)
            .expectNext(dataBuffer)
            .expectComplete()
            .verify();
  }

  @Test // SPR-17054
  public void unsupportedMediaTypeShouldConsumeAndCancel() {
    NettyDataBufferFactory factory = new NettyDataBufferFactory(new PooledByteBufAllocator(true));
    NettyDataBuffer buffer = factory.wrap(ByteBuffer.wrap("spring".getBytes(StandardCharsets.UTF_8)));
    TestPublisher<DataBuffer> body = TestPublisher.create();

    MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
    response.getHeaders().setContentType(MediaType.APPLICATION_PDF);
    response.setBody(body.flux());

    BodyExtractor<Mono<User>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(User.class);
    StepVerifier.create(extractor.extract(response, this.context))
            .then(() -> {
              body.assertWasSubscribed();
              body.emit(buffer);
            })
            .expectErrorSatisfies(throwable -> {
              boolean condition = throwable instanceof UnsupportedMediaTypeException;
              assertThat(condition).isTrue();
              assertThatExceptionOfType(IllegalReferenceCountException.class).isThrownBy(
                      buffer::release);
              body.assertCancelled();
            }).verify();
  }

  interface SafeToDeserialize { }

  @SuppressWarnings("unused")
  private static class User {

    @JsonView(SafeToDeserialize.class)
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
