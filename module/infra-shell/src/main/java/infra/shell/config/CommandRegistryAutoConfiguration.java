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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.MethodIntrospector;
import infra.core.annotation.AnnotatedElementUtils;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.shell.command.Command;
import infra.shell.command.CommandRegistry;
import infra.shell.command.annotation.support.CommandFactoryBean;
import infra.shell.utils.Utils;
import infra.util.ReflectionUtils;

import static infra.shell.utils.Utils.isProfileActive;

@AutoConfiguration
@EnableConfigurationProperties(InfraShellProperties.class)
public class CommandRegistryAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(CommandRegistryAutoConfiguration.class);

  @Bean
  @ConditionalOnMissingBean
  public CommandRegistry commandRegistry(ApplicationContext applicationContext,
          InfraShellProperties infraShellProperties) {
    CommandRegistry commandRegistry = new CommandRegistry();
    registerProgrammaticCommands(applicationContext, commandRegistry);
    registerAnnotatedCommands(applicationContext, commandRegistry);
    boolean quitCommandEnabled = infraShellProperties.getCommand().getQuit().isEnabled();
    if (!quitCommandEnabled && infraShellProperties.getInteractive().isEnabled()) {
      // in interactive mode, typing quit/exit always works regardless of the
      // command registry (see InteractiveShellRunner), so keep it listed in help to
      // avoid hiding a still-functional way to leave the shell
      log.warn(
              "shell.command.quit.enabled=false has no effect in interactive mode: the quit/exit command remains registered so it stays discoverable via 'help'");
      quitCommandEnabled = true;
    }
    if (quitCommandEnabled) {
      commandRegistry.registerCommand(Utils.QUIT_COMMAND);
    }
    return commandRegistry;
  }

  private void registerProgrammaticCommands(ApplicationContext applicationContext, CommandRegistry commandRegistry) {
    Map<String, Command> commandBeans = applicationContext.getBeansOfType(Command.class);
    commandBeans.values().forEach(commandRegistry::registerCommand);
  }

  private void registerAnnotatedCommands(ApplicationContext applicationContext, CommandRegistry commandRegistry) {
    ReflectionUtils.MethodFilter filter = method -> AnnotatedElementUtils.hasAnnotation(method,
            infra.shell.command.annotation.Command.class)
            && isProfileActive(method, applicationContext.getEnvironment());

    for (String beanName : applicationContext.getBeanDefinitionNames()) {
      Class<?> type = applicationContext.getType(beanName, false);
      if (type == null) {
        continue;
      }
      String className = type.getName();
      if (className.startsWith("infra.app")) {
        continue;
      }

      Set<Method> methods = MethodIntrospector.filterMethods(type, filter);
      if (methods.isEmpty()) {
        continue;
      }

      log.debug("Registering commands from component: " + className);
      for (Method method : methods) {
        CommandFactoryBean factoryBean = new CommandFactoryBean(method);
        factoryBean.setApplicationContext(applicationContext);
        commandRegistry.registerCommand(factoryBean.getObject());
      }
    }
  }

}
