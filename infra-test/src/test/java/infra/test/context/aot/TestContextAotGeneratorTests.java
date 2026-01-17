/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.aot;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.aot.AotDetector;
import infra.aot.generate.DefaultGenerationContext;
import infra.aot.generate.GeneratedFiles.Kind;
import infra.aot.generate.InMemoryGeneratedFiles;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.TypeReference;
import infra.aot.test.generate.CompilerFiles;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.core.test.tools.CompileWithForkedClassLoader;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.ClassName;
import infra.test.context.BootstrapUtils;
import infra.test.context.MergedContextConfiguration;
import infra.test.context.TestContextBootstrapper;
import infra.test.context.aot.samples.basic.BasicInfraJupiterSharedConfigTests;
import infra.test.context.aot.samples.basic.BasicInfraJupiterTests;
import infra.test.context.aot.samples.basic.BasicInfraVintageTests;
import infra.test.context.aot.samples.basic.SpanishActiveProfilesResolver;
import infra.test.context.aot.samples.common.MessageService;
import infra.test.context.aot.samples.jdbc.SqlScriptsInfraJupiterTests;
import infra.test.context.aot.samples.web.WebInfraJupiterTests;
import infra.test.context.aot.samples.web.WebInfraVintageTests;
import infra.test.context.aot.samples.xml.XmlInfraJupiterTests;
import infra.test.context.aot.samples.xml.XmlInfraVintageTests;
import infra.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import infra.test.context.env.YamlPropertySourceFactory;
import infra.test.context.event.ApplicationEventsTestExecutionListener;
import infra.test.context.event.EventPublishingTestExecutionListener;
import infra.test.context.jdbc.SqlScriptsTestExecutionListener;
import infra.test.context.support.AnnotationConfigContextLoader;
import infra.test.context.support.DefaultBootstrapContext;
import infra.test.context.support.DefaultTestContextBootstrapper;
import infra.test.context.support.DelegatingSmartContextLoader;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import infra.test.context.support.DirtiesContextTestExecutionListener;
import infra.test.context.transaction.TransactionalTestExecutionListener;
import infra.test.context.web.MockTestExecutionListener;
import infra.test.context.web.WebAppConfiguration;
import infra.test.context.web.WebDelegatingSmartContextLoader;
import infra.test.context.web.WebTestContextBootstrapper;
import infra.test.web.mock.MockMvc;
import infra.util.function.ThrowingConsumer;
import infra.web.mock.WebApplicationContext;

import static infra.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static infra.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static infra.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static infra.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;
import static infra.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static infra.aot.hint.predicate.RuntimeHintsPredicates.resource;
import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
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
            "org.opentest4j.TestAbortedException",
            "org.junit.AssumptionViolatedException",
            "org.testng.SkipException"
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type));

    Stream.of(
            DefaultCacheAwareContextLoaderDelegate.class,
            DefaultBootstrapContext.class
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_PUBLIC_CONSTRUCTORS));

    Stream.of(
            DefaultTestContextBootstrapper.class,
            DelegatingSmartContextLoader.class,
            WebDelegatingSmartContextLoader.class,
            WebTestContextBootstrapper.class
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

    Stream.of(
            WebAppConfiguration.class
    ).forEach(type -> assertAnnotationRegistered(runtimeHints, type));

    // TestExecutionListener
    Stream.of(
            // @TestExecutionListeners
            BasicInfraJupiterTests.DummyTestExecutionListener.class,
            ApplicationEventsTestExecutionListener.class,
            EventPublishingTestExecutionListener.class,
            SqlScriptsTestExecutionListener.class,
            DependencyInjectionTestExecutionListener.class,
            DirtiesContextBeforeModesTestExecutionListener.class,
            DirtiesContextTestExecutionListener.class,
            TransactionalTestExecutionListener.class,
            MockTestExecutionListener.class
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

    // ContextCustomizerFactory
    Stream.of(
            "infra.test.context.support.DynamicPropertiesContextCustomizerFactory",
            "infra.test.context.web.socket.MockServerContainerContextCustomizerFactory",
            "infra.test.context.aot.samples.basic.ImportsContextCustomizerFactory"
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

    Stream.of(
            // @BootstrapWith
            BasicInfraVintageTests.CustomXmlBootstrapper.class,
            // @ContextConfiguration(loader = ...)
            AnnotationConfigContextLoader.class,
            // @ActiveProfiles(resolver = ...)
            SpanishActiveProfilesResolver.class
    ).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

    // @ContextConfiguration(locations = ...)
    assertThat(resource().forResource("infra/test/context/aot/samples/xml/test-config.xml"))
            .accepts(runtimeHints);

    // @TestPropertySource(locations = ...)
    assertThat(resource().forResource("infra/test/context/aot/samples/basic/BasicInfraVintageTests.properties"))
            .as("@TestPropertySource(locations)")
            .accepts(runtimeHints);

    // @YamlTestProperties(...)
    assertThat(resource().forResource("infra/test/context/aot/samples/basic/test1.yaml"))
            .as("@YamlTestProperties: test1.yaml")
            .accepts(runtimeHints);
    assertThat(resource().forResource("infra/test/context/aot/samples/basic/test2.yaml"))
            .as("@YamlTestProperties: test2.yaml")
            .accepts(runtimeHints);

    // @TestPropertySource(factory = ...)
    assertReflectionRegistered(runtimeHints, YamlPropertySourceFactory.class.getName(), INVOKE_DECLARED_CONSTRUCTORS);

    // @WebAppConfiguration(value = ...)
    assertThat(resource().forResource("META-INF/web-resources/resources/Infra.js")).accepts(runtimeHints);
    assertThat(resource().forResource("META-INF/web-resources/WEB-INF/views/home.jsp")).accepts(runtimeHints);

    // @Sql(scripts = ...)
    assertThat(resource().forResource("infra/test/context/jdbc/schema.sql"))
            .accepts(runtimeHints);
    assertThat(resource().forResource("infra/test/context/aot/samples/jdbc/SqlScriptsInfraJupiterTests.test.sql"))
            .accepts(runtimeHints);
  }

  private static void assertReflectionRegistered(RuntimeHints runtimeHints, String type) {
    assertThat(reflection().onType(TypeReference.of(type)))
            .as("Reflection hint for %s", type)
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
          "infra/test/context/aot/AotTestContextInitializers__Generated.java",
          "infra/test/context/aot/AotTestAttributes__Generated.java",
          // BasicInfraJupiterSharedConfigTests
          "infra/context/event/DefaultEventListenerFactory__TestContext001_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext001_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext001_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext001_BeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext001_ManagementApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterSharedConfigTests__TestContext001_ManagementBeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicTestConfiguration__TestContext001_BeanDefinitions.java",
          "infra/test/context/aot/samples/management/ManagementConfiguration__TestContext001_BeanDefinitions.java",
          "infra/test/context/aot/samples/management/ManagementMessageService__TestContext001_ManagementBeanDefinitions.java",

          // BasicInfraJupiterTests -- not generated b/c already generated for BasicInfraJupiterSharedConfigTests.
          // BasicInfraJupiterTests.NestedTests
          "infra/context/event/DefaultEventListenerFactory__TestContext002_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext002_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext002_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext002_BeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext002_ManagementApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext002_ManagementBeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicTestConfiguration__TestContext002_BeanDefinitions.java",
          "infra/test/context/aot/samples/management/ManagementConfiguration__TestContext002_BeanDefinitions.java",
          "infra/test/context/aot/samples/management/ManagementMessageService__TestContext002_ManagementBeanDefinitions.java",

          // BasicInfraVintageTests
          "infra/context/event/DefaultEventListenerFactory__TestContext003_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext003_BeanDefinitions.java",
          "infra/test/context/aot/samples/basic/BasicInfraVintageTests__TestContext003_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/basic/BasicInfraVintageTests__TestContext003_BeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/basic/BasicTestConfiguration__TestContext003_BeanDefinitions.java",
          // SqlScriptsInfraJupiterTests
          "infra/context/event/DefaultEventListenerFactory__TestContext004_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext004_BeanDefinitions.java",
          "infra/test/context/aot/samples/jdbc/SqlScriptsInfraJupiterTests__TestContext004_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/jdbc/SqlScriptsInfraJupiterTests__TestContext004_BeanFactoryRegistrations.java",
          "infra/test/context/jdbc/EmptyDatabaseConfig__TestContext004_BeanDefinitions.java",
          // WebInfraJupiterTests
          "infra/context/event/DefaultEventListenerFactory__TestContext005_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext005_BeanDefinitions.java",
          "infra/test/context/aot/samples/web/WebInfraJupiterTests__TestContext005_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/web/WebInfraJupiterTests__TestContext005_BeanFactoryRegistrations.java",
          "infra/test/context/aot/samples/web/WebTestConfiguration__TestContext005_BeanDefinitions.java",
          "infra/web/config/annotation/DelegatingWebMvcConfiguration__TestContext005_BeanDefinitions.java",
          // XmlInfraJupiterTests
          "infra/context/event/DefaultEventListenerFactory__TestContext006_BeanDefinitions.java",
          "infra/context/event/EventListenerMethodProcessor__TestContext006_BeanDefinitions.java",
          "infra/test/context/aot/samples/common/DefaultMessageService__TestContext006_BeanDefinitions.java",
          "infra/test/context/aot/samples/xml/XmlInfraJupiterTests__TestContext006_ApplicationContextInitializer.java",
          "infra/test/context/aot/samples/xml/XmlInfraJupiterTests__TestContext006_BeanFactoryRegistrations.java",

          "infra/test/context/aot/samples/basic/BasicInfraJupiterTests_NestedTests__TestContext002_EnvironmentPostProcessor.java"
  };

}
