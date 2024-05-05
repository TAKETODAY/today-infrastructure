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

package cn.taketoday.test.web.mock.setup;

import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.mock.DispatcherCustomizer;
import cn.taketoday.test.web.mock.MockMvcBuilder;
import cn.taketoday.test.web.mock.RequestBuilder;
import cn.taketoday.test.web.mock.ResultHandler;
import cn.taketoday.test.web.mock.ResultMatcher;
import cn.taketoday.mock.api.DispatcherType;
import cn.taketoday.mock.api.Filter;
import cn.taketoday.mock.api.FilterConfig;
import cn.taketoday.web.mock.MockDispatcher;

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
   * {@link cn.taketoday.mock.web.MockFilterConfig} is created without
   * a name, which defaults to an empty String for the name
   * @param initParams the init parameters to initialize the filter with
   * @param dispatcherTypes dispatcher types the filter applies to
   * @param urlPatterns the URL patterns to map to; if empty, matches all requests
   * @see cn.taketoday.mock.web.MockFilterConfig
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
   * {@link cn.taketoday.test.web.mock.request.MockMvcRequestBuilders}
   */
  <T extends B> T defaultRequest(RequestBuilder requestBuilder);

  /**
   * Define the default character encoding to be applied to every response.
   * <p>The default implementation of this method throws an
   * {@link UnsupportedOperationException}. Concrete implementations are therefore
   * encouraged to override this method.
   *
   * @param defaultResponseCharacterEncoding the default response character encoding
   * @since 5.3.10
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
   * {@link cn.taketoday.test.web.mock.result.MockMvcResultMatchers}
   */
  <T extends B> T alwaysExpect(ResultMatcher resultMatcher);

  /**
   * Define a global action that should <em>always</em> be applied to every
   * response. For example, writing detailed information about the performed
   * request and resulting response to {@code System.out}.
   *
   * @param resultHandler a ResultHandler; see static factory methods in
   * {@link cn.taketoday.test.web.mock.result.MockMvcResultHandlers}
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
