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

import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.client.RestTemplate;

/**
 * Convenient super class for application classes that need REST access.
 *
 * <p>Requires a {@link ClientHttpRequestFactory} or a {@link RestTemplate} instance to be set.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setRestTemplate
 * @see cn.taketoday.web.client.RestTemplate
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
