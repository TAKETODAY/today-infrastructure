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

package infra.http.codec;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonView;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import infra.core.ResolvableType;
import infra.core.codec.CodecException;
import infra.core.codec.DecodingException;
import infra.core.codec.EncodingException;
import infra.core.codec.StringDecoder;
import infra.http.MediaType;
import infra.http.ProblemDetail;
import infra.http.codec.json.JacksonJsonDecoder;
import infra.http.codec.json.JacksonJsonEncoder;
import infra.web.testfixture.http.server.reactive.MockServerHttpRequest;
import infra.web.testfixture.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JacksonHttpMessageCodecTests {

  private static final ResolvableType DOCUMENT = ResolvableType.forClass(Document.class);

  private static final Duration TIMEOUT = Duration.ofSeconds(5);

  private final MockServerHttpResponse response = new MockServerHttpResponse();

  @Test
  void advertisesTheProblemJsonMediaTypeAndSerializesProblemExtensions() {
    var encoder = new JacksonJsonEncoder(JsonMapper.builder());
    var type = ResolvableType.forClass(ProblemDetail.class);
    assertThat(encoder.getEncodableMimeTypes(type)).containsExactly(MediaType.APPLICATION_PROBLEM_JSON);
    var problem = ProblemDetail.forRawStatusCode(422);
    problem.setProperty("ticket", "123");

    StepVerifier.create(new EncoderHttpMessageWriter<>(encoder).write(Mono.just(problem), type,
                    MediaType.APPLICATION_PROBLEM_JSON, this.response, Map.of()))
            .expectComplete().verify(TIMEOUT);
    assertThat(this.response.getBodyAsString().block(TIMEOUT)).contains("\"status\":422", "\"ticket\":\"123\"")
            .doesNotContain("\"properties\"");
  }

  @Test
  void keepsGenericCollectionsIntactAndAppliesPropertyFilters() {
    var type = ResolvableType.forClassWithGenerics(List.class, FilteredDocument.class);
    var doc = new FilteredDocument("public", "private");
    var encoder = new EncoderHttpMessageWriter<>(new JacksonJsonEncoder());
    var filters = new SimpleFilterProvider().addFilter("documents", SimpleBeanPropertyFilter.filterOutAllExcept("name"));
    StepVerifier.create(encoder.write(Mono.just(List.of(doc)), type, MediaType.APPLICATION_JSON,
                    this.response, Map.of(JacksonCodecSupport.FILTER_PROVIDER_HINT, filters)))
            .expectComplete().verify(TIMEOUT);
    assertThat(this.response.getBodyAsString().block(TIMEOUT)).isEqualTo("[{\"name\":\"public\"}]");

    var decoder = new DecoderHttpMessageReader<>(new JacksonJsonDecoder(JsonMapper.builder()));
    StepVerifier.create(decoder.read(type, jsonRequest("[{\"name\":\"public\"}]"), Map.of()))
            .expectNext(List.of(new FilteredDocument("public", null))).expectComplete().verify(TIMEOUT);
  }

  @Test
  void rejectsInvalidJsonViewAnnotationsOnServerArguments() throws Exception {
    var decoder = new DecoderHttpMessageReader<>(new JacksonJsonDecoder());
    var invalid = ResolvableType.forMethodParameter(getClass().getDeclaredMethod("invalidView", Document.class), 0);
    assertThatIllegalArgumentException().isThrownBy(() ->
            decoder.readMono(invalid, DOCUMENT, jsonRequest("{}"), this.response, Map.of()))
            .withMessageContaining("@JsonView");
  }

  @Test
  void reportsTypeDefinitionAndSerializationFailuresForSingleAndStreamingWrites() {
    var mapper = JsonMapper.builder().enable(SerializationFeature.FAIL_ON_EMPTY_BEANS).build();
    var writer = new EncoderHttpMessageWriter<>(new JacksonJsonEncoder(mapper));
    for (boolean streaming : List.of(false, true)) {
      var undefined = new EmptyBean();
      var failing = new FailingBean();
      StepVerifier.create(writer.write(streaming ? Flux.just(undefined) : Mono.just(undefined),
                      ResolvableType.forClass(EmptyBean.class), MediaType.APPLICATION_JSON, new MockServerHttpResponse(), Map.of()))
              .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(CodecException.class)
                      .hasMessageContaining("Type definition error"))
              .verify(TIMEOUT);
      StepVerifier.create(writer.write(streaming ? Flux.just(failing) : Mono.just(failing),
                      ResolvableType.forClass(FailingBean.class), MediaType.APPLICATION_JSON, new MockServerHttpResponse(), Map.of()))
              .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(EncodingException.class)
                      .hasMessageContaining("property unavailable"))
              .verify(TIMEOUT);
    }
  }

  @Test
  void serverReaderAppliesJsonViewToGenericRequestForSingleAndStreamingReads() throws Exception {
    var method = JacksonHttpMessageCodecTests.class.getDeclaredMethod("receive", List.class);
    var actualType = ResolvableType.forMethodParameter(method, 0);
    var reader = new DecoderHttpMessageReader<>(new JacksonJsonDecoder());
    String json = "{\"displayName\":\"sample\",\"internalNote\":\"private\"}";
    MockServerHttpRequest request = jsonRequest(json);
    Document expected = new Document("sample", null);

    StepVerifier.create(reader.readMono(actualType, DOCUMENT, request, this.response, Map.of()))
            .expectNext(expected).expectComplete().verify(TIMEOUT);
    StepVerifier.create(reader.read(actualType, DOCUMENT, jsonRequest("[" + json + "]"), this.response, Map.of())
                    .contextWrite(context -> context.put("request-id", "sample")))
            .expectNext(expected).expectComplete().verify(TIMEOUT);
  }

  @Test
  void serverWriterAppliesJsonViewToGenericResponse() throws Exception {
    var actualType = ResolvableType.forMethodParameter(JacksonHttpMessageCodecTests.class.getDeclaredMethod("send"), -1);
    var writer = new EncoderHttpMessageWriter<>(new JacksonJsonEncoder());

    StepVerifier.create(writer.write(Flux.just(new Document("example", "private")), actualType, DOCUMENT,
                    MediaType.APPLICATION_JSON, jsonRequest(""), this.response, Map.of()))
            .expectComplete().verify(TIMEOUT);

    assertThat(this.response.getBodyAsString().block(TIMEOUT)).isEqualTo("[{\"displayName\":\"example\"}]");
  }

  @Test
  void customMapperIsSelectedOnlyForItsRegisteredTypeAndMediaType() {
    MediaType mediaType = MediaType.parseMediaType("application/vnd.example+json");
    JsonMapper mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();
    var decoder = new JacksonJsonDecoder();
    var encoder = new JacksonJsonEncoder();
    decoder.registerMappersForType(Document.class, registrations -> registrations.put(mediaType, mapper));
    encoder.registerMappersForType(Document.class, registrations -> registrations.put(mediaType, mapper));
    var reader = new DecoderHttpMessageReader<>(decoder);
    var writer = new EncoderHttpMessageWriter<>(encoder);

    assertThat(reader.getReadableMediaTypes(DOCUMENT)).containsExactly(mediaType);
    assertThat(writer.getWritableMediaTypes(DOCUMENT)).containsExactly(mediaType);
    assertThat(decoder.getMappersForType(Document.class)).containsEntry(mediaType, mapper);
    assertThat(encoder.getMappersForType(String.class)).isEmpty();
    assertThat(reader.canRead(DOCUMENT, MediaType.APPLICATION_JSON)).isFalse();
    assertThat(writer.canWrite(DOCUMENT, MediaType.APPLICATION_JSON)).isFalse();
    assertThat(reader.canRead(ResolvableType.forClass(Map.class), MediaType.APPLICATION_JSON)).isTrue();

    var request = MockServerHttpRequest.post("/").contentType(mediaType).body("{\"display_name\":\"sample\"}");
    var document = new Document("sample", null);
    StepVerifier.create(reader.readMono(DOCUMENT, request, Map.of()))
            .expectNext(document).expectComplete().verify(TIMEOUT);
    StepVerifier.create(writer.write(Mono.just(document), DOCUMENT, mediaType, this.response, Map.of()))
            .expectComplete().verify(TIMEOUT);
    assertThat(this.response.getBodyAsString().block(TIMEOUT)).contains("\"display_name\":\"sample\"");

    StepVerifier.create(reader.read(DOCUMENT, jsonRequest("{}"), Map.of()))
            .expectErrorMessage("No ObjectMapper for " + DOCUMENT).verify(TIMEOUT);
    StepVerifier.create(writer.write(Flux.just(document), DOCUMENT, MediaType.APPLICATION_JSON, new MockServerHttpResponse(), Map.of()))
            .expectErrorMessage("No ObjectMapper for " + DOCUMENT).verify(TIMEOUT);
  }

  @Test
  void readerUsesAnOctetStreamFallbackWithAnOrdinaryDecoder() {
    var reader = new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes());
    var string = ResolvableType.forClass(String.class);
    var request = MockServerHttpRequest.post("/").body("alpha\nbeta\n");
    StepVerifier.create(reader.read(string, string, request, this.response, Map.of()))
            .expectNext("alpha", "beta").expectComplete().verify(TIMEOUT);
  }

  @Test
  void reportsTypeMismatchInBothAggregatedAndStreamingJson() {
    var reader = new DecoderHttpMessageReader<>(new JacksonJsonDecoder());
    String json = "{\"displayName\":[1,2]}";
    StepVerifier.create(reader.readMono(DOCUMENT, jsonRequest(json), Map.of()))
            .expectError(DecodingException.class).verify(TIMEOUT);
    StepVerifier.create(reader.read(DOCUMENT, jsonRequest("[" + json + "]"), Map.of()))
            .expectError(DecodingException.class).verify(TIMEOUT);
  }

  private static MockServerHttpRequest jsonRequest(String content) {
    return MockServerHttpRequest.post("/").contentType(MediaType.APPLICATION_JSON).body(content);
  }

  private void receive(@JsonView(PublicView.class) List<Document> documents) {
  }

  private void invalidView(@JsonView({ PublicView.class, InternalView.class }) Document document) {
  }

  @JsonView(PublicView.class)
  private List<Document> send() {
    return List.of();
  }

  private interface PublicView { }

  private interface InternalView extends PublicView { }

  record Document(@JsonView(PublicView.class) String displayName, @JsonView(InternalView.class) String internalNote) { }

  @JsonFilter("documents")
  record FilteredDocument(String name, String note) { }

  static class EmptyBean { }

  static class FailingBean {

    public String getName() {
      throw new IllegalStateException("property unavailable");
    }
  }
}
