/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.JdkClientHttpRequestFactory;
import cn.taketoday.http.client.JettyClientHttpRequestFactory;
import cn.taketoday.http.client.ReactorNettyClientRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.testfixture.Pojo;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

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
            named("JDK HttpURLConnection", new SimpleClientHttpRequestFactory()),
            named("HttpComponents", new HttpComponentsClientHttpRequestFactory()),
            named("Jetty", new JettyClientHttpRequestFactory()),
            named("JDK HttpClient", new JdkClientHttpRequestFactory()),
            named("Reactor Netty", new ReactorNettyClientRequestFactory())
    );
  }

  private MockWebServer server;

  private RestClient restClient;

  private void startServer(ClientHttpRequestFactory requestFactory) {
    this.server = new MockWebServer();
    this.restClient = RestClient
            .builder()
            .requestFactory(requestFactory)
            .baseUrl(this.server.url("/").toString())
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
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.getHeaders().getContentLength()).isEqualTo(31);
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
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.getHeaders().getContentLength()).isEqualTo(31);
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
    assertThat(result.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(result.getHeaders().getContentLength()).isEqualTo(58);
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
            this.restClient.get()
                    .uri("/greeting")
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
            this.restClient.get()
                    .uri("/").accept(MediaType.APPLICATION_JSON)
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
      assertThat(ex.getMessage()).isEqualTo("555 Server Error: \"Something went wrong\"");
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

    ResponseEntity<String> result = this.restClient.get()
            .uri("/").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> { })
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
            this.restClient.get().uri(url).retrieve().toBodilessEntity()
    );

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
