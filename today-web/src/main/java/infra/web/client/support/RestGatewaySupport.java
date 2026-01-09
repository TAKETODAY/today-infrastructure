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

package infra.web.client.support;

import infra.http.client.ClientHttpRequestFactory;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.client.RestTemplate;

/**
 * Convenient super class for application classes that need REST access.
 *
 * <p>Requires a {@link ClientHttpRequestFactory} or a {@link RestTemplate} instance to be set.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setRestTemplate
 * @see infra.web.client.RestTemplate
 * @since 4.0
 */
public class RestGatewaySupport {

  /** Logger available to subclasses. */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private RestTemplate restTemplate;

  /**
   * Construct a new instance of the {@link RestGatewaySupport}, with default parameters.
   */
  public RestGatewaySupport() {
    this.restTemplate = new RestTemplate();
  }

  /**
   * Construct a new instance of the {@link RestGatewaySupport}, with the given {@link ClientHttpRequestFactory}.
   *
   * @see RestTemplate#RestTemplate(ClientHttpRequestFactory)
   */
  public RestGatewaySupport(ClientHttpRequestFactory requestFactory) {
    Assert.notNull(requestFactory, "'requestFactory' is required");
    this.restTemplate = new RestTemplate(requestFactory);
  }

  /**
   * Sets the {@link RestTemplate} for the gateway.
   */
  public void setRestTemplate(RestTemplate restTemplate) {
    Assert.notNull(restTemplate, "'restTemplate' is required");
    this.restTemplate = restTemplate;
  }

  /**
   * Returns the {@link RestTemplate} for the gateway.
   */
  public RestTemplate getRestTemplate() {
    return this.restTemplate;
  }

}
