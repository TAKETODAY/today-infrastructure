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

package cn.taketoday.test.web.servlet.setup;

import java.nio.charset.Charset;

import cn.taketoday.test.web.servlet.DispatcherServletCustomizer;
import cn.taketoday.test.web.servlet.MockMvcBuilder;
import cn.taketoday.test.web.servlet.RequestBuilder;
import cn.taketoday.test.web.servlet.ResultHandler;
import cn.taketoday.test.web.servlet.ResultMatcher;
import jakarta.servlet.Filter;

/**
 * Defines common methods for building a {@code MockMvc}.
 *
 * @param <B> a self reference to the builder type
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
public interface ConfigurableMockMvcBuilder<B extends ConfigurableMockMvcBuilder<B>> extends MockMvcBuilder {

  /**
   * Add filters mapped to any request (i.e. "/*"). For example:
   * <pre class="code">
   * mockMvcBuilder.addFilters(springSecurityFilterChain);
   * </pre>
   * <p>It is the equivalent of the following web.xml configuration:
   * <pre class="code">
   * &lt;filter-mapping&gt;
   *     &lt;filter-name&gt;springSecurityFilterChain&lt;/filter-name&gt;
   *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
   * &lt;/filter-mapping&gt;
   * </pre>
   * <p>Filters will be invoked in the order in which they are provided.
   *
   * @param filters the filters to add
   */
  <T extends B> T addFilters(Filter... filters);

  /**
   * Add a filter mapped to a specific set of patterns. For example:
   * <pre class="code">
   * mockMvcBuilder.addFilter(myResourceFilter, "/resources/*");
   * </pre>
   * <p>It is the equivalent of:
   * <pre class="code">
   * &lt;filter-mapping&gt;
   *     &lt;filter-name&gt;myResourceFilter&lt;/filter-name&gt;
   *     &lt;url-pattern&gt;/resources/*&lt;/url-pattern&gt;
   * &lt;/filter-mapping&gt;
   * </pre>
   * <p>Filters will be invoked in the order in which they are provided.
   *
   * @param filter the filter to add
   * @param urlPatterns the URL patterns to map to; if empty, "/*" is used by default
   */
  <T extends B> T addFilter(Filter filter, String... urlPatterns);

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
   * {@link cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders}
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
   * {@link cn.taketoday.test.web.servlet.result.MockMvcResultMatchers}
   */
  <T extends B> T alwaysExpect(ResultMatcher resultMatcher);

  /**
   * Define a global action that should <em>always</em> be applied to every
   * response. For example, writing detailed information about the performed
   * request and resulting response to {@code System.out}.
   *
   * @param resultHandler a ResultHandler; see static factory methods in
   * {@link cn.taketoday.test.web.servlet.result.MockMvcResultHandlers}
   */
  <T extends B> T alwaysDo(ResultHandler resultHandler);

  /**
   * that allows customizing any {@link cn.taketoday.web.servlet.DispatcherServlet}
   * property.
   */
  <T extends B> T addDispatcherServletCustomizer(DispatcherServletCustomizer customizer);

  /**
   * Add a {@code MockMvcConfigurer} that automates MockMvc setup and
   * configures it for some specific purpose (e.g. security).
   * <p>There is a built-in {@link SharedHttpSessionConfigurer} that can be
   * used to re-use the HTTP session across requests. 3rd party frameworks
   * like Infra Security also use this mechanism to provide configuration
   * shortcuts.
   *
   * @see SharedHttpSessionConfigurer
   */
  <T extends B> T apply(MockMvcConfigurer configurer);

}
