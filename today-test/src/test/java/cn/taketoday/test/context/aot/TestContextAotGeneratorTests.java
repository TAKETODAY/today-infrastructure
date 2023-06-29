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

package cn.taketoday.test.context.aot;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.aot.generate.DefaultGenerationContext;
import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.aot.generate.InMemoryGeneratedFiles;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.test.generate.CompilerFiles;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.test.tools.CompileWithForkedClassLoader;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.test.context.BootstrapUtils;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.TestContextBootstrapper;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterSharedConfigTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraVintageTests;
import cn.taketoday.test.context.aot.samples.common.MessageService;
import cn.taketoday.test.context.aot.samples.jdbc.SqlScriptsInfraJupiterTests;
import cn.taketoday.test.context.aot.samples.web.WebInfraJupiterTests;
import cn.taketoday.test.context.aot.samples.web.WebInfraVintageTests;
import cn.taketoday.test.context.aot.samples.xml.XmlInfraJupiterTests;
import cn.taketoday.test.context.aot.samples.xml.XmlInfraVintageTests;
import cn.taketoday.test.web.servlet.MockMvc;
import cn.taketoday.util.function.ThrowingConsumer;
import cn.taketoday.web.servlet.WebApplicationContext;

import static cn.taketoday.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static cn.taketoday.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static cn.taketoday.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static cn.taketoday.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;
import static cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates.resource;
import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.content;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.status;
import static cn.taketoday.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link TestContextAotGenerator}, {@link AotTestContextInitializers},
 * {@link AotTestAttributes}, {@link AotContextLoader}, and run-time hints.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@CompileWithForkedClassLoader
class TestContextAotGeneratorTests extends AbstractAotTests {

  /**
   * End-to-end tests within the scope of the {@link TestContextAotGenerator}.
   *
   * @see AotIntegrationTests
   */
  @Test
  void endToEndTests() {
    Set<Class<?>> testClasses = Set.of(
            BasicInfraJupiterSharedConfigTests.class,
            BasicInfraJupiterTests.class,
            BasicInfraJupiterTests.NestedTests.class,
            BasicInfraVintageTests.class,
            SqlScriptsInfraJupiterTests.class,
            XmlInfraJupiterTests.class,
            WebInfraJupiterTests.class);

    InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
    TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);

    generator.processAheadOfTime(testClasses.stream().sorted(comparing(Class::getName)));

    assertRuntimeHints(generator.getRuntimeHints());

    List<String> sourceFiles = generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream().toList();
    assertThat(sourceFiles).containsExactlyInAnyOrder(expectedSourceFiles);

    TestCompiler.forSystem().with(CompilerFiles.from(generatedFiles)).compile(ThrowingConsumer.of(compiled -> {
      try {
        System.setProperty(AotDetector.AOT_ENABLED, "true");
        AotTestAttributesFactory.reset();
        AotTestContextInitializersFactory.reset();

        AotTestAttributes aotAttributes = AotTestAttributes.getInstance();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> aotAttributes.setAttribute("foo", "bar"))
                .withMessage("AOT attributes cannot be modified during AOT run-time execution");
        String key = "@InfraConfiguration-" + BasicInfraVintageTests.class.getName();
        assertThat(aotAttributes.getString(key)).isEqualTo("org.example.Main");
        assertThat(aotAttributes.getBoolean(key + "-active1")).isTrue();
        assertThat(aotAttributes.getBoolean(key + "-active2")).isTrue();
        assertThat(aotAttributes.getString("bogus")).isNull();
        assertThat(aotAttributes.getBoolean("bogus")).isFalse();

        AotTestContextInitializers aotContextInitializers = new AotTestContextInitializers();
        for (Class<?> testClass : testClasses) {
          MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
          ApplicationContextInitializer contextInitializer =
                  aotContextInitializers.getContextInitializer(testClass);
          assertThat(contextInitializer).isNotNull();
          ApplicationContext context = ((AotContextLoader) mergedConfig.getContextLoader())
                  .loadContextForAotRuntime(mergedConfig, contextInitializer);
          if (context instanceof WebApplicationContext wac) {
            assertContextForWebTests(wac);
          }
          else if (testClass.getPackageName().contains("jdbc")) {
            assertContextForJdbcTests(context);
          }
          else {
            assertContextForBasicTests(context);
          }
        }
      }
      finally {
        System.clearProperty(AotDetector.AOT_ENABLED);
        AotTestAttributesFactory.reset();
      }
    }));
  }

  private static void assertRuntimeHints(RuntimeHints runtimeHints) {
    assertReflectionRegistered(runtimeHints, AotTestContextInitializersCodeGenerator.GENERATED_MAPPINGS_CLASS_NAME, INVOKE_PUBLIC_METHODS);
    assertReflectionRegistered(runtimeHints, AotTestAttributesCodeGenerator.GENERATED_ATTRIBUTES_CLASS_NAME, INVOKE_PUBLIC_METHODS);

    Stream.of(
            cn.taketoday.test.context.cache.DefaultCacheAwareContextLoaderDelegate.class,
            cn.taketoday.test.context.support.DefaultBootstrapContext.class
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_PUBLIC_CONSTRUCTORS));

    Stream.of(
            cn.taketoday.test.context.support.DefaultTestContextBootstrapper.class,
            cn.taketoday.test.context.support.DelegatingSmartContextLoader.class,
            cn.taketoday.test.context.web.WebDelegatingSmartContextLoader.class,
            cn.taketoday.test.context.web.WebTestContextBootstrapper.class
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

    Stream.of(
            cn.taketoday.test.context.web.WebAppConfiguration.class
    ).forEach(type -> assertAnnotationRegistered(runtimeHints, type));

    // TestExecutionListener
    Stream.of(
            // @TestExecutionListeners
            cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterTests.DummyTestExecutionListener.class,
            cn.taketoday.test.context.event.ApplicationEventsTestExecutionListener.class,
            cn.taketoday.test.context.event.EventPublishingTestExecutionListener.class,
            cn.taketoday.test.context.jdbc.SqlScriptsTestExecutionListener.class,
            cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener.class,
            cn.taketoday.test.context.support.DirtiesContextBeforeModesTestExecutionListener.class,
            cn.taketoday.test.context.support.DirtiesContextTestExecutionListener.class,
            cn.taketoday.test.context.transaction.TransactionalTestExecutionListener.class,
            cn.taketoday.test.context.web.ServletTestExecutionListener.class
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

    // ContextCustomizerFactory
    Stream.of(
            "cn.taketoday.test.context.support.DynamicPropertiesContextCustomizerFactory",
            "cn.taketoday.test.context.web.socket.MockServerContainerContextCustomizerFactory",
            "cn.taketoday.test.context.aot.samples.basic.ImportsContextCustomizerFactory"
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

    Stream.of(
            // @BootstrapWith
            BasicInfraVintageTests.CustomXmlBootstrapper.class,
            // @ContextConfiguration(loader = ...)
            cn.taketoday.test.context.support.AnnotationConfigContextLoader.class,
            // @ActiveProfiles(resolver = ...)
            cn.taketoday.test.context.aot.samples.basic.SpanishActiveProfilesResolver.class
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

    // @ContextConfiguration(locations = ...)
    assertThat(resource().forResource("cn/taketoday/test/context/aot/samples/xml/test-config.xml"))
            .accepts(runtimeHints);

    // @TestPropertySource(locations = ...)
    assertThat(resource().forResource("cn/taketoday/test/context/aot/samples/basic/BasicInfraVintageTests.properties"))
            .accepts(runtimeHints);

    // @WebAppConfiguration(value = ...)
    assertThat(resource().forResource("META-INF/web-resources/resources/Infra.js")).accepts(runtimeHints);
    assertThat(resource().forResource("META-INF/web-resources/WEB-INF/views/home.jsp")).accepts(runtimeHints);

    // @Sql(scripts = ...)
    assertThat(resource().forResource("cn/taketoday/test/context/jdbc/schema.sql"))
            .accepts(runtimeHints);
    assertThat(resource().forResource("cn/taketoday/test/context/aot/samples/jdbc/SqlScriptsInfraJupiterTests.test.sql"))
            .accepts(runtimeHints);
  }

  private static void assertReflectionRegistered(RuntimeHints runtimeHints, String type, MemberCategory memberCategory) {
    assertThat(reflection().onType(TypeReference.of(type)).withMemberCategory(memberCategory))
            .as("Reflection hint for %s with category %s", type, memberCategory)
            .accepts(runtimeHints);
  }

  private static void assertReflectionRegistered(RuntimeHints runtimeHints, Class<?> type, MemberCategory memberCategory) {
    assertThat(reflection().onType(type).withMemberCategory(memberCategory))
            .as("Reflection hint for %s with category %s", type.getSimpleName(), memberCategory)
            .accepts(runtimeHints);
  }

  private static void assertAnnotationRegistered(RuntimeHints runtimeHints, Class<? extends Annotation> annotationType) {
    assertReflectionRegistered(runtimeHints, annotationType, INVOKE_DECLARED_METHODS);
  }

  @Test
  void processAheadOfTimeWithBasicTests() {
    // We cannot parameterize with the test classes, since @CompileWithTargetClassAccess
    // cannot support @ParameterizedTest methods.
    Set<Class<?>> testClasses = Set.of(
            BasicInfraJupiterSharedConfigTests.class,
            BasicInfraJupiterTests.class,
            BasicInfraJupiterTests.NestedTests.class,
            BasicInfraVintageTests.class);

    processAheadOfTime(testClasses, this::assertContextForBasicTests);
  }

  private void assertContextForBasicTests(ApplicationContext context) {
    assertThat(context.getEnvironment().getProperty("test.engine")).as("Environment").isNotNull();

    MessageService messageService = context.getBean(MessageService.class);
    ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
    String expectedMessage = cac.getEnvironment().matchesProfiles("spanish") ?
                             "¡Hola, AOT!" : "Hello, AOT!";
    assertThat(messageService.generateMessage()).isEqualTo(expectedMessage);
  }

  private void assertContextForJdbcTests(ApplicationContext context) throws Exception {
    assertThat(context.getEnvironment().getProperty("test.engine")).as("Environment").isNotNull();
    assertThat(context.getBean(DataSource.class)).as("DataSource").isNotNull();
  }

  private void assertContextForWebTests(WebApplicationContext wac) throws Exception {
    assertThat(wac.getEnvironment().getProperty("test.engine")).as("Environment").isNotNull();

    MockMvc mockMvc = webAppContextSetup(wac).build();
    mockMvc.perform(get("/hello")).andExpectAll(status().isOk(), content().string("Hello, AOT!"));
  }

  @Test
  void processAheadOfTimeWithXmlTests() {
    // We cannot parameterize with the test classes, since @CompileWithTargetClassAccess
    // cannot support @ParameterizedTest methods.
    Set<Class<?>> testClasses = Set.of(
            XmlInfraJupiterTests.class,
            XmlInfraVintageTests.class);

    processAheadOfTime(testClasses, context -> {
      assertThat(context.getEnvironment().getProperty("test.engine"))
              .as("Environment").isNotNull();

      MessageService messageService = context.getBean(MessageService.class);
      assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
    });
  }

  @Test
  void processAheadOfTimeWithWebTests() {
    // We cannot parameterize with the test classes, since @CompileWithTargetClassAccess
    // cannot support @ParameterizedTest methods.
    Set<Class<?>> testClasses = Set.of(
            WebInfraJupiterTests.class,
            WebInfraVintageTests.class);

    processAheadOfTime(testClasses, context -> {
      assertThat(context.getEnvironment().getProperty("test.engine"))
              .as("Environment").isNotNull();

      MockMvc mockMvc = webAppContextSetup((WebApplicationContext) context).build();
      mockMvc.perform(get("/hello"))
              .andExpectAll(status().isOk(), content().string("Hello, AOT!"));
    });
  }

  private void processAheadOfTime(Set<Class<?>> testClasses, ThrowingConsumer<ApplicationContext> result) {
    InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
    TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
    List<Mapping> mappings = processAheadOfTime(generator, testClasses);
    TestCompiler.forSystem().with(CompilerFiles.from(generatedFiles)).compile(ThrowingConsumer.of(compiled -> {
      for (Mapping mapping : mappings) {
        MergedContextConfiguration mergedConfig = mapping.mergedConfig();
        ApplicationContextInitializer contextInitializer =
                compiled.getInstance(ApplicationContextInitializer.class, mapping.className().reflectionName());
        ApplicationContext context = ((AotContextLoader) mergedConfig.getContextLoader())
                .loadContextForAotRuntime(mergedConfig, contextInitializer);
        result.accept(context);
      }
    }));
  }

  private List<Mapping> processAheadOfTime(TestContextAotGenerator generator, Set<Class<?>> testClasses) {
    List<Mapping> mappings = new ArrayList<>();
    testClasses.forEach(testClass -> {
      DefaultGenerationContext generationContext = generator.createGenerationContext(testClass);
      MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
      ClassName className = generator.processAheadOfTime(mergedConfig, generationContext);
      assertThat(className).isNotNull();
      mappings.add(new Mapping(mergedConfig, className));
      generationContext.writeGeneratedContent();
    });
    return mappings;
  }

  private static MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass) {
    TestContextBootstrapper testContextBootstrapper = BootstrapUtils.resolveTestContextBootstrapper(testClass);
    return testContextBootstrapper.buildMergedContextConfiguration();
  }

  record Mapping(MergedContextConfiguration mergedConfig, ClassName className) {
  }

  private static final String[] expectedSourceFiles = {
          // Global
          "cn/taketoday/test/context/aot/AotTestContextInitializers__Generated.java",
          "cn/taketoday/test/context/aot/AotTestAttributes__Generated.java",
          // BasicInfraJupiterSharedConfigTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext001_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext001_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext001_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext001_BeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicTestConfiguration__TestContext001_BeanDefinitions.java",
          // BasicInfraJupiterTests -- not generated b/c already generated for BasicInfraJupiterSharedConfigTests.
          // BasicInfraJupiterTests.NestedTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext002_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext002_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext002_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext002_BeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicTestConfiguration__TestContext002_BeanDefinitions.java",
          // BasicInfraVintageTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext003_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext003_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraVintageTests__TestContext003_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicInfraVintageTests__TestContext003_BeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/basic/BasicTestConfiguration__TestContext003_BeanDefinitions.java",
          // SqlScriptsInfraJupiterTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext004_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext004_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/jdbc/SqlScriptsInfraJupiterTests__TestContext004_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/jdbc/SqlScriptsInfraJupiterTests__TestContext004_BeanFactoryRegistrations.java",
          "cn/taketoday/test/context/jdbc/EmptyDatabaseConfig__TestContext004_BeanDefinitions.java",
          // WebInfraJupiterTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext005_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext005_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/web/WebInfraJupiterTests__TestContext005_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/web/WebInfraJupiterTests__TestContext005_BeanFactoryRegistrations.java",
          "cn/taketoday/test/context/aot/samples/web/WebTestConfiguration__TestContext005_BeanDefinitions.java",
          "cn/taketoday/web/config/DelegatingWebMvcConfiguration__TestContext005_BeanDefinitions.java",
          "cn/taketoday/web/config/WebMvcConfigurationSupport__TestContext005_BeanDefinitions.java",
          // XmlInfraJupiterTests
          "cn/taketoday/context/event/DefaultEventListenerFactory__TestContext006_BeanDefinitions.java",
          "cn/taketoday/context/event/EventListenerMethodProcessor__TestContext006_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/common/DefaultMessageService__TestContext006_BeanDefinitions.java",
          "cn/taketoday/test/context/aot/samples/xml/XmlInfraJupiterTests__TestContext006_ApplicationContextInitializer.java",
          "cn/taketoday/test/context/aot/samples/xml/XmlInfraJupiterTests__TestContext006_BeanFactoryRegistrations.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext001_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext002_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext003_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext004_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext005_BeanDefinitions.java",
          "cn/taketoday/framework/test/mock/mockito/MockitoPostProcessor__TestContext006_BeanDefinitions.java"
  };

}
