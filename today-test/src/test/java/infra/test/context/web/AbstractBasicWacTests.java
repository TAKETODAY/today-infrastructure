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

package infra.test.context.web;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import infra.beans.factory.annotation.Autowired;
import infra.mock.api.MockContext;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockHttpSession;
import infra.test.context.junit4.InfraRunner;
import infra.web.RequestContext;
import infra.web.mock.MockContextAware;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@WebAppConfiguration
public abstract class AbstractBasicWacTests implements MockContextAware {

  protected MockContext mockContext;

  @Autowired
  protected WebApplicationContext wac;

  @Autowired
  protected MockContext mockContextIn;

  @Autowired
  protected HttpMockRequest request;

  @Autowired
  protected MockHttpResponseImpl response;

  @Autowired
  protected MockHttpSession session;

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

    assertThat(mockContextIn).as("MockContext should have been autowired from the WAC.").isNotNull();
    assertThat(request).as("MockHttpServletRequest should have been autowired from the WAC.").isNotNull();
    assertThat(response).as("MockHttpServletResponse should have been autowired from the WAC.").isNotNull();
    assertThat(session).as("MockHttpSession should have been autowired from the WAC.").isNotNull();
    assertThat(webRequest).as("ServletWebRequest should have been autowired from the WAC.").isNotNull();

    Object rootWac = mockContextIn.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    assertThat(rootWac).as("Root WAC must be stored in the MockContext as: "
            + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE).isNotNull();
    assertThat(rootWac).as("test WAC and Root WAC in MockContext must be the same object.").isSameAs(wac);
    assertThat(wac.getMockContext()).as("MockContext instances must be the same object.").isSameAs(mockContextIn);
    assertThat(request.getMockContext()).as("MockContext in the WAC and in the mock request").isSameAs(mockContextIn);

    assertThat(mockContextIn.getRealPath("index.jsp")).as("Getting real path for MockContext resource.")
            .isEqualTo(new File("src/main/webapp/index.jsp")
                    .getCanonicalPath());

  }

}
