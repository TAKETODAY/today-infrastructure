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

package infra.http.service.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseEntity;
import infra.http.StreamingHttpOutputMessage;
import infra.http.service.annotation.GetExchange;
import infra.http.service.annotation.PostExchange;
import infra.http.service.annotation.PutExchange;
import infra.http.service.invoker.HttpExchangeAdapter;
import infra.http.service.invoker.HttpServiceProxyFactory;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.annotation.CookieValue;
import infra.web.annotation.PathVariable;
import infra.web.annotation.RequestBody;
import infra.web.annotation.RequestHeader;
import infra.web.annotation.RequestParam;
import infra.web.annotation.RequestPart;
import infra.web.client.ApiVersionInserter;
import infra.web.client.RestClient;
import infra.web.client.RestTemplate;
import infra.web.multipart.Part;
import infra.web.testfixture.MockMultipartFile;
import infra.web.util.DefaultUriBuilderFactory;
import infra.web.util.UriBuilderFactory;
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
  void multipart(MockWebServer server, Service service) throws Exception {
    Part file = new MockMultipartFile(
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

  @Test
  void apiVersion() throws Exception {
    RestClient restClient = RestClient.builder()
            .baseURI(anotherServer.url("/").toString())
            .apiVersionInserter(ApiVersionInserter.forHeader("X-API-Version"))
            .build();

    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    Service service = HttpServiceProxyFactory.forAdapter(adapter).build().createClient(Service.class);

    service.getGreetingWithVersion();

    RecordedRequest request = anotherServer.takeRequest();
    assertThat(request.getHeader("X-API-Version")).isEqualTo("1.2");
  }

  @ParameterizedAdapterTest
  void postSet(MockWebServer server, Service service) throws InterruptedException {
    Set<Person> persons = new LinkedHashSet<>();
    persons.add(new Person("John"));
    persons.add(new Person("Richard"));
    service.postPersonSet(persons);

    RecordedRequest request = server.takeRequest();
    assertThat(request.getMethod()).isEqualTo("POST");
    assertThat(request.getPath()).isEqualTo("/persons");
    assertThat(request.getBody().readUtf8()).isEqualTo("[{\"name\":\"John\"},{\"name\":\"Richard\"}]");
  }

  @ParameterizedAdapterTest
  void postOutputStream(MockWebServer server, Service service) throws Exception {
    String body = "test stream";
    service.postOutputStream(outputStream -> outputStream.write(body.getBytes()));

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/output-stream");
    assertThat(request.getBody().readUtf8()).isEqualTo(body);
  }

  private Service initService(MockWebServer server) {
    String url = server.url("/").toString();
    RestClient restClient = RestClient.builder().baseURI(url).build();
    RestClientAdapter adapter = RestClientAdapter.create(restClient);
    return HttpServiceProxyFactory.forAdapter(adapter).build().createClient(Service.class);
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

    @GetExchange(url = "/greeting", version = "1.2")
    String getGreetingWithVersion();

    @GetExchange("/greeting/{id}")
    ResponseEntity<String> getGreetingById(@PathVariable String id);

    @GetExchange("/greeting/{id}")
    Optional<String> getGreetingWithDynamicUri(@Nullable URI uri, @PathVariable String id);

    @PostExchange("/greeting")
    void postWithHeader(@RequestHeader("testHeaderName") String testHeader, @RequestBody String requestBody);

    @PostExchange(contentType = "application/x-www-form-urlencoded")
    void postForm(@RequestParam MultiValueMap<String, String> params);

    @PostExchange
    void postMultipart(Part file, @RequestPart String anotherPart);

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

    @PostExchange(url = "/output-stream")
    void postOutputStream(StreamingHttpOutputMessage.Body body);

    @PostExchange(url = "/persons", contentType = MediaType.APPLICATION_JSON_VALUE)
    void postPersonSet(@RequestBody Set<Person> set);

  }

  record Person(String name) {
  }

}
