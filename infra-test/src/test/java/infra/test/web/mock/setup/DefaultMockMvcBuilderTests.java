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

package infra.test.web.mock.setup;

import org.junit.jupiter.api.Test;

import infra.beans.DirectFieldAccessor;
import infra.context.ApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.support.StaticApplicationContext;
import infra.web.mock.MockContextImpl;
import infra.test.web.mock.MockMvc;
import infra.web.config.annotation.EnableWebMvc;

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
  public void rootWacMockContainerAttributeNotPreviouslySetWithContextHierarchy() {
    StaticApplicationContext ear = new StaticApplicationContext();
    AnnotationConfigApplicationContext root = new AnnotationConfigApplicationContext();
    root.setParent(ear);
    AnnotationConfigApplicationContext dispatcher = new AnnotationConfigApplicationContext();
    dispatcher.setParent(root);

    DefaultMockMvcBuilder builder = webAppContextSetup(dispatcher);
    ApplicationContext wac = builder.initWebAppContext();

    assertThat(wac).isSameAs(dispatcher);
    assertThat(wac.getParent()).isSameAs(root);
    assertThat(wac.getParent().getParent()).isSameAs(ear);
  }

  @Test
  public void dispatcherCustomizer() {
    StubWebApplicationContext root = new StubWebApplicationContext();
    var reader = new AnnotatedBeanDefinitionReader(root);
    reader.register(WebConfig.class);
    root.refresh();

    DefaultMockMvcBuilder builder = webAppContextSetup(root);
    builder.addDispatcherCustomizer(ds -> ds.setDetectAllHandlerMapping(false));
    MockMvc mvc = builder.build();
    boolean detectAllHandlerMapping = (boolean) new DirectFieldAccessor(mvc)
            .getPropertyValue("dispatcherHandler.detectAllHandlerMapping");
    assertThat(detectAllHandlerMapping).isEqualTo(false);
  }

  @Test
  public void dispatcherCustomizerProcessedInOrder() {
    StubWebApplicationContext root = new StubWebApplicationContext();
    var reader = new AnnotatedBeanDefinitionReader(root);
    reader.register(WebConfig.class);
    root.refresh();

    DefaultMockMvcBuilder builder = webAppContextSetup(root);
    builder.addDispatcherCustomizer(ds -> ds.setDetectAllHandlerMapping(false));
    builder.addDispatcherCustomizer(ds -> ds.setDetectAllHandlerMapping(true));
    MockMvc mvc = builder.build();
    boolean detectAllHandlerMapping = (boolean) new DirectFieldAccessor(mvc)
            .getPropertyValue("dispatcherHandler.detectAllHandlerMapping");
    assertThat(detectAllHandlerMapping).isEqualTo(true);
  }

  @EnableWebMvc
  @Configuration
  static class WebConfig {

  }

}
