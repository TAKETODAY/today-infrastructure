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

package infra.web.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import infra.core.Pair;
import infra.core.ParameterizedTypeReference;
import infra.http.HttpHeaders;
import infra.http.HttpRequest;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.StreamingHttpOutputMessage;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.client.ClientHttpResponse;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.client.ReactorClientHttpRequestFactory;
import infra.util.CollectionUtils;
import infra.util.FastByteArrayOutputStream;
import infra.util.FileCopyUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.concurrent.Future;
import infra.web.testfixture.Pojo;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Named.named;

/**
 * Integration tests for {@link RestClient}.
 *
 * @author Arjen Poutsma
 */
class RestClientIntegrationTests {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("clientHttpRequestFactories")
  @interface ParameterizedRestClientTest {
  }

  static Stream<Named<ClientHttpRequestFactory>> clientHttpRequestFactories() {
    return Stream.of(
            named("HttpComponents", new HttpComponentsClientHttpRequestFactory()),
            named("JDK HttpClient", new JdkClientHttpRequestFactory()),
            named("Reactor Netty", new ReactorClientHttpRequestFactory())
    );
  }

  private MockWebServer server;

  private RestClient restClient;

  private void startServer(ClientHttpRequestFactory requestFactory) {
    this.server = new MockWebServer();
    this.restClient = RestClient
            .builder()
            .requestFactory(requestFactory)
            .baseURI(this.server.url("/").toString())
            .build();
  }

  @AfterEach
  void shutdown() throws IOException {
    if (server != null) {
      this.server.shutdown();
    }
  }

  @ParameterizedRestClientTest
  void retrieve(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response ->
            response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

    String result = this.restClient.get()
            .uri("/greeting")
            .header("X-Test-Header", "testvalue")
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getHeader("X-Test-Header")).isEqualTo("testvalue");
      assertThat(request.getPath()).isEqualTo("/greeting");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJson(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json")
            .setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

    Pojo result = this.restClient.get()
            .uri("/pojo")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(Pojo.class);

    assertThat(result.getFoo()).isEqualTo("foofoo");
    assertThat(result.getBar()).isEqualTo("barbar");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/pojo");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonWithParameterizedTypeReference(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "{\"containerValue\":{\"bar\":\"barbar\",\"foo\":\"foofoo\"}}";
    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody(content));

    ValueContainer<Pojo> result = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new ParameterizedTypeReference<ValueContainer<Pojo>>() { });

    assertThat(result.getContainerValue()).isNotNull();
    Pojo pojo = result.getContainerValue();
    assertThat(pojo.getFoo()).isEqualTo("foofoo");
    assertThat(pojo.getBar()).isEqualTo("barbar");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonWithListParameterizedTypeReference(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "{\"containerValue\":[{\"bar\":\"barbar\",\"foo\":\"foofoo\"}]}";
    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody(content));

    ValueContainer<List<Pojo>> result = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new ParameterizedTypeReference<ValueContainer<List<Pojo>>>() { });

    assertThat(result.containerValue).isNotNull();
    assertThat(result.containerValue).containsExactly(new Pojo("foofoo", "barbar"));

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonAsResponseEntity(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody(content));

    ResponseEntity<String> result = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(String.class);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.headers().getContentLength()).isEqualTo(31);
    assertThat(result.getBody()).isEqualTo(content);

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonAsBodilessEntity(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

    ResponseEntity<Void> result = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toBodilessEntity();

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.headers().getContentLength()).isEqualTo(31);
    assertThat(result.getBody()).isNull();

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonArray(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json")
            .setBody("[{\"bar\":\"bar1\",\"foo\":\"foo1\"},{\"bar\":\"bar2\",\"foo\":\"foo2\"}]"));

    List<Pojo> result = this.restClient.get()
            .uri("/pojos")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new ParameterizedTypeReference<>() { });

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getFoo()).isEqualTo("foo1");
    assertThat(result.get(0).getBar()).isEqualTo("bar1");
    assertThat(result.get(1).getFoo()).isEqualTo("foo2");
    assertThat(result.get(1).getBar()).isEqualTo("bar2");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/pojos");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonArrayAsResponseEntityList(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "[{\"bar\":\"bar1\",\"foo\":\"foo1\"}, {\"bar\":\"bar2\",\"foo\":\"foo2\"}]";
    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody(content));

    ResponseEntity<List<Pojo>> result = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<>() { });

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.headers().getContentLength()).isEqualTo(58);
    assertThat(result.getBody()).hasSize(2);
    assertThat(result.getBody().get(0).getFoo()).isEqualTo("foo1");
    assertThat(result.getBody().get(0).getBar()).isEqualTo("bar1");
    assertThat(result.getBody().get(1).getFoo()).isEqualTo("foo2");
    assertThat(result.getBody().get(1).getBar()).isEqualTo("bar2");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonAsSerializedText(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody(content));

    String result = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo(content);

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void retrieveJsonNull(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("null"));

    Map result = this.restClient.get()
            .uri("/null")
            .retrieve()
            .body(Map.class);

    assertThat(result).isNull();
  }

  @ParameterizedRestClientTest
  void retrieveJsonEmpty(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

    Pojo result = this.restClient.get()
            .uri("/null")
            .retrieve()
            .body(Pojo.class);

    assertThat(result).isNull();
  }

  @ParameterizedRestClientTest
  void retrieve404(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(404)
            .setHeader("Content-Type", "text/plain"));

    assertThatExceptionOfType(HttpClientErrorException.NotFound.class).isThrownBy(() ->
            this.restClient.get().uri("/greeting")
                    .retrieve()
                    .body(String.class)
    );

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/greeting"));

  }

  @ParameterizedRestClientTest
  void retrieve404WithBody(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(404)
            .setHeader("Content-Type", "text/plain").setBody("Not Found"));

    assertThatExceptionOfType(HttpClientErrorException.NotFound.class).isThrownBy(() ->
            this.restClient.get("/greeting")
                    .retrieve()
                    .body(String.class)
    );

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/greeting"));
  }

  @ParameterizedRestClientTest
  void retrieve500(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String errorMessage = "Internal Server error";
    prepareResponse(response -> response.setResponseCode(500)
            .setHeader("Content-Type", "text/plain").setBody(errorMessage));

    String path = "/greeting";
    try {
      this.restClient.get()
              .uri(path)
              .retrieve()
              .body(String.class);
    }
    catch (HttpServerErrorException ex) {
      assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assumeFalse(requestFactory instanceof JdkClientHttpRequestFactory, "JDK HttpClient does not expose status text");
      assertThat(ex.getStatusText()).isEqualTo("Server Error");
      assertThat(ex.getResponseHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
      assertThat(ex.getResponseBodyAsString()).isEqualTo(errorMessage);
    }

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo(path));
  }

  @ParameterizedRestClientTest
  void retrieve500AsEntity(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(500)
            .setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

    assertThatExceptionOfType(HttpServerErrorException.InternalServerError.class).isThrownBy(() ->
            this.restClient.get()
                    .uri("/").accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(String.class)
    );

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieve500AsBodilessEntity(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(500)
            .setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

    assertThatExceptionOfType(HttpServerErrorException.InternalServerError.class).isThrownBy(() ->
            this.restClient.get("/")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity()
    );

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieve555UnknownStatus(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    int errorStatus = 555;
    assertThat(HttpStatus.resolve(errorStatus)).isNull();
    String errorMessage = "Something went wrong";
    prepareResponse(response -> response.setResponseCode(errorStatus)
            .setHeader("Content-Type", "text/plain").setBody(errorMessage));

    try {
      this.restClient.get()
              .uri("/unknownPage")
              .retrieve()
              .body(String.class);

    }
    catch (HttpServerErrorException ex) {
      assumeFalse(requestFactory instanceof JdkClientHttpRequestFactory, "JDK HttpClient does not expose status text");
      assertThat(ex.getMessage()).startsWith("555 Server Error on GET request for ");
      assertThat(ex.getMessage()).endsWith("unknownPage\": \"Something went wrong\"");
      assertThat(ex.getStatusText()).isEqualTo("Server Error");
      assertThat(ex.getResponseHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
      assertThat(ex.getResponseBodyAsString()).isEqualTo(errorMessage);
    }

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/unknownPage"));
  }

  @ParameterizedRestClientTest
  void postPojoAsJson(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setHeader("Content-Type", "application/json")
            .setBody("{\"bar\":\"BARBAR\",\"foo\":\"FOOFOO\"}"));

    Pojo result = this.restClient.post()
            .uri("/pojo/capitalize")
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new Pojo("foofoo", "barbar"))
            .retrieve()
            .body(Pojo.class);

    assertThat(result).isNotNull();
    assertThat(result.getFoo()).isEqualTo("FOOFOO");
    assertThat(result.getBar()).isEqualTo("BARBAR");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/pojo/capitalize");
      assertThat(request.getBody().readUtf8()).isEqualTo("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
      assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void postStreamingBody(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);
    prepareResponse(response -> response.setResponseCode(200));

    StreamingHttpOutputMessage.Body testBody = out -> {
      assertThat(out).as("Not a streaming response").isNotInstanceOf(FastByteArrayOutputStream.class);
      new ByteArrayInputStream("test-data".getBytes(UTF_8)).transferTo(out);
    };

    ResponseEntity<Void> result = this.restClient.post()
            .uri("/streaming/body")
            .body(testBody)
            .retrieve()
            .toBodilessEntity();

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/streaming/body");
      assertThat(request.getBody().readUtf8()).isEqualTo("test-data");
    });
  }

  @ParameterizedRestClientTest
  public void postForm(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(200));

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("foo", "bar");
    formData.add("baz", "qux");

    ResponseEntity<Void> result = this.restClient.post()
            .uri("/form")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(formData)
            .retrieve()
            .toBodilessEntity();

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/form");
      String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
      assertThat(contentType).startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
      String[] lines = request.getBody().readUtf8().split("\r\n");
      assertThat(lines).hasSize(13);
      assertThat(lines[0]).startsWith("--"); // boundary
      assertThat(lines[1]).isEqualTo("Content-Disposition: form-data; name=\"foo\"");
      assertThat(lines[2]).isEqualTo("Content-Type: text/plain;charset=UTF-8");
      assertThat(lines[3]).isEqualTo("Content-Length: 3");
      assertThat(lines[4]).isEmpty();
      assertThat(lines[5]).isEqualTo("bar");
      assertThat(lines[6]).startsWith("--"); // boundary
      assertThat(lines[7]).isEqualTo("Content-Disposition: form-data; name=\"baz\"");
      assertThat(lines[8]).isEqualTo("Content-Type: text/plain;charset=UTF-8");
      assertThat(lines[9]).isEqualTo("Content-Length: 3");
      assertThat(lines[10]).isEmpty();
      assertThat(lines[11]).isEqualTo("qux");
      assertThat(lines[12]).startsWith("--"); // boundary
      assertThat(lines[12]).endsWith("--"); // boundary
    });
  }

  @ParameterizedRestClientTest
  void statusHandler(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(500)
            .setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

    assertThatExceptionOfType(MyException.class).isThrownBy(() ->
            this.restClient.get()
                    .uri("/greeting")
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                      throw new MyException("500 error!");
                    })
                    .body(String.class)
    );

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/greeting"));
  }

  @ParameterizedRestClientTest
  void statusHandlerIOException(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(500)
            .setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

    assertThatExceptionOfType(RestClientException.class).isThrownBy(() ->
            this.restClient.get()
                    .uri("/greeting")
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                      throw new IOException("500 error!");
                    })
                    .body(String.class)
    ).withCauseInstanceOf(IOException.class);

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/greeting"));
  }

  @ParameterizedRestClientTest
  void statusHandlerParameterizedTypeReference(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(500)
            .setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

    assertThatExceptionOfType(MyException.class).isThrownBy(() ->
            this.restClient.get()
                    .uri("/greeting")
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                      throw new MyException("500 error!");
                    })
                    .body(new ParameterizedTypeReference<String>() {
                    })
    );

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/greeting"));
  }

  @ParameterizedRestClientTest
  void statusHandlerSuppressedErrorSignal(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(500)
            .setHeader("Content-Type", "text/plain").setBody("Internal Server error"));

    String result = this.restClient.get()
            .uri("/greeting")
            .retrieve()
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> { })
            .body(String.class);

    assertThat(result).isEqualTo("Internal Server error");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/greeting"));
  }

  @ParameterizedRestClientTest
  void statusHandlerSuppressedErrorSignalWithEntity(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "Internal Server error";
    prepareResponse(response -> response.setResponseCode(500)
            .setHeader("Content-Type", "text/plain").setBody(content));

    ResponseEntity<String> result = this.restClient.get("/")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(new ResponseErrorHandler() {
              @Override
              public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().is5xxServerError();
              }

              @Override
              public void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {

              }
            })
            .toEntity(String.class);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isEqualTo(content);

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void exchangeForPlainText(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setBody("Hello Spring!"));

    String result = this.restClient.get()
            .uri("/greeting")
            .header("X-Test-Header", "testvalue")
            .exchange((request, response) -> new String(RestClientUtils.getBody(response), StandardCharsets.UTF_8));

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getHeader("X-Test-Header")).isEqualTo("testvalue");
      assertThat(request.getPath()).isEqualTo("/greeting");
    });
  }

  @ParameterizedRestClientTest
  void exchangeForJson(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json")
            .setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

    Pojo result = this.restClient.get()
            .uri("/pojo")
            .accept(MediaType.APPLICATION_JSON)
            .exchange((request, response) -> response.bodyTo(Pojo.class));

    assertThat(result.getFoo()).isEqualTo("foofoo");
    assertThat(result.getBar()).isEqualTo("barbar");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/pojo");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void exchangeForJsonArray(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json")
            .setBody("[{\"bar\":\"bar1\",\"foo\":\"foo1\"},{\"bar\":\"bar2\",\"foo\":\"foo2\"}]"));

    List<Pojo> result = this.restClient.get()
            .uri("/pojo")
            .accept(MediaType.APPLICATION_JSON)
            .exchange((request, response) -> response.bodyTo(new ParameterizedTypeReference<>() { }));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getFoo()).isEqualTo("foo1");
    assertThat(result.get(0).getBar()).isEqualTo("bar1");
    assertThat(result.get(1).getFoo()).isEqualTo("foo2");
    assertThat(result.get(1).getBar()).isEqualTo("bar2");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/pojo");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void exchangeFor404(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setResponseCode(404)
            .setHeader("Content-Type", "text/plain").setBody("Not Found"));

    String result = this.restClient.get()
            .uri("/greeting")
            .exchange((request, response) -> new String(RestClientUtils.getBody(response), StandardCharsets.UTF_8));

    assertThat(result).isEqualTo("Not Found");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/greeting"));
  }

  @ParameterizedRestClientTest
  void requestInitializer(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
            .setBody("Hello Spring!"));

    RestClient initializedClient = this.restClient.mutate()
            .requestInitializer(request -> request.getHeaders().add("foo", "bar"))
            .build();

    String result = initializedClient.get()
            .uri("/greeting")
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getHeader("foo")).isEqualTo("bar"));
  }

  @ParameterizedRestClientTest
  void requestInterceptor(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
            .setBody("Hello Spring!"));

    RestClient interceptedClient = this.restClient.mutate()
            .requestInterceptor((request, body, execution) -> {
              request.getHeaders().add("foo", "bar");
              return execution.execute(request, body);
            })
            .build();

    String result = interceptedClient.get()
            .uri("/greeting")
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getHeader("foo")).isEqualTo("bar"));
  }

  @ParameterizedRestClientTest
  void requestInterceptorWithResponseBuffering(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response ->
            response.setHeader("Content-Type", "text/plain").setBody("Hello Infra!"));

    RestClient interceptedClient = this.restClient.mutate()
            .requestInterceptor((request, body, execution) -> {
              ClientHttpResponse response = execution.execute(request, body);
              byte[] result = FileCopyUtils.copyToByteArray(response.getBody());
              assertThat(result).isEqualTo("Hello Infra!".getBytes(UTF_8));
              return response;
            })
            .bufferContent(request -> true)
            .build();

    String result = interceptedClient.get()
            .uri("/greeting")
            .retrieve()
            .body(String.class);

    expectRequestCount(1);
    assertThat(result).isEqualTo("Hello Infra!");
  }

  @ParameterizedRestClientTest
  void bufferContent(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response ->
            response.setHeader("Content-Type", "text/plain").setBody("Hello Infra!"));

    RestClient bufferingClient = this.restClient.mutate()
            .bufferContent(request -> true)
            .build();

    String result = bufferingClient.get()
            .uri("/greeting")
            .exchange((request, response) -> {
              byte[] bytes = FileCopyUtils.copyToByteArray(response.getBody());
              assertThat(bytes).isEqualTo("Hello Infra!".getBytes(UTF_8));
              bytes = FileCopyUtils.copyToByteArray(response.getBody());
              assertThat(bytes).isEqualTo("Hello Infra!".getBytes(UTF_8));
              return new String(bytes, UTF_8);
            });

    expectRequestCount(1);
    assertThat(result).isEqualTo("Hello Infra!");
  }

  @ParameterizedRestClientTest
  void retrieveDefaultCookiesAsCookieHeader(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);
    prepareResponse(response ->
            response.setHeader("Content-Type", "text/plain").setBody("Hello"));

    RestClient restClientWithCookies = this.restClient.mutate()
            .defaultCookie("testCookie", "firstValue", "secondValue")
            .build();

    restClientWithCookies.get()
            .uri("/greeting")
            .header("X-Test-Header", "testvalue")
            .retrieve()
            .body(String.class);

    expectRequest(request ->
            assertThat(request.getHeader(HttpHeaders.COOKIE))
                    .isEqualTo("testCookie=firstValue; testCookie=secondValue")
    );
  }

  @ParameterizedRestClientTest
  void filterForErrorHandling(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
      ClientHttpResponse response = execution.execute(request, body);
      List<String> headerValues = response.getHeaders().get("Foo");
      if (CollectionUtils.isEmpty(headerValues)) {
        throw new MyException("Response does not contain Foo header");
      }
      else {
        return response;
      }
    };

    RestClient interceptedClient = this.restClient.mutate().requestInterceptor(interceptor).build();

    // header not present
    prepareResponse(response -> response
            .setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

    assertThatExceptionOfType(MyException.class).isThrownBy(() ->
            interceptedClient.get()
                    .uri("/greeting")
                    .retrieve()
                    .body(String.class)
    );

    // header present

    prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
            .setHeader("Foo", "Bar")
            .setBody("Hello Spring!"));

    String result = interceptedClient.get()
            .uri("/greeting")
            .retrieve().body(String.class);

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(2);
  }

  @ParameterizedRestClientTest
  void invalidDomain(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String url = "http://example.invalid";
    assertThatExceptionOfType(ResourceAccessException.class).isThrownBy(() ->
            this.restClient.get().uri(url).retrieve().toBodiless()
    );

  }

  @ParameterizedRestClientTest
  void defaultHeaders(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
            .setBody("Hello Spring!"));

    RestClient headersClient = this.restClient.mutate()
            .defaultHeaders(headers -> headers.add("foo", "bar"))
            .build();

    String result = headersClient.get()
            .uri("/greeting")
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getHeader("foo")).isEqualTo("bar"));
  }

  @ParameterizedRestClientTest
  void defaultRequest(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
            .setBody("Hello Spring!"));

    RestClient headersClient = this.restClient.mutate()
            .defaultRequest(request -> request.header("foo", "bar"))
            .build();

    String result = headersClient.get()
            .uri("/greeting")
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getHeader("foo")).isEqualTo("bar"));
  }

  @ParameterizedRestClientTest
  void defaultRequestOverride(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
            .setBody("Hello Spring!"));

    RestClient headersClient = this.restClient.mutate()
            .defaultRequest(request -> request.accept(MediaType.APPLICATION_JSON))
            .build();

    String result = headersClient.get()
            .uri("/greeting")
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getHeader("Accept")).isEqualTo(MediaType.TEXT_PLAIN_VALUE));
  }

  @ParameterizedRestClientTest
  void relativeUri(ClientHttpRequestFactory requestFactory) throws URISyntaxException {
    startServer(requestFactory);

    prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
            .setBody("Hello Spring!"));

    URI uri = new URI(null, null, "/foo bar", null);

    String result = this.restClient
            .get(uri)
            .accept(MediaType.TEXT_PLAIN)
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/foo%20bar"));
  }

  // Future

  @ParameterizedRestClientTest
  void relativeUriFuture(ClientHttpRequestFactory requestFactory) throws URISyntaxException {
    startServer(requestFactory);

    prepareResponse(response -> response.setHeader("Content-Type", "text/plain")
            .setBody("Hello Spring!"));

    URI uri = new URI(null, null, "/foo bar", null);

    Future<String> result = this.restClient
            .get(uri)
            .accept(MediaType.TEXT_PLAIN)
            .async()
            .ignoreStatus(false)
            .body(String.class);

    assertThat(result).succeedsWithin(Duration.ofSeconds(1))
            .isEqualTo("Hello Spring!");

    expectRequestCount(1);
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/foo%20bar"));
  }

  @ParameterizedRestClientTest
  void retrieveJsonWithParameterizedTypeReferenceFuture(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "{\"containerValue\":{\"bar\":\"barbar\",\"foo\":\"foofoo\"}}";
    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody(content));

    Future<ValueContainer<Pojo>> future = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .async()
            .body(new ParameterizedTypeReference<>() { });

    assertThat(future).succeedsWithin(Duration.ofSeconds(1));

    ValueContainer<Pojo> result = future.getNow();
    assertThat(result).isNotNull();
    assertThat(result.getContainerValue()).isNotNull();
    Pojo pojo = result.getContainerValue();
    assertThat(pojo.getFoo()).isEqualTo("foofoo");
    assertThat(pojo.getBar()).isEqualTo("barbar");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonWithListParameterizedTypeReferenceFuture(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "{\"containerValue\":[{\"bar\":\"barbar\",\"foo\":\"foofoo\"}]}";
    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody(content));

    Future<ValueContainer<List<Pojo>>> future = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .async()
            .body(new ParameterizedTypeReference<>() { });

    assertThat(future).succeedsWithin(Duration.ofSeconds(1));

    ValueContainer<List<Pojo>> result = future.getNow();
    assertThat(result).isNotNull();
    assertThat(result.containerValue).isNotNull();
    assertThat(result.containerValue).containsExactly(new Pojo("foofoo", "barbar"));

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonAsResponseEntityFuture(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json").setBody(content));

    Future<ResponseEntity<String>> future = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .async()
            .ignoreStatus()
            .toEntity(String.class);

    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    ResponseEntity<String> result = future.getNow();
    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.headers().getContentLength()).isEqualTo(31);
    assertThat(result.getBody()).isEqualTo(content);

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void retrieveJsonAsBodilessEntityFuture(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json")
            .setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

    Future<ResponseEntity<Void>> future = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .async()
            .toBodilessEntity();

    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    ResponseEntity<Void> result = future.getNow();
    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.headers().getContentLength()).isEqualTo(31);
    assertThat(result.getBody()).isNull();

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });
  }

  @ParameterizedRestClientTest
  void executeAsync(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json")
            .setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

    var future = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .send();

    try (var response = future.join()) {
      assertThat(response).isNotNull();
      Map<String, String> body = response.bodyTo(new ParameterizedTypeReference<>() {

      });
      assertThat(body).contains(Pair.of("bar", "barbar"), Pair.of("foo", "foofoo"));

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
      assertThat(response.getHeaders().getContentLength()).isEqualTo(31);

      expectRequestCount(1);
      expectRequest(request -> {
        assertThat(request.getPath()).isEqualTo("/json");
        assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
      });
    }
  }

  @ParameterizedRestClientTest
  void executeAsyncBody(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    prepareResponse(response -> response
            .setHeader("Content-Type", "application/json")
            .setBody("{\"bar\":\"barbar\",\"foo\":\"foofoo\"}"));

    var future = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .send();

    try (var response = future.block().orElse(null)) {
      assertThat(response).isNotNull();

      Map<String, String> body = response.bodyTo(Map.class);
      assertThat(body).contains(Pair.of("bar", "barbar"), Pair.of("foo", "foofoo"));

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
      assertThat(response.getHeaders().getContentLength()).isEqualTo(31);

      expectRequestCount(1);
      expectRequest(request -> {
        assertThat(request.getPath()).isEqualTo("/json");
        assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
      });
    }
  }

  @ParameterizedRestClientTest
  void retrieveJsonAsResponseEntityFutureIgnoreStatus(ClientHttpRequestFactory requestFactory) {
    startServer(requestFactory);

    String content = "{\"bar\":\"barbar\",\"foo\":\"foofoo\"}";
    prepareResponse(response -> response
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json").setBody(content));

    Future<ResponseEntity<String>> future = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .async()
            .ignoreStatus()
            .toEntity(String.class);

    assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    ResponseEntity<String> result = future.getNow();
    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(result.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.headers().getContentLength()).isEqualTo(31);
    assertThat(result.getBody()).isEqualTo(content);

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getPath()).isEqualTo("/json");
      assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo("application/json");
    });

    // ignoreStatus = false

    prepareResponse(response -> response
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json").setBody(content));

    future = this.restClient.get()
            .uri("/json").accept(MediaType.APPLICATION_JSON)
            .async()
            .ignoreStatus(false)
            .toEntity(String.class);

    assertThat(future).failsWithin(Duration.ofSeconds(1));
    assertThat(future.getCause()).isInstanceOf(HttpStatusCodeException.class)
            .isInstanceOf(HttpClientErrorException.BadRequest.class);
  }

  @ParameterizedRestClientTest
  void sendNullHeaderValue(ClientHttpRequestFactory requestFactory) throws IOException {
    startServer(requestFactory);

    prepareResponse(builder -> builder
            .setHeader("Content-Type", "text/plain").setBody("Hello!"));

    String result = this.restClient.get()
            .uri("/greeting")
            .httpRequest(request -> request.getHeaders().add("X-Test-Header", null))
            .retrieve()
            .body(String.class);

    assertThat(result).isEqualTo("Hello!");

    expectRequestCount(1);
    expectRequest(request -> {
      assertThat(request.getHeaders().get("X-Test-Header")).isNullOrEmpty();
      assertThat(request.getPath()).isEqualTo("/greeting");
    });
  }

  private void prepareResponse(Consumer<MockResponse> consumer) {
    MockResponse response = new MockResponse();
    consumer.accept(response);
    this.server.enqueue(response);
  }

  private void expectRequest(Consumer<RecordedRequest> consumer) {
    try {
      consumer.accept(this.server.takeRequest());
    }
    catch (InterruptedException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void expectRequestCount(int count) {
    assertThat(this.server.getRequestCount()).isEqualTo(count);
  }

  @SuppressWarnings("serial")
  private static class MyException extends RuntimeException {

    MyException(String message) {
      super(message);
    }
  }

  static class ValueContainer<T> {

    private T containerValue;

    public T getContainerValue() {
      return containerValue;
    }

    public void setContainerValue(T containerValue) {
      this.containerValue = containerValue;
    }
  }

}
