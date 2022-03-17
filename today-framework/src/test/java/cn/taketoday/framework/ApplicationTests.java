/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.support.MockEnvironment;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.CommandLinePropertySource;
import cn.taketoday.core.env.CompositePropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.framework.WebApplication;
import cn.taketoday.web.framework.reactive.EnableNettyHandling;
import jakarta.annotation.PostConstruct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/21 14:47
 */
@ExtendWith(OutputCaptureExtension.class)
class ApplicationTests {

  private String headlessProperty;

  private ConfigurableApplicationContext context;

  private Environment getEnvironment() {
    if (this.context != null) {
      return this.context.getEnvironment();
    }
    throw new IllegalStateException("Could not obtain Environment");
  }

  @BeforeEach
  void storeAndClearHeadlessProperty() {
    this.headlessProperty = System.getProperty("java.awt.headless");
    System.clearProperty("java.awt.headless");
  }

  @AfterEach
  void reinstateHeadlessProperty() {
    if (this.headlessProperty == null) {
      System.clearProperty("java.awt.headless");
    }
    else {
      System.setProperty("java.awt.headless", this.headlessProperty);
    }
  }

  @AfterEach
  void cleanUp() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void sourcesMustBeAccessible() {
    assertThatThrownBy(() -> new Application(InaccessibleConfiguration.class).run())
            .isInstanceOf(ApplicationContextException.class)
            .hasMessageContaining("No visible constructors");
  }

  @Test
  void logsNoActiveProfiles(CapturedOutput output) {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
    assertThat(output).contains("No active profile set, falling back to default profiles: default");
  }

  @Test
  void logsActiveProfiles(CapturedOutput output) {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run("--context.profiles.active=myprofiles");
    assertThat(output).contains("The following profiles are active: myprofile");
  }

  @Test
  void customId() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run("--application.name=foo");
    assertThat(this.context.getApplicationName()).startsWith("foo");
  }

  @Test
  void specificApplicationContextFactory() {
    Application application = new Application(ExampleConfig.class);
    application
            .setApplicationContextFactory(ApplicationContextFactory.fromClass(StaticApplicationContext.class));
    this.context = application.run();
    assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
  }

  @Test
  void specificApplicationContextInitializer() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    final AtomicReference<ApplicationContext> reference = new AtomicReference<>();
    application.setInitializers(Collections.singletonList(reference::set));
    this.context = application.run("--foo=bar");
    assertThat(this.context).isSameAs(reference.get());
    // Custom initializers do not switch off the defaults
    assertThat(getEnvironment().getProperty("foo")).isEqualTo("bar");
  }

  @Test
  void defaultApplicationContext() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
    assertThat(this.context).isInstanceOf(StandardApplicationContext.class);
  }

  @Test
  void defaultApplicationContextForWeb() {
    Application application = new WebApplication(ExampleWebConfig.class);
    application.setApplicationType(ApplicationType.SERVLET_WEB);
    this.context = application.run();
    assertThat(this.context).isInstanceOf(AnnotationConfigServletWebServerApplicationContext.class);
  }

//  @Test
//  @DisabledIfEnvironmentVariable(named = "CI", matches = "true", disabledReason = "CI")
//  @DisabledIfSystemProperty(
//          named = "coverage", matches = "true", disabledReason = "don't know")
//  void defaultApplicationContextForReactiveWeb() {
//    Application application = new Application(ExampleReactiveWebConfig.class);
//    application.setApplicationType(ApplicationType.REACTIVE_WEB);
//    this.context = application.run();
//    assertThat(this.context).isInstanceOf(StandardWebServerApplicationContext.class);
//  }

  @Test
  void commandLinePropertySource() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    ConfigurableEnvironment environment = new StandardEnvironment();
    application.setEnvironment(environment);
    this.context = application.run("--foo=bar");
    assertThat(environment).has(matchingPropertySource(CommandLinePropertySource.class, "commandLineArgs"));
  }

  @Test
  void commandLinePropertySourceEnhancesEnvironment() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    ConfigurableEnvironment environment = new StandardEnvironment();
    environment.getPropertySources()
            .addFirst(new MapPropertySource("commandLineArgs", Collections.singletonMap("foo", "original")));
    application.setEnvironment(environment);
    this.context = application.run("--foo=bar", "--bar=foo");
    assertThat(environment).has(matchingPropertySource(CompositePropertySource.class, "commandLineArgs"));
    assertThat(environment.getProperty("bar")).isEqualTo("foo");
    // New command line properties take precedence
    assertThat(environment.getProperty("foo")).isEqualTo("bar");
    CompositePropertySource composite = (CompositePropertySource) environment.getPropertySources()
            .get("commandLineArgs");
    assertThat(composite.getPropertySources()).hasSize(2);
    assertThat(composite.getPropertySources()).first().matches(
            (source) -> source.getName().equals("applicationCommandLineArgs"),
            "is named applicationCommandLineArgs");
    assertThat(composite.getPropertySources()).element(1)
            .matches((source) -> source.getName().equals("commandLineArgs"), "is named commandLineArgs");
  }

  @Test
  void propertiesFileEnhancesEnvironment() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    ConfigurableEnvironment environment = new StandardEnvironment();
    application.setEnvironment(environment);
    this.context = application.run();
    assertThat(environment.getProperty("foo")).isEqualTo("bucket");
  }

  @Test
  void emptyCommandLinePropertySourceNotAdded() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    ConfigurableEnvironment environment = new StandardEnvironment();
    application.setEnvironment(environment);
    this.context = application.run();
    assertThat(environment.getProperty("foo")).isEqualTo("bucket");
  }

  @Test
  void disableCommandLinePropertySource() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    application.setAddCommandLineProperties(false);
    ConfigurableEnvironment environment = new StandardEnvironment();
    application.setEnvironment(environment);
    this.context = application.run("--foo=bar");
    assertThat(environment).doesNotHave(matchingPropertySource(PropertySource.class, "commandLineArgs"));
  }

  @Test
  void contextUsesApplicationConversionService() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
    assertThat(this.context.getBeanFactory().getConversionService())
            .isInstanceOf(ApplicationConversionService.class);
    assertThat(this.context.getEnvironment().getConversionService())
            .isInstanceOf(ApplicationConversionService.class);
  }

  @Test
  void contextWhenHasAddConversionServiceFalseUsesRegularConversionService() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    application.setAddConversionService(false);
    this.context = application.run();
    assertThat(this.context.getBeanFactory().getConversionService()).isNull();
    assertThat(this.context.getEnvironment().getConversionService())
            .isNotInstanceOf(ApplicationConversionService.class);
  }

  @Test
  void runCommandLineRunnersAndApplicationRunners() {
    Application application = new Application(CommandLineRunConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run("arg");
    assertThat(this.context).has(runTestRunnerBean("runnerA"));
    assertThat(this.context).has(runTestRunnerBean("runnerB"));
    assertThat(this.context).has(runTestRunnerBean("runnerC"));
  }

  @Test
  void runnersAreCalledAfterStartedIsLoggedAndBeforeApplicationReadyEventIsPublished(CapturedOutput output)
          throws Exception {
    Application application = new Application(ExampleConfig.class);
    ApplicationRunner applicationRunner = mock(ApplicationRunner.class);
    CommandLineRunner commandLineRunner = mock(CommandLineRunner.class);
    application.addInitializers((context) -> {
      ConfigurableBeanFactory beanFactory = context.getBeanFactory();
      beanFactory.registerSingleton("commandLineRunner", (CommandLineRunner) (args) -> {
        assertThat(output).contains("Started");
        commandLineRunner.run(args);
      });
      beanFactory.registerSingleton("applicationRunner", (ApplicationRunner) (args) -> {
        assertThat(output).contains("Started");
        applicationRunner.run(args);
      });
    });
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
  }

  @Test
  void run() {
    this.context = Application.run(ExampleWebConfig.class);
    assertThat(this.context).isNotNull();
  }

  @Test
  void exit() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
    assertThat(this.context).isNotNull();
    assertThat(Application.exit(this.context)).isEqualTo(0);
  }

  @Test
  void exitWithExplicitCode() {
    Application application = new Application(ExampleConfig.class);
    ExitCodeListener listener = new ExitCodeListener();
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
    context.addApplicationListener(listener);
    assertThat(this.context).isNotNull();
    assertThat(Application.exit(this.context, () -> 2)).isEqualTo(2);
    assertThat(listener.getExitCode()).isEqualTo(2);
  }

  @Test
  void headless() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
    assertThat(System.getProperty("java.awt.headless")).isEqualTo("true");
  }

  @Test
  void headlessFalse() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    application.setHeadless(false);
    this.context = application.run();
    assertThat(System.getProperty("java.awt.headless")).isEqualTo("false");
  }

  @Test
  void headlessSystemPropertyTakesPrecedence() {
    System.setProperty("java.awt.headless", "false");
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
    assertThat(System.getProperty("java.awt.headless")).isEqualTo("false");
  }

  @Test
  void getApplicationArgumentsBean() {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run("--debug", "today", "framework");
    ApplicationArguments args = this.context.getBean(ApplicationArguments.class);
    assertThat(args.getNonOptionArgs()).containsExactly("today", "framework");
    assertThat(args.containsOption("debug")).isTrue();
  }

  @Test
  @Disabled
  void failureResultsInSingleStackTrace(CapturedOutput output) throws Exception {
    ThreadGroup group = new ThreadGroup("main");
    Thread thread = new Thread(group, "main") {

      @Override
      public void run() {
        Application application = new Application(FailingConfig.class);
        application.setApplicationType(ApplicationType.NONE_WEB);
        application.run();
      }

    };
    thread.start();
    thread.join(6000);
    assertThat(output).containsOnlyOnce("Caused by: java.lang.RuntimeException: ExpectedError");
  }

  @Test
  void lazyInitializationIsDisabledByDefault() {
    assertThat(new Application(LazyInitializationConfig.class).run("--today.main.application-type=NONE_WEB")
            .getBean(AtomicInteger.class)).hasValue(1);
  }

  @Test
  void lazyInitializationIgnoresBeansThatAreExplicitlyNotLazy() {
    assertThat(new Application(NotLazyInitializationConfig.class)
            .run("--today.main.application-type=NONE_WEB", "--today.main.lazy-initialization=true")
            .getBean(AtomicInteger.class)).hasValue(1);
  }

  @Test
  void logsActiveProfilesWithoutProfileAndSingleDefault(CapturedOutput output) {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run();
    assertThat(output).contains("No active profile set, falling back to 1 default profile: \"default\"");
  }

  @Test
  void logsActiveProfilesWithoutProfileAndMultipleDefaults(CapturedOutput output) {
    MockEnvironment environment = new MockEnvironment();
    environment.setDefaultProfiles("p0,p1", "default");
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    application.setEnvironment(environment);
    this.context = application.run();
    assertThat(output)
            .contains("No active profile set, falling back to 2 default profiles: \"p0,p1\", \"default\"");
  }

  @Test
  void logsActiveProfilesWithSingleProfile(CapturedOutput output) {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    this.context = application.run("--context.profiles.active=myprofiles");
    assertThat(output).contains("The following 1 profile is active: \"myprofiles\"");
  }

  @Test
  void logsActiveProfilesWithMultipleProfiles(CapturedOutput output) {
    Application application = new Application(ExampleConfig.class);
    application.setApplicationType(ApplicationType.NONE_WEB);
    application.setAdditionalProfiles("p1,p2", "p3");
    application.run();
    assertThat(output).contains("The following 2 profiles are active: \"p1,p2\", \"p3\"");
  }

  private Condition<ConfigurableEnvironment> matchingPropertySource(
          final Class<?> propertySourceClass, final String name) {

    return new Condition<>("has property source") {

      @Override
      public boolean matches(ConfigurableEnvironment value) {
        for (PropertySource<?> source : value.getPropertySources()) {
          if (propertySourceClass.isInstance(source) && (name == null || name.equals(source.getName()))) {
            return true;
          }
        }
        return false;
      }

    };
  }

  private Condition<ConfigurableApplicationContext> runTestRunnerBean(final String name) {
    return new Condition<>("run testrunner bean") {

      @Override
      public boolean matches(ConfigurableApplicationContext value) {
        return value.getBean(name, AbstractTestRunner.class).hasRun();
      }

    };
  }

  @Configuration
  static class InaccessibleConfiguration {

    private InaccessibleConfiguration() {
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ExampleConfig {

    @Bean
    String someBean() {
      return "test";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class BrokenPostConstructConfig {

    @Bean
    Thing thing() {
      return new Thing();
    }

    static class Thing {

      @PostConstruct
      void boom() {
        throw new IllegalStateException();
      }

    }

  }

  @EnableWebMvc
  @Configuration(proxyBeanMethods = false)
  static class ExampleWebConfig {

  }

  @EnableWebMvc
  @EnableNettyHandling
  @Configuration(proxyBeanMethods = false)
  static class ExampleReactiveWebConfig {

  }

  @Configuration(proxyBeanMethods = false)
  static class FailingConfig {

    @Bean
    Object fail() {
      throw new RuntimeException("ExpectedError");
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class CommandLineRunConfig {

    @Bean
    TestCommandLineRunner runnerC() {
      return new TestCommandLineRunner(Ordered.LOWEST_PRECEDENCE, "runnerB", "runnerA");
    }

    @Bean
    ApplicationRunner runnerB() {
      return new TestApplicationRunner(Ordered.LOWEST_PRECEDENCE - 1, "runnerA");
    }

    @Bean
    TestCommandLineRunner runnerA() {
      return new TestCommandLineRunner(Ordered.HIGHEST_PRECEDENCE);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class RefreshFailureConfig {

    @PostConstruct
    void fail() {
      throw new RefreshFailureException();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class LazyInitializationConfig {

    @Bean
    AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Bean
    LazyBean lazyBean(AtomicInteger counter) {
      return new LazyBean(counter);
    }

    static class LazyBean {

      LazyBean(AtomicInteger counter) {
        counter.incrementAndGet();
      }

    }

  }

  @Configuration(proxyBeanMethods = false)
  static class NotLazyInitializationConfig {

    @Bean
    AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Bean
    @Lazy(false)
    NotLazyBean NotLazyBean(AtomicInteger counter) {
      return new NotLazyBean(counter);
    }

    static class NotLazyBean {

      NotLazyBean(AtomicInteger counter) {
        counter.getAndIncrement();
      }

    }

  }

  static class RefreshFailureException extends RuntimeException {

  }

  abstract static class AbstractTestRunner implements ApplicationContextAware, Ordered {

    private final String[] expectedBefore;

    private ApplicationContext applicationContext;

    private final int order;

    private boolean run;

    AbstractTestRunner(int order, String... expectedBefore) {
      this.expectedBefore = expectedBefore;
      this.order = order;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
      return this.order;
    }

    void markAsRan() {
      this.run = true;
      for (String name : this.expectedBefore) {
        AbstractTestRunner bean = this.applicationContext.getBean(name, AbstractTestRunner.class);
        assertThat(bean.hasRun()).isTrue();
      }
    }

    boolean hasRun() {
      return this.run;
    }

  }

  static class TestCommandLineRunner extends AbstractTestRunner implements CommandLineRunner {

    TestCommandLineRunner(int order, String... expectedBefore) {
      super(order, expectedBefore);
    }

    @Override
    public void run(String... args) {
      markAsRan();
    }

  }

  static class TestApplicationRunner extends AbstractTestRunner implements ApplicationRunner {

    TestApplicationRunner(int order, String... expectedBefore) {
      super(order, expectedBefore);
    }

    @Override
    public void run(ApplicationArguments args) {
      markAsRan();
    }

  }

  static class ExitCodeListener implements ApplicationListener<ExitCodeEvent> {

    private Integer exitCode;

    @Override
    public void onApplicationEvent(ExitCodeEvent event) {
      this.exitCode = event.getExitCode();
    }

    Integer getExitCode() {
      return this.exitCode;
    }

  }

  static class Example {

  }

  @FunctionalInterface
  interface ExampleConfigurer {

    void configure(Example example);

  }
}
