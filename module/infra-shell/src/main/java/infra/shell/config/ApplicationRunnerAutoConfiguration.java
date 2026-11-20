/*
 * Copyright 2021-present the original author or authors.
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

package infra.shell.config;

import infra.app.context.event.ApplicationReadyEvent;
import infra.context.ApplicationListener;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnProperty;
import infra.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(InfraShellProperties.class)
public class ApplicationRunnerAutoConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "shell.context", name = "close", havingValue = "true")
  public ApplicationReadyEventListener applicationReadyEventListener() {
    return new ApplicationReadyEventListener();
  }

  static class ApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
      // request context close after application runners so that
      // shell exits in case that context is kept alive
      event.getApplicationContext().close();
    }

  }

}
