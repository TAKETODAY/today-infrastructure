/*
 * Copyright 2022 the original author or authors.
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

package infra.shell.jline.config;

import org.jline.terminal.Terminal;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.core.annotation.Order;
import infra.core.io.ResourceLoader;
import infra.shell.jline.tui.component.flow.ComponentFlow;
import infra.shell.jline.tui.component.flow.ComponentFlow.Builder;
import infra.shell.jline.tui.style.TemplateExecutor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link ComponentFlow}.
 *
 * @author Janne Valkealahti
 */
@AutoConfiguration
@ConditionalOnClass(ComponentFlow.class)
public class ComponentFlowAutoConfiguration {

  @Bean
  @Scope("prototype")
  @ConditionalOnMissingBean
  public ComponentFlow.Builder componentFlowBuilder(ObjectProvider<ComponentFlowCustomizer> customizerProvider) {
    ComponentFlow.Builder builder = ComponentFlow.builder();
    customizerProvider.orderedStream().forEach((customizer) -> customizer.customize(builder));
    return builder;
  }

  @Configuration(proxyBeanMethods = false)
  protected static class ComponentFlowConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Order(0)
    public ComponentFlowCustomizer shellCommonComponentFlowCustomizer(ObjectProvider<Terminal> terminal,
            ObjectProvider<ResourceLoader> resourceLoader, ObjectProvider<TemplateExecutor> templateExecutor) {
      return new CommonComponentFlowCustomizer(terminal, resourceLoader, templateExecutor);
    }

  }

  private static class CommonComponentFlowCustomizer implements ComponentFlowCustomizer {

    private final ObjectProvider<Terminal> terminal;

    private final ObjectProvider<ResourceLoader> resourceLoader;

    private final ObjectProvider<TemplateExecutor> templateExecutor;

    CommonComponentFlowCustomizer(ObjectProvider<Terminal> terminal, ObjectProvider<ResourceLoader> resourceLoader,
            ObjectProvider<TemplateExecutor> templateExecutor) {
      this.terminal = terminal;
      this.resourceLoader = resourceLoader;
      this.templateExecutor = templateExecutor;
    }

    @Override
    public void customize(Builder componentFlowBuilder) {
      terminal.ifAvailable(dep -> componentFlowBuilder.terminal(dep));
      resourceLoader.ifAvailable(dep -> componentFlowBuilder.resourceLoader(dep));
      templateExecutor.ifAvailable(dep -> componentFlowBuilder.templateExecutor(dep));
    }

  }

}
