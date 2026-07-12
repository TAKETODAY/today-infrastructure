/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import infra.context.ApplicationContext;
import infra.test.context.TestContext;
import infra.web.HttpContext;
import infra.web.HttpContextHolder;
import infra.web.mock.MockHttpContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;

import static infra.test.context.web.WebMockTestExecutionListener.POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE;
import static infra.test.context.web.WebMockTestExecutionListener.RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link WebMockTestExecutionListener}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 */
class WebMockTestExecutionListenerTests {

  private static final String SET_UP_OUTSIDE_OF_STEL = "setUpOutsideOfStel";

  private final ApplicationContext wac = mock(ApplicationContext.class);
  private final TestContext testContext = mock(TestContext.class);
  private final WebMockTestExecutionListener listener = new WebMockTestExecutionListener();

  @BeforeEach
  void setUp() {
    given(testContext.getApplicationContext()).willReturn(wac);

    MockRequest request = new MockRequest();
    MockResponse response = new MockResponse();
    HttpContext servletWebRequest = new MockHttpContext(null, request, response);

    request.setAttribute(SET_UP_OUTSIDE_OF_STEL, "true");

    HttpContextHolder.set(servletWebRequest);
    assertSetUpOutsideOfStelAttributeExists();
  }

  @Test
  void standardApplicationContext() throws Exception {
    BDDMockito.<Class<?>>given(testContext.getTestClass()).willReturn(getClass());
    given(testContext.getApplicationContext()).willReturn(mock(ApplicationContext.class));

    listener.beforeTestClass(testContext);
    assertSetUpOutsideOfStelAttributeExists();

    listener.prepareTestInstance(testContext);
    assertSetUpOutsideOfStelAttributeExists();

    listener.beforeTestMethod(testContext);
    assertSetUpOutsideOfStelAttributeExists();

    listener.afterTestMethod(testContext);
    assertSetUpOutsideOfStelAttributeExists();
  }

  @Test
  void legacyWebTestCaseWithoutExistingRequestAttributes() throws Exception {
    BDDMockito.<Class<?>>given(testContext.getTestClass()).willReturn(LegacyWebTestCase.class);

    HttpContextHolder.cleanup();
    assertRequestAttributesDoNotExist();

    listener.beforeTestClass(testContext);

    listener.prepareTestInstance(testContext);
    assertRequestAttributesDoNotExist();
    verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
    given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

    listener.beforeTestMethod(testContext);
    assertRequestAttributesDoNotExist();
    verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

    listener.afterTestMethod(testContext);
    verify(testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
    assertRequestAttributesDoNotExist();
  }

  @Test
  void legacyWebTestCaseWithPresetRequestAttributes() throws Exception {
    BDDMockito.<Class<?>>given(testContext.getTestClass()).willReturn(LegacyWebTestCase.class);

    listener.beforeTestClass(testContext);
    assertSetUpOutsideOfStelAttributeExists();

    listener.prepareTestInstance(testContext);
    assertSetUpOutsideOfStelAttributeExists();
    verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
    given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

    listener.beforeTestMethod(testContext);
    assertSetUpOutsideOfStelAttributeExists();
    verify(testContext, times(0)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
    given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(null);

    listener.afterTestMethod(testContext);
    verify(testContext, times(1)).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
    assertSetUpOutsideOfStelAttributeExists();
  }

  @Test
  void atWebAppConfigTestCaseWithoutExistingRequestAttributes() throws Exception {
    BDDMockito.<Class<?>>given(testContext.getTestClass()).willReturn(AtWebAppConfigWebTestCase.class);

    HttpContextHolder.cleanup();
    listener.beforeTestClass(testContext);
    assertRequestAttributesDoNotExist();

    assertWebAppConfigTestCase();
  }

  @Test
  void atWebAppConfigTestCaseWithPresetRequestAttributes() throws Exception {
    BDDMockito.<Class<?>>given(testContext.getTestClass()).willReturn(AtWebAppConfigWebTestCase.class);

    listener.beforeTestClass(testContext);
    assertRequestAttributesExist();

    assertWebAppConfigTestCase();
  }

  /**
   * @since 4.0
   */
  @Test
  void activateListenerWithoutExistingRequestAttributes() throws Exception {
    BDDMockito.<Class<?>>given(testContext.getTestClass()).willReturn(NoAtWebAppConfigWebTestCase.class);
    given(testContext.getAttribute(WebMockTestExecutionListener.ACTIVATE_LISTENER)).willReturn(true);

    HttpContextHolder.cleanup();
    listener.beforeTestClass(testContext);
    assertRequestAttributesDoNotExist();

    assertWebAppConfigTestCase();
  }

  private HttpContext assertRequestAttributesExist() {
    HttpContext requestAttributes = HttpContextHolder.current();
    assertThat(requestAttributes).as("request attributes should exist").isNotNull();
    return requestAttributes;
  }

  private void assertRequestAttributesDoNotExist() {
    assertThat(HttpContextHolder.current()).as("request attributes should not exist").isNull();
  }

  private void assertSetUpOutsideOfStelAttributeExists() {
    HttpContext requestAttributes = assertRequestAttributesExist();
    Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL);
    assertThat(setUpOutsideOfStel).as(SET_UP_OUTSIDE_OF_STEL + " should exist as a request attribute").isNotNull();
  }

  private void assertSetUpOutsideOfStelAttributeDoesNotExist() {
    HttpContext requestAttributes = assertRequestAttributesExist();
    Object setUpOutsideOfStel = requestAttributes.getAttribute(SET_UP_OUTSIDE_OF_STEL);
    assertThat(setUpOutsideOfStel).as(SET_UP_OUTSIDE_OF_STEL + " should NOT exist as a request attribute").isNull();
  }

  private void assertWebAppConfigTestCase() throws Exception {
    listener.prepareTestInstance(testContext);
    assertRequestAttributesExist();
    assertSetUpOutsideOfStelAttributeDoesNotExist();
    verify(testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
    verify(testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
    given(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(Boolean.TRUE);
    given(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE)).willReturn(Boolean.TRUE);

    listener.beforeTestMethod(testContext);
    assertRequestAttributesExist();
    assertSetUpOutsideOfStelAttributeDoesNotExist();
    verify(testContext, times(1)).setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
    verify(testContext, times(1)).setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

    listener.afterTestMethod(testContext);
    verify(testContext).removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
    verify(testContext).removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
    assertRequestAttributesDoNotExist();
  }

  static class LegacyWebTestCase {
  }

  @WebAppConfiguration
  static class AtWebAppConfigWebTestCase {
  }

  static class NoAtWebAppConfigWebTestCase {
  }

}
