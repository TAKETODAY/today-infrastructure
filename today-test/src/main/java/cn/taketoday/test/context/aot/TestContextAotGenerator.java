/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.aot.generate.ClassNameGenerator;
import cn.taketoday.aot.generate.DefaultGenerationContext;
import cn.taketoday.aot.generate.GeneratedClasses;
import cn.taketoday.aot.generate.GeneratedFiles;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.annotation.ImportRuntimeHints;
import cn.taketoday.context.aot.ApplicationContextAotGenerator;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.test.context.BootstrapUtils;
import cn.taketoday.test.context.ContextLoadException;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.MergedContextConfiguration;
import cn.taketoday.test.context.SmartContextLoader;
import cn.taketoday.test.context.TestContextBootstrapper;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static cn.taketoday.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

/**
 * {@code TestContextAotGenerator} generates AOT artifacts for integration tests
 * that depend on support from the <em>Infra TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationContextAotGenerator
 * @since 4.0
 */
public class TestContextAotGenerator {

  /**
   * JVM system property used to set the {@code failOnError} flag: {@value}.
   * <p>The {@code failOnError} flag controls whether errors encountered during
   * AOT processing in the <em>Infra TestContext Framework</em> should result
   * in an exception that fails the overall process.
   * <p>Defaults to {@code true}.
   * <p>Supported values include {@code true} or {@code false}, ignoring case.
   * For example, the default may be changed to {@code false} by supplying
   * the following JVM system property via the command line.
   * <pre style="code">-Dinfra.test.aot.processing.failOnError=false</pre>
   * <p>May alternatively be configured via the
   * {@link cn.taketoday.lang.TodayStrategies TodayStrategies}
   * mechanism.
   */
  public static final String FAIL_ON_ERROR_PROPERTY_NAME = "infra.test.aot.processing.failOnError";

  private static final Log logger = LogFactory.getLog(TestContextAotGenerator.class);

  private final ApplicationContextAotGenerator aotGenerator = new ApplicationContextAotGenerator();

  private final AotServices<TestRuntimeHintsRegistrar> testRuntimeHintsRegistrars;

  private final MergedContextConfigurationRuntimeHints mergedConfigRuntimeHints =
      new MergedContextConfigurationRuntimeHints();

  private final AtomicInteger sequence = new AtomicInteger();

  private final GeneratedFiles generatedFiles;

  private final RuntimeHints runtimeHints;

  final boolean failOnError;

  /**
   * Create a new {@link TestContextAotGenerator} that uses the supplied
   * {@link GeneratedFiles}.
   *
   * @param generatedFiles the {@code GeneratedFiles} to use
   * @see #TestContextAotGenerator(GeneratedFiles, RuntimeHints)
   */
  public TestContextAotGenerator(GeneratedFiles generatedFiles) {
    this(generatedFiles, new RuntimeHints());
  }

  /**
   * Create a new {@link TestContextAotGenerator} that uses the supplied
   * {@link GeneratedFiles} and {@link RuntimeHints}.
   * <p>This constructor looks up the value of the {@code failOnError} flag via
   * the {@value #FAIL_ON_ERROR_PROPERTY_NAME} property, defaulting to
   * {@code true} if the property is not set.
   *
   * @param generatedFiles the {@code GeneratedFiles} to use
   * @param runtimeHints the {@code RuntimeHints} to use
   * @see #TestContextAotGenerator(GeneratedFiles, RuntimeHints, boolean)
   */
  public TestContextAotGenerator(GeneratedFiles generatedFiles, RuntimeHints runtimeHints) {
    this(generatedFiles, runtimeHints, getFailOnErrorFlag());
  }

  /**
   * Create a new {@link TestContextAotGenerator} that uses the supplied
   * {@link GeneratedFiles}, {@link RuntimeHints}, and {@code failOnError} flag.
   *
   * @param generatedFiles the {@code GeneratedFiles} to use
   * @param runtimeHints the {@code RuntimeHints} to use
   * @param failOnError {@code true} if errors encountered during AOT processing
   * should result in an exception that fails the overall process
   * @since 6.1
   */
  public TestContextAotGenerator(GeneratedFiles generatedFiles, RuntimeHints runtimeHints, boolean failOnError) {
    this.testRuntimeHintsRegistrars = AotServices.factories().load(TestRuntimeHintsRegistrar.class);
    this.generatedFiles = generatedFiles;
    this.runtimeHints = runtimeHints;
    this.failOnError = failOnError;
  }

  /**
   * Get the {@link RuntimeHints} gathered during {@linkplain #processAheadOfTime(Stream)
   * AOT processing}.
   */
  public final RuntimeHints getRuntimeHints() {
    return this.runtimeHints;
  }

  /**
   * Process each of the supplied Infra integration test classes and generate
   * AOT artifacts.
   *
   * @throws TestContextAotException if an error occurs during AOT processing
   */
  public void processAheadOfTime(Stream<Class<?>> testClasses) throws TestContextAotException {
    Assert.state(!AotDetector.useGeneratedArtifacts(), "Cannot perform AOT processing during AOT run-time execution");
    try {
      resetAotFactories();

      Set<Class<? extends RuntimeHintsRegistrar>> coreRuntimeHintsRegistrarClasses = new LinkedHashSet<>();
      ReflectiveRuntimeHintsRegistrar reflectiveRuntimeHintsRegistrar = new ReflectiveRuntimeHintsRegistrar();

      MultiValueMap<MergedContextConfiguration, Class<?>> mergedConfigMappings = new LinkedMultiValueMap<>();
      ClassLoader classLoader = getClass().getClassLoader();
      testClasses.forEach(testClass -> {
        MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
        mergedConfigMappings.add(mergedConfig, testClass);
        collectRuntimeHintsRegistrarClasses(testClass, coreRuntimeHintsRegistrarClasses);
        reflectiveRuntimeHintsRegistrar.registerRuntimeHints(this.runtimeHints, testClass);
        this.testRuntimeHintsRegistrars.forEach(registrar -> {
          if (logger.isTraceEnabled()) {
            logger.trace("Processing RuntimeHints contribution from class [%s]"
                .formatted(registrar.getClass().getCanonicalName()));
          }
          registrar.registerHints(this.runtimeHints, testClass, classLoader);
        });
      });

      coreRuntimeHintsRegistrarClasses.stream()
          .map(BeanUtils::newInstance)
          .forEach(registrar -> {
            if (logger.isTraceEnabled()) {
              logger.trace("Processing RuntimeHints contribution from class [%s]"
                  .formatted(registrar.getClass().getCanonicalName()));
            }
            registrar.registerHints(this.runtimeHints, classLoader);
          });

      MultiValueMap<ClassName, Class<?>> initializerClassMappings = processAheadOfTime(mergedConfigMappings);
      generateAotTestContextInitializerMappings(initializerClassMappings);
      generateAotTestAttributeMappings();
    }
    finally {
      resetAotFactories();
    }
  }

  /**
   * Collect all {@link RuntimeHintsRegistrar} classes declared via
   * {@link ImportRuntimeHints @ImportRuntimeHints} on the supplied test class
   * and add them to the supplied {@link Set}.
   *
   * @param testClass the test class on which to search for {@code @ImportRuntimeHints}
   * @param coreRuntimeHintsRegistrarClasses the set of registrar classes
   */
  private void collectRuntimeHintsRegistrarClasses(
      Class<?> testClass, Set<Class<? extends RuntimeHintsRegistrar>> coreRuntimeHintsRegistrarClasses) {

    MergedAnnotations.from(testClass, SearchStrategy.TYPE_HIERARCHY)
        .stream(ImportRuntimeHints.class)
        .filter(MergedAnnotation::isPresent)
        .map(MergedAnnotation::synthesize)
        .map(ImportRuntimeHints::value)
        .flatMap(Arrays::stream)
        .forEach(coreRuntimeHintsRegistrarClasses::add);
  }

  private void resetAotFactories() {
    AotTestAttributesFactory.reset();
    AotTestContextInitializersFactory.reset();
  }

  private MultiValueMap<ClassName, Class<?>> processAheadOfTime(
      MultiValueMap<MergedContextConfiguration, Class<?>> mergedConfigMappings) {

    ClassLoader classLoader = getClass().getClassLoader();
    MultiValueMap<ClassName, Class<?>> initializerClassMappings = new LinkedMultiValueMap<>();
    mergedConfigMappings.forEach((mergedConfig, testClasses) -> {
      if (logger.isDebugEnabled()) {
        logger.debug("Generating AOT artifacts for test classes " +
            testClasses.stream().map(Class::getName).toList());
      }
      this.mergedConfigRuntimeHints.registerHints(this.runtimeHints, mergedConfig, classLoader);
      try {
        // Use first test class discovered for a given unique MergedContextConfiguration.
        Class<?> testClass = testClasses.get(0);
        DefaultGenerationContext generationContext = createGenerationContext(testClass);
        ClassName initializer = processAheadOfTime(mergedConfig, generationContext);
        Assert.state(!initializerClassMappings.containsKey(initializer),
            () -> "ClassName [%s] already encountered".formatted(initializer.reflectionName()));
        initializerClassMappings.addAll(initializer, testClasses);
        generationContext.writeGeneratedContent();
      }
      catch (Exception ex) {
        if (this.failOnError) {
          throw new TestContextAotException("Failed to generate AOT artifacts for test classes " +
              testClasses.stream().map(Class::getName).toList(), ex);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Failed to generate AOT artifacts for test classes " +
              testClasses.stream().map(Class::getName).toList(), ex);
        }
        else if (logger.isWarnEnabled()) {
          logger.warn("""
              Failed to generate AOT artifacts for test classes %s. \
              Enable DEBUG logging to view the stack trace. %s"""
              .formatted(testClasses.stream().map(Class::getName).toList(), ex));
        }
      }
    });
    return initializerClassMappings;
  }

  /**
   * Process the specified {@link MergedContextConfiguration} ahead-of-time
   * using the specified {@link GenerationContext}.
   * <p>Return the {@link ClassName} of the {@link ApplicationContextInitializer}
   * to use to restore an optimized state of the test application context for
   * the given {@code MergedContextConfiguration}.
   *
   * @param mergedConfig the {@code MergedContextConfiguration} to process
   * @param generationContext the generation context to use
   * @return the {@link ClassName} for the generated {@code ApplicationContextInitializer}
   * @throws TestContextAotException if an error occurs during AOT processing
   */
  ClassName processAheadOfTime(MergedContextConfiguration mergedConfig,
      GenerationContext generationContext) throws TestContextAotException {

    GenericApplicationContext gac = loadContextForAotProcessing(mergedConfig);
    try {
      return this.aotGenerator.processAheadOfTime(gac, generationContext);
    }
    catch (Throwable ex) {
      throw new TestContextAotException("Failed to process test class [%s] for AOT"
          .formatted(mergedConfig.getTestClass().getName()), ex);
    }
  }

  /**
   * Load the {@code GenericApplicationContext} for the supplied merged context
   * configuration for AOT processing.
   * <p>Only supports {@link SmartContextLoader SmartContextLoaders} that
   * create {@link GenericApplicationContext GenericApplicationContexts}.
   *
   * @throws TestContextAotException if an error occurs while loading the application
   * context or if one of the prerequisites is not met
   * @see AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
   */
  private GenericApplicationContext loadContextForAotProcessing(
      MergedContextConfiguration mergedConfig) throws TestContextAotException {

    Class<?> testClass = mergedConfig.getTestClass();
    ContextLoader contextLoader = mergedConfig.getContextLoader();
    Assert.notNull(contextLoader, () -> """
        Cannot load an ApplicationContext with a NULL 'contextLoader'. \
        Consider annotating test class [%s] with @ContextConfiguration or \
        @ContextHierarchy.""".formatted(testClass.getName()));

    if (contextLoader instanceof AotContextLoader aotContextLoader) {
      try {
        ApplicationContext context = aotContextLoader.loadContextForAotProcessing(mergedConfig);
        if (context instanceof GenericApplicationContext gac) {
          return gac;
        }
      }
      catch (Exception ex) {
        Throwable cause = (ex instanceof ContextLoadException cle ? cle.getCause() : ex);
        throw new TestContextAotException(
            "Failed to load ApplicationContext for AOT processing for test class [%s]"
                .formatted(testClass.getName()), cause);
      }
    }
    throw new TestContextAotException("""
        Cannot generate AOT artifacts for test class [%s]. The configured \
        ContextLoader [%s] must be an AotContextLoader and must create a \
        GenericApplicationContext.""".formatted(testClass.getName(),
        contextLoader.getClass().getName()));
  }

  private MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass) {
    TestContextBootstrapper testContextBootstrapper =
        BootstrapUtils.resolveTestContextBootstrapper(testClass);
    registerDeclaredConstructors(testContextBootstrapper.getClass()); // @BootstrapWith
    testContextBootstrapper.getTestExecutionListeners().forEach(listener -> {
      registerDeclaredConstructors(listener.getClass()); // @TestExecutionListeners
      if (listener instanceof AotTestExecutionListener aotListener) {
        aotListener.processAheadOfTime(this.runtimeHints, testClass, getClass().getClassLoader());
      }
    });
    return testContextBootstrapper.buildMergedContextConfiguration();
  }

  DefaultGenerationContext createGenerationContext(Class<?> testClass) {
    ClassNameGenerator classNameGenerator = new ClassNameGenerator(ClassName.get(testClass));
    TestContextGenerationContext generationContext =
        new TestContextGenerationContext(classNameGenerator, this.generatedFiles, this.runtimeHints);
    return generationContext.withName(nextTestContextId());
  }

  private String nextTestContextId() {
    return "TestContext%03d_".formatted(this.sequence.incrementAndGet());
  }

  private void generateAotTestContextInitializerMappings(MultiValueMap<ClassName, Class<?>> initializerClassMappings) {
    ClassNameGenerator classNameGenerator = new ClassNameGenerator(ClassName.get(AotTestContextInitializers.class));
    DefaultGenerationContext generationContext =
        new DefaultGenerationContext(classNameGenerator, this.generatedFiles, this.runtimeHints);
    GeneratedClasses generatedClasses = generationContext.getGeneratedClasses();

    AotTestContextInitializersCodeGenerator codeGenerator =
        new AotTestContextInitializersCodeGenerator(initializerClassMappings, generatedClasses);
    generationContext.writeGeneratedContent();
    String className = codeGenerator.getGeneratedClass().getName().reflectionName();
    registerPublicMethods(className);
  }

  private void generateAotTestAttributeMappings() {
    ClassNameGenerator classNameGenerator = new ClassNameGenerator(ClassName.get(AotTestAttributes.class));
    DefaultGenerationContext generationContext =
        new DefaultGenerationContext(classNameGenerator, this.generatedFiles, this.runtimeHints);
    GeneratedClasses generatedClasses = generationContext.getGeneratedClasses();

    Map<String, String> attributes = AotTestAttributesFactory.getAttributes();
    AotTestAttributesCodeGenerator codeGenerator =
        new AotTestAttributesCodeGenerator(attributes, generatedClasses);
    generationContext.writeGeneratedContent();
    String className = codeGenerator.getGeneratedClass().getName().reflectionName();
    registerPublicMethods(className);
  }

  private void registerPublicMethods(String className) {
    this.runtimeHints.reflection().registerType(TypeReference.of(className), INVOKE_PUBLIC_METHODS);
  }

  private void registerDeclaredConstructors(Class<?> type) {
    this.runtimeHints.reflection().registerType(type, INVOKE_DECLARED_CONSTRUCTORS);
  }

  private static boolean getFailOnErrorFlag() {
    String failOnError = TodayStrategies.getProperty(FAIL_ON_ERROR_PROPERTY_NAME);
    if (StringUtils.hasText(failOnError)) {
      return Boolean.parseBoolean(failOnError.trim());
    }
    return true;
  }

}
