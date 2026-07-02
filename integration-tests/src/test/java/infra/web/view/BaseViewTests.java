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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import infra.context.ApplicationContext;
import infra.context.ApplicationContextException;
import infra.mock.web.MockRequest;
import infra.mock.web.MockResponse;
import infra.web.HandlerMatchingMetadata;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
    ApplicationContext wac = Mockito.mock(ApplicationContext.class);

    MockRequest request = new MockRequest();
    MockResponse response = new infra.mock.web.MockResponse();
    TestView tv = new TestView(wac);

    // Check superclass handles duplicate init
    tv.setApplicationContext(wac);
    tv.setApplicationContext(wac);

    Map<String, Object> model = new HashMap<>();
    model.put("foo", "bar");
    model.put("something", new Object());

    RequestContext requestContext = new MockRequestContext(wac, request, response);

    tv.render(model, requestContext);

    checkContainsAll(model, tv.model);

    assertThat(tv.initialized).isTrue();
  }

  /**
   * Test attribute passing, NOT CSV parsing.
   */
  @Test
  public void renderWithStaticAttributesNoCollision() throws Exception {
    ApplicationContext wac = Mockito.mock(ApplicationContext.class);

    MockRequest request = new MockRequest();
    MockResponse response = new infra.mock.web.MockResponse();
    TestView tv = new TestView(wac);

    tv.setApplicationContext(wac);
    Properties p = new Properties();
    p.setProperty("foo", "bar");
    p.setProperty("something", "else");
    tv.setAttributes(p);

    Map<String, Object> model = new HashMap<>();
    model.put("one", new HashMap<>());
    model.put("two", new Object());

    RequestContext requestContext = new MockRequestContext(wac, request, response);

    tv.render(model, requestContext);

    checkContainsAll(model, tv.model);
    checkContainsAll(p, tv.model);

    assertThat(tv.initialized).isTrue();
  }

  @Test
  public void pathVarsOverrideStaticAttributes() throws Exception {
    ApplicationContext wac = Mockito.mock(ApplicationContext.class);

    MockRequest request = new MockRequest();
    MockResponse response = new infra.mock.web.MockResponse();

    TestView tv = new TestView(wac);
    tv.setApplicationContext(wac);

    Properties p = new Properties();
    p.setProperty("one", "bar");
    p.setProperty("something", "else");
    tv.setAttributes(p);

    Map<String, Object> pathVars = new HashMap<>();
    pathVars.put("one", new HashMap<>());
    pathVars.put("two", new Object());
    RequestContext requestContext = new MockRequestContext(wac, request, response);
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
    ApplicationContext wac = Mockito.mock(ApplicationContext.class);

    MockRequest request = new MockRequest();
    MockResponse response = new infra.mock.web.MockResponse();
    TestView tv = new TestView(wac);

    tv.setApplicationContext(wac);
    Properties p = new Properties();
    p.setProperty("one", "bar");
    p.setProperty("something", "else");
    tv.setAttributes(p);

    Map<String, Object> model = new HashMap<>();
    model.put("one", new HashMap<>());
    model.put("two", new Object());
    RequestContext requestContext = new MockRequestContext(wac, request, response);

    tv.render(model, requestContext);

    // Check it contains all
    checkContainsAll(model, tv.model);

    assertThat(tv.model.size()).isEqualTo(3);
    assertThat(tv.model.get("something")).isEqualTo("else");
    assertThat(tv.initialized).isTrue();
  }

  @Test
  public void dynamicModelOverridesPathVariables() throws Exception {
    ApplicationContext wac = Mockito.mock(ApplicationContext.class);

    TestView tv = new TestView(wac);
    tv.setApplicationContext(wac);

    MockRequest request = new MockRequest();
    infra.mock.web.MockResponse response = new infra.mock.web.MockResponse();

    Map<String, Object> pathVars = new HashMap<>();
    pathVars.put("one", "bar");
    pathVars.put("something", "else");
    RequestContext requestContext = new MockRequestContext(wac, request, response);

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

    private final ApplicationContext wac;

    boolean initialized;

    /** Captured model in render */
    Map<String, Object> model;

    TestView(ApplicationContext wac) {
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
