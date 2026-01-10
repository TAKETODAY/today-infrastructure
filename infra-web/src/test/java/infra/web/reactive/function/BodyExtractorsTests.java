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

package infra.web.reactive.function;

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

import infra.core.ParameterizedTypeReference;
import infra.core.codec.ByteBufferDecoder;
import infra.core.codec.StringDecoder;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DefaultDataBuffer;
import infra.core.io.buffer.DefaultDataBufferFactory;
import infra.core.io.buffer.NettyDataBuffer;
import infra.core.io.buffer.NettyDataBufferFactory;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ReactiveHttpInputMessage;
import infra.http.codec.DecoderHttpMessageReader;
import infra.http.codec.FormHttpMessageReader;
import infra.http.codec.HttpMessageReader;
import infra.http.codec.json.JacksonJsonDecoder;
import infra.http.codec.multipart.DefaultPartHttpMessageReader;
import infra.http.codec.multipart.FilePart;
import infra.http.codec.multipart.FormFieldPart;
import infra.http.codec.multipart.MultipartHttpMessageReader;
import infra.http.codec.multipart.Part;
import infra.http.server.reactive.ServerHttpRequest;
import infra.http.server.reactive.ServerHttpResponse;
import infra.util.MultiValueMap;
import infra.web.testfixture.http.client.reactive.MockClientHttpResponse;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.IllegalReferenceCountException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static infra.http.codec.JacksonCodecSupport.JSON_VIEW_HINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
class BodyExtractorsTests {

  private BodyExtractor.Context context;

  private Map<String, Object> hints;

  private Optional<ServerHttpResponse> serverResponse = Optional.empty();

  @BeforeEach
  void createContext() {
    final List<HttpMessageReader<?>> messageReaders = new ArrayList<>();
    messageReaders.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
    messageReaders.add(new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
    messageReaders.add(new DecoderHttpMessageReader<>(new JacksonJsonDecoder()));
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
  void toMono() {
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
  void toMonoParameterizedTypeReference() {
    BodyExtractor<Mono<Map<String, String>>, ReactiveHttpInputMessage> extractor =
            BodyExtractors.toMono(new ParameterizedTypeReference<>() { });

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
  void toMonoWithHints() {
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

  @Test
  void toMonoWithEmptyBodyAndNoContentType() {
    BodyExtractor<Mono<Map<String, String>>, ReactiveHttpInputMessage> extractor =
            BodyExtractors.toMono(new ParameterizedTypeReference<>() { });

    MockServerHttpRequest request = MockServerHttpRequest.post("/").body(Flux.empty());
    Mono<Map<String, String>> result = extractor.extract(request, this.context);

    StepVerifier.create(result).expectComplete().verify();
  }

  @Test
  void toMonoVoidAsClientShouldConsumeAndCancel() {
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
  void toMonoVoidAsClientWithEmptyBody() {
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
  void toFlux() {
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
  void toFluxWithHints() {
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
  void toFluxUnacceptable() {
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
  void toFormData() {
    byte[] bytes = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3".getBytes(StandardCharsets.UTF_8);
    DefaultDataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(ByteBuffer.wrap(bytes));
    Flux<DataBuffer> body = Flux.just(dataBuffer);

    MockServerHttpRequest request = MockServerHttpRequest.post("/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body);

    Mono<MultiValueMap<String, String>> result = BodyExtractors.toFormData().extract(request, this.context);

    StepVerifier.create(result)
            .consumeNextWith(form -> {
              assertThat(form).as("Invalid result").hasSize(3);
              assertThat(form.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
              List<String> values = form.get("name 2");
              assertThat(values).as("Invalid result").containsExactly("value 2+1", "value 2+2");
              assertThat(form.getFirst("name 3")).as("Invalid result").isNull();
            })
            .expectComplete()
            .verify();
  }

  @Test
  void toParts() {
    BodyExtractor<Flux<Part>, ServerHttpRequest> extractor = BodyExtractors.toParts();

    String bodyContents = """
            -----------------------------9051914041544843365972754266
            Content-Disposition: form-data; name="text"
            
            text default
            -----------------------------9051914041544843365972754266
            Content-Disposition: form-data; name="file1"; filename="a.txt"
            Content-Type: text/plain
            
            Content of a.txt.
            
            -----------------------------9051914041544843365972754266
            Content-Disposition: form-data; name="file2"; filename="a.html"
            Content-Type: text/html
            
            <!DOCTYPE html><title>Content of a.html.</title>
            
            -----------------------------9051914041544843365972754266--
            """.replace("\n", "\r\n");

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
              assertThat(part).asInstanceOf(type(FormFieldPart.class)).satisfies(
                      formFieldPart -> assertThat(formFieldPart.value()).isEqualTo("text default"));
            })
            .consumeNextWith(part -> {
              assertThat(part.name()).isEqualTo("file1");
              assertThat(part).asInstanceOf(type(FilePart.class)).satisfies(
                      filePart -> assertThat(filePart.filename()).isEqualTo("a.txt"),
                      filePart -> assertThat(filePart.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN));
            })
            .consumeNextWith(part -> {
              assertThat(part.name()).isEqualTo("file2");
              assertThat(part).asInstanceOf(type(FilePart.class)).satisfies(
                      filePart -> assertThat(filePart.filename()).isEqualTo("a.html"),
                      filePart -> assertThat(filePart.headers().getContentType()).isEqualTo(MediaType.TEXT_HTML));
            })
            .expectComplete()
            .verify();
  }

  @Test
  void toDataBuffers() {
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

  @Test
  void unsupportedMediaTypeShouldConsumeAndCancel() {
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
              assertThat(buffer.release()).isFalse();
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
