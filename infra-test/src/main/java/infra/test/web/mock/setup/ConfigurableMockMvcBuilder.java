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

package infra.test.web.mock.setup;

import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Map;

import infra.mock.api.DispatcherType;
import infra.mock.api.Filter;
import infra.mock.api.FilterConfig;
import infra.mock.web.MockFilterConfig;
import infra.test.web.mock.DispatcherCustomizer;
import infra.test.web.mock.MockMvcBuilder;
import infra.test.web.mock.RequestBuilder;
import infra.test.web.mock.ResultHandler;
import infra.test.web.mock.ResultMatcher;
import infra.test.web.mock.request.MockMvcRequestBuilders;
import infra.test.web.mock.result.MockMvcResultHandlers;
import infra.test.web.mock.result.MockMvcResultMatchers;
import infra.web.mock.MockDispatcher;

/**
 * Defines common methods for building a {@code MockMvc}.
 *
 * @param <B> a self reference to the builder type
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ConfigurableMockMvcBuilder<B extends ConfigurableMockMvcBuilder<B>> extends MockMvcBuilder {

  /**
   * Add filters mapped to all requests. Filters are invoked in the same order.
   * <p>Note: if you need the filter to be initialized with {@link Filter#init(FilterConfig)},
   * please use {@link #addFilter(Filter, String, Map, EnumSet, String...)} instead.
   *
   * @param filters the filters to add
   */
  <T extends B> T addFilters(Filter... filters);

  /**
   * Add a filter mapped to specific patterns.
   * <p>Note: if you need the filter to be initialized with {@link Filter#init(FilterConfig)},
   * please use {@link #addFilter(Filter, String, Map, EnumSet, String...)} instead.
   *
   * @param filter the filter to add
   * @param urlPatterns the URL patterns to map to; if empty, matches all requests
   */
  <T extends B> T addFilter(Filter filter, String... urlPatterns);

  /**
   * Add a filter that will be initialized via {@link Filter#init(FilterConfig)}
   * with the given init parameters, and will also apply only to requests that
   * match the given dispatcher types and URL patterns.
   *
   * @param filter the filter to add
   * @param filterName the name to use for the filter; if {@code null}, then
   * {@link MockFilterConfig} is created without
   * a name, which defaults to an empty String for the name
   * @param initParams the init parameters to initialize the filter with
   * @param dispatcherTypes dispatcher types the filter applies to
   * @param urlPatterns the URL patterns to map to; if empty, matches all requests
   * @see MockFilterConfig
   */
  <T extends B> T addFilter(Filter filter, @Nullable String filterName,
          Map<String, String> initParams, EnumSet<DispatcherType> dispatcherTypes, String... urlPatterns);

  /**
   * Define default request properties that should be merged into all
   * performed requests. In effect this provides a mechanism for defining
   * common initialization for all requests such as the content type, request
   * parameters, session attributes, and any other request property.
   *
   * <p>Properties specified at the time of performing a request override the
   * default properties defined here.
   *
   * @param requestBuilder a RequestBuilder; see static factory methods in
   * {@link MockMvcRequestBuilders}
   */
  <T extends B> T defaultRequest(RequestBuilder requestBuilder);

  /**
   * Define the default character encoding to be applied to every response.
   * <p>The default implementation of this method throws an
   * {@link UnsupportedOperationException}. Concrete implementations are therefore
   * encouraged to override this method.
   *
   * @param defaultResponseCharacterEncoding the default response character encoding
   */
  default <T extends B> T defaultResponseCharacterEncoding(Charset defaultResponseCharacterEncoding) {
    throw new UnsupportedOperationException("defaultResponseCharacterEncoding is not supported by this MockMvcBuilder");
  }

  /**
   * Define a global expectation that should <em>always</em> be applied to
   * every response. For example, status code 200 (OK), content type
   * {@code "application/json"}, etc.
   *
   * @param resultMatcher a ResultMatcher; see static factory methods in
   * {@link MockMvcResultMatchers}
   */
  <T extends B> T alwaysExpect(ResultMatcher resultMatcher);

  /**
   * Define a global action that should <em>always</em> be applied to every
   * response. For example, writing detailed information about the performed
   * request and resulting response to {@code System.out}.
   *
   * @param resultHandler a ResultHandler; see static factory methods in
   * {@link MockMvcResultHandlers}
   */
  <T extends B> T alwaysDo(ResultHandler resultHandler);

  /**
   * A more advanced that allows
   * customizing any {@link MockDispatcher}
   * property.
   */
  <T extends B> T addDispatcherCustomizer(DispatcherCustomizer customizer);

  /**
   * Add a {@code MockMvcConfigurer} that automates MockMvc setup and
   * configures it for some specific purpose (e.g. security).
   */
  <T extends B> T apply(MockMvcConfigurer configurer);

}
