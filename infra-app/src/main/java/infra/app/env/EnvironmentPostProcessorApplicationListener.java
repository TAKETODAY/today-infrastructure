/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.env;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Modifier;

import infra.aot.AotDetector;
import infra.aot.generate.GeneratedClass;
import infra.aot.generate.GenerationContext;
import infra.app.Application;
import infra.app.BootstrapContext;
import infra.app.BootstrapRegistry;
import infra.app.ConfigurableBootstrapContext;
import infra.app.context.event.ApplicationEnvironmentPreparedEvent;
import infra.beans.BeanInstantiationException;
import infra.beans.BeanUtils;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationEvent;
import infra.context.event.SmartApplicationListener;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.io.ResourceLoader;
import infra.javapoet.CodeBlock;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.util.ClassUtils;
import infra.util.Instantiator;
import infra.util.ObjectUtils;

/**
 * {@link SmartApplicationListener} used to trigger {@link EnvironmentPostProcessor
 * EnvironmentPostProcessors} registered in the {@code today.strategies} file.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/3 00:32
 */
public class EnvironmentPostProcessorApplicationListener implements SmartApplicationListener, Ordered {

  private static final String AOT_FEATURE_NAME = "EnvironmentPostProcessor";

  /**
   * The default order for the processor.
   */
  public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

  private int order = DEFAULT_ORDER;

  @Override
  public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
    return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (event instanceof ApplicationEnvironmentPreparedEvent e) {
      Application application = e.getApplication();
      ConfigurableEnvironment environment = e.getEnvironment();
      List<EnvironmentPostProcessor> postProcessors = getPostProcessors(application.getResourceLoader(), e.getBootstrapContext());
      addAotGeneratedEnvironmentPostProcessorIfNecessary(postProcessors, application);
      for (var postProcessor : postProcessors) {
        postProcessor.postProcessEnvironment(environment, application);
      }
    }
  }

  List<EnvironmentPostProcessor> getPostProcessors(@Nullable ResourceLoader resourceLoader, ConfigurableBootstrapContext bootstrapContext) {
    ClassLoader classLoader = resourceLoader != null ? resourceLoader.getClassLoader() : null;

    Instantiator<EnvironmentPostProcessor> instantiator = new Instantiator<>(EnvironmentPostProcessor.class,
            parameters -> {
              parameters.add(BootstrapContext.class, bootstrapContext);
              parameters.add(BootstrapRegistry.class, bootstrapContext);
              parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
            });

    List<String> strategiesNames = TodayStrategies.findNames(EnvironmentPostProcessor.class, classLoader);
    return instantiator.instantiate(strategiesNames);
  }

  @SuppressWarnings("NullAway")
  private void addAotGeneratedEnvironmentPostProcessorIfNecessary(List<EnvironmentPostProcessor> postProcessors, Application application) {
    if (AotDetector.useGeneratedArtifacts()) {
      ClassLoader classLoader = (application.getResourceLoader() != null)
              ? application.getResourceLoader().getClassLoader() : null;
      String postProcessorClassName = application.getMainApplicationClass().getName() + "__" + AOT_FEATURE_NAME;
      if (ClassUtils.isPresent(postProcessorClassName, classLoader)) {
        postProcessors.add(0, instantiateEnvironmentPostProcessor(postProcessorClassName, classLoader));
      }
    }
  }

  private EnvironmentPostProcessor instantiateEnvironmentPostProcessor(String postProcessorClassName, ClassLoader classLoader) {
    try {
      Class<?> initializerClass = ClassUtils.resolveClassName(postProcessorClassName, classLoader);
      Assert.isAssignable(EnvironmentPostProcessor.class, initializerClass);
      return (EnvironmentPostProcessor) BeanUtils.newInstance(initializerClass);
    }
    catch (BeanInstantiationException ex) {
      throw new IllegalArgumentException(
              "Failed to instantiate EnvironmentPostProcessor: " + postProcessorClassName, ex);
    }
  }

  /**
   * Contribute a {@code <Application>__EnvironmentPostProcessor} class that stores AOT
   * optimizations.
   */
  static class EnvironmentBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Nullable
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableBeanFactory beanFactory) {
      Environment environment = beanFactory.getBean(Environment.ENVIRONMENT_BEAN_NAME, Environment.class);
      String[] activeProfiles = environment.getActiveProfiles();
      String[] defaultProfiles = environment.getDefaultProfiles();
      if (ObjectUtils.isNotEmpty(activeProfiles) && !Arrays.equals(activeProfiles, defaultProfiles)) {
        return new EnvironmentAotContribution(activeProfiles);
      }
      return null;
    }

  }

  private static final class EnvironmentAotContribution implements BeanFactoryInitializationAotContribution {

    private static final String ENVIRONMENT_VARIABLE = "environment";

    private final String[] activeProfiles;

    private EnvironmentAotContribution(String[] activeProfiles) {
      this.activeProfiles = activeProfiles;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
      GeneratedClass generatedClass = generationContext.getGeneratedClasses()
              .addForFeature(AOT_FEATURE_NAME, (type) -> {
                type.addModifiers(Modifier.PUBLIC);
                type.addJavadoc("Configure the environment with AOT optimizations.");
                type.addSuperinterface(EnvironmentPostProcessor.class);
              });
      generatedClass.getMethods().add("postProcessEnvironment", (method) -> {
        method.addModifiers(Modifier.PUBLIC);
        method.addAnnotation(Override.class);
        method.addParameter(ConfigurableEnvironment.class, ENVIRONMENT_VARIABLE);
        method.addParameter(Application.class, "application");
        method.addCode(generateActiveProfilesInitializeCode());
      });
    }

    private CodeBlock generateActiveProfilesInitializeCode() {
      CodeBlock.Builder code = CodeBlock.builder();
      for (String activeProfile : this.activeProfiles) {
        code.addStatement("$L.addActiveProfile($S)", ENVIRONMENT_VARIABLE, activeProfile);
      }
      return code.build();
    }

  }

}
