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

package cn.taketoday.http.server.reactive;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.RequestEntity;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import cn.taketoday.web.http.server.reactive.HttpServer;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @author Rossen Stoyanchev
 */
@Execution(ExecutionMode.SAME_THREAD)
public class CookieIntegrationTests extends AbstractHttpHandlerIntegrationTests {

  private final CookieHandler cookieHandler = new CookieHandler();

  @Override
  protected HttpHandler createHttpHandler() {
    return this.cookieHandler;
  }

  @ParameterizedHttpServerTest
  public void basicTest(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    URI url = URI.create("http://localhost:" + port);
    String header = "SID=31d4d96e407aad42; lang=en-US";
    ResponseEntity<Void> response = new RestTemplate().exchange(
            RequestEntity.get(url).header("Cookie", header).build(), Void.class);

    Map<String, List<HttpCookie>> requestCookies = this.cookieHandler.requestCookies;
    assertThat(requestCookies).hasSize(2);
    assertThat(requestCookies.get("SID")).extracting(HttpCookie::getValue).containsExactly("31d4d96e407aad42");
    assertThat(requestCookies.get("lang")).extracting(HttpCookie::getValue).containsExactly("en-US");

    List<String> headerValues = response.getHeaders().get("Set-Cookie");
    assertThat(headerValues).hasSize(2);

    List<String> cookie0 = splitCookie(headerValues.get(0));
    assertThat(cookie0.remove("SID=31d4d96e407aad42")).as("SID").isTrue();
    assertThat(cookie0.stream().map(String::toLowerCase))
            .containsExactlyInAnyOrder("path=/", "secure", "httponly");
    List<String> cookie1 = splitCookie(headerValues.get(1));
    assertThat(cookie1.remove("lang=en-US")).as("lang").isTrue();
    assertThat(cookie1.stream().map(String::toLowerCase))
            .containsExactlyInAnyOrder("path=/", "domain=example.com");
  }

  @ParameterizedHttpServerTest
  public void cookiesWithSameNameTest(HttpServer httpServer) throws Exception {
    startServer(httpServer);

    URI url = new URI("http://localhost:" + port);
    String header = "SID=31d4d96e407aad42; lang=en-US; lang=zh-CN";
    new RestTemplate().exchange(
            RequestEntity.get(url).header("Cookie", header).build(), Void.class);

    Map<String, List<HttpCookie>> requestCookies = this.cookieHandler.requestCookies;
    assertThat(requestCookies.size()).isEqualTo(2);
    assertThat(requestCookies.get("SID")).extracting(HttpCookie::getValue).containsExactly("31d4d96e407aad42");
    assertThat(requestCookies.get("lang")).extracting(HttpCookie::getValue).containsExactly("en-US", "zh-CN");
  }

  // No client side HttpCookie support yet
  private List<String> splitCookie(String value) {
    List<String> list = new ArrayList<>();
    for (String s : value.split(";")) {
      list.add(s.trim());
    }
    return list;
  }

  private class CookieHandler implements HttpHandler {

    private Map<String, List<HttpCookie>> requestCookies;

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {

      this.requestCookies = request.getCookies();
      this.requestCookies.size(); // Cause lazy loading

      response.getCookies().add("SID", ResponseCookie.from("SID", "31d4d96e407aad42")
              .path("/").secure(true).httpOnly(true).build());
      response.getCookies().add("lang", ResponseCookie.from("lang", "en-US")
              .domain("example.com").path("/").build());

      return response.setComplete();
    }
  }

}
