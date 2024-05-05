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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.RequestContext;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.mock.ServletRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for redirect view, and query string construction.
 * Doesn't test URL encoding, although it does check that it's called.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 27.05.2003
 */
public class RedirectViewTests {

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  private RequestContext context;

  @BeforeEach
  public void setUp() throws Exception {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    this.request = new MockHttpServletRequest();
    this.response = new MockHttpServletResponse();

    context.refresh();

    this.context = new ServletRequestContext(context, request, response);
  }

  @Test
  public void noUrlSet() throws Exception {
    RedirectView rv = new RedirectView();
    assertThatIllegalArgumentException().isThrownBy(
            rv::afterPropertiesSet);
  }

  @Test
  public void http11() throws Exception {
    RedirectView rv = new RedirectView();
    rv.setUrl("https://url.somewhere.com");
    rv.setHttp10Compatible(false);
    rv.render(new HashMap<>(), context);
    assertThat(response.getStatus()).isEqualTo(303);
    assertThat(context.responseHeaders().getFirst("Location")).isEqualTo("https://url.somewhere.com");
  }

  @Test
  public void explicitStatusCodeHttp11() throws Exception {
    RedirectView rv = new RedirectView();
    rv.setUrl("https://url.somewhere.com");
    rv.setHttp10Compatible(false);
    rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
    rv.render(new HashMap<>(), context);
    assertThat(response.getStatus()).isEqualTo(301);
    assertThat(context.responseHeaders().getFirst("Location")).isEqualTo("https://url.somewhere.com");
  }

  @Test
  public void explicitStatusCodeHttp10() throws Exception {
    RedirectView rv = new RedirectView();
    rv.setUrl("https://url.somewhere.com");
    rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
    rv.render(new HashMap<>(), context);
    assertThat(response.getStatus()).isEqualTo(301);
    assertThat(context.responseHeaders().getFirst("Location")).isEqualTo("https://url.somewhere.com");
  }

  @Test
  public void attributeStatusCodeHttp10() throws Exception {
    RedirectView rv = new RedirectView();
    rv.setUrl("https://url.somewhere.com");
    request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.CREATED);
    rv.render(new HashMap<>(), context);
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(context.responseHeaders().getFirst("Location")).isEqualTo("https://url.somewhere.com");
  }

  @Test
  public void attributeStatusCodeHttp11() throws Exception {
    RedirectView rv = new RedirectView();
    rv.setUrl("https://url.somewhere.com");
    rv.setHttp10Compatible(false);
    request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.CREATED);
    rv.render(new HashMap<>(), context);
    assertThat(response.getStatus()).isEqualTo(201);
    assertThat(context.responseHeaders().getFirst("Location")).isEqualTo("https://url.somewhere.com");
  }

  @Test // SPR-16752
  public void contextRelativeWithValidatedContextPath() throws Exception {
    String url = "/myUrl";

    this.response = new MockHttpServletResponse();
    this.context = null;
    doTest(new HashMap<>(), url, true, url);
    this.response = new MockHttpServletResponse();
    this.context = null;
    doTest(new HashMap<>(), url, true, url);
  }

  @Test
  public void emptyMap() throws Exception {
    String url = "/myUrl";
    doTest(new HashMap<>(), url, false, url);
  }

  @Test
  public void emptyMapWithContextRelative() throws Exception {
    String url = "/myUrl";
    doTest(new HashMap<>(), url, true, url);
  }

  @Test
  public void singleParam() throws Exception {
    String url = "https://url.somewhere.com";
    String key = "foo";
    String val = "bar";
    Map<String, String> model = new HashMap<>();
    model.put(key, val);
    String expectedUrlForEncoding = url + "?" + key + "=" + val;
    doTest(model, url, false, expectedUrlForEncoding);
  }

  @Test
  public void singleParamWithoutExposingModelAttributes() throws Exception {
    String url = "https://url.somewhere.com";
    Map<String, String> model = Collections.singletonMap("foo", "bar");

    TestRedirectView rv = new TestRedirectView(url, false, model);
    rv.setExposeModelAttributes(false);
    rv.render(model, context);

    assertThat(this.response.getRedirectedUrl()).isEqualTo(url);
  }

  @Test
  public void paramWithAnchor() throws Exception {
    String url = "https://url.somewhere.com/test.htm#myAnchor";
    String key = "foo";
    String val = "bar";
    Map<String, String> model = new HashMap<>();
    model.put(key, val);
    String expectedUrlForEncoding = "https://url.somewhere.com/test.htm" + "?" + key + "=" + val + "#myAnchor";
    doTest(model, url, false, expectedUrlForEncoding);
  }

  @Test
  public void contextRelativeQueryParam() throws Exception {
    String url = "/test.html?id=1";
    doTest(new HashMap<>(), url, true, url);
  }

  @Test
  public void twoParams() throws Exception {
    String url = "https://url.somewhere.com";
    String key = "foo";
    String val = "bar";
    String key2 = "thisIsKey2";
    String val2 = "andThisIsVal2";
    Map<String, String> model = new HashMap<>();
    model.put(key, val);
    model.put(key2, val2);
    try {
      String expectedUrlForEncoding = url + "?" + key + "=" + val + "&" + key2 + "=" + val2;
      doTest(model, url, false, expectedUrlForEncoding);
    }
    catch (AssertionError err) {
      // OK, so it's the other order... probably on Sun JDK 1.6 or IBM JDK 1.5
      String expectedUrlForEncoding = url + "?" + key2 + "=" + val2 + "&" + key + "=" + val;
      doTest(model, url, false, expectedUrlForEncoding);
    }
  }

  @Test
  public void arrayParam() throws Exception {
    String url = "https://url.somewhere.com";
    String key = "foo";
    String[] val = new String[] { "bar", "baz" };
    Map<String, String[]> model = new HashMap<>();
    model.put(key, val);
    try {
      String expectedUrlForEncoding = url + "?" + key + "=" + val[0] + "&" + key + "=" + val[1];
      doTest(model, url, false, expectedUrlForEncoding);
    }
    catch (AssertionError err) {
      // OK, so it's the other order... probably on Sun JDK 1.6 or IBM JDK 1.5
      String expectedUrlForEncoding = url + "?" + key + "=" + val[1] + "&" + key + "=" + val[0];
      doTest(model, url, false, expectedUrlForEncoding);
    }
  }

  @Test
  public void collectionParam() throws Exception {
    String url = "https://url.somewhere.com";
    String key = "foo";
    List<String> val = new ArrayList<>();
    val.add("bar");
    val.add("baz");
    Map<String, List<String>> model = new HashMap<>();
    model.put(key, val);
    try {
      String expectedUrlForEncoding = url + "?" + key + "=" + val.get(0) + "&" + key + "=" + val.get(1);
      doTest(model, url, false, expectedUrlForEncoding);
    }
    catch (AssertionError err) {
      // OK, so it's the other order... probably on Sun JDK 1.6 or IBM JDK 1.5
      String expectedUrlForEncoding = url + "?" + key + "=" + val.get(1) + "&" + key + "=" + val.get(0);
      doTest(model, url, false, expectedUrlForEncoding);
    }
  }

  @Test
  public void objectConversion() throws Exception {
    String url = "https://url.somewhere.com";
    String key = "foo";
    String val = "bar";
    String key2 = "int2";
    Object val2 = 611;
    String key3 = "tb";
    Object val3 = new TestBean();
    Map<String, Object> model = new LinkedHashMap<>();
    model.put(key, val);
    model.put(key2, val2);
    model.put(key3, val3);
    String expectedUrlForEncoding = url + "?" + key + "=" + val + "&" + key2 + "=" + val2;
    doTest(model, url, false, expectedUrlForEncoding);
  }

  @Test
  public void propagateQueryParams() throws Exception {
    RedirectView rv = new RedirectView();
    rv.setPropagateQueryParams(true);
    rv.setUrl("https://url.somewhere.com?foo=bar#bazz");
    request.setQueryString("a=b&c=d");
    rv.render(new HashMap<>(), context);
    assertThat(response.getStatus()).isEqualTo(302);
    assertThat(response.getHeader("Location")).isEqualTo("https://url.somewhere.com?foo=bar&a=b&c=d#bazz");
  }

  private void doTest(Map<String, ?> map, String url, boolean contextRelative, String expectedUrl)
          throws Exception {
    if (this.context == null) {
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
      this.context = new ServletRequestContext(context, request, response);
    }

    TestRedirectView rv = new TestRedirectView(url, contextRelative, map);
    rv.render(map, context);

    assertThat(rv.queryPropertiesCalled).as("queryProperties() should have been called.").isTrue();
    assertThat(this.response.getRedirectedUrl()).isEqualTo(expectedUrl);
  }

  private static class TestRedirectView extends RedirectView {

    private Map<String, ?> expectedModel;

    private boolean queryPropertiesCalled = false;

    public TestRedirectView(String url, boolean contextRelative, Map<String, ?> expectedModel) {
      super(url);
      this.expectedModel = expectedModel;
    }

    /**
     * Test whether this callback method is called with correct args
     */
    @Override
    protected Map<String, Object> queryProperties(Map<String, Object> model) {
      assertThat(this.expectedModel.equals(model)).as("Map and model must be equal.").isTrue();
      this.queryPropertiesCalled = true;
      return super.queryProperties(model);
    }
  }

}
