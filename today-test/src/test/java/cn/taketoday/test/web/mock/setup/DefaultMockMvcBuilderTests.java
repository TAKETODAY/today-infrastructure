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

package cn.taketoday.test.web.mock.setup;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.test.web.mock.MockMvc;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.mock.WebApplicationContext;
import cn.taketoday.web.mock.support.StaticWebApplicationContext;
import cn.taketoday.web.mock.support.WebApplicationContextUtils;

import static cn.taketoday.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultMockMvcBuilder}.
 *
 * @author Rob Winch
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
public class DefaultMockMvcBuilderTests {

  private final MockContextImpl mockContext = new MockContextImpl();

  /**
   * See SPR-12553 and SPR-13075.
   */
  @Test
  public void rootWacServletContainerAttributePreviouslySet() {
    StubWebApplicationContext child = new StubWebApplicationContext(this.mockContext);
    this.mockContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, child);

    DefaultMockMvcBuilder builder = webAppContextSetup(child);
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.mockContext)).isSameAs(builder.initWebAppContext());
  }

  /**
   * See SPR-12553 and SPR-13075.
   */
  @Test
  public void rootWacServletContainerAttributePreviouslySetWithContextHierarchy() {
    StubWebApplicationContext root = new StubWebApplicationContext(this.mockContext);

    this.mockContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, root);

    StaticWebApplicationContext child = new StaticWebApplicationContext();
    child.setParent(root);
    child.setMockContext(this.mockContext);

    DefaultMockMvcBuilder builder = webAppContextSetup(child);
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.mockContext)).isSameAs(builder.initWebAppContext().getParent());
  }

  /**
   * See SPR-12553 and SPR-13075.
   */
  @Test
  public void rootWacServletContainerAttributeNotPreviouslySet() {
    StubWebApplicationContext root = new StubWebApplicationContext(this.mockContext);
    DefaultMockMvcBuilder builder = webAppContextSetup(root);
    ApplicationContext wac = builder.initWebAppContext();
    assertThat(wac).isSameAs(root);
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.mockContext)).isSameAs(root);
  }

  /**
   * See SPR-12553 and SPR-13075.
   */
  @Test
  public void rootWacServletContainerAttributeNotPreviouslySetWithContextHierarchy() {
    StaticApplicationContext ear = new StaticApplicationContext();
    StaticWebApplicationContext root = new StaticWebApplicationContext();
    root.setParent(ear);
    root.setMockContext(this.mockContext);
    StaticWebApplicationContext dispatcher = new StaticWebApplicationContext();
    dispatcher.setParent(root);
    dispatcher.setMockContext(this.mockContext);

    DefaultMockMvcBuilder builder = webAppContextSetup(dispatcher);
    ApplicationContext wac = builder.initWebAppContext();

    assertThat(wac).isSameAs(dispatcher);
    assertThat(wac.getParent()).isSameAs(root);
    assertThat(wac.getParent().getParent()).isSameAs(ear);
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.mockContext)).isSameAs(root);
  }

  /**
   * See /SPR-14277
   */
  @Test
  public void dispatcherCustomizer() {
    StubWebApplicationContext root = new StubWebApplicationContext(this.mockContext);
    var reader = new AnnotatedBeanDefinitionReader(root);
    reader.register(WebConfig.class);

    DefaultMockMvcBuilder builder = webAppContextSetup(root);
    builder.addDispatcherCustomizer(ds -> ds.setContextId("test-id"));
    MockMvc mvc = builder.build();
    String contextId = (String) new DirectFieldAccessor(mvc)
            .getPropertyValue("servlet.contextId");
    assertThat(contextId).isEqualTo("test-id");
  }

  @Test
  public void dispatcherCustomizerProcessedInOrder() {
    StubWebApplicationContext root = new StubWebApplicationContext(this.mockContext);
    var reader = new AnnotatedBeanDefinitionReader(root);
    reader.register(WebConfig.class);

    DefaultMockMvcBuilder builder = webAppContextSetup(root);
    builder.addDispatcherCustomizer(ds -> ds.setContextId("test-id"));
    builder.addDispatcherCustomizer(ds -> ds.setContextId("override-id"));
    MockMvc mvc = builder.build();
    String contextId = (String) new DirectFieldAccessor(mvc)
            .getPropertyValue("servlet.contextId");
    assertThat(contextId).isEqualTo("override-id");
  }

  @EnableWebMvc
  @Configuration
  static class WebConfig {

  }

}
