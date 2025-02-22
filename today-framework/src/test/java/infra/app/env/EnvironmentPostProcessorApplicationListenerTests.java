/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.env;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import infra.aot.AotDetector;
import infra.aot.test.generate.TestGenerationContext;
import infra.app.Application;
import infra.app.ApplicationArguments;
import infra.app.ApplicationType;
import infra.app.BootstrapRegistry;
import infra.app.ConfigurableBootstrapContext;
import infra.app.DefaultBootstrapContext;
import infra.app.context.event.ApplicationEnvironmentPreparedEvent;
import infra.app.context.event.ApplicationFailedEvent;
import infra.app.context.event.ApplicationPreparedEvent;
import infra.app.context.event.ApplicationStartingEvent;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.aot.ApplicationContextAotGenerator;
import infra.context.support.GenericApplicationContext;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.StandardEnvironment;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.ClassName;
import infra.lang.Nullable;
import infra.lang.TodayStrategies;
import infra.mock.env.MockEnvironment;
import infra.mock.env.MockPropertySource;
import infra.util.StringUtils;

import static infra.app.env.EnvironmentPostProcessorApplicationListener.EnvironmentBeanFactoryInitializationAotProcessor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link EnvironmentPostProcessorApplicationListener}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class EnvironmentPostProcessorApplicationListenerTests {

  @Nested
  class ListenerTests {

    private final DefaultBootstrapContext bootstrapContext = spy(new DefaultBootstrapContext());

    private final EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener() {

      @Override
      List<EnvironmentPostProcessor> getPostProcessors(@Nullable ResourceLoader resourceLoader, ConfigurableBootstrapContext bootstrapContext) {
        List<EnvironmentPostProcessor> postProcessors = super.getPostProcessors(resourceLoader, bootstrapContext);
        postProcessors.add(new TestEnvironmentPostProcessor(bootstrapContext));
        return postProcessors;
      }

    };

    @BeforeEach
    void setup() {

    }

    @Test
    void createUsesFactories() {
      EnvironmentPostProcessorApplicationListener listener = new EnvironmentPostProcessorApplicationListener();
      assertThat(listener.getPostProcessors(null, this.bootstrapContext)).hasSizeGreaterThan(1);
    }

    @Test
    void supportsEventTypeWhenApplicationEnvironmentPreparedEventReturnsTrue() {
      assertThat(this.listener.supportsEventType(ApplicationEnvironmentPreparedEvent.class)).isTrue();
    }

    @Test
    void supportsEventTypeWhenApplicationPreparedEventReturnsTrue() {
      assertThat(this.listener.supportsEventType(ApplicationPreparedEvent.class)).isFalse();
    }

    @Test
    void supportsEventTypeWhenApplicationFailedEventReturnsTrue() {
      assertThat(this.listener.supportsEventType(ApplicationFailedEvent.class)).isFalse();
    }

    @Test
    void supportsEventTypeWhenOtherEventReturnsFalse() {
      assertThat(this.listener.supportsEventType(ApplicationStartingEvent.class)).isFalse();
    }

    @Test
    void onApplicationEventWhenApplicationEnvironmentPreparedEventCallsPostProcessors() {
      Application application = mock(Application.class);
      MockEnvironment environment = new MockEnvironment();
      ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(this.bootstrapContext,
              application, new ApplicationArguments(), environment);
      this.listener.onApplicationEvent(event);
      assertThat(environment.getProperty("processed")).isEqualTo("true");
    }

    @Test
    void onApplicationEventWhenApplicationPreparedEventSwitchesLogs() {
      Application application = mock(Application.class);
      ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
      ApplicationPreparedEvent event = new ApplicationPreparedEvent(application, new ApplicationArguments(), context);
      this.listener.onApplicationEvent(event);
    }

    @Test
    void onApplicationEventWhenApplicationFailedEventSwitchesLogs() {
      Application application = mock(Application.class);
      ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
      ApplicationFailedEvent event = new ApplicationFailedEvent(application, new ApplicationArguments(), context,
              new RuntimeException());
      this.listener.onApplicationEvent(event);
    }

    static class TestEnvironmentPostProcessor implements EnvironmentPostProcessor {

      TestEnvironmentPostProcessor(BootstrapRegistry bootstrapRegistry) {
        assertThat(bootstrapRegistry).isNotNull();
      }

      @Override
      public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
        ((MockEnvironment) environment).setProperty("processed", "true");
      }

    }

  }

  @Nested
  class AotTests {

    private static final ClassName TEST_APP = ClassName.get("com.example", "TestApp");

    @Test
    void aotContributionIsNotNecessaryWithDefaultConfiguration() {
      assertThat(getContribution(new StandardEnvironment())).isNull();
    }

    @Test
    void aotContributionIsNotNecessaryWithDefaultProfileActive() {
      StandardEnvironment environment = new StandardEnvironment();
      environment.setDefaultProfiles("fallback");
      environment.setActiveProfiles("fallback");
      assertThat(getContribution(environment)).isNull();
    }

    @Test
    void aotContributionRegistersActiveProfiles() {
      ConfigurableEnvironment environment = new StandardEnvironment();
      environment.setActiveProfiles("one", "two");
      compile(createContext(environment), (compiled) -> {
        EnvironmentPostProcessor environmentPostProcessor = compiled.getInstance(EnvironmentPostProcessor.class,
                ClassName.get("com.example", "TestApp__EnvironmentPostProcessor").toString());
        StandardEnvironment freshEnvironment = new StandardEnvironment();
        environmentPostProcessor.postProcessEnvironment(freshEnvironment, new Application());
        assertThat(freshEnvironment.getActiveProfiles()).containsExactly("one", "two");
      });
    }

    @Test
    void shouldUseAotEnvironmentPostProcessor() {
      Application application = new Application(ExampleAotProcessedApp.class);
      application.setApplicationType(ApplicationType.NORMAL);
      application.setMainApplicationClass(ExampleAotProcessedApp.class);
      System.setProperty(AotDetector.AOT_ENABLED, "true");
      try {
        ApplicationContext context = application.run();
        assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("one", "three");
        assertThat(context.getBean("test")).isEqualTo("test");
      }
      finally {
        System.clearProperty(AotDetector.AOT_ENABLED);
      }
    }

    @Test
    void aotEnvironmentPostProcessorShouldBeAppliedFirst(@TempDir Path tempDir) {
      Properties properties = new Properties();
      properties.put(EnvironmentPostProcessor.class.getName(), TestEnvironmentPostProcessor.class.getName());
      ClassLoader classLoader = createClassLoaderWithAdditionalFactories(tempDir, properties);
      DefaultResourceLoader resourceLoader = new DefaultResourceLoader(classLoader);

      Application application = new Application(ExampleAotProcessedApp.class);
      application.setResourceLoader(resourceLoader);
      application.setApplicationType(ApplicationType.NORMAL);
      application.setMainApplicationClass(ExampleAotProcessedApp.class);
      System.setProperty(AotDetector.AOT_ENABLED, "true");
      try {
        ApplicationContext context = application.run();
        // See TestEnvironmentPostProcessor
        assertThat(context.getEnvironment().getProperty("test.activeProfiles")).isEqualTo("one,three");
        assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("one", "three");
        assertThat(context.getBean("test")).isEqualTo("test");
      }
      finally {
        System.clearProperty(AotDetector.AOT_ENABLED);
      }
    }

    @Test
    void shouldBeLenientIfAotEnvironmentPostProcessorDoesNotExist() {
      Application application = new Application(ExampleAotProcessedNoProfileApp.class);
      application.setApplicationType(ApplicationType.NORMAL);
      application.setMainApplicationClass(ExampleAotProcessedNoProfileApp.class);
      System.setProperty(AotDetector.AOT_ENABLED, "true");
      try {
        ApplicationContext context = application.run();
        assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
        assertThat(context.getBean("test")).isEqualTo("test");
      }
      finally {
        System.clearProperty(AotDetector.AOT_ENABLED);
      }
    }

    @Nullable
    private BeanFactoryInitializationAotContribution getContribution(ConfigurableEnvironment environment) {
      StandardBeanFactory beanFactory = new StandardBeanFactory();
      beanFactory.registerSingleton(Environment.ENVIRONMENT_BEAN_NAME, environment);
      return new EnvironmentBeanFactoryInitializationAotProcessor().processAheadOfTime(beanFactory);
    }

    private GenericApplicationContext createContext(ConfigurableEnvironment environment) {
      GenericApplicationContext context = new GenericApplicationContext();
      context.setEnvironment(environment);
      return context;
    }

    private void compile(GenericApplicationContext context, Consumer<Compiled> compiled) {
      TestGenerationContext generationContext = new TestGenerationContext(TEST_APP);
      new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext);
      generationContext.writeGeneratedContent();
      TestCompiler.forSystem().with(generationContext).compile(compiled);
    }

    private ClassLoader createClassLoaderWithAdditionalFactories(Path tempDir, Properties properties) {
      return new ClassLoader() {
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
          Enumeration<URL> resources = super.getResources(name);
          if (TodayStrategies.STRATEGIES_LOCATION.equals(name)) {
            Path strategies = tempDir.resolve("today.strategies");
            try (BufferedWriter writer = Files.newBufferedWriter(strategies)) {
              properties.store(writer, "");
            }
            List<URL> allResources = new ArrayList<>();
            allResources.add(strategies.toUri().toURL());
            allResources.addAll(Collections.list(resources));
            return Collections.enumeration(allResources);
          }
          return resources;
        }
      };
    }

    static class TestEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

      @Override
      public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
        MockPropertySource propertySource = new MockPropertySource().withProperty("test.activeProfiles",
                StringUtils.arrayToCommaDelimitedString(environment.getActiveProfiles()));
        environment.getPropertySources().addLast(propertySource);
      }

      @Override
      public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
      }

    }

    static class ExampleAotProcessedApp {

    }

    static class ExampleAotProcessedApp__ApplicationContextInitializer
            implements ApplicationContextInitializer {

      @Override
      public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.getBeanFactory().registerSingleton("test", "test");
      }

    }

    static class ExampleAotProcessedApp__EnvironmentPostProcessor implements EnvironmentPostProcessor {

      @Override
      public void postProcessEnvironment(ConfigurableEnvironment environment, Application application) {
        environment.addActiveProfile("one");
        environment.addActiveProfile("three");
      }

    }

    static class ExampleAotProcessedNoProfileApp {

    }

    static class ExampleAotProcessedNoProfileApp__ApplicationContextInitializer
            implements ApplicationContextInitializer {

      @Override
      public void initialize(ConfigurableApplicationContext applicationContext) {
        applicationContext.getBeanFactory().registerSingleton("test", "test");
      }

    }

  }

}
