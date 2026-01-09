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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import infra.context.ApplicationContext;
import infra.lang.Assert;
import infra.mock.api.DispatcherType;
import infra.mock.api.Filter;
import infra.mock.api.MockContext;
import infra.mock.api.MockException;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockMockConfig;
import infra.test.web.mock.DispatcherCustomizer;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MockMvcBuilder;
import infra.test.web.mock.MockMvcBuilderSupport;
import infra.test.web.mock.RequestBuilder;
import infra.test.web.mock.ResultHandler;
import infra.test.web.mock.ResultMatcher;
import infra.test.web.mock.request.ConfigurableSmartRequestBuilder;
import infra.test.web.mock.request.MockMvcRequestBuilders;
import infra.test.web.mock.request.RequestPostProcessor;
import infra.web.mock.WebApplicationContext;

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

  private final List<DispatcherCustomizer> dispatcherCustomizers = new ArrayList<>();

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
  public final <T extends B> T addDispatcherCustomizer(DispatcherCustomizer customizer) {
    this.dispatcherCustomizers.add(customizer);
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
   * Build a {@link MockMvc} instance.
   */
  @Override
  @SuppressWarnings("rawtypes")
  public final MockMvc build() {
    ApplicationContext ctx = initWebAppContext();
    MockContext mockContext;
    MockMockConfig mockServletConfig;

    if (ctx instanceof WebApplicationContext wac) {
      mockContext = wac.getMockContext();
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
        catch (MockException ex) {
          throw new IllegalStateException("Failed to initialize Filter " + filter, ex);
        }
      }
    }

    return super.createMockMvc(filterArray, mockServletConfig, ctx, this.defaultRequestBuilder,
            this.defaultResponseCharacterEncoding, this.globalResultMatchers, this.globalResultHandlers,
            this.dispatcherCustomizers);
  }

  /**
   * A method to obtain the {@code WebApplicationContext} to be passed to the
   * {@code DispatcherHandler}. Invoked from {@link #build()} before the
   * {@link MockMvc} instance is created.
   */
  protected abstract ApplicationContext initWebAppContext();

}
