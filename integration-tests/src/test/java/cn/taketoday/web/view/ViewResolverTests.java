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

package cn.taketoday.web.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.support.StaticWebApplicationContext;

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
    this.wac.setServletContext(this.sc);
  }

  @Test
  public void urlBasedViewResolverOverridesCustomRequestContextAttributeWithNonNullValue() throws Exception {
    assertThat(new TestView().getRequestContextAttribute())
            .as("requestContextAttribute when instantiated directly")
            .isEqualTo("testRequestContext");

    UrlBasedViewResolver vr = new UrlBasedViewResolver();
    vr.setViewClass(TestView.class);
    vr.setApplicationContext(this.wac);
    vr.setRequestContextAttribute("viewResolverRequestContext");
    this.wac.refresh();

    View view = vr.resolveViewName("example", Locale.getDefault());
    assertThat(view).isInstanceOf(TestView.class);
    assertThat(((TestView) view).getRequestContextAttribute())
            .as("requestContextAttribute when instantiated dynamically by UrlBasedViewResolver")
            .isEqualTo("viewResolverRequestContext");
  }

  @Test
  public void urlBasedViewResolverDoesNotOverrideCustomRequestContextAttributeWithNull() throws Exception {
    assertThat(new TestView().getRequestContextAttribute())
            .as("requestContextAttribute when instantiated directly")
            .isEqualTo("testRequestContext");

    UrlBasedViewResolver vr = new UrlBasedViewResolver();
    vr.setViewClass(TestView.class);
    vr.setApplicationContext(this.wac);
    this.wac.refresh();

    View view = vr.resolveViewName("example", Locale.getDefault());
    assertThat(view).isInstanceOf(TestView.class);
    assertThat(((TestView) view).getRequestContextAttribute())
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
