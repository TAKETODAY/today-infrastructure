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

package cn.taketoday.test.context.web;

import org.junit.Test;

import java.io.File;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.mock.web.MockHttpSession;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.AbstractJUnit4ContextTests;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mock.MockContextAware;
import cn.taketoday.web.mock.WebApplicationContext;

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

    assertThat(mockContextIm).as("MockContext should have been autowired from the WAC.").isNotNull();
    assertThat(request).as("MockHttpServletRequest should have been autowired from the WAC.").isNotNull();
    assertThat(response).as("MockHttpServletResponse should have been autowired from the WAC.").isNotNull();
    assertThat(session).as("MockHttpSession should have been autowired from the WAC.").isNotNull();
    assertThat(webRequest).as("RequestContext should have been autowired from the WAC.").isNotNull();

    Object rootWac = mockContextIm.getAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    assertThat(rootWac).as("Root WAC must be stored in the MockContext as: "
            + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE).isNotNull();
    assertThat(rootWac).as("test WAC and Root WAC in MockContext must be the same object.").isSameAs(wac);
    assertThat(wac.getMockContext()).as("MockContext instances must be the same object.").isSameAs(mockContext);
    assertThat(request.getMockContext()).as("MockContext in the WAC and in the mock request").isSameAs(mockContext);

    assertThat(mockContextIm.getRealPath("index.jsp")).as("Getting real path for MockContext resource.")
            .isEqualTo(new File("src/main/webapp/index.jsp").getCanonicalPath());
  }

  @Test
  public void fooEnigmaAutowired() {
    assertThat(foo).isEqualTo("enigma");
  }

}
