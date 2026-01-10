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

package infra.web.view;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import infra.mock.web.MockContextImpl;
import infra.web.RequestContext;
import infra.web.mock.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BeanNameViewResolver}, {@link UrlBasedViewResolver},
 * and {@link AbstractCachingViewResolver}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
public class ViewResolverTests {

  private final StaticWebApplicationContext wac = new StaticWebApplicationContext();
  private final MockContextImpl sc = new MockContextImpl();

  @BeforeEach
  public void setUp() {
    this.wac.setMockContext(this.sc);
  }

  @Test
  public void urlBasedViewResolverOverridesCustomRequestContextAttributeWithNonNullValue() throws Exception {
    Assertions.assertThat(new TestView().getRequestContextAttribute())
            .as("requestContextAttribute when instantiated directly")
            .isEqualTo("testRequestContext");

    UrlBasedViewResolver vr = new UrlBasedViewResolver();
    vr.setViewClass(TestView.class);
    vr.setApplicationContext(this.wac);
    vr.setRequestContextAttribute("viewResolverRequestContext");
    this.wac.refresh();

    View view = vr.resolveViewName("example", Locale.getDefault());
    assertThat(view).isInstanceOf(TestView.class);
    Assertions.assertThat(((TestView) view).getRequestContextAttribute())
            .as("requestContextAttribute when instantiated dynamically by UrlBasedViewResolver")
            .isEqualTo("viewResolverRequestContext");
  }

  @Test
  public void urlBasedViewResolverDoesNotOverrideCustomRequestContextAttributeWithNull() throws Exception {
    Assertions.assertThat(new TestView().getRequestContextAttribute())
            .as("requestContextAttribute when instantiated directly")
            .isEqualTo("testRequestContext");

    UrlBasedViewResolver vr = new UrlBasedViewResolver();
    vr.setViewClass(TestView.class);
    vr.setApplicationContext(this.wac);
    this.wac.refresh();

    View view = vr.resolveViewName("example", Locale.getDefault());
    assertThat(view).isInstanceOf(TestView.class);
    Assertions.assertThat(((TestView) view).getRequestContextAttribute())
            .as("requestContextAttribute when instantiated dynamically by UrlBasedViewResolver")
            .isEqualTo("testRequestContext");
  }

  @Test
  public void cacheUnresolved() throws Exception {
    final AtomicInteger count = new AtomicInteger();
    AbstractCachingViewResolver viewResolver = new AbstractCachingViewResolver() {
      @Override
      protected View loadView(String viewName, Locale locale) {
        count.incrementAndGet();
        return null;
      }
    };

    viewResolver.setCacheUnresolved(false);

    viewResolver.resolveViewName("view", Locale.getDefault());
    viewResolver.resolveViewName("view", Locale.getDefault());

    assertThat(count.intValue()).isEqualTo(2);

    viewResolver.setCacheUnresolved(true);

    viewResolver.resolveViewName("view", Locale.getDefault());
    viewResolver.resolveViewName("view", Locale.getDefault());
    viewResolver.resolveViewName("view", Locale.getDefault());
    viewResolver.resolveViewName("view", Locale.getDefault());
    viewResolver.resolveViewName("view", Locale.getDefault());

    assertThat(count.intValue()).isEqualTo(3);
  }

  @Test
  public void cacheFilterEnabled() throws Exception {
    AtomicInteger count = new AtomicInteger();

    // filter is enabled by default
    AbstractCachingViewResolver viewResolver = new AbstractCachingViewResolver() {
      @Override
      protected View loadView(String viewName, Locale locale) {
        assertThat(viewName).isEqualTo("view");
        assertThat(locale).isEqualTo(Locale.getDefault());
        count.incrementAndGet();
        return new TestView();
      }
    };

    viewResolver.resolveViewName("view", Locale.getDefault());
    viewResolver.resolveViewName("view", Locale.getDefault());

    assertThat(count.intValue()).isEqualTo(1);
  }

  @Test
  public void cacheFilterDisabled() throws Exception {
    AtomicInteger count = new AtomicInteger();

    AbstractCachingViewResolver viewResolver = new AbstractCachingViewResolver() {
      @Override
      protected View loadView(String viewName, Locale locale) {
        count.incrementAndGet();
        return new TestView();
      }
    };

    viewResolver.setCacheFilter((view, viewName, locale) -> false);

    viewResolver.resolveViewName("view", Locale.getDefault());
    viewResolver.resolveViewName("view", Locale.getDefault());

    assertThat(count.intValue()).isEqualTo(2);
  }

  public static class TestView extends AbstractUrlBasedView {

    public TestView() {
      setRequestContextAttribute("testRequestContext");
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {

    }
  }

}
