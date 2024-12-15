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

package infra.test.web.mock.setup;

import org.junit.jupiter.api.Test;

import infra.beans.DirectFieldAccessor;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.Configuration;
import infra.context.support.StaticApplicationContext;
import infra.mock.web.MockContextImpl;
import infra.test.web.mock.MockMvc;
import infra.web.config.EnableWebMvc;
import infra.web.mock.WebApplicationContext;
import infra.web.mock.support.StaticWebApplicationContext;
import infra.web.mock.support.WebApplicationContextUtils;

import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
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

  @Test
  public void rootWacMockContainerAttributePreviouslySet() {
    StubWebApplicationContext child = new StubWebApplicationContext(this.mockContext);
    this.mockContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, child);

    DefaultMockMvcBuilder builder = webAppContextSetup(child);
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.mockContext)).isSameAs(builder.initWebAppContext());
  }

  @Test
  public void rootWacMockContainerAttributePreviouslySetWithContextHierarchy() {
    StubWebApplicationContext root = new StubWebApplicationContext(this.mockContext);

    this.mockContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, root);

    StaticWebApplicationContext child = new StaticWebApplicationContext();
    child.setParent(root);
    child.setMockContext(this.mockContext);

    DefaultMockMvcBuilder builder = webAppContextSetup(child);
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.mockContext)).isSameAs(builder.initWebAppContext().getParent());
  }

  @Test
  public void rootWacMockContainerAttributeNotPreviouslySet() {
    StubWebApplicationContext root = new StubWebApplicationContext(this.mockContext);
    DefaultMockMvcBuilder builder = webAppContextSetup(root);
    ApplicationContext wac = builder.initWebAppContext();
    assertThat(wac).isSameAs(root);
    assertThat(WebApplicationContextUtils.getRequiredWebApplicationContext(this.mockContext)).isSameAs(root);
  }

  @Test
  public void rootWacMockContainerAttributeNotPreviouslySetWithContextHierarchy() {
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

  @Test
  public void dispatcherCustomizer() {
    StubWebApplicationContext root = new StubWebApplicationContext(this.mockContext);
    var reader = new AnnotatedBeanDefinitionReader(root);
    reader.register(WebConfig.class);

    DefaultMockMvcBuilder builder = webAppContextSetup(root);
    builder.addDispatcherCustomizer(ds -> ds.setContextId("test-id"));
    MockMvc mvc = builder.build();
    String contextId = (String) new DirectFieldAccessor(mvc)
            .getPropertyValue("mock.contextId");
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
            .getPropertyValue("mock.contextId");
    assertThat(contextId).isEqualTo("override-id");
  }

  @EnableWebMvc
  @Configuration
  static class WebConfig {

  }

}
