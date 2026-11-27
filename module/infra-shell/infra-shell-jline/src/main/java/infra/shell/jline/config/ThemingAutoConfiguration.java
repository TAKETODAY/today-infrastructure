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

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.shell.config.InfraShellProperties;
import infra.shell.jline.tui.style.TemplateExecutor;
import infra.shell.jline.tui.style.Theme;
import infra.shell.jline.tui.style.ThemeActive;
import infra.shell.jline.tui.style.ThemeRegistry;
import infra.shell.jline.tui.style.ThemeResolver;
import infra.shell.jline.tui.style.ThemeSettings;
import infra.util.StringUtils;

@AutoConfiguration
@EnableConfigurationProperties(InfraShellProperties.class)
public class ThemingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ThemeActive themeActive() {
    return () -> {
      if (System.getenv("CI") != null || System.getenv("NO_COLOR") != null) {
        return "dump";
      }
      return "default";
    };
  }

  @Bean
  public ThemeRegistry themeRegistry(ObjectProvider<Theme> themes) {
    ThemeRegistry registry = new ThemeRegistry();
    registry.register(Theme.of("default", ThemeSettings.defaults()));
    registry.register(Theme.of("dump", ThemeSettings.dump()));
    themes.orderedStream().forEachOrdered(registry::register);
    return registry;
  }

  @Bean
  public ThemeResolver shellThemeResolver(ThemeRegistry themeRegistry, InfraShellProperties properties,
          ThemeActive themeActive) {
    String themeName = properties.getTheme().getName();
    if (!StringUtils.hasText(themeName)) {
      themeName = themeActive.get();
    }
    if (!StringUtils.hasText(themeName)) {
      themeName = "default";
    }
    return new ThemeResolver(themeRegistry, themeName);
  }

  @Bean
  public TemplateExecutor templateExecutor(ThemeResolver themeResolver) {
    return new TemplateExecutor(themeResolver);
  }

}
