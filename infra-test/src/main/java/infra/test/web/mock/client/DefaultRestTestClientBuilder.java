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

package infra.test.web.mock.client;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import infra.context.ApplicationContext;
import infra.http.HttpHeaders;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestInterceptor;
import infra.http.converter.HttpMessageConverters;
import infra.lang.Assert;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MockMvcBuilder;
import infra.test.web.mock.client.RestTestClient.MockMvcSetupBuilder;
import infra.test.web.mock.client.RestTestClient.StandaloneSetupBuilder;
import infra.test.web.mock.client.RestTestClient.WebAppContextSetupBuilder;
import infra.test.web.mock.setup.DefaultMockMvcBuilder;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.test.web.mock.setup.StandaloneMockMvcBuilder;
import infra.util.MultiValueMap;
import infra.web.client.ApiVersionInserter;
import infra.web.client.RestClient;
import infra.web.util.UriBuilderFactory;

/**
 * Default implementation of {@link RestTestClient.Builder}.
 *
 * @param <B> the type of the builder
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultRestTestClientBuilder<B extends RestTestClient.Builder<B>> implements RestTestClient.Builder<B> {

  private final RestClient.Builder restClientBuilder;

  private @Nullable Consumer<HttpMessageConverters.ClientBuilder> convertersConfigurer;

  private Consumer<EntityExchangeResult<?>> entityResultConsumer = result -> { };

  DefaultRestTestClientBuilder() {
    this(RestClient.builder());
  }

  DefaultRestTestClientBuilder(RestClient.Builder restClientBuilder) {
    this.restClientBuilder = restClientBuilder.bufferContent(httpRequest -> true);
  }

  DefaultRestTestClientBuilder(DefaultRestTestClientBuilder<B> other) {
    this.restClientBuilder = other.restClientBuilder.clone();
    this.convertersConfigurer = other.convertersConfigurer;
    this.entityResultConsumer = other.entityResultConsumer;
  }

  @Override
  public <T extends B> T baseURI(String uri) {
    this.restClientBuilder.baseURI(uri);
    return self();
  }

  @Override
  public <T extends B> T uriBuilderFactory(UriBuilderFactory uriFactory) {
    this.restClientBuilder.uriBuilderFactory(uriFactory);
    return self();
  }

  @Override
  public <T extends B> T defaultHeader(String headerName, String... headerValues) {
    this.restClientBuilder.defaultHeader(headerName, headerValues);
    return self();
  }

  @Override
  public <T extends B> T defaultHeaders(Consumer<HttpHeaders> headersConsumer) {
    this.restClientBuilder.defaultHeaders(headersConsumer);
    return self();
  }

  @Override
  public <T extends B> T defaultCookie(String cookieName, String... cookieValues) {
    this.restClientBuilder.defaultCookie(cookieName, cookieValues);
    return self();
  }

  @Override
  public <T extends B> T defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
    this.restClientBuilder.defaultCookies(cookiesConsumer);
    return self();
  }

  @Override
  public <T extends B> T defaultApiVersion(Object version) {
    this.restClientBuilder.defaultApiVersion(version);
    return self();
  }

  @Override
  public <T extends B> T apiVersionInserter(@Nullable ApiVersionInserter apiVersionInserter) {
    this.restClientBuilder.apiVersionInserter(apiVersionInserter);
    return self();
  }

  @Override
  public <T extends B> T requestInterceptor(ClientHttpRequestInterceptor interceptor) {
    this.restClientBuilder.requestInterceptor(interceptor);
    return self();
  }

  @Override
  public <T extends B> T requestInterceptors(Consumer<List<ClientHttpRequestInterceptor>> interceptorsConsumer) {
    this.restClientBuilder.requestInterceptors(interceptorsConsumer);
    return self();
  }

  @Override
  public <T extends B> T configureMessageConverters(Consumer<HttpMessageConverters.ClientBuilder> configurer) {
    this.convertersConfigurer = (this.convertersConfigurer != null ?
            this.convertersConfigurer.andThen(configurer) : configurer);
    return self();
  }

  @Override
  public <T extends B> T entityExchangeResultConsumer(Consumer<EntityExchangeResult<?>> entityResultConsumer) {
    Assert.notNull(entityResultConsumer, "'entityResultConsumer' is required");
    this.entityResultConsumer = this.entityResultConsumer.andThen(entityResultConsumer);
    return self();
  }

  @SuppressWarnings("unchecked")
  protected <T extends B> T self() {
    return (T) this;
  }

  protected void setClientHttpRequestFactory(ClientHttpRequestFactory requestFactory) {
    this.restClientBuilder.requestFactory(requestFactory);
  }

  @Override
  public RestTestClient build() {

    if (this.convertersConfigurer != null) {
      this.restClientBuilder.configureMessageConverters(this.convertersConfigurer);
    }

    return new DefaultRestTestClient(
            this.restClientBuilder, this.entityResultConsumer, new DefaultRestTestClientBuilder<>(this));
  }

  /**
   * Base class for implementations for {@link MockMvcSetupBuilder}.
   *
   * @param <S> the "self" type of the builder
   * @param <M> the type of {@link MockMvc} builder
   */
  static class AbstractMockMvcSetupBuilder<S extends RestTestClient.Builder<S>, M extends MockMvcBuilder>
          extends DefaultRestTestClientBuilder<S> implements MockMvcSetupBuilder<S, M> {

    private final M mockMvcBuilder;

    public AbstractMockMvcSetupBuilder(M mockMvcBuilder) {
      this.mockMvcBuilder = mockMvcBuilder;
    }

    @Override
    public <T extends S> T configureServer(Consumer<M> consumer) {
      consumer.accept(this.mockMvcBuilder);
      return self();
    }

    @Override
    public RestTestClient build() {
      MockMvc mockMvc = this.mockMvcBuilder.build();
      setClientHttpRequestFactory(new MockMvcClientHttpRequestFactory(mockMvc));
      return super.build();
    }
  }

  /**
   * Default implementation of {@link StandaloneSetupBuilder}.
   */
  static class DefaultStandaloneSetupBuilder
          extends AbstractMockMvcSetupBuilder<StandaloneSetupBuilder, StandaloneMockMvcBuilder>
          implements StandaloneSetupBuilder {

    DefaultStandaloneSetupBuilder(Object... controllers) {
      super(MockMvcBuilders.standaloneSetup(controllers));
    }
  }

  /**
   * Default implementation of {@link WebAppContextSetupBuilder}.
   */
  static class DefaultWebAppContextSetupBuilder
          extends AbstractMockMvcSetupBuilder<WebAppContextSetupBuilder, DefaultMockMvcBuilder>
          implements WebAppContextSetupBuilder {

    DefaultWebAppContextSetupBuilder(ApplicationContext context) {
      super(MockMvcBuilders.webAppContextSetup(context));
    }
  }

}
