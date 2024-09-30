/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.client.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.CookieValue;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestHeader;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.client.RestClient;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.service.annotation.GetExchange;
import cn.taketoday.web.service.annotation.PostExchange;
import cn.taketoday.web.service.annotation.PutExchange;
import cn.taketoday.web.service.invoker.HttpExchangeAdapter;
import cn.taketoday.web.service.invoker.HttpServiceProxyFactory;
import cn.taketoday.web.testfixture.MockMultipartFile;
import cn.taketoday.web.util.DefaultUriBuilderFactory;
import cn.taketoday.web.util.UriBuilderFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HttpServiceProxyFactory} with {@link RestClient}
 * and {@link RestTemplate} connecting to {@link MockWebServer}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("JUnitMalformedDeclaration")
class RestClientAdapterTests {

  private final MockWebServer anotherServer = anotherServer();

  @SuppressWarnings("ConstantValue")
  @AfterEach
  void shutdown() throws IOException {
    if (this.anotherServer != null) {
      this.anotherServer.shutdown();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest
  @MethodSource("arguments")
  @interface ParameterizedAdapterTest {
  }

  public static Stream<Arguments> arguments() {
    return Stream.of(
            args(url -> {
              RestClient restClient = RestClient.builder().baseURI(url).build();
              return RestClientAdapter.create(restClient);
            }));
  }

  private static Arguments args(Function<String, HttpExchangeAdapter> adapterFactory) {
    MockWebServer server = new MockWebServer();

    MockResponse response = new MockResponse();
    response.setHeader("Content-Type", "text/plain").setBody("Hello Infra!");
    server.enqueue(response);

    HttpExchangeAdapter adapter = adapterFactory.apply(server.url("/").toString());
    Service service = HttpServiceProxyFactory.forAdapter(adapter).createClient(Service.class);

    return Arguments.of(server, service);
  }

  @ParameterizedAdapterTest
  void greeting(MockWebServer server, Service service) throws Exception {
    String response = service.getGreeting();

    RecordedRequest request = server.takeRequest();
    assertThat(response).isEqualTo("Hello Infra!");
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/greeting");
  }

  @ParameterizedAdapterTest
  void greetingById(MockWebServer server, Service service) throws Exception {
    ResponseEntity<String> response = service.getGreetingById("456");

    RecordedRequest request = server.takeRequest();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("Hello Infra!");
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/greeting/456");
  }

  @ParameterizedAdapterTest
  void greetingWithDynamicUri(MockWebServer server, Service service) throws Exception {
    URI dynamicUri = server.url("/greeting/123").uri();
    Optional<String> response = service.getGreetingWithDynamicUri(dynamicUri, "456");

    RecordedRequest request = server.takeRequest();
    assertThat(response.orElse("empty")).isEqualTo("Hello Infra!");
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getRequestUrl().uri()).isEqualTo(dynamicUri);
  }

  @ParameterizedAdapterTest
  void postWithHeader(MockWebServer server, Service service) throws Exception {
    service.postWithHeader("testHeader", "testBody");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath()).isEqualTo("/greeting");
    assertThat(request.getHeaders().get("testHeaderName")).isEqualTo("testHeader");
    assertThat(request.getBody().readUtf8()).isEqualTo("testBody");
  }

  @ParameterizedAdapterTest
  void formData(MockWebServer server, Service service) throws Exception {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("param1", "value 1");
    map.add("param2", "value 2");

    service.postForm(map);

    RecordedRequest request = server.takeRequest();
    assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
    assertThat(request.getBody().readUtf8()).isEqualTo("param1=value+1&param2=value+2");
  }

  @ParameterizedAdapterTest
    // gh-30342
  void multipart(MockWebServer server, Service service) throws Exception {
    MultipartFile file = new MockMultipartFile(
            "testFileName", "originalTestFileName", MediaType.APPLICATION_JSON_VALUE, "test".getBytes());

    service.postMultipart(file, "test2");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getHeaders().get("Content-Type")).startsWith("multipart/form-data;boundary=");
    assertThat(request.getBody().readUtf8()).containsSubsequence(
            "Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
            "Content-Type: application/json", "Content-Length: 4", "test",
            "Content-Disposition: form-data; name=\"anotherPart\"", "Content-Type: text/plain;charset=UTF-8",
            "Content-Length: 5", "test2");
  }

  @ParameterizedAdapterTest
  void putWithCookies(MockWebServer server, Service service) throws Exception {
    service.putWithCookies("test1", "test2");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getMethod()).isEqualTo("PUT");
    assertThat(request.getHeader("Cookie")).isEqualTo("firstCookie=test1; secondCookie=test2");
  }

  @ParameterizedAdapterTest
  void putWithSameNameCookies(MockWebServer server, Service service) throws Exception {
    service.putWithSameNameCookies("test1", "test2");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getMethod()).isEqualTo("PUT");
    assertThat(request.getHeader("Cookie")).isEqualTo("testCookie=test1; testCookie=test2");
  }

  @ParameterizedAdapterTest
  void getWithUriBuilderFactory(MockWebServer server, Service service) throws InterruptedException {
    String url = this.anotherServer.url("/").toString();
    UriBuilderFactory factory = new DefaultUriBuilderFactory(url);

    ResponseEntity<String> actualResponse = service.getWithUriBuilderFactory(factory);

    RecordedRequest request = this.anotherServer.takeRequest();
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actualResponse.getBody()).isEqualTo("Hello Infra 2!");
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/greeting");
    assertThat(server.getRequestCount()).isEqualTo(0);
  }

  @ParameterizedAdapterTest
  void getWithFactoryPathVariableAndRequestParam(MockWebServer server, Service service) throws InterruptedException {
    String url = this.anotherServer.url("/").toString();
    UriBuilderFactory factory = new DefaultUriBuilderFactory(url);

    ResponseEntity<String> actualResponse = service.getWithUriBuilderFactory(factory, "123", "test");

    RecordedRequest request = this.anotherServer.takeRequest();
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actualResponse.getBody()).isEqualTo("Hello Infra 2!");
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/greeting/123?param=test");
    assertThat(server.getRequestCount()).isEqualTo(0);
  }

  @ParameterizedAdapterTest
  void getWithIgnoredUriBuilderFactory(MockWebServer server, Service service) throws InterruptedException {
    URI dynamicUri = server.url("/greeting/123").uri();
    UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

    ResponseEntity<String> actualResponse = service.getWithIgnoredUriBuilderFactory(dynamicUri, factory);

    RecordedRequest request = server.takeRequest();
    assertThat(actualResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(actualResponse.getBody()).isEqualTo("Hello Infra!");
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(request.getPath()).isEqualTo("/greeting/123");
    assertThat(this.anotherServer.getRequestCount()).isEqualTo(0);
  }

  private static MockWebServer anotherServer() {
    MockWebServer server = new MockWebServer();
    MockResponse response = new MockResponse();
    response.setHeader("Content-Type", "text/plain").setBody("Hello Infra 2!");
    server.enqueue(response);
    return server;
  }

  private interface Service {

    @GetExchange("/greeting")
    String getGreeting();

    @GetExchange("/greeting/{id}")
    ResponseEntity<String> getGreetingById(@PathVariable String id);

    @GetExchange("/greeting/{id}")
    Optional<String> getGreetingWithDynamicUri(@Nullable URI uri, @PathVariable String id);

    @PostExchange("/greeting")
    void postWithHeader(@RequestHeader("testHeaderName") String testHeader, @RequestBody String requestBody);

    @PostExchange(contentType = "application/x-www-form-urlencoded")
    void postForm(@RequestParam MultiValueMap<String, String> params);

    @PostExchange
    void postMultipart(MultipartFile file, @RequestPart String anotherPart);

    @PutExchange
    void putWithCookies(@CookieValue String firstCookie, @CookieValue String secondCookie);

    @PutExchange
    void putWithSameNameCookies(
            @CookieValue("testCookie") String firstCookie, @CookieValue("testCookie") String secondCookie);

    @GetExchange("/greeting")
    ResponseEntity<String> getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory);

    @GetExchange("/greeting/{id}")
    ResponseEntity<String> getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory,
            @PathVariable String id, @RequestParam String param);

    @GetExchange("/greeting")
    ResponseEntity<String> getWithIgnoredUriBuilderFactory(URI uri, UriBuilderFactory uriBuilderFactory);
  }

}
