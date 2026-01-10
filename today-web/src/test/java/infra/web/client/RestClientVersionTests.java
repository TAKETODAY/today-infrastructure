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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Consumer;

import infra.http.MediaType;
import infra.http.client.JdkClientHttpRequestFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * {@link RestClient} tests for sending API versions.
 *
 * @author Rossen Stoyanchev
 */
public class RestClientVersionTests {

  private final MockWebServer server = new MockWebServer();

  private final RestClient.Builder restClientBuilder = RestClient.builder()
          .requestFactory(new JdkClientHttpRequestFactory())
          .baseURI(this.server.url("/").toString());

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
    performRequest(ApiVersionInserter.builder().useHeader("X-API-Version"));
    expectRequest(request -> assertThat(request.getHeader("X-API-Version")).isEqualTo("1.2"));
  }

  @Test
  void queryParam() {
    performRequest(ApiVersionInserter.builder().useQueryParam("api-version"));
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/path?api-version=1.2"));
  }

  @Test
  void mediaTypeParam() {
    performRequest(ApiVersionInserter.builder().useMediaTypeParam("v"));
    expectRequest(request -> assertThat(request.getHeaders().get("Content-Type")).isEqualTo("application/json;v=1.2"));
  }

  @Test
  void pathSegmentIndexLessThanSize() {
    performRequest(ApiVersionInserter.builder().usePathSegment(0).withVersionFormatter(v -> "v" + v));
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/v1.2/path"));
  }

  @Test
  void pathSegmentIndexEqualToSize() {
    performRequest(ApiVersionInserter.builder().usePathSegment(1).withVersionFormatter(v -> "v" + v));
    expectRequest(request -> assertThat(request.getPath()).isEqualTo("/path/v1.2"));
  }

  @Test
  void pathSegmentIndexGreaterThanSize() {
    assertThatIllegalStateException()
            .isThrownBy(() -> performRequest(ApiVersionInserter.builder().usePathSegment(2)))
            .withMessage("Cannot insert version into '/path' at path segment index 2");
  }

  @Test
  void defaultVersion() {
    ApiVersionInserter inserter = ApiVersionInserter.forHeader("X-API-Version");
    RestClient restClient = restClientBuilder.defaultApiVersion(1.2).apiVersionInserter(inserter).build();
    restClient.get().uri("/path").retrieve().body(String.class);

    expectRequest(request -> assertThat(request.getHeader("X-API-Version")).isEqualTo("1.2"));
  }

  private void performRequest(ApiVersionInserter.Builder builder) {
    restClientBuilder.apiVersionInserter(builder.build()).build()
            .post().uri("/path")
            .contentType(MediaType.APPLICATION_JSON)
            .apiVersion(1.2)
            .retrieve()
            .body(String.class);
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
