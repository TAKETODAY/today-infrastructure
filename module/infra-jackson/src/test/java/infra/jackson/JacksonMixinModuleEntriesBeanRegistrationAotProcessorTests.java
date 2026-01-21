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

package infra.jackson;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.jackson.scan.a.RenameMixInClass;
import infra.jackson.types.Name;
import infra.jackson.types.NameAndAge;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.aot.ApplicationContextAotGenerator;
import infra.context.support.GenericApplicationContext;
import infra.core.test.tools.CompileWithForkedClassLoader;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.ClassName;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link JacksonMixinModuleEntriesBeanRegistrationAotProcessor}.
 *
 * @author Stephane Nicoll
 */
@CompileWithForkedClassLoader
class JacksonMixinModuleEntriesBeanRegistrationAotProcessorTests {

  private final TestGenerationContext generationContext = new TestGenerationContext();

  private final GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();

  @Test
  void processAheadOfTimeShouldRegisterBindingHintsForMixins() {
    registerEntries(RenameMixInClass.class);
    processAheadOfTime();
    RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
    assertThat(RuntimeHintsPredicates.reflection()
            .onType(RenameMixInClass.class)
            .withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
  }

  @Test
  void processAheadOfTimeWhenPublicClassShouldRegisterClass() {
    registerEntries(RenameMixInClass.class);
    compile((freshContext, compiled) -> {
      assertThat(freshContext.getBean(TestConfiguration.class).scanningInvoked).isFalse();
      JacksonMixinModuleEntries jacksonMixinModuleEntries = freshContext.getBean(JacksonMixinModuleEntries.class);
      assertThat(jacksonMixinModuleEntries).extracting("entries", InstanceOfAssertFactories.MAP)
              .containsExactly(entry(Name.class, RenameMixInClass.class),
                      entry(NameAndAge.class, RenameMixInClass.class));
    });
  }

  @Test
  void processAheadOfTimeWhenNonAccessibleClassShouldRegisterClassName() {
    Class<?> privateMixinClass = ClassUtils
            .resolveClassName("infra.app.jackson.scan.e.PrivateMixInClass", null);
    registerEntries(privateMixinClass);
    compile((freshContext, compiled) -> {
      assertThat(freshContext.getBean(TestConfiguration.class).scanningInvoked).isFalse();
      JacksonMixinModuleEntries jacksonMixinModuleEntries = freshContext.getBean(JacksonMixinModuleEntries.class);
      assertThat(jacksonMixinModuleEntries).extracting("entries", InstanceOfAssertFactories.MAP)
              .containsExactly(entry(Name.class.getName(), privateMixinClass.getName()),
                      entry(NameAndAge.class.getName(), privateMixinClass.getName()));
    });
  }

  private ClassName processAheadOfTime() {
    ClassName className = new ApplicationContextAotGenerator().processAheadOfTime(this.applicationContext,
            this.generationContext);
    this.generationContext.writeGeneratedContent();
    return className;
  }

  private void compile(BiConsumer<GenericApplicationContext, Compiled> result) {
    ClassName className = processAheadOfTime();
    TestCompiler.forSystem()
            .withCompilerOptions("-Xlint:deprecation,removal", "-Werror")
            .with(this.generationContext)
            .compile((compiled) -> {
              GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
              ApplicationContextInitializer initializer = compiled.getInstance(ApplicationContextInitializer.class, className.toString());
              initializer.initialize(freshApplicationContext);
              freshApplicationContext.refresh();
              result.accept(freshApplicationContext, compiled);
            });
  }

  private void registerEntries(Class<?>... basePackageClasses) {
    List<String> packageNames = Arrays.stream(basePackageClasses).map(Class::getPackageName).toList();
    this.applicationContext.registerBeanDefinition("configuration",
            BeanDefinitionBuilder.rootBeanDefinition(TestConfiguration.class)
                    .addConstructorArgValue(packageNames)
                    .getBeanDefinition());
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    public boolean scanningInvoked;

    private final Collection<String> packageNames;

    TestConfiguration(Collection<String> packageNames) {
      this.packageNames = packageNames;
    }

    @Bean
    JacksonMixinModuleEntries jacksonMixinModuleEntries(ApplicationContext applicationContext) {
      this.scanningInvoked = true;
      return JacksonMixinModuleEntries.scan(applicationContext, this.packageNames);
    }

  }

}
