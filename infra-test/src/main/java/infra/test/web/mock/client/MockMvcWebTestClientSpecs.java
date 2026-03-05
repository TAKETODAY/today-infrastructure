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

import java.util.function.Supplier;

import infra.format.support.FormattingConversionService;
import infra.http.client.reactive.ClientHttpConnector;
import infra.http.converter.HttpMessageConverter;
import infra.test.web.reactive.server.WebTestClient;
import infra.test.web.mock.DispatcherServletCustomizer;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.RequestBuilder;
import infra.test.web.mock.ResultMatcher;
import infra.test.web.mock.client.MockMvcWebTestClient.MockMvcServerSpec;
import infra.test.web.mock.setup.ConfigurableMockMvcBuilder;
import infra.test.web.mock.setup.DefaultMockMvcBuilder;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.test.web.mock.setup.MockMvcConfigurer;
import infra.test.web.mock.setup.RouterFunctionMockMvcBuilder;
import infra.test.web.mock.setup.StandaloneMockMvcBuilder;
import infra.validation.Validator;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.ContentNegotiationManager;
import infra.web.context.WebApplicationContext;
import infra.web.method.support.HandlerMethodArgumentResolver;
import infra.web.method.support.HandlerMethodReturnValueHandler;
import infra.web.servlet.FlashMapManager;
import infra.web.servlet.HandlerExceptionResolver;
import infra.web.servlet.HandlerInterceptor;
import infra.web.servlet.LocaleResolver;
import infra.web.servlet.View;
import infra.web.servlet.ViewResolver;
import infra.web.servlet.function.RouterFunction;
import infra.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import infra.web.util.pattern.PathPatternParser;
import jakarta.servlet.Filter;

/**
 * Container class to encapsulate the {@link MockMvcServerSpec} implementation
 * hierarchy. This class was added in 7.0 to reduce mixing WebTestClient and
 * RestTestClient classes in the same package.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
abstract class MockMvcWebTestClientSpecs {

  /**
   * Base class for implementations of {@link MockMvcServerSpec}
   * that simply delegates to a {@link ConfigurableMockMvcBuilder} supplied by
   * the concrete subclasses.
   *
   * @param <B> the type of the concrete subclass spec
   * @author Rossen Stoyanchev
   * @since 5.3
   */
  abstract static class AbstractMockMvcServerSpec<B extends MockMvcServerSpec<B>>
          implements MockMvcServerSpec<B> {

    @Override
    public <T extends B> T filters(Filter... filters) {
      getMockMvcBuilder().addFilters(filters);
      return self();
    }

    @Override
    public final <T extends B> T filter(Filter filter, String... urlPatterns) {
      getMockMvcBuilder().addFilter(filter, urlPatterns);
      return self();
    }

    @Override
    public <T extends B> T defaultRequest(RequestBuilder requestBuilder) {
      getMockMvcBuilder().defaultRequest(requestBuilder);
      return self();
    }

    @Override
    public <T extends B> T alwaysExpect(ResultMatcher resultMatcher) {
      getMockMvcBuilder().alwaysExpect(resultMatcher);
      return self();
    }

    @Override
    public <T extends B> T dispatchOptions(boolean dispatchOptions) {
      getMockMvcBuilder().dispatchOptions(dispatchOptions);
      return self();
    }

    @Override
    public <T extends B> T dispatcherServletCustomizer(DispatcherServletCustomizer customizer) {
      getMockMvcBuilder().addDispatcherServletCustomizer(customizer);
      return self();
    }

    @Override
    public <T extends B> T apply(MockMvcConfigurer configurer) {
      getMockMvcBuilder().apply(configurer);
      return self();
    }

    @SuppressWarnings("unchecked")
    private <T extends B> T self() {
      return (T) this;
    }

    /**
     * Return the concrete {@link ConfigurableMockMvcBuilder} to delegate
     * configuration methods and to use to create the {@link MockMvc}.
     */
    protected abstract ConfigurableMockMvcBuilder<?> getMockMvcBuilder();

    @Override
    public WebTestClient.Builder configureClient() {
      MockMvc mockMvc = getMockMvcBuilder().build();
      ClientHttpConnector connector = new MockMvcHttpConnector(mockMvc);
      return WebTestClient.bindToServer(connector);
    }

    @Override
    public WebTestClient build() {
      return configureClient().build();
    }

  }

  /**
   * Simple wrapper around a {@link DefaultMockMvcBuilder}.
   *
   * @author Rossen Stoyanchev
   * @since 5.3
   */
  static class ApplicationContextMockMvcSpec extends AbstractMockMvcServerSpec<ApplicationContextMockMvcSpec> {

    private final DefaultMockMvcBuilder mockMvcBuilder;

    public ApplicationContextMockMvcSpec(WebApplicationContext context) {
      this.mockMvcBuilder = MockMvcBuilders.webAppContextSetup(context);
    }

    @Override
    protected ConfigurableMockMvcBuilder<?> getMockMvcBuilder() {
      return this.mockMvcBuilder;
    }

  }

  /**
   * Simple wrapper around a {@link RouterFunctionMockMvcBuilder} that implements
   * {@link MockMvcWebTestClient.RouterFunctionSpec}.
   *
   * @author Arjen Poutsma
   * @since 5.0
   */
  static class RouterFunctionMockMvcSpec extends AbstractMockMvcServerSpec<MockMvcWebTestClient.RouterFunctionSpec>
          implements MockMvcWebTestClient.RouterFunctionSpec {

    private final RouterFunctionMockMvcBuilder mockMvcBuilder;

    RouterFunctionMockMvcSpec(RouterFunction<?>... routerFunctions) {
      this.mockMvcBuilder = MockMvcBuilders.routerFunctions(routerFunctions);
    }

    @Override
    public MockMvcWebTestClient.RouterFunctionSpec messageConverters(HttpMessageConverter<?>... messageConverters) {
      this.mockMvcBuilder.setMessageConverters(messageConverters);
      return this;
    }

    @Override
    public MockMvcWebTestClient.RouterFunctionSpec interceptors(HandlerInterceptor... interceptors) {
      mappedInterceptors(null, interceptors);
      return this;
    }

    @Override
    public MockMvcWebTestClient.RouterFunctionSpec mappedInterceptors(String @Nullable [] pathPatterns, HandlerInterceptor... interceptors) {
      this.mockMvcBuilder.addMappedInterceptors(pathPatterns, interceptors);
      return this;
    }

    @Override
    public MockMvcWebTestClient.RouterFunctionSpec asyncRequestTimeout(long timeout) {
      this.mockMvcBuilder.setAsyncRequestTimeout(timeout);
      return this;
    }

    @Override
    public MockMvcWebTestClient.RouterFunctionSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
      this.mockMvcBuilder.setHandlerExceptionResolvers(exceptionResolvers);
      return this;
    }

    @Override
    public MockMvcWebTestClient.RouterFunctionSpec viewResolvers(ViewResolver... resolvers) {
      this.mockMvcBuilder.setViewResolvers(resolvers);
      return this;
    }

    @Override
    public MockMvcWebTestClient.RouterFunctionSpec singleView(View view) {
      this.mockMvcBuilder.setSingleView(view);
      return this;
    }

    @Override
    public MockMvcWebTestClient.RouterFunctionSpec patternParser(PathPatternParser parser) {
      this.mockMvcBuilder.setPatternParser(parser);
      return this;
    }

    @Override
    protected ConfigurableMockMvcBuilder<?> getMockMvcBuilder() {
      return this.mockMvcBuilder;
    }
  }

  /**
   * Simple wrapper around a {@link StandaloneMockMvcBuilder} that implements
   * {@link MockMvcWebTestClient.ControllerSpec}.
   *
   * @author Rossen Stoyanchev
   * @since 5.3
   */
  static class StandaloneMockMvcSpec extends AbstractMockMvcServerSpec<MockMvcWebTestClient.ControllerSpec>
          implements MockMvcWebTestClient.ControllerSpec {

    private final StandaloneMockMvcBuilder mockMvcBuilder;

    StandaloneMockMvcSpec(Object... controllers) {
      this.mockMvcBuilder = MockMvcBuilders.standaloneSetup(controllers);
    }

    @Override
    public StandaloneMockMvcSpec controllerAdvice(Object... controllerAdvice) {
      this.mockMvcBuilder.setControllerAdvice(controllerAdvice);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec messageConverters(HttpMessageConverter<?>... messageConverters) {
      this.mockMvcBuilder.setMessageConverters(messageConverters);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec validator(Validator validator) {
      this.mockMvcBuilder.setValidator(validator);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec conversionService(FormattingConversionService conversionService) {
      this.mockMvcBuilder.setConversionService(conversionService);
      return this;
    }

    @Override
    public MockMvcWebTestClient.ControllerSpec apiVersionStrategy(ApiVersionStrategy versionStrategy) {
      this.mockMvcBuilder.setApiVersionStrategy(versionStrategy);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec interceptors(HandlerInterceptor... interceptors) {
      mappedInterceptors(null, interceptors);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec mappedInterceptors(
            String @Nullable [] pathPatterns, HandlerInterceptor... interceptors) {

      this.mockMvcBuilder.addMappedInterceptors(pathPatterns, interceptors);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec contentNegotiationManager(ContentNegotiationManager manager) {
      this.mockMvcBuilder.setContentNegotiationManager(manager);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec asyncRequestTimeout(long timeout) {
      this.mockMvcBuilder.setAsyncRequestTimeout(timeout);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec customArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers) {
      this.mockMvcBuilder.setCustomArgumentResolvers(argumentResolvers);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec customReturnValueHandlers(HandlerMethodReturnValueHandler... handlers) {
      this.mockMvcBuilder.setCustomReturnValueHandlers(handlers);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers) {
      this.mockMvcBuilder.setHandlerExceptionResolvers(exceptionResolvers);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec viewResolvers(ViewResolver... resolvers) {
      this.mockMvcBuilder.setViewResolvers(resolvers);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec singleView(View view) {
      this.mockMvcBuilder.setSingleView(view);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec localeResolver(LocaleResolver localeResolver) {
      this.mockMvcBuilder.setLocaleResolver(localeResolver);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec flashMapManager(FlashMapManager flashMapManager) {
      this.mockMvcBuilder.setFlashMapManager(flashMapManager);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec patternParser(PathPatternParser parser) {
      this.mockMvcBuilder.setPatternParser(parser);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec placeholderValue(String name, String value) {
      this.mockMvcBuilder.addPlaceholderValue(name, value);
      return this;
    }

    @Override
    public StandaloneMockMvcSpec customHandlerMapping(Supplier<RequestMappingHandlerMapping> factory) {
      this.mockMvcBuilder.setCustomHandlerMapping(factory);
      return this;
    }

    @Override
    public ConfigurableMockMvcBuilder<?> getMockMvcBuilder() {
      return this.mockMvcBuilder;
    }
  }
}
