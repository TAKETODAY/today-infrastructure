/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.builder;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Profiles;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.Application;
import cn.taketoday.framework.ApplicationArguments;
import cn.taketoday.framework.ApplicationContextFactory;
import cn.taketoday.framework.ApplicationShutdownHookInstance;
import cn.taketoday.framework.ApplicationType;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/19 23:24
 */
class ApplicationBuilderTests {

  private ConfigurableApplicationContext context;

  @AfterEach
  void close() {
    close(this.context);
    ApplicationShutdownHookInstance.reset();
  }

  private void close(ApplicationContext context) {
    if (context != null) {
      if (context instanceof ConfigurableApplicationContext configurableContext) {
        configurableContext.close();
      }
      close(context.getParent());
    }
  }

  @Test
  void profileAndProperties() {
    ApplicationBuilder application = new ApplicationBuilder().sources(ExampleConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(StaticApplicationContext.class))
            .profiles("foo")
            .properties("foo=bar");
    this.context = application.run();
    assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
    assertThat(this.context.getEnvironment().getProperty("foo")).isEqualTo("bucket");
    assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("foo"))).isTrue();
  }

  @Test
  void propertiesAsMap() {
    ApplicationBuilder application = new ApplicationBuilder().sources(ExampleConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(StaticApplicationContext.class))
            .properties(Collections.singletonMap("bar", "foo"));
    this.context = application.run();
    assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("foo");
  }

  @Test
  void propertiesAsProperties() {
    ApplicationBuilder application = new ApplicationBuilder().sources(ExampleConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(StaticApplicationContext.class))
            .properties(StringUtils.splitArrayElementsIntoProperties(new String[] { "bar=foo" }, "="));
    this.context = application.run();
    assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("foo");
  }

  @Test
  void propertiesWithRepeatSeparator() {
    ApplicationBuilder application = new ApplicationBuilder().sources(ExampleConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(StaticApplicationContext.class))
            .properties("one=c:\\logging.file.name", "two=a:b", "three:c:\\logging.file.name", "four:a:b");
    this.context = application.run();
    ConfigurableEnvironment environment = this.context.getEnvironment();
    assertThat(environment.getProperty("one")).isEqualTo("c:\\logging.file.name");
    assertThat(environment.getProperty("two")).isEqualTo("a:b");
    assertThat(environment.getProperty("three")).isEqualTo("c:\\logging.file.name");
    assertThat(environment.getProperty("four")).isEqualTo("a:b");
  }

  @Test
  void specificApplicationContextFactory() {
    ApplicationBuilder application = new ApplicationBuilder().sources(ExampleConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(StaticApplicationContext.class));
    this.context = application.run();
    assertThat(this.context).isInstanceOf(StaticApplicationContext.class);
  }

  @Test
  void parentContextCreationThatIsRunDirectly() {
    ApplicationBuilder application = new ApplicationBuilder(ChildConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(SpyApplicationContext.class));
    application.parent(ExampleConfig.class);
    this.context = application.run("foo.bar=baz");
    then(((SpyApplicationContext) this.context).getApplicationContext()).should()
            .setParent(any(ApplicationContext.class));
    assertThat(ApplicationShutdownHookInstance.get()).didNotRegisterApplicationContext(this.context);
    assertThat(this.context.getParent().getBean(ApplicationArguments.class).getNonOptionArgs())
            .contains("foo.bar=baz");
    assertThat(this.context.getBean(ApplicationArguments.class).getNonOptionArgs()).contains("foo.bar=baz");
  }

  @Test
  void parentContextCreationThatIsBuiltThenRun() {
    ApplicationBuilder application = new ApplicationBuilder(ChildConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(SpyApplicationContext.class));
    application.parent(ExampleConfig.class);
    this.context = application.build("a=alpha").run("b=bravo");
    then(((SpyApplicationContext) this.context).getApplicationContext()).should()
            .setParent(any(ApplicationContext.class));
    assertThat(ApplicationShutdownHookInstance.get()).didNotRegisterApplicationContext(this.context);
    assertThat(this.context.getParent().getBean(ApplicationArguments.class).getNonOptionArgs()).contains("a=alpha");
    assertThat(this.context.getBean(ApplicationArguments.class).getNonOptionArgs()).contains("b=bravo");
  }

  @Test
  void parentContextCreationWithChildShutdown() {
    ApplicationBuilder application = new ApplicationBuilder(ChildConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(SpyApplicationContext.class))
            .registerShutdownHook(true);
    application.parent(ExampleConfig.class);
    this.context = application.run();
    then(((SpyApplicationContext) this.context).getApplicationContext()).should()
            .setParent(any(ApplicationContext.class));
    assertThat(ApplicationShutdownHookInstance.get()).registeredApplicationContext(this.context);
  }

  @Test
  void contextWithClassLoader() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(SpyApplicationContext.class));
    ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
    application.resourceLoader(new DefaultResourceLoader(classLoader));
    this.context = application.run();
    assertThat(this.context.getClassLoader()).isEqualTo(classLoader);
  }

  @Test
  void parentContextWithClassLoader() {
    ApplicationBuilder application = new ApplicationBuilder(ChildConfig.class)
            .contextFactory(ApplicationContextFactory.fromClass(SpyApplicationContext.class));
    ClassLoader classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());
    application.resourceLoader(new DefaultResourceLoader(classLoader));
    application.parent(ExampleConfig.class);
    this.context = application.run();
    assertThat(((SpyApplicationContext) this.context).getResourceLoader().getClassLoader()).isEqualTo(classLoader);
  }

  @Test
  void parentFirstCreation() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class)
            .child(ChildConfig.class);
    application.contextFactory(ApplicationContextFactory.fromClass(SpyApplicationContext.class));
    this.context = application.run();
    then(((SpyApplicationContext) this.context).getApplicationContext()).should()
            .setParent(any(ApplicationContext.class));
    assertThat(ApplicationShutdownHookInstance.get()).didNotRegisterApplicationContext(this.context);
  }

  @Test
  void parentFirstCreationWithProfileAndDefaultArgs() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class).profiles("node")
            .properties("transport=redis")
            .child(ChildConfig.class)
            .type(ApplicationType.NORMAL);
    this.context = application.run();
    assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("node"))).isTrue();
    assertThat(this.context.getEnvironment().getProperty("transport")).isEqualTo("redis");
    assertThat(this.context.getParent().getEnvironment().acceptsProfiles(Profiles.of("node"))).isTrue();
    assertThat(this.context.getParent().getEnvironment().getProperty("transport")).isEqualTo("redis");
    // only defined in node profile
    assertThat(this.context.getEnvironment().getProperty("bar")).isEqualTo("spam");
  }

  @Test
  void parentFirstWithDifferentProfile() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class).profiles("node")
            .properties("transport=redis")
            .child(ChildConfig.class)
            .profiles("admin")
            .type(ApplicationType.NORMAL);
    this.context = application.run();
    assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("node", "admin"))).isTrue();
    assertThat(this.context.getParent().getEnvironment().acceptsProfiles(Profiles.of("admin"))).isFalse();
  }

  @Test
  void parentWithDifferentProfile() {
    ApplicationBuilder shared = new ApplicationBuilder(ExampleConfig.class).profiles("node")
            .properties("transport=redis");
    ApplicationBuilder application = shared.child(ChildConfig.class)
            .profiles("admin")
            .type(ApplicationType.NORMAL);
    shared.profiles("parent");
    this.context = application.run();
    assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("node", "admin"))).isTrue();
    assertThat(this.context.getParent().getEnvironment().acceptsProfiles(Profiles.of("node", "parent"))).isTrue();
    assertThat(this.context.getParent().getEnvironment().acceptsProfiles(Profiles.of("admin"))).isFalse();
  }

  @Test
  void parentFirstWithDifferentProfileAndExplicitEnvironment() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class)
            .environment(new StandardEnvironment())
            .profiles("node")
            .properties("transport=redis")
            .child(ChildConfig.class)
            .profiles("admin")
            .type(ApplicationType.NORMAL);
    this.context = application.run();
    assertThat(this.context.getEnvironment().acceptsProfiles(Profiles.of("node", "admin"))).isTrue();
    // Now they share an Environment explicitly so there's no way to keep the profiles
    // separate
    assertThat(this.context.getParent().getEnvironment().acceptsProfiles(Profiles.of("admin"))).isTrue();
  }

  @Test
  void parentContextIdentical() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class);
    application.parent(ExampleConfig.class);
    application.contextFactory(ApplicationContextFactory.fromClass(SpyApplicationContext.class));
    this.context = application.run();
    then(((SpyApplicationContext) this.context).getApplicationContext()).should()
            .setParent(any(ApplicationContext.class));
  }

  @Test
  void initializersCreatedOnce() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class)
            .type(ApplicationType.NORMAL);
    this.context = application.run();
    assertThat(application.application().getInitializers()).hasSize(3);
  }

  @Test
  void initializersCreatedOnceForChild() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class)
            .child(ChildConfig.class)
            .type(ApplicationType.NORMAL);
    this.context = application.run();
    assertThat(application.application().getInitializers()).hasSize(4);
  }

  @Test
  void initializersIncludeDefaults() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class)
            .type(ApplicationType.NORMAL)
            .initializers((ConfigurableApplicationContext applicationContext) -> {
            });
    this.context = application.run();
    assertThat(application.application().getInitializers()).hasSize(4);
  }

  @Test
  void sourcesWithBoundSources() {
    ApplicationBuilder application = new ApplicationBuilder()
            .type(ApplicationType.NORMAL)
            .sources(ExampleConfig.class)
            .properties("app.main.sources=" + ChildConfig.class.getName());
    this.context = application.run();
    this.context.getBean(ExampleConfig.class);
    this.context.getBean(ChildConfig.class);
  }

  @Test
  void addBootstrapRegistryInitializer() {
    ApplicationBuilder application = new ApplicationBuilder(ExampleConfig.class)
            .type(ApplicationType.NORMAL)
            .addBootstrapRegistryInitializer(context -> context.addCloseListener(
                    event -> event.getApplicationContext().getBeanFactory().registerSingleton("test", "infra")));
    this.context = application.run();
    assertThat(this.context.getBean("test")).isEqualTo("infra");
  }

  @Test
  void setEnvironmentPrefix() {
    ApplicationBuilder builder = new ApplicationBuilder(ExampleConfig.class).environmentPrefix("test");
    assertThat(builder.application().getEnvironmentPrefix()).isEqualTo("test");
  }

  @Test
  void customApplicationWithResourceLoader() {
    ResourceLoader resourceLoader = mock(ResourceLoader.class);
    ApplicationBuilder applicationBuilder = new ApplicationBuilder(resourceLoader,
            ExampleConfig.class) {
      @Override
      protected Application createApplication(ResourceLoader resourceLoader, Class<?>... sources) {
        return new CustomApplication(resourceLoader, sources);
      }
    };
    Application application = applicationBuilder.build();
    assertThat(application).asInstanceOf(InstanceOfAssertFactories.type(CustomApplication.class))
            .satisfies((customApp) -> assertThat(customApp.resourceLoader).isEqualTo(resourceLoader));
  }

  @Configuration(proxyBeanMethods = false)
  static class ExampleConfig {

  }

  @Configuration(proxyBeanMethods = false)
  static class ChildConfig {

  }

  static class CustomApplication extends Application {

    private final ResourceLoader resourceLoader;

    CustomApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
      super(resourceLoader, primarySources);
      this.resourceLoader = resourceLoader;
    }

  }

  static class SpyApplicationContext extends AnnotationConfigApplicationContext {

    private final ConfigurableApplicationContext applicationContext = spy(new AnnotationConfigApplicationContext());

    private ResourceLoader resourceLoader;

    @Override
    public void setParent(ApplicationContext parent) {
      this.applicationContext.setParent(parent);
    }

    ConfigurableApplicationContext getApplicationContext() {
      return this.applicationContext;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
      super.setResourceLoader(resourceLoader);
      this.resourceLoader = resourceLoader;
    }

    ResourceLoader getResourceLoader() {
      return this.resourceLoader;
    }

    @Override
    public void close() {
      super.close();
      this.applicationContext.close();
    }

    @Override
    public ApplicationContext getParent() {
      return this.applicationContext.getParent();
    }

  }

}