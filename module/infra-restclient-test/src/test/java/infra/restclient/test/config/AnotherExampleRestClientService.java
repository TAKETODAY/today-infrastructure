/*
 * Copyright 2012-present the original author or authors.
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

package infra.restclient.test.config;

import org.jspecify.annotations.Nullable;

import infra.stereotype.Service;
import infra.web.client.RestClient;

/**
 * A second example web client used with {@link RestClientTest @RestClientTest} tests.
 *
 * @author Scott Frederick
 */
@Service
public class AnotherExampleRestClientService {

  private final RestClient.Builder builder;

  private final RestClient restClient;

  public AnotherExampleRestClientService(RestClient.Builder builder) {
    this.builder = builder;
    this.restClient = builder.baseURI("https://example.com").build();
  }

  protected RestClient.Builder getRestClientBuilder() {
    return this.builder;
  }

  public @Nullable String test() {
    return this.restClient.get().uri("/test").retrieve().toEntity(String.class).getBody();
  }

}
