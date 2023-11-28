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

package cn.taketoday.framework.logging.logback;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.model.ComponentModel;
import ch.qos.logback.core.model.ImplicitModel;
import ch.qos.logback.core.model.ImportModel;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.aot.generate.InMemoryGeneratedFiles;
import cn.taketoday.aot.hint.JavaSerializationHint;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.SerializationHints;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.framework.logging.logback.InfraJoranConfigurator.LogbackConfigurationAotContribution;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackConfigurationAotContribution}.
 *
 * @author Andy Wilkinson
 */
class LogbackConfigurationAotContributionTests {

  @BeforeEach
  @AfterEach
  void prepare() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.reset();
  }

  @Test
  void contributionOfBasicModel() {
    TestGenerationContext generationContext = applyContribution(new Model());
    InMemoryGeneratedFiles generatedFiles = generationContext.getGeneratedFiles();
    assertThat(generatedFiles).has(resource("META-INF/config/logback-model"));
    assertThat(generatedFiles).has(resource("META-INF/config/logback-pattern-rules"));
    SerializationHints serializationHints = generationContext.getRuntimeHints().serialization();
    assertThat(serializationHints.javaSerializationHints()
            .map(JavaSerializationHint::getType)
            .map(TypeReference::getName))
            .containsExactlyInAnyOrder(namesOf(Model.class, ArrayList.class, Boolean.class, Integer.class));
    assertThat(generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
    Properties patternRules = load(
            generatedFiles.getGeneratedFile(Kind.RESOURCE, "META-INF/config/logback-pattern-rules"));
    assertThat(patternRules).isEmpty();
  }

  @Test
  void patternRulesAreStoredAndRegisteredForReflection() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.putObject(CoreConstants.PATTERN_RULE_REGISTRY,
            Map.of("a", "com.example.Alpha", "b", "com.example.Bravo"));
    TestGenerationContext generationContext = applyContribution(new Model());
    assertThat(invokePublicConstructorsOf("com.example.Alpha")).accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsOf("com.example.Bravo")).accepts(generationContext.getRuntimeHints());
    Properties patternRules = load(generationContext.getGeneratedFiles()
            .getGeneratedFile(Kind.RESOURCE, "META-INF/config/logback-pattern-rules"));
    assertThat(patternRules).hasSize(2);
    assertThat(patternRules).containsEntry("a", "com.example.Alpha");
    assertThat(patternRules).containsEntry("b", "com.example.Bravo");
  }

  @Test
  void componentModelClassAndSetterParametersAreRegisteredForReflection() {
    ComponentModel component = new ComponentModel();
    component.setClassName(SizeAndTimeBasedRollingPolicy.class.getName());
    Model model = new Model();
    model.getSubModels().add(component);
    TestGenerationContext generationContext = applyContribution(model);
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(SizeAndTimeBasedRollingPolicy.class))
            .accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(FileAppender.class))
            .accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(FileSize.class))
            .accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(
            TimeBasedFileNamingAndTriggeringPolicy.class))
            .accepts(generationContext.getRuntimeHints());
  }

  @Test
  void implicitModelClassAndSetterParametersAreRegisteredForReflection() {
    ImplicitModel implicit = new ImplicitModel();
    implicit.setTag("encoder");
    Model model = new Model();
    model.getSubModels().add(implicit);
    TestGenerationContext generationContext = applyContribution(model);
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(PatternLayoutEncoder.class))
            .accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Layout.class))
            .accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Charset.class))
            .accepts(generationContext.getRuntimeHints());
  }

  @Test
  void componentModelReferencingImportedClassNameIsRegisteredForReflection() {
    ImportModel importModel = new ImportModel();
    importModel.setClassName(SizeAndTimeBasedRollingPolicy.class.getName());
    ComponentModel component = new ComponentModel();
    component.setClassName(SizeAndTimeBasedRollingPolicy.class.getSimpleName());
    Model model = new Model();
    model.getSubModels().addAll(List.of(importModel, component));
    TestGenerationContext generationContext = applyContribution(model);
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(SizeAndTimeBasedRollingPolicy.class))
            .accepts(generationContext.getRuntimeHints());
  }

  @Test
  void typeFromParentsSetterIsRegisteredForReflection() {
    ImplicitModel implementation = new ImplicitModel();
    implementation.setTag("implementation");
    ComponentModel component = new ComponentModel();
    component.setClassName(Outer.class.getName());
    component.getSubModels().add(implementation);
    TestGenerationContext generationContext = applyContribution(component);
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Outer.class))
            .accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Implementation.class))
            .accepts(generationContext.getRuntimeHints());
  }

  @Test
  void typeFromParentsDefaultClassAnnotatedSetterIsRegisteredForReflection() {
    ImplicitModel contract = new ImplicitModel();
    contract.setTag("contract");
    ComponentModel component = new ComponentModel();
    component.setClassName(OuterWithDefaultClass.class.getName());
    component.getSubModels().add(contract);
    TestGenerationContext generationContext = applyContribution(component);
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(OuterWithDefaultClass.class))
            .accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Implementation.class))
            .accepts(generationContext.getRuntimeHints());
  }

  @Test
  void componentTypesOfArraysAreRegisteredForReflection() {
    ComponentModel component = new ComponentModel();
    component.setClassName(ArrayParmeters.class.getName());
    TestGenerationContext generationContext = applyContribution(component);
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(InetSocketAddress.class))
            .accepts(generationContext.getRuntimeHints());
  }

  @Test
  void placeholdersInComponentClassAttributeAreReplaced() {
    ComponentModel component = new ComponentModel();
    component.setClassName("${VARIABLE_CLASS_NAME}");
    TestGenerationContext generationContext = applyContribution(component,
            (context) -> context.putProperty("VARIABLE_CLASS_NAME", Outer.class.getName()));
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Outer.class))
            .accepts(generationContext.getRuntimeHints());
    assertThat(invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Implementation.class))
            .accepts(generationContext.getRuntimeHints());
  }

  private Predicate<RuntimeHints> invokePublicConstructorsOf(String name) {
    return RuntimeHintsPredicates.reflection()
            .onType(TypeReference.of(name))
            .withMemberCategory(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
  }

  private Predicate<RuntimeHints> invokePublicConstructorsAndInspectAndInvokePublicMethodsOf(Class<?> type) {
    return RuntimeHintsPredicates.reflection()
            .onType(TypeReference.of(type))
            .withMemberCategories(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.INTROSPECT_PUBLIC_METHODS,
                    MemberCategory.INVOKE_PUBLIC_METHODS);
  }

  private Properties load(InputStreamSource source) {
    try (InputStream inputStream = source.getInputStream()) {
      Properties properties = new Properties();
      properties.load(inputStream);
      return properties;
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Condition<InMemoryGeneratedFiles> resource(String name) {
    return new Condition<>((files) -> files.getGeneratedFile(Kind.RESOURCE, name) != null,
            "has a resource named '%s'", name);
  }

  private TestGenerationContext applyContribution(Model model) {
    return this.applyContribution(model, (context) -> {
    });
  }

  private TestGenerationContext applyContribution(Model model, Consumer<LoggerContext> contextCustomizer) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    contextCustomizer.accept(context);
    InfraJoranConfigurator configurator = new InfraJoranConfigurator(null);
    configurator.setContext(context);
    withSystemProperty("infra.aot.processing", "true", () -> configurator.processModel(model));
    LogbackConfigurationAotContribution contribution = (LogbackConfigurationAotContribution) context
            .getObject(BeanFactoryInitializationAotContribution.class.getName());
    TestGenerationContext generationContext = new TestGenerationContext();
    contribution.applyTo(generationContext, null);
    return generationContext;
  }

  private String[] namesOf(Class<?>... types) {
    return Stream.of(types).map(Class::getName).toArray(String[]::new);
  }

  private void withSystemProperty(String name, String value, Runnable action) {
    System.setProperty(name, value);
    try {
      action.run();
    }
    finally {
      System.clearProperty(name);
    }
  }

  public static class Outer {

    public void setImplementation(Implementation implementation) {
    }

  }

  public static class OuterWithDefaultClass {

    @DefaultClass(Implementation.class)
    public void setContract(Contract contract) {
    }

  }

  public static class Implementation implements Contract {

  }

  public interface Contract {

  }

  public static class ArrayParmeters {

    public void addDestinations(InetSocketAddress... addresses) {

    }

  }

}
