/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.htmlunit;

import org.htmlunit.WebClient;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.setup.MockMvcConfigurer;
import cn.taketoday.web.servlet.WebApplicationContext;

/**
 * {@code MockMvcWebClientBuilder} simplifies the creation of an HtmlUnit
 * {@link WebClient} that delegates to a {@link MockMvc} instance.
 *
 * <p>The {@code MockMvc} instance used by the builder may be
 * {@linkplain #mockMvcSetup supplied directly} or created transparently
 * from a {@link #webAppContextSetup WebApplicationContext}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @see #mockMvcSetup(MockMvc)
 * @see #webAppContextSetup(WebApplicationContext)
 * @see #webAppContextSetup(WebApplicationContext, MockMvcConfigurer)
 * @see #withDelegate(WebClient)
 * @see #build()
 * @since 4.0
 */
public class MockMvcWebClientBuilder extends MockMvcWebConnectionBuilderSupport<MockMvcWebClientBuilder> {

  @Nullable
  private WebClient webClient;

  protected MockMvcWebClientBuilder(MockMvc mockMvc) {
    super(mockMvc);
  }

  protected MockMvcWebClientBuilder(WebApplicationContext context) {
    super(context);
  }

  protected MockMvcWebClientBuilder(WebApplicationContext context, MockMvcConfigurer configurer) {
    super(context, configurer);
  }

  /**
   * Create a new {@code MockMvcWebClientBuilder} based on the supplied
   * {@link MockMvc} instance.
   *
   * @param mockMvc the {@code MockMvc} instance to use; never {@code null}
   * @return the MockMvcWebClientBuilder to customize
   */
  public static MockMvcWebClientBuilder mockMvcSetup(MockMvc mockMvc) {
    Assert.notNull(mockMvc, "MockMvc must not be null");
    return new MockMvcWebClientBuilder(mockMvc);
  }

  /**
   * Create a new {@code MockMvcWebClientBuilder} based on the supplied
   * {@link WebApplicationContext}.
   *
   * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
   * instance from; never {@code null}
   * @return the MockMvcWebClientBuilder to customize
   */
  public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context) {
    Assert.notNull(context, "WebApplicationContext must not be null");
    return new MockMvcWebClientBuilder(context);
  }

  /**
   * Create a new {@code MockMvcWebClientBuilder} based on the supplied
   * {@link WebApplicationContext} and {@link MockMvcConfigurer}.
   *
   * @param context the {@code WebApplicationContext} to create a {@link MockMvc}
   * instance from; never {@code null}
   * @param configurer the {@code MockMvcConfigurer} to apply; never {@code null}
   * @return the MockMvcWebClientBuilder to customize
   */
  public static MockMvcWebClientBuilder webAppContextSetup(WebApplicationContext context, MockMvcConfigurer configurer) {
    Assert.notNull(context, "WebApplicationContext must not be null");
    Assert.notNull(configurer, "MockMvcConfigurer must not be null");
    return new MockMvcWebClientBuilder(context, configurer);
  }

  /**
   * Supply the {@code WebClient} that the client {@linkplain #build built}
   * by this builder should delegate to when processing
   * non-{@linkplain WebRequestMatcher matching} requests.
   *
   * @param webClient the {@code WebClient} to delegate to for requests
   * that do not match; never {@code null}
   * @return this builder for further customization
   * @see #build()
   */
  public MockMvcWebClientBuilder withDelegate(WebClient webClient) {
    Assert.notNull(webClient, "WebClient must not be null");
    webClient.setWebConnection(createConnection(webClient));
    this.webClient = webClient;
    return this;
  }

  /**
   * Build the {@link WebClient} configured via this builder.
   * <p>The returned client will use the configured {@link MockMvc} instance
   * for processing any {@linkplain WebRequestMatcher matching} requests
   * and a delegate {@code WebClient} for all other requests.
   * <p>If a {@linkplain #withDelegate delegate} has been explicitly configured,
   * it will be used; otherwise, a default {@code WebClient} will be configured
   * as the delegate.
   *
   * @return the {@code WebClient} to use
   * @see #withDelegate(WebClient)
   */
  public WebClient build() {
    return (this.webClient != null ? this.webClient : withDelegate(new WebClient()).build());
  }

}
