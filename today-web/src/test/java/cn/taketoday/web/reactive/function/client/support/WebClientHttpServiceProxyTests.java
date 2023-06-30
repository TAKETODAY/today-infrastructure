/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
import cn.taketoday.web.testfixture.servlet.MockMultipartFile;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/12/3 13:06
 */
public class WebClientHttpServiceProxyTests {

  private MockWebServer server;

  @BeforeEach
  void setUp() {
    this.server = new MockWebServer();
  }

  @SuppressWarnings("ConstantConditions")
  @AfterEach
  void shutdown() throws IOException {
    if (this.server != null) {
      this.server.shutdown();
    }
  }

  @Test
  void greeting() throws Exception {

    prepareResponse(response ->
            response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

    StepVerifier.create(initHttpService().getGreeting())
            .expectNext("Hello Spring!")
            .expectComplete()
            .verify(Duration.ofSeconds(5));
  }

  @Test
  void greetingWithRequestAttribute() {

    Map<String, Object> attributes = new HashMap<>();

    WebClient webClient = WebClient.builder()
            .baseUrl(this.server.url("/").toString())
            .filter((request, next) -> {
              attributes.putAll(request.attributes());
              return next.exchange(request);
            })
            .build();

    prepareResponse(response ->
            response.setHeader("Content-Type", "text/plain").setBody("Hello Spring!"));

    StepVerifier.create(initHttpService(webClient).getGreetingWithAttribute("myAttributeValue"))
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
    String actualBody = initHttpService().getGreetingById(dynamicUri, "456");

    assertThat(actualBody).isEqualTo(expectedBody);
    assertThat(this.server.takeRequest().getRequestUrl().uri()).isEqualTo(dynamicUri);
  }

  @Test
  void formData() throws Exception {
    prepareResponse(response -> response.setResponseCode(201));

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("param1", "value 1");
    map.add("param2", "value 2");

    initHttpService().postForm(map);

    RecordedRequest request = this.server.takeRequest();
    assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/x-www-form-urlencoded;charset=UTF-8");
    assertThat(request.getBody().readUtf8()).isEqualTo("param1=value+1&param2=value+2");
  }

  @Test
  void multipart() throws InterruptedException {
    prepareResponse(response -> response.setResponseCode(201));
    String fileName = "testFileName";
    String originalFileName = "originalTestFileName";
    MultipartFile file = new MockMultipartFile(fileName, originalFileName,
            MediaType.APPLICATION_JSON_VALUE, "test".getBytes());

    initHttpService().postMultipart(file, "test2");

    RecordedRequest request = this.server.takeRequest();
    assertThat(request.getHeaders().get("Content-Type")).startsWith("multipart/form-data;boundary=");
    assertThat(request.getBody().readUtf8())
            .containsSubsequence("Content-Disposition: form-data; name=\"file\"; filename=\"originalTestFileName\"",
                    "Content-Type: application/json", "Content-Length: 4", "test",
                    "Content-Disposition: form-data; name=\"anotherPart\"",
                    "Content-Type: text/plain;charset=UTF-8", "Content-Length: 5", "test2");
  }

  private TestHttpService initHttpService() {
    WebClient webClient = WebClient.builder().baseUrl(this.server.url("/").toString()).build();
    return initHttpService(webClient);
  }

  private TestHttpService initHttpService(WebClient webClient) {
    HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(WebClientAdapter.forClient(webClient));
    proxyFactory.afterPropertiesSet();
    return proxyFactory.createClient(TestHttpService.class);
  }

  private void prepareResponse(Consumer<MockResponse> consumer) {
    MockResponse response = new MockResponse();
    consumer.accept(response);
    this.server.enqueue(response);
  }

  private interface TestHttpService {

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

  }

}
