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

package cn.taketoday.test.web.servlet.setup;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.web.MockMockConfig;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.test.web.servlet.DispatcherServletCustomizer;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.test.web.servlet.MockMvcBuilder;
import cn.taketoday.test.web.servlet.MockMvcBuilderSupport;
import cn.taketoday.test.web.servlet.RequestBuilder;
import cn.taketoday.test.web.servlet.ResultHandler;
import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.test.web.servlet.request.ConfigurableSmartRequestBuilder;
import cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders;
import cn.taketoday.test.web.servlet.request.RequestPostProcessor;
import cn.taketoday.web.mock.WebApplicationContext;
import cn.taketoday.mock.api.DispatcherType;
import cn.taketoday.mock.api.Filter;
import cn.taketoday.mock.api.ServletException;

/**
 * Abstract implementation of {@link MockMvcBuilder} with common methods for
 * configuring filters, default request properties, global expectations and
 * global result actions.
 *
 * <p>Subclasses can use different strategies to prepare the Infra
 * {@code WebApplicationContext} that will be passed to the
 * {@code DispatcherServlet}.
 *
 * @param <B> a self reference to the builder type
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractMockMvcBuilder<B extends AbstractMockMvcBuilder<B>>
        extends MockMvcBuilderSupport implements ConfigurableMockMvcBuilder<B> {

  private final List<Filter> filters = new ArrayList<>();

  @Nullable
  private RequestBuilder defaultRequestBuilder;

  @Nullable
  private Charset defaultResponseCharacterEncoding;

  private final List<ResultMatcher> globalResultMatchers = new ArrayList<>();

  private final List<ResultHandler> globalResultHandlers = new ArrayList<>();

  private final List<DispatcherServletCustomizer> dispatcherServletCustomizers = new ArrayList<>();

  private final List<MockMvcConfigurer> configurers = new ArrayList<>(4);

  @Override
  public final <T extends B> T addFilters(Filter... filters) {
    Assert.notNull(filters, "filters cannot be null");
    for (Filter filter : filters) {
      Assert.notNull(filter, "filters cannot contain null values");
      this.filters.add(filter);
    }
    return self();
  }

  @Override
  public final <T extends B> T addFilter(Filter filter, String... urlPatterns) {
    Assert.notNull(filter, "filter cannot be null");
    Assert.notNull(urlPatterns, "urlPatterns cannot be null");
    if (urlPatterns.length > 0) {
      filter = new MockMvcFilterDecorator(filter, urlPatterns);
    }
    this.filters.add(filter);
    return self();
  }

  @Override
  public <T extends B> T addFilter(Filter filter, @Nullable String filterName,
          Map<String, String> initParams, EnumSet<DispatcherType> dispatcherTypes, String... urlPatterns) {

    filter = new MockMvcFilterDecorator(filter, filterName, initParams, dispatcherTypes, urlPatterns);
    this.filters.add(filter);
    return self();
  }

  @Override
  public final <T extends B> T defaultRequest(RequestBuilder requestBuilder) {
    this.defaultRequestBuilder = requestBuilder;
    return self();
  }

  /**
   * Define the default character encoding to be applied to every response.
   *
   * @param defaultResponseCharacterEncoding the default response character encoding
   */
  @Override
  public final <T extends B> T defaultResponseCharacterEncoding(Charset defaultResponseCharacterEncoding) {
    this.defaultResponseCharacterEncoding = defaultResponseCharacterEncoding;
    return self();
  }

  @Override
  public final <T extends B> T alwaysExpect(ResultMatcher resultMatcher) {
    this.globalResultMatchers.add(resultMatcher);
    return self();
  }

  @Override
  public final <T extends B> T alwaysDo(ResultHandler resultHandler) {
    this.globalResultHandlers.add(resultHandler);
    return self();
  }

  @Override
  public final <T extends B> T addDispatcherServletCustomizer(DispatcherServletCustomizer customizer) {
    this.dispatcherServletCustomizers.add(customizer);
    return self();
  }

  @Override
  public final <T extends B> T apply(MockMvcConfigurer configurer) {
    configurer.afterConfigurerAdded(this);
    this.configurers.add(configurer);
    return self();
  }

  @SuppressWarnings("unchecked")
  protected <T extends B> T self() {
    return (T) this;
  }

  /**
   * Build a {@link cn.taketoday.test.web.servlet.MockMvc} instance.
   */
  @Override
  @SuppressWarnings("rawtypes")
  public final MockMvc build() {
    ApplicationContext ctx = initWebAppContext();
    MockContext mockContext;
    MockMockConfig mockServletConfig;

    if (ctx instanceof WebApplicationContext wac) {
      mockContext = wac.getServletContext();
      mockServletConfig = new MockMockConfig(mockContext);
    }
    else {
      mockContext = new MockContextImpl();
      mockServletConfig = new MockMockConfig(mockContext);
    }

    for (MockMvcConfigurer configurer : this.configurers) {
      RequestPostProcessor processor = configurer.beforeMockMvcCreated(this, ctx);
      if (processor != null) {
        if (this.defaultRequestBuilder == null) {
          this.defaultRequestBuilder = MockMvcRequestBuilders.get("/");
        }
        if (this.defaultRequestBuilder instanceof ConfigurableSmartRequestBuilder configurableBuilder) {
          configurableBuilder.with(processor);
        }
      }
    }

    Filter[] filterArray = this.filters.toArray(new Filter[0]);
    for (Filter filter : filterArray) {
      if (filter instanceof MockMvcFilterDecorator filterDecorator) {
        try {
          filterDecorator.initIfRequired(mockContext);
        }
        catch (ServletException ex) {
          throw new IllegalStateException("Failed to initialize Filter " + filter, ex);
        }
      }
    }

    return super.createMockMvc(filterArray, mockServletConfig, ctx, this.defaultRequestBuilder,
            this.defaultResponseCharacterEncoding, this.globalResultMatchers, this.globalResultHandlers,
            this.dispatcherServletCustomizers);
  }

  /**
   * A method to obtain the {@code WebApplicationContext} to be passed to the
   * {@code DispatcherServlet}. Invoked from {@link #build()} before the
   * {@link MockMvc} instance is created.
   */
  protected abstract ApplicationContext initWebAppContext();

}
