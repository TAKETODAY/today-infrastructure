/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.client.reactive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Consumer;

import infra.web.client.ApiVersionInserter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * {@link WebClient} tests for sending API versions.
 *
 * @author Rossen Stoyanchev
 */
public class WebClientVersionTests {

  private final MockWebServer server = new MockWebServer();

  private final WebClient.Builder webClientBuilder =
          WebClient.builder().baseURI(this.server.url("/").toString());

  @BeforeEach
  void setUp() {
    MockResponse response = new MockResponse();
    response.setHeader("Content-Type", "text/plain").setBody("body");
    this.server.enqueue(response);
  }

  @AfterEach
  void shutdown() throws IOException {
    this.server.shutdown();
  }

  @Test
  void header() {
    performRequest(ApiVersionInserter.forHeader("X-API-Version"));
    expectRequest(request -> assertThat(request.getHeader("X-API-Version")).isEqualTo("1.2"));
  }

  @Test
  void queryParam() {
    performRequest(ApiVersionInserter.forQueryParam("api-version"));
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/path?api-version=1.2"));
  }

  @Test
  void pathSegmentIndexLessThanSize() {
    performRequest(ApiVersionInserter.forPathSegment(0).withVersionFormatter(v -> "v" + v));
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/v1.2/path"));
  }

  @Test
  void pathSegmentIndexEqualToSize() {
    performRequest(ApiVersionInserter.forPathSegment(1).withVersionFormatter(v -> "v" + v));
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/path/v1.2"));
  }

  @Test
  void pathSegmentIndexGreaterThanSize() {
    assertThatIllegalStateException()
            .isThrownBy(() -> performRequest(ApiVersionInserter.forPathSegment(2)))
            .withMessage("Cannot insert version into '/path' at path segment index 2");
  }

  @Test
  void defaultVersion() {
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-API-Version").build();
    WebClient webClient = webClientBuilder.defaultApiVersion(1.2).apiVersionInserter(inserter).build();
    webClient.get().uri("/path").retrieve().bodyToMono(String.class).block();

    expectRequest(request -> assertThat(request.getHeader("X-API-Version")).isEqualTo("1.2"));
  }

  private void performRequest(ApiVersionInserter.Builder builder) {
    ApiVersionInserter versionInserter = builder.build();
    WebClient webClient = webClientBuilder.apiVersionInserter(versionInserter).build();
    webClient.get().uri("/path").apiVersion(1.2).retrieve().bodyToMono(String.class).block();
  }

  private void expectRequest(Consumer<RecordedRequest> consumer) {
    try {
      consumer.accept(this.server.takeRequest());
    }
    catch (InterruptedException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
