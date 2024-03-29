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

package cn.taketoday.test.context.web;

import org.junit.Test;

import java.io.File;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.mock.web.MockHttpSession;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.AbstractJUnit4ContextTests;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletContextAware;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.ServletContext;

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
public class JUnit4ContextWebTests extends AbstractJUnit4ContextTests implements ServletContextAware {

  @Configuration
  static class Config {

    @Bean
    public String foo() {
      return "enigma";
    }
  }

  protected ServletContext servletContext;

  @Autowired
  protected WebApplicationContext wac;

  @Autowired
  protected MockServletContext mockServletContext;

  @Autowired
  protected MockHttpServletRequest request;

  @Autowired
  protected MockHttpServletResponse response;

  @Autowired
  protected MockHttpSession session;

  @Autowired
  protected RequestContext webRequest;

  @Autowired
  protected String foo;

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Test
  public void basicWacFeatures() throws Exception {
    assertThat(wac.getServletContext()).as("ServletContext should be set in the WAC.").isNotNull();

    assertThat(servletContext).as("ServletContext should have been set via ServletContextAware.").isNotNull();

    assertThat(mockServletContext).as("ServletContext should have been autowired from the WAC.").isNotNull();
    assertThat(request).as("MockHttpServletRequest should have been autowired from the WAC.").isNotNull();
    assertThat(response).as("MockHttpServletResponse should have been autowired from the WAC.").isNotNull();
    assertThat(session).as("MockHttpSession should have been autowired from the WAC.").isNotNull();
    assertThat(webRequest).as("RequestContext should have been autowired from the WAC.").isNotNull();

    Object rootWac = mockServletContext.getAttribute(
            WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    assertThat(rootWac).as("Root WAC must be stored in the ServletContext as: "
            + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE).isNotNull();
    assertThat(rootWac).as("test WAC and Root WAC in ServletContext must be the same object.").isSameAs(wac);
    assertThat(wac.getServletContext()).as("ServletContext instances must be the same object.").isSameAs(mockServletContext);
    assertThat(request.getServletContext()).as("ServletContext in the WAC and in the mock request").isSameAs(mockServletContext);

    assertThat(mockServletContext.getRealPath("index.jsp")).as("Getting real path for ServletContext resource.")
            .isEqualTo(new File("src/main/webapp/index.jsp").getCanonicalPath());
  }

  @Test
  public void fooEnigmaAutowired() {
    assertThat(foo).isEqualTo("enigma");
  }

}
