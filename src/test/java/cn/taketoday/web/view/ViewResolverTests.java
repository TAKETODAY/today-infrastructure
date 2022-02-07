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

package cn.taketoday.web.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.beans.PropertyValue;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.support.TestBean;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.LocaleResolver;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.StaticWebApplicationContext;
import cn.taketoday.web.i18n.AcceptHeaderLocaleResolver;
import cn.taketoday.web.i18n.FixedLocaleResolver;
import cn.taketoday.web.mock.MockHttpServletRequest;
import cn.taketoday.web.mock.MockHttpServletResponse;
import cn.taketoday.web.mock.MockRequestDispatcher;
import cn.taketoday.web.mock.MockServletContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.view.InternalResourceView;
import cn.taketoday.web.servlet.view.InternalResourceViewResolver;
import cn.taketoday.web.servlet.view.JstlView;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.jstl.core.Config;
import jakarta.servlet.jsp.jstl.fmt.LocalizationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BeanNameViewResolver}, {@link UrlBasedViewResolver},
 * {@link InternalResourceViewResolver}, and {@link AbstractCachingViewResolver}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 18.06.2003
 */
public class ViewResolverTests {

  private final StaticWebApplicationContext wac = new StaticWebApplicationContext();
  private final MockServletContext sc = new MockServletContext();
  private final MockHttpServletRequest request = new MockHttpServletRequest(this.sc);
  private final HttpServletResponse response = new MockHttpServletResponse();
  RequestContext requestContext = new ServletRequestContext(wac, request, response);

  @BeforeEach
  public void setUp() {
    this.wac.setServletContext(this.sc);
  }

  @Test
  public void beanNameViewResolver() {
    PropertyValues pvs1 = new PropertyValues();
    pvs1.add(new PropertyValue("url", "/example1.jsp"));
    this.wac.registerSingleton("example1", InternalResourceView.class, pvs1);
    PropertyValues pvs2 = new PropertyValues();
    pvs2.add(new PropertyValue("url", "/example2.jsp"));
    this.wac.registerSingleton("example2", JstlView.class, pvs2);
    BeanNameViewResolver vr = new BeanNameViewResolver();
    vr.setApplicationContext(this.wac);
    this.wac.refresh();

    View view = vr.resolveViewName("example1", Locale.getDefault());
    assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/example1.jsp");

    view = vr.resolveViewName("example2", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("/example2.jsp");
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
  public void urlBasedViewResolverWithoutPrefixes() throws Exception {
    UrlBasedViewResolver vr = new UrlBasedViewResolver();
    vr.setViewClass(JstlView.class);
    doTestUrlBasedViewResolverWithoutPrefixes(vr);
  }

  @Test
  public void urlBasedViewResolverWithPrefixes() throws Exception {
    UrlBasedViewResolver vr = new UrlBasedViewResolver();
    vr.setViewClass(JstlView.class);
    doTestUrlBasedViewResolverWithPrefixes(vr);
  }

  @Test
  public void internalResourceViewResolverWithoutPrefixes() throws Exception {
    doTestUrlBasedViewResolverWithoutPrefixes(new InternalResourceViewResolver());
  }

  @Test
  public void internalResourceViewResolverWithPrefixes() throws Exception {
    doTestUrlBasedViewResolverWithPrefixes(new InternalResourceViewResolver());
  }

  private void doTestUrlBasedViewResolverWithoutPrefixes(UrlBasedViewResolver vr) throws Exception {
    this.wac.refresh();
    vr.setApplicationContext(this.wac);
    vr.setContentType("myContentType");
    vr.setRequestContextAttribute("rc");

    View view = vr.resolveViewName("example1", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example1");
    assertThat(view.getContentType()).as("Correct textContentType").isEqualTo("myContentType");

    view = vr.resolveViewName("example2", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example2");
    assertThat(view.getContentType()).as("Correct textContentType").isEqualTo("myContentType");

    this.wac.registerSingleton(LocaleResolver.BEAN_NAME, new AcceptHeaderLocaleResolver());
    Map<String, Object> model = new HashMap<>();
    TestBean tb = new TestBean();
    model.put("tb", tb);
    view.render(model, this.requestContext);
    assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
    boolean condition = this.request.getAttribute("rc") instanceof RequestContext;
    assertThat(condition).as("Correct rc attribute").isTrue();

    view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
    assertThat(view.getClass()).as("Correct view class").isEqualTo(RedirectView.class);
    assertThat(((RedirectView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
    assertThat(((RedirectView) view).getApplicationContext()).as("View not initialized as bean").isSameAs(this.wac);

    view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
    assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
  }

  private void doTestUrlBasedViewResolverWithPrefixes(UrlBasedViewResolver vr) throws Exception {
    this.wac.refresh();
    vr.setPrefix("/WEB-INF/");
    vr.setSuffix(".jsp");
    vr.setApplicationContext(this.wac);

    View view = vr.resolveViewName("example1", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/WEB-INF/example1.jsp");

    view = vr.resolveViewName("example2", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("/WEB-INF/example2.jsp");

    view = vr.resolveViewName("redirect:myUrl", Locale.getDefault());
    assertThat(view.getClass()).as("Correct view class").isEqualTo(RedirectView.class);
    assertThat(((RedirectView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");

    view = vr.resolveViewName("forward:myUrl", Locale.getDefault());
    assertThat(view.getClass()).as("Correct view class").isEqualTo(InternalResourceView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("myUrl");
  }

  @Test
  public void internalResourceViewResolverWithAttributes() throws Exception {
    this.wac.refresh();
    InternalResourceViewResolver vr = new InternalResourceViewResolver();
    Properties props = new Properties();
    props.setProperty("key1", "value1");
    vr.setAttributes(props);
    Map<String, Object> map = new HashMap<>();
    map.put("key2", 2);
    vr.setAttributesMap(map);
    vr.setApplicationContext(this.wac);

    View view = vr.resolveViewName("example1", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example1");
    Map<String, Object> attributes = ((InternalResourceView) view).getStaticAttributes();
    assertThat(attributes.get("key1")).isEqualTo("value1");
    assertThat(attributes.get("key2")).isEqualTo(2);

    view = vr.resolveViewName("example2", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((InternalResourceView) view).getUrl()).as("Correct URL").isEqualTo("example2");
    attributes = ((InternalResourceView) view).getStaticAttributes();
    assertThat(attributes.get("key1")).isEqualTo("value1");
    assertThat(attributes.get("key2")).isEqualTo(2);

    this.wac.registerSingleton(LocaleResolver.BEAN_NAME, new AcceptHeaderLocaleResolver());
    Map<String, Object> model = new HashMap<>();
    TestBean tb = new TestBean();
    model.put("tb", tb);
    view.render(model, this.requestContext);

    assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
    assertThat(this.request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
    assertThat(this.request.getAttribute("key1")).isEqualTo("value1");
    assertThat(this.request.getAttribute("key2")).isEqualTo(2);
  }

  @Test
  public void internalResourceViewResolverWithContextBeans() throws Exception {
    this.wac.registerSingleton("myBean", TestBean.class);
    this.wac.registerSingleton("myBean2", TestBean.class);
    this.wac.refresh();
    InternalResourceViewResolver vr = new InternalResourceViewResolver();
    Properties props = new Properties();
    props.setProperty("key1", "value1");
    vr.setAttributes(props);
    Map<String, Object> map = new HashMap<>();
    map.put("key2", 2);
    vr.setAttributesMap(map);
    vr.setExposeContextBeansAsAttributes(true);
    vr.setApplicationContext(this.wac);

    HttpServletRequest request = new MockHttpServletRequest(this.sc) {
      @Override
      public RequestDispatcher getRequestDispatcher(String path) {
        return new MockRequestDispatcher(path) {
          @Override
          public void forward(ServletRequest forwardRequest, ServletResponse forwardResponse) {
            assertThat(forwardRequest.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
            assertThat(forwardRequest.getAttribute("key1")).isEqualTo("value1");
            assertThat(forwardRequest.getAttribute("key2")).isEqualTo(2);
            assertThat(forwardRequest.getAttribute("myBean")).isSameAs(wac.getBean("myBean"));
            assertThat(forwardRequest.getAttribute("myBean2")).isSameAs(wac.getBean("myBean2"));
          }
        };
      }
    };
    this.wac.registerSingleton(LocaleResolver.BEAN_NAME, new AcceptHeaderLocaleResolver());

    View view = vr.resolveViewName("example1", Locale.getDefault());
    view.render(new HashMap<String, Object>(), new ServletRequestContext(null, request, response));
  }

  @Test
  public void internalResourceViewResolverWithSpecificContextBeans() throws Exception {
    this.wac.registerSingleton("myBean", TestBean.class);
    this.wac.registerSingleton("myBean2", TestBean.class);
    this.wac.refresh();
    InternalResourceViewResolver vr = new InternalResourceViewResolver();
    Properties props = new Properties();
    props.setProperty("key1", "value1");
    vr.setAttributes(props);
    Map<String, Object> map = new HashMap<>();
    map.put("key2", 2);
    vr.setAttributesMap(map);
    vr.setExposedContextBeanNames(new String[] { "myBean2" });
    vr.setApplicationContext(this.wac);

    HttpServletRequest request = new MockHttpServletRequest(this.sc) {
      @Override
      public RequestDispatcher getRequestDispatcher(String path) {
        return new MockRequestDispatcher(path) {
          @Override
          public void forward(ServletRequest forwardRequest, ServletResponse forwardResponse) {
            assertThat(forwardRequest.getAttribute("rc") == null).as("Correct rc attribute").isTrue();
            assertThat(forwardRequest.getAttribute("key1")).isEqualTo("value1");
            assertThat(forwardRequest.getAttribute("key2")).isEqualTo(2);
            assertThat(forwardRequest.getAttribute("myBean")).isNull();
            assertThat(forwardRequest.getAttribute("myBean2")).isSameAs(wac.getBean("myBean2"));
          }
        };
      }
    };
    this.wac.registerSingleton(LocaleResolver.BEAN_NAME, new AcceptHeaderLocaleResolver());

    View view = vr.resolveViewName("example1", Locale.getDefault());
    view.render(new HashMap<String, Object>(), new ServletRequestContext(wac, request, response));
  }

  @Test
  public void internalResourceViewResolverWithJstl() throws Exception {
    Locale locale = !Locale.GERMAN.equals(Locale.getDefault()) ? Locale.GERMAN : Locale.FRENCH;

    this.wac.addMessage("code1", locale, "messageX");
    this.wac.refresh();
    InternalResourceViewResolver vr = new InternalResourceViewResolver();
    vr.setViewClass(JstlView.class);
    vr.setApplicationContext(this.wac);

    View view = vr.resolveViewName("example1", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example1");

    view = vr.resolveViewName("example2", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example2");
    this.wac.registerSingleton(LocaleResolver.BEAN_NAME, new FixedLocaleResolver(locale));
    Map<String, Object> model = new HashMap<>();
    TestBean tb = new TestBean();
    model.put("tb", tb);
    view.render(model, this.requestContext);

    assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
    assertThat(this.request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();

    assertThat(Config.get(this.request, Config.FMT_LOCALE)).isEqualTo(locale);
    LocalizationContext lc = (LocalizationContext) Config.get(this.request, Config.FMT_LOCALIZATION_CONTEXT);
    assertThat(lc.getResourceBundle().getString("code1")).isEqualTo("messageX");
  }

  @Test
  public void internalResourceViewResolverWithJstlAndContextParam() throws Exception {
    Locale locale = !Locale.GERMAN.equals(Locale.getDefault()) ? Locale.GERMAN : Locale.FRENCH;

    this.sc.addInitParameter(Config.FMT_LOCALIZATION_CONTEXT, "cn/taketoday/web/context/WEB-INF/context-messages");
    this.wac.addMessage("code1", locale, "messageX");
    this.wac.refresh();
    InternalResourceViewResolver vr = new InternalResourceViewResolver();
    vr.setViewClass(JstlView.class);
    vr.setApplicationContext(this.wac);

    View view = vr.resolveViewName("example1", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example1");

    view = vr.resolveViewName("example2", Locale.getDefault());
    assertThat(view).isInstanceOf(JstlView.class);
    assertThat(((JstlView) view).getUrl()).as("Correct URL").isEqualTo("example2");

    wac.registerSingleton(LocaleResolver.BEAN_NAME, new FixedLocaleResolver(locale));
    Map<String, Object> model = new HashMap<>();
    TestBean tb = new TestBean();
    model.put("tb", tb);
    view.render(model, this.requestContext);

    assertThat(tb.equals(this.request.getAttribute("tb"))).as("Correct tb attribute").isTrue();
    assertThat(this.request.getAttribute("rc") == null).as("Correct rc attribute").isTrue();

    assertThat(Config.get(this.request, Config.FMT_LOCALE)).isEqualTo(locale);
    LocalizationContext lc = (LocalizationContext) Config.get(this.request, Config.FMT_LOCALIZATION_CONTEXT);
    assertThat(lc.getResourceBundle().getString("code1")).isEqualTo("message1");
    assertThat(lc.getResourceBundle().getString("code2")).isEqualTo("message2");
  }

  @Test
  public void cacheRemoval() throws Exception {
    this.wac.refresh();
    InternalResourceViewResolver vr = new InternalResourceViewResolver();
    vr.setViewClass(JstlView.class);
    vr.setApplicationContext(this.wac);

    View view = vr.resolveViewName("example1", Locale.getDefault());
    View cached = vr.resolveViewName("example1", Locale.getDefault());
    assertThat(cached).isSameAs(view);

    vr.removeFromCache("example1", Locale.getDefault());
    cached = vr.resolveViewName("example1", Locale.getDefault());
    // the chance of having the same reference (hashCode) twice is negligible.
    assertThat(cached).as("removed from cache").isNotSameAs(view);
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

  public static class TestView extends InternalResourceView {

    public TestView() {
      setRequestContextAttribute("testRequestContext");
    }

    public void setLocation(Resource location) {
//      if (!(location instanceof ServletContextResource)) {
//        throw new IllegalArgumentException("Expecting ServletContextResource, not " + location.getClass().getName());
//      }
    }
  }

}
