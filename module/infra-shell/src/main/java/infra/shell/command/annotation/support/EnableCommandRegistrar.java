/*
 * Copyright 2023-present the original author or authors.
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

package infra.shell.command.annotation.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.BootstrapContext;
import infra.context.EnvironmentAware;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.core.MethodIntrospector;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.env.Environment;
import infra.core.type.AnnotationMetadata;
import infra.shell.ConsoleInputProvider;
import infra.shell.SystemShellRunner;
import infra.shell.command.Clear;
import infra.shell.command.CommandRegistry;
import infra.shell.command.DefaultCommandParser;
import infra.shell.command.Help;
import infra.shell.command.Script;
import infra.shell.command.Version;
import infra.shell.command.annotation.Command;
import infra.shell.command.annotation.EnableCommand;
import infra.util.ReflectionUtils;

import static infra.shell.utils.Utils.isProfileActive;

/**
 * {@link ImportBeanDefinitionRegistrar} for {@link EnableCommand @EnableCommands}.
 *
 * @author Janne Valkealahti
 * @author Mahmoud Ben Hassine
 * @author David Pilar
 */
public final class EnableCommandRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

  private Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void registerBeanDefinitions(AnnotationMetadata metadata, BootstrapContext context) {
    BeanDefinitionRegistry registry = context.getRegistry();
    EnableCommand shellAnnotation = metadata.getAnnotations().get(EnableCommand.class).synthesize();
    // register built-in commands
    registry.registerBeanDefinition("help", new RootBeanDefinition(Help.class));
    registry.registerBeanDefinition("clear", new RootBeanDefinition(Clear.class));
    registry.registerBeanDefinition("version", new RootBeanDefinition(Version.class));
    registry.registerBeanDefinition("script", new RootBeanDefinition(Script.class));

    // register user defined commands
    Class<?>[] candidateClasses = shellAnnotation.value();
    for (Class<?> candidateClass : candidateClasses) {
      registerCommands(candidateClass, registry);
      registerAnnotatedMethods(candidateClass, registry);
    }

    // register command registry
    if (!registry.containsBeanDefinition("commandRegistry")) {
      RootBeanDefinition beanDefinition = new RootBeanDefinition(CommandRegistry.class);
      registry.registerBeanDefinition("commandRegistry", beanDefinition);
    }

    // register command parser
    if (!registry.containsBeanDefinition("commandParser")) {
      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
              .genericBeanDefinition(DefaultCommandParser.class)
              .addConstructorArgReference("commandRegistry");
      registry.registerBeanDefinition("commandParser", beanDefinitionBuilder.getBeanDefinition());
    }

    // register shell runner (default to interactive if none is defined)
    if (!registry.containsBeanDefinition("shellRunner")) {
      boolean debugModeValue = System.getProperty("shell.debug.enabled", "false").equalsIgnoreCase("true");
      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
              .genericBeanDefinition(SystemShellRunner.class)
              .addConstructorArgValue(new ConsoleInputProvider())
              .addConstructorArgReference("commandParser")
              .addConstructorArgReference("commandRegistry")
              .addPropertyValue("debugMode", debugModeValue);
      registry.registerBeanDefinition("shellRunner", beanDefinitionBuilder.getBeanDefinition());
    }
  }

  private void registerCommands(Class<?> candidateClass, BeanDefinitionRegistry registry) {
    Arrays.stream(candidateClass.getDeclaredMethods())
            .filter(method -> method.getReturnType().equals(infra.shell.command.Command.class))
            .forEach(method -> {
              RootBeanDefinition beanDefinition = new RootBeanDefinition(
                      infra.shell.command.Command.class);
              registry.registerBeanDefinition(method.getName(), beanDefinition);
            });
  }

  private void registerAnnotatedMethods(Class<?> candidateClass, BeanDefinitionRegistry registry) {
    ReflectionUtils.MethodFilter filter = method -> AnnotatedElementUtils.hasAnnotation(method, Command.class)
            && isProfileActive(method, this.environment);
    Set<Method> methods = MethodIntrospector.filterMethods(candidateClass, filter);
    for (Method method : methods) {
      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
              .genericBeanDefinition(CommandFactoryBean.class);
      beanDefinitionBuilder.addConstructorArgValue(method);
      registry.registerBeanDefinition(method.getName(), beanDefinitionBuilder.getBeanDefinition());
    }
  }

}
