/*
 * Copyright 2023-2024 the original author or authors.
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
import infra.context.annotation.Scope;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.shell.jline.tui.component.ViewComponentBuilder;
import infra.shell.jline.tui.component.ViewComponentExecutor;
import infra.shell.jline.tui.component.view.TerminalUI;
import infra.shell.jline.tui.component.view.TerminalUIBuilder;
import infra.shell.jline.tui.component.view.TerminalUICustomizer;
import infra.shell.jline.tui.style.ThemeActive;
import infra.shell.jline.tui.style.ThemeResolver;

@AutoConfiguration
@ConditionalOnClass(TerminalUI.class)
public class TerminalUIAutoConfiguration {

  @Bean
  @Scope("prototype")
  @ConditionalOnMissingBean
  public TerminalUIBuilder terminalUIBuilder(Terminal terminal, ThemeResolver themeResolver, ThemeActive themeActive,
          ObjectProvider<TerminalUICustomizer> customizerProvider) {
    TerminalUIBuilder builder = new TerminalUIBuilder(terminal);
    builder = builder.themeName(themeActive.get());
    builder = builder.themeResolver(themeResolver);
    builder = builder.customizers(customizerProvider.orderedStream().toList());
    return builder;
  }

  @Bean
  @Scope("prototype")
  @ConditionalOnMissingBean
  public ViewComponentBuilder viewComponentBuilder(TerminalUIBuilder terminalUIBuilder,
          ViewComponentExecutor viewComponentExecutor, Terminal terminal) {
    return new ViewComponentBuilder(terminalUIBuilder, viewComponentExecutor, terminal);
  }

  @Bean
  @ConditionalOnMissingBean
  public ViewComponentExecutor viewComponentExecutor() {
    return new ViewComponentExecutor();
  }

}
