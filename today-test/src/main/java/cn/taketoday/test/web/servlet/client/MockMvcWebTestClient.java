/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.web.servlet.client;

import java.util.function.Supplier;

import cn.taketoday.format.support.FormattingConversionService;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.reactive.server.ExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import cn.taketoday.test.web.servlet.setup.MockMvcBuilders;
import cn.taketoday.test.web.servlet.setup.MockMvcConfigurer;
import cn.taketoday.test.web.servlet.setup.StandaloneMockMvcBuilder;
import cn.taketoday.test.web.servlet.DispatcherServletCustomizer;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MvcResult;
import cn.taketoday.test.web.servlet.RequestBuilder;
import cn.taketoday.test.web.servlet.ResultActions;
import cn.taketoday.test.web.servlet.ResultHandler;
import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.validation.Validator;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.context.WebApplicationContext;
import cn.taketoday.web.method.support.HandlerMethodArgumentResolver;
import cn.taketoday.web.method.support.HandlerMethodReturnValueHandler;
import cn.taketoday.web.servlet.FlashMapManager;
import cn.taketoday.web.servlet.HandlerExceptionResolver;
import cn.taketoday.web.servlet.HandlerInterceptor;
import cn.taketoday.web.servlet.LocaleResolver;
import cn.taketoday.web.servlet.View;
import cn.taketoday.web.servlet.ViewResolver;
import cn.taketoday.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import cn.taketoday.web.util.pattern.PathPatternParser;
import jakarta.servlet.Filter;

/**
 * The main class for testing Spring MVC applications via {@link WebTestClient}
 * with {@link MockMvc} for server request handling.
 *
 * <p>Provides static factory methods and specs to initialize {@code MockMvc}
 * to which the {@code WebTestClient} connects to. For example:
 * <pre class="code">
 * WebTestClient client = MockMvcWebTestClient.bindToController(myController)
 *         .controllerAdvice(myControllerAdvice)
 *         .validator(myValidator)
 *         .build()
 * </pre>
 *
 * <p>The client itself can also be configured. For example:
 * <pre class="code">
 * WebTestClient client = MockMvcWebTestClient.bindToController(myController)
 *         .validator(myValidator)
 *         .configureClient()
 *         .baseUrl("/path")
 *         .build();
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public interface MockMvcWebTestClient {

  /**
   * Begin creating a {@link WebTestClient} by providing the {@code @Controller}
   * instance(s) to handle requests with.
   * <p>Internally this is delegated to and equivalent to using
   * {@link MockMvcBuilders#standaloneSetup(Object...)}.
   * to initialize {@link MockMvc}.
   */
  static ControllerSpec bindToController(Object... controllers) {
    return new StandaloneMockMvcSpec(controllers);
  }

  /**
   * Begin creating a {@link WebTestClient} by providing a
   * {@link WebApplicationContext} with Spring MVC infrastructure and
   * controllers.
   * <p>Internally this is delegated to and equivalent to using
   * {@link MockMvcBuilders#webAppContextSetup(WebApplicationContext)}
   * to initialize {@code MockMvc}.
   */
  static MockMvcServerSpec<?> bindToApplicationContext(WebApplicationContext context) {
    return new ApplicationContextMockMvcSpec(context);
  }

  /**
   * Begin creating a {@link WebTestClient} by providing an already
   * initialized {@link MockMvc} instance to use as the server.
   */
  static WebTestClient.Builder bindTo(MockMvc mockMvc) {
    ClientHttpConnector connector = new MockMvcHttpConnector(mockMvc);
    return WebTestClient.bindToServer(connector);
  }

  /**
   * This method can be used to apply further assertions on a given
   * {@link ExchangeResult} based the state of the server response.
   * <p>Normally {@link WebTestClient} is used to assert the client response
   * including HTTP status, headers, and body. That is all that is available
   * when making a live request over HTTP. However when the server is
   * {@link MockMvc}, many more assertions are possible against the server
   * response, e.g. model attributes, flash attributes, etc.
   *
   * <p>Example:
   * <pre class="code">
   * EntityExchangeResult&lt;Void&gt; result =
   * 		webTestClient.post().uri("/people/123")
   * 				.exchange()
   * 				.expectStatus().isFound()
   * 				.expectHeader().location("/persons/Joe")
   * 				.expectBody().isEmpty();
   *
   * MockMvcWebTestClient.resultActionsFor(result)
   * 		.andExpect(model().size(1))
   * 		.andExpect(model().attributeExists("name"))
   * 		.andExpect(flash().attributeCount(1))
   * 		.andExpect(flash().attribute("message", "success!"));
   * </pre>
   * <p>Note: this method works only if the {@link WebTestClient} used to
   * perform the request was initialized through one of bind method in this
   * class, and therefore requests are handled by {@link MockMvc}.
   */
  static ResultActions resultActionsFor(ExchangeResult exchangeResult) {
    Object serverResult = exchangeResult.getMockServerResult();
    Assert.notNull(serverResult, "No MvcResult");
    Assert.isInstanceOf(MvcResult.class, serverResult);
    return new ResultActions() {
      @Override
      public ResultActions andExpect(ResultMatcher matcher) throws Exception {
        matcher.match((MvcResult) serverResult);
        return this;
      }

      @Override
      public ResultActions andDo(ResultHandler handler) throws Exception {
        handler.handle((MvcResult) serverResult);
        return this;
      }

      @Override
      public MvcResult andReturn() {
        return (MvcResult) serverResult;
      }
    };
  }

  /**
   * Base specification for configuring {@link MockMvc}, and a simple facade
   * around {@link ConfigurableMockMvcBuilder}.
   *
   * @param <B> a self reference to the builder type
   */
  interface MockMvcServerSpec<B extends MockMvcServerSpec<B>> {

    /**
     * Add a global filter.
     * <p>This is delegated to
     * {@link ConfigurableMockMvcBuilder#addFilters(Filter...)}.
     */
    <T extends B> T filters(Filter... filters);

    /**
     * Add a filter for specific URL patterns.
     * <p>This is delegated to
     * {@link ConfigurableMockMvcBuilder#addFilter(Filter, String...)}.
     */
    <T extends B> T filter(Filter filter, String... urlPatterns);

    /**
     * Define default request properties that should be merged into all
     * performed requests such that input from the client request override
     * the default properties defined here.
     * <p>This is delegated to
     * {@link ConfigurableMockMvcBuilder#defaultRequest(RequestBuilder)}.
     */
    <T extends B> T defaultRequest(RequestBuilder requestBuilder);

    /**
     * Define a global expectation that should <em>always</em> be applied to
     * every response.
     * <p>This is delegated to
     * {@link ConfigurableMockMvcBuilder#alwaysExpect(ResultMatcher)}.
     */
    <T extends B> T alwaysExpect(ResultMatcher resultMatcher);

    /**
     * Whether to handle HTTP OPTIONS requests.
     * <p>This is delegated to
     * {@link ConfigurableMockMvcBuilder#dispatchOptions(boolean)}.
     */
    <T extends B> T dispatchOptions(boolean dispatchOptions);

    /**
     * Allow customization of {@code DispatcherServlet}.
     * <p>This is delegated to
     * {@link ConfigurableMockMvcBuilder#addDispatcherServletCustomizer(DispatcherServletCustomizer)}.
     */
    <T extends B> T dispatcherServletCustomizer(DispatcherServletCustomizer customizer);

    /**
     * Add a {@code MockMvcConfigurer} that automates MockMvc setup.
     * <p>This is delegated to
     * {@link ConfigurableMockMvcBuilder#apply(MockMvcConfigurer)}.
     */
    <T extends B> T apply(MockMvcConfigurer configurer);

    /**
     * Proceed to configure and build the test client.
     */
    WebTestClient.Builder configureClient();

    /**
     * Shortcut to build the test client.
     */
    WebTestClient build();
  }

  /**
   * Specification for configuring {@link MockMvc} to test one or more
   * controllers directly, and a simple facade around
   * {@link StandaloneMockMvcBuilder}.
   */
  interface ControllerSpec extends MockMvcServerSpec<ControllerSpec> {

    /**
     * Register {@link cn.taketoday.web.bind.annotation.ControllerAdvice}
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setControllerAdvice(Object...)}.
     */
    ControllerSpec controllerAdvice(Object... controllerAdvice);

    /**
     * Set the message converters to use.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setMessageConverters(HttpMessageConverter[])}.
     */
    ControllerSpec messageConverters(HttpMessageConverter<?>... messageConverters);

    /**
     * Provide a custom {@link Validator}.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setValidator(Validator)}.
     */
    ControllerSpec validator(Validator validator);

    /**
     * Provide a conversion service.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setConversionService(FormattingConversionService)}.
     */
    ControllerSpec conversionService(FormattingConversionService conversionService);

    /**
     * Add global interceptors.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#addInterceptors(HandlerInterceptor...)}.
     */
    ControllerSpec interceptors(HandlerInterceptor... interceptors);

    /**
     * Add interceptors for specific patterns.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#addMappedInterceptors(String[], HandlerInterceptor...)}.
     */
    ControllerSpec mappedInterceptors(
            @Nullable String[] pathPatterns, HandlerInterceptor... interceptors);

    /**
     * Set a ContentNegotiationManager.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setContentNegotiationManager(ContentNegotiationManager)}.
     */
    ControllerSpec contentNegotiationManager(ContentNegotiationManager manager);

    /**
     * Specify the timeout value for async execution.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setAsyncRequestTimeout(long)}.
     */
    ControllerSpec asyncRequestTimeout(long timeout);

    /**
     * Provide custom argument resolvers.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setCustomArgumentResolvers(HandlerMethodArgumentResolver...)}.
     */
    ControllerSpec customArgumentResolvers(HandlerMethodArgumentResolver... argumentResolvers);

    /**
     * Provide custom return value handlers.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setCustomReturnValueHandlers(HandlerMethodReturnValueHandler...)}.
     */
    ControllerSpec customReturnValueHandlers(HandlerMethodReturnValueHandler... handlers);

    /**
     * Set the HandlerExceptionResolver types to use.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setHandlerExceptionResolvers(HandlerExceptionResolver...)}.
     */
    ControllerSpec handlerExceptionResolvers(HandlerExceptionResolver... exceptionResolvers);

    /**
     * Set up view resolution.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setViewResolvers(ViewResolver...)}.
     */
    ControllerSpec viewResolvers(ViewResolver... resolvers);

    /**
     * Set up a single {@link ViewResolver} with a fixed view.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setSingleView(View)}.
     */
    ControllerSpec singleView(View view);

    /**
     * Provide the LocaleResolver to use.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setLocaleResolver(LocaleResolver)}.
     */
    ControllerSpec localeResolver(LocaleResolver localeResolver);

    /**
     * Provide a custom FlashMapManager.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setFlashMapManager(FlashMapManager)}.
     */
    ControllerSpec flashMapManager(FlashMapManager flashMapManager);

    /**
     * Enable URL path matching with parsed
     * {@link cn.taketoday.web.util.pattern.PathPattern PathPatterns}.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setPatternParser(PathPatternParser)}.
     */
    ControllerSpec patternParser(PathPatternParser parser);

    /**
     * Whether to match trailing slashes.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setUseTrailingSlashPatternMatch(boolean)}.
     */
    ControllerSpec useTrailingSlashPatternMatch(boolean useTrailingSlashPatternMatch);

    /**
     * Configure placeholder values to use.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#addPlaceholderValue(String, String)}.
     */
    ControllerSpec placeholderValue(String name, String value);

    /**
     * Configure factory for a custom {@link RequestMappingHandlerMapping}.
     * <p>This is delegated to
     * {@link StandaloneMockMvcBuilder#setCustomHandlerMapping(Supplier)}.
     */
    ControllerSpec customHandlerMapping(Supplier<RequestMappingHandlerMapping> factory);
  }

}
