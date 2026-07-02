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

import org.junit.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.web.mock.api.MockContext;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.MockSession;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.AbstractJUnit4ContextTests;
import infra.web.RequestContext;
import infra.web.mock.MockContextAware;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit-based integration tests that verify support for loading a
 * {@link WebApplicationContext} when extending {@link AbstractJUnit4ContextTests}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
@WebAppConfiguration
public class JUnit4ContextWebTests extends AbstractJUnit4ContextTests implements MockContextAware {

  @Configuration
  static class Config {

    @Bean
    public String foo() {
      return "enigma";
    }
  }

  protected MockContext mockContext;

  @Autowired
  protected WebApplicationContext wac;

  @Autowired
  protected MockContext mockContextIm;

  @Autowired
  protected MockRequest request;

  @Autowired
  protected MockResponse response;

  @Autowired
  protected MockSession session;

  @Autowired
  protected RequestContext webRequest;

  @Autowired
  protected String foo;

  @Override
  public void setMockContext(MockContext mockContext) {
    this.mockContext = mockContext;
  }

  @Test
  public void basicWacFeatures() throws Exception {
    assertThat(wac.getMockContext()).as("MockContext should be set in the WAC.").isNotNull();

    assertThat(mockContext).as("MockContext should have been set via MockContextAware.").isNotNull();

    assertThat(mockContextIm).as("MockContext should have been autowired from the WAC.").isNotNull();
    assertThat(request).as("MockRequest should have been autowired from the WAC.").isNotNull();
    assertThat(response).as("MockHttpResponseImpl should have been autowired from the WAC.").isNotNull();
    assertThat(session).as("MockHttpSession should have been autowired from the WAC.").isNotNull();
    assertThat(webRequest).as("RequestContext should have been autowired from the WAC.").isNotNull();

    Object rootWac = mockContextIm.getAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    assertThat(rootWac).as("Root WAC must be stored in the MockContext as: "
            + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE).isNotNull();
    assertThat(rootWac).as("test WAC and Root WAC in MockContext must be the same object.").isSameAs(wac);
    assertThat(wac.getMockContext()).as("MockContext instances must be the same object.").isSameAs(mockContext);

  }

  @Test
  public void fooEnigmaAutowired() {
    assertThat(foo).isEqualTo("enigma");
  }

}
