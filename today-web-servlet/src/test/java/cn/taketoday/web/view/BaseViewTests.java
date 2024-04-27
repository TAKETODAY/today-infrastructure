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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.servlet.WebApplicationContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import cn.taketoday.web.mock.http.HttpServletRequest;
import cn.taketoday.web.mock.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Base tests for {@link AbstractView}.
 *
 * <p>Not called {@code AbstractViewTests} since doing so would cause it
 * to be ignored in the Gradle build.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 */
public class BaseViewTests {

  @Test
  public void renderWithoutStaticAttributes() throws Exception {
    WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
    given(wac.getServletContext()).willReturn(new MockServletContext());

    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    TestView tv = new TestView(wac);

    // Check superclass handles duplicate init
    tv.setApplicationContext(wac);
    tv.setApplicationContext(wac);

    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar");
    model.put("something", new Object());

    RequestContext requestContext = ServletUtils.getRequestContext(request, response);

    tv.render(model, requestContext);

    checkContainsAll(model, tv.model);

    assertThat(tv.initialized).isTrue();
  }

  /**
   * Test attribute passing, NOT CSV parsing.
   */
  @Test
  public void renderWithStaticAttributesNoCollision() throws Exception {
    WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
    given(wac.getServletContext()).willReturn(new MockServletContext());

    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    TestView tv = new TestView(wac);

    tv.setApplicationContext(wac);
    Properties p = new Properties();
    p.setProperty("foo", "bar");
    p.setProperty("something", "else");
    tv.setAttributes(p);

    Map<String, Object> model = new HashMap<>();
    model.put("one", new HashMap<>());
    model.put("two", new Object());

    RequestContext requestContext = ServletUtils.getRequestContext(request, response);

    tv.render(model, requestContext);

    checkContainsAll(model, tv.model);
    checkContainsAll(p, tv.model);

    assertThat(tv.initialized).isTrue();
  }

  @Test
  public void pathVarsOverrideStaticAttributes() throws Exception {
    WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
    given(wac.getServletContext()).willReturn(new MockServletContext());

    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();

    TestView tv = new TestView(wac);
    tv.setApplicationContext(wac);

    Properties p = new Properties();
    p.setProperty("one", "bar");
    p.setProperty("something", "else");
    tv.setAttributes(p);

    Map<String, Object> pathVars = new HashMap<>();
    pathVars.put("one", new HashMap<>());
    pathVars.put("two", new Object());
    RequestContext requestContext = ServletUtils.getRequestContext(wac, request, response);
    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(requestContext);
    metadata.getPathVariables().putAll(pathVars);
    requestContext.setMatchingMetadata(metadata);

    tv.render(new HashMap<>(), requestContext);

    checkContainsAll(pathVars, tv.model);

    assertThat(tv.model.size()).isEqualTo(3);
    assertThat(tv.model.get("something")).isEqualTo("else");
    assertThat(tv.initialized).isTrue();
  }

  @Test
  public void dynamicModelOverridesStaticAttributesIfCollision() throws Exception {
    WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
    given(wac.getServletContext()).willReturn(new MockServletContext());

    HttpServletRequest request = new MockHttpServletRequest();
    HttpServletResponse response = new MockHttpServletResponse();
    TestView tv = new TestView(wac);

    tv.setApplicationContext(wac);
    Properties p = new Properties();
    p.setProperty("one", "bar");
    p.setProperty("something", "else");
    tv.setAttributes(p);

    Map<String, Object> model = new HashMap<>();
    model.put("one", new HashMap<>());
    model.put("two", new Object());
    RequestContext requestContext = ServletUtils.getRequestContext(request, response);

    tv.render(model, requestContext);

    // Check it contains all
    checkContainsAll(model, tv.model);

    assertThat(tv.model.size()).isEqualTo(3);
    assertThat(tv.model.get("something")).isEqualTo("else");
    assertThat(tv.initialized).isTrue();
  }

  @Test
  public void dynamicModelOverridesPathVariables() throws Exception {
    WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
    given(wac.getServletContext()).willReturn(new MockServletContext());

    TestView tv = new TestView(wac);
    tv.setApplicationContext(wac);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    Map<String, Object> pathVars = new HashMap<>();
    pathVars.put("one", "bar");
    pathVars.put("something", "else");
    RequestContext requestContext = ServletUtils.getRequestContext(request, response);

    HandlerMatchingMetadata metadata = new HandlerMatchingMetadata(requestContext);
    metadata.getPathVariables().putAll(pathVars);
    requestContext.setMatchingMetadata(metadata);

    Map<String, Object> model = new HashMap<>();
    model.put("one", new HashMap<>());
    model.put("two", new Object());

    tv.render(model, requestContext);

    checkContainsAll(model, tv.model);
    assertThat(tv.model.size()).isEqualTo(3);
    assertThat(tv.model.get("something")).isEqualTo("else");
    assertThat(tv.initialized).isTrue();
  }

  @Test
  public void ignoresNullAttributes() {
    AbstractView v = new ConcreteView();
    v.setAttributes(null);
    assertThat(v.getStaticAttributes().size()).isEqualTo(0);
  }

  /**
   * Test only the CSV parsing implementation.
   */
  @Test
  public void attributeCSVParsingIgnoresNull() {
    AbstractView v = new ConcreteView();
    v.setAttributesCSV(null);
    assertThat(v.getStaticAttributes()).isNull();
  }

  @Test
  public void attributeCSVParsingIgnoresEmptyString() {
    AbstractView v = new ConcreteView();
    v.setAttributesCSV("");
    assertThat(v.getStaticAttributes()).isNull();
  }

  /**
   * Format is attname0={value1},attname1={value1}
   */
  @Test
  public void attributeCSVParsingValid() {
    AbstractView v = new ConcreteView();
    v.setAttributesCSV("foo=[bar],king=[kong]");
    assertThat(v.getStaticAttributes().size() == 2).isTrue();
    assertThat(v.getStaticAttributes().get("foo").equals("bar")).isTrue();
    assertThat(v.getStaticAttributes().get("king").equals("kong")).isTrue();
  }

  @Test
  public void attributeCSVParsingValidWithWeirdCharacters() {
    AbstractView v = new ConcreteView();
    String fooval = "owfie   fue&3[][[[2 \n\n \r  \t 8\ufffd3";
    // Also tests empty value
    String kingval = "";
    v.setAttributesCSV("foo=(" + fooval + "),king={" + kingval + "},f1=[we]");
    assertThat(v.getStaticAttributes().size() == 3).isTrue();
    assertThat(v.getStaticAttributes().get("foo").equals(fooval)).isTrue();
    assertThat(v.getStaticAttributes().get("king").equals(kingval)).isTrue();
  }

  @Test
  public void attributeCSVParsingInvalid() {
    AbstractView v = new ConcreteView();
    // No equals
    assertThatIllegalArgumentException().isThrownBy(() ->
            v.setAttributesCSV("fweoiruiu"));

    // No value
    assertThatIllegalArgumentException().isThrownBy(() ->
            v.setAttributesCSV("fweoiruiu="));

    // No closing ]
    assertThatIllegalArgumentException().isThrownBy(() ->
            v.setAttributesCSV("fweoiruiu=["));

    // Second one is bogus
    assertThatIllegalArgumentException().isThrownBy(() ->
            v.setAttributesCSV("fweoiruiu=[de],="));
  }

  @Test
  public void attributeCSVParsingIgnoresTrailingComma() {
    AbstractView v = new ConcreteView();
    v.setAttributesCSV("foo=[de],");
    assertThat(v.getStaticAttributes().size()).isEqualTo(1);
  }

  /**
   * Check that all keys in expected have same values in actual.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void checkContainsAll(Map expected, Map<String, Object> actual) {
    expected.forEach((k, v) -> Assertions.assertThat(actual.get(k)).as("Values for model key '" + k
            + "' must match").isEqualTo(expected.get(k)));
  }

  /**
   * Trivial concrete subclass we can use when we're interested only
   * in CSV parsing, which doesn't require lifecycle management
   */
  private static class ConcreteView extends AbstractView {
    // Do-nothing concrete subclass
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws IOException {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Single threaded subclass of AbstractView to check superclass behavior.
   */
  private static class TestView extends AbstractView {

    private final WebApplicationContext wac;

    boolean initialized;

    /** Captured model in render */
    Map<String, Object> model;

    TestView(WebApplicationContext wac) {
      this.wac = wac;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws IOException {
      this.model = model;
    }

    @Override
    protected void initApplicationContext() throws ApplicationContextException {
      if (initialized) {
        throw new RuntimeException("Already initialized");
      }
      this.initialized = true;
      assertThat(getApplicationContext() == wac).isTrue();
    }
  }

}
