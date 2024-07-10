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

package cn.taketoday.web.reactive.function.client.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.annotation.RequestAttribute;
import cn.taketoday.web.annotation.RequestParam;
import cn.taketoday.web.annotation.RequestPart;
import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.reactive.function.client.WebClient;
import cn.taketoday.web.service.annotation.GetExchange;
import cn.taketoday.web.service.annotation.PostExchange;
import cn.taketoday.web.service.invoker.HttpServiceProxyFactory;
import cn.taketoday.web.testfixture.MockMultipartFile;
import cn.taketoday.web.util.DefaultUriBuilderFactory;
import cn.taketoday.web.util.UriBuilderFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/15 21:03
 */
class WebClientAdapterTests {

  private static final String ANOTHER_SERVER_RESPONSE_BODY = "Hello Spring 2!";

  private MockWebServer server;

  private MockWebServer anotherServer;

  @BeforeEach
  void setUp() {
    this.server = new MockWebServer();
    this.anotherServer = anotherServer();
  }

  @SuppressWarnings("ConstantConditions")
  @AfterEach
  void shutdown() throws IOException {
    if (this.server != null) {
      this.server.shutdown();
    }

    if (this.anotherServer != null) {
      this.anotherServer.shutdown();
    }
  }

  @Test
  void greeting() {
    prepareResponse(response ->
            response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

    StepVerifier.create(initService().getGreeting())
            .expectNext("Hello Spring!")
            .expectComplete()
            .verify(Duration.ofSeconds(5));
  }

  @Test
  void greetingWithRequestAttribute() {
    Map<String, Object> attributes = new HashMap<>();

    WebClient webClient = WebClient.builder()
            .baseURI(this.server.url("/").toString())
            .filter((request, next) -> {
              attributes.putAll(request.attributes());
              return next.exchange(request);
            })
            .build();

    prepareResponse(response ->
            response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

    StepVerifier.create(initService(webClient).getGreetingWithAttribute("myAttributeValue"))
            .expectNext("Hello Spring!")
            .expectComplete()
            .verify(Duration.ofSeconds(5));

    assertThat(attributes).containsEntry("myAttribute", "myAttributeValue");
  }

  @Test
    // gh-29624
  void uri() throws Exception {
    String expectedBody = "hello";
    prepareResponse(response -> response.setResponseCode(200).setBody(expectedBody));

    URI dynamicUri = this.server.url("/greeting/123").uri();
    String actualBody = initService().getGreetingById(dynamicUri, "456");

    assertThat(actualBody).isEqualTo(expectedBody);
    assertThat(this.server.takeRequest().getRequestUrl().uri()).isEqualTo(dynamicUri);
  }

  @Test
  void formData() throws Exception {
    prepareResponse(response -> response.setResponseCode(201));

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("param1", "value 1");
    map.add("param2", "value 2");

    initService().postForm(map);

    RecordedRequest request = this.server.takeRequest();
    assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded");
    assertThat(request.getBody().readUtf8()).isEqualTo("param1=value+1&param2=value+2");
  }

  @Test
  void multipart() throws InterruptedException {
    prepareResponse(response -> response.setResponseCode(201));
    String fileName = "testFileName";
    String originalFileName = "originalTestFileName";
    MultipartFile file = new MockMultipartFile(fileName, originalFileName,
            MediaType.APPLICATION_JSON_VALUE, "test".getBytes());

    initService().postMultipart(file, "test2");

    RecordedRequest request = this.server.takeRequest();
    assertThat(request.getHeaders().get("Content-Type")).startsWith("multipart/form-data;boundary=");
    assertThat(request.getBody().readUtf8())
            .containsSubsequence("Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
                    "Content-Type: application/json", "Content-Length: 4", "test",
                    "Content-Disposition: form-data; name=\"anotherPart\"",
                    "Content-Type: text/plain;charset=UTF-8", "Content-Length: 5", "test2");
  }

  @Test
  void uriBuilderFactory() throws Exception {
    String ignoredResponseBody = "hello";
    prepareResponse(response -> response.setResponseCode(200).setBody(ignoredResponseBody));
    UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

    String actualBody = initService().getWithUriBuilderFactory(factory);

    assertThat(actualBody).isEqualTo(ANOTHER_SERVER_RESPONSE_BODY);
    assertThat(this.anotherServer.takeRequest().getPath()).isEqualTo("/greeting");
    assertThat(this.server.getRequestCount()).isEqualTo(0);
  }

  @Test
  void uriBuilderFactoryWithPathVariableAndRequestParam() throws Exception {
    String ignoredResponseBody = "hello";
    prepareResponse(response -> response.setResponseCode(200).setBody(ignoredResponseBody));
    UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

    String actualBody = initService().getWithUriBuilderFactory(factory, "123", "test");

    assertThat(actualBody).isEqualTo(ANOTHER_SERVER_RESPONSE_BODY);
    assertThat(this.anotherServer.takeRequest().getPath()).isEqualTo("/greeting/123?param=test");
    assertThat(this.server.getRequestCount()).isEqualTo(0);
  }

  @Test
  void ignoredUriBuilderFactory() throws Exception {
    String expectedResponseBody = "hello";
    prepareResponse(response -> response.setResponseCode(200).setBody(expectedResponseBody));
    URI dynamicUri = this.server.url("/greeting/123").uri();
    UriBuilderFactory factory = new DefaultUriBuilderFactory(this.anotherServer.url("/").toString());

    String actualBody = initService().getWithIgnoredUriBuilderFactory(dynamicUri, factory);

    assertThat(actualBody).isEqualTo(expectedResponseBody);
    assertThat(this.server.takeRequest().getRequestUrl().uri()).isEqualTo(dynamicUri);
    assertThat(this.anotherServer.getRequestCount()).isEqualTo(0);
  }

  private static MockWebServer anotherServer() {
    MockWebServer anotherServer = new MockWebServer();
    MockResponse response = new MockResponse();
    response.setHeader("Content-Type", "text/plain").setBody(ANOTHER_SERVER_RESPONSE_BODY);
    anotherServer.enqueue(response);
    return anotherServer;
  }

  private Service initService() {
    WebClient webClient = WebClient.create(this.server.url("/").toString());
    return initService(webClient);
  }

  private Service initService(WebClient webClient) {
    WebClientAdapter adapter = WebClientAdapter.forClient(webClient);
    return HttpServiceProxyFactory.forAdapter(adapter).createClient(Service.class);
  }

  private void prepareResponse(Consumer<MockResponse> consumer) {
    MockResponse response = new MockResponse();
    consumer.accept(response);
    this.server.enqueue(response);
  }

  private interface Service {

    @GetExchange("/greeting")
    Mono<String> getGreeting();

    @GetExchange("/greeting")
    Mono<String> getGreetingWithAttribute(@RequestAttribute String myAttribute);

    @GetExchange("/greetings/{id}")
    String getGreetingById(@Nullable URI uri, @PathVariable String id);

    @PostExchange(contentType = "application/x-www-form-urlencoded")
    void postForm(@RequestParam MultiValueMap<String, String> params);

    @PostExchange
    void postMultipart(MultipartFile file, @RequestPart String anotherPart);

    @GetExchange("/greeting")
    String getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory);

    @GetExchange("/greeting/{id}")
    String getWithUriBuilderFactory(UriBuilderFactory uriBuilderFactory,
            @PathVariable String id, @RequestParam String param);

    @GetExchange("/greeting")
    String getWithIgnoredUriBuilderFactory(URI uri, UriBuilderFactory uriBuilderFactory);

  }

}