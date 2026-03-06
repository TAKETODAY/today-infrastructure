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

package infra.test.web.mock.client.samples;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.http.MediaType;
import infra.test.web.mock.client.RestTestClient;
import infra.web.RequestContext;
import infra.web.accept.ApiVersionResolver;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.PathApiVersionResolver;
import infra.web.accept.SemanticApiVersionParser;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import infra.web.client.ApiVersionInserter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RestTestClient} tests for sending API versions.
 *
 * @author Rossen Stoyanchev
 */
public class ApiVersionTests {

  @Test
  void header() {
    String header = "API-Version";

    Map<String, String> result = performRequest(
            request -> request.getHeader(header), ApiVersionInserter.forHeader(header));

    assertThat(result.get(header)).isEqualTo("1.2");
  }

  @Test
  void queryParam() {
    String param = "api-version";

    Map<String, String> result = performRequest(
            request -> request.getParameter(param), ApiVersionInserter.forQueryParam(param));

    assertThat(result.get("query")).isEqualTo(param + "=1.2");
  }

  @Test
  void pathSegment() {
    Map<String, String> result = performRequest(
            new PathApiVersionResolver(0), ApiVersionInserter.forPathSegment(0));

    assertThat(result.get("path")).isEqualTo("/1.2/path");
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> performRequest(
          ApiVersionResolver versionResolver, ApiVersionInserter inserter) {

    DefaultApiVersionStrategy versionStrategy = new DefaultApiVersionStrategy(
            List.of(versionResolver), new SemanticApiVersionParser(),
            true, null, true, null, null);

    RestTestClient client = RestTestClient.bindToController(new TestController())
            .configureServer(mockMvcBuilder -> mockMvcBuilder.setApiVersionStrategy(versionStrategy))
            .baseURI("/path")
            .apiVersionInserter(inserter)
            .build();

    return client.get()
            .accept(MediaType.APPLICATION_JSON)
            .apiVersion(1.2)
            .exchange()
            .returnResult(Map.class)
            .getResponseBody();
  }

  @RestController
  private static class TestController {

    private static final String HEADER = "API-Version";

    @GetMapping(path = "/**", version = "1.2")
    Map<String, String> handle(RequestContext request) {
      String query = request.getQueryString();
      String versionHeader = request.getHeader(HEADER);
      return Map.of("path", request.getRequestURI(),
              "query", (query != null ? query : ""),
              HEADER, (versionHeader != null ? versionHeader : ""));
    }
  }
}
