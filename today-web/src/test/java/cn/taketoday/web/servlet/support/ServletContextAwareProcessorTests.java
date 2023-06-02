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

package cn.taketoday.web.servlet.support;

import org.junit.jupiter.api.Test;

import cn.taketoday.web.testfixture.servlet.MockServletConfig;
import cn.taketoday.web.testfixture.servlet.MockServletContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 19:12
 */
class ServletContextAwareProcessorTests {

  @Test
  public void servletContextAwareWithServletContext() {
    ServletContext servletContext = new MockServletContext();
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
    ServletContextAwareBean bean = new ServletContextAwareBean();
    assertThat(bean.getServletContext()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
    assertThat(bean.getServletContext()).isEqualTo(servletContext);
  }

  @Test
  public void servletContextAwareWithServletConfig() {
    ServletContext servletContext = new MockServletContext();
    ServletConfig servletConfig = new MockServletConfig(servletContext);
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletConfig);
    ServletContextAwareBean bean = new ServletContextAwareBean();
    assertThat(bean.getServletContext()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
    assertThat(bean.getServletContext()).isEqualTo(servletContext);
  }

  @Test
  public void servletContextAwareWithServletContextAndServletConfig() {
    ServletContext servletContext = new MockServletContext();
    ServletConfig servletConfig = new MockServletConfig(servletContext);
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, servletConfig);
    ServletContextAwareBean bean = new ServletContextAwareBean();
    assertThat(bean.getServletContext()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
    assertThat(bean.getServletContext()).isEqualTo(servletContext);
  }

  @Test
  public void servletContextAwareWithNullServletContextAndNonNullServletConfig() {
    ServletContext servletContext = new MockServletContext();
    ServletConfig servletConfig = new MockServletConfig(servletContext);
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(null, servletConfig);
    ServletContextAwareBean bean = new ServletContextAwareBean();
    assertThat(bean.getServletContext()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
    assertThat(bean.getServletContext()).isEqualTo(servletContext);
  }

  @Test
  public void servletContextAwareWithNonNullServletContextAndNullServletConfig() {
    ServletContext servletContext = new MockServletContext();
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, null);
    ServletContextAwareBean bean = new ServletContextAwareBean();
    assertThat(bean.getServletContext()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletContext()).as("ServletContext should have been set").isNotNull();
    assertThat(bean.getServletContext()).isEqualTo(servletContext);
  }

  @Test
  public void servletContextAwareWithNullServletContext() {
    ServletContext servletContext = null;
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
    ServletContextAwareBean bean = new ServletContextAwareBean();
    assertThat(bean.getServletContext()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletContext()).isNull();
  }

  @Test
  public void servletConfigAwareWithServletContextOnly() {
    ServletContext servletContext = new MockServletContext();
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
    ServletConfigAwareBean bean = new ServletConfigAwareBean();
    assertThat(bean.getServletConfig()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletConfig()).isNull();
  }

  @Test
  public void servletConfigAwareWithServletConfig() {
    ServletContext servletContext = new MockServletContext();
    ServletConfig servletConfig = new MockServletConfig(servletContext);
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletConfig);
    ServletConfigAwareBean bean = new ServletConfigAwareBean();
    assertThat(bean.getServletConfig()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletConfig()).as("ServletConfig should have been set").isNotNull();
    assertThat(bean.getServletConfig()).isEqualTo(servletConfig);
  }

  @Test
  public void servletConfigAwareWithServletContextAndServletConfig() {
    ServletContext servletContext = new MockServletContext();
    ServletConfig servletConfig = new MockServletConfig(servletContext);
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, servletConfig);
    ServletConfigAwareBean bean = new ServletConfigAwareBean();
    assertThat(bean.getServletConfig()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletConfig()).as("ServletConfig should have been set").isNotNull();
    assertThat(bean.getServletConfig()).isEqualTo(servletConfig);
  }

  @Test
  public void servletConfigAwareWithNullServletContextAndNonNullServletConfig() {
    ServletContext servletContext = new MockServletContext();
    ServletConfig servletConfig = new MockServletConfig(servletContext);
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(null, servletConfig);
    ServletConfigAwareBean bean = new ServletConfigAwareBean();
    assertThat(bean.getServletConfig()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletConfig()).as("ServletConfig should have been set").isNotNull();
    assertThat(bean.getServletConfig()).isEqualTo(servletConfig);
  }

  @Test
  public void servletConfigAwareWithNonNullServletContextAndNullServletConfig() {
    ServletContext servletContext = new MockServletContext();
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext, null);
    ServletConfigAwareBean bean = new ServletConfigAwareBean();
    assertThat(bean.getServletConfig()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletConfig()).isNull();
  }

  @Test
  public void servletConfigAwareWithNullServletContext() {
    ServletContext servletContext = null;
    ServletContextAwareProcessor processor = new ServletContextAwareProcessor(servletContext);
    ServletConfigAwareBean bean = new ServletConfigAwareBean();
    assertThat(bean.getServletConfig()).isNull();
    processor.postProcessBeforeInitialization(bean, "testBean");
    assertThat(bean.getServletConfig()).isNull();
  }

}