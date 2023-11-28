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

package cn.taketoday.framework.jackson;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aot.ApplicationContextAotGenerator;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.test.tools.CompileWithForkedClassLoader;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.framework.jackson.scan.a.RenameMixInClass;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 23:08
 */
@CompileWithForkedClassLoader
class JsonMixinModuleEntriesBeanRegistrationAotProcessorTests {

  private final TestGenerationContext generationContext = new TestGenerationContext();

  private final GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();

  @Test
  void processAheadOfTimeShouldRegisterBindingHintsForMixins() {
    registerEntries(RenameMixInClass.class);
    processAheadOfTime();
    RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();
    assertThat(RuntimeHintsPredicates.reflection()
            .onType(RenameMixInClass.class)
            .withMemberCategories(MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
            .accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onMethod(RenameMixInClass.class, "getName").introspect())
            .accepts(runtimeHints);
  }

  @Test
  void processAheadOfTimeWhenPublicClassShouldRegisterClass() {
    registerEntries(RenameMixInClass.class);
    compile((freshContext, compiled) -> {
      assertThat(freshContext.getBean(TestConfiguration.class).scanningInvoked).isFalse();
      JsonMixinModuleEntries jsonMixinModuleEntries = freshContext.getBean(JsonMixinModuleEntries.class);
      assertThat(jsonMixinModuleEntries).extracting("entries", InstanceOfAssertFactories.MAP)
              .containsExactly(entry(Name.class, RenameMixInClass.class),
                      entry(NameAndAge.class, RenameMixInClass.class));
    });
  }

  @Test
  void processAheadOfTimeWhenNonAccessibleClassShouldRegisterClassName() {
    Class<?> privateMixinClass = ClassUtils
            .resolveClassName("cn.taketoday.framework.jackson.scan.e.PrivateMixInClass", null);
    registerEntries(privateMixinClass);
    compile((freshContext, compiled) -> {
      assertThat(freshContext.getBean(TestConfiguration.class).scanningInvoked).isFalse();
      JsonMixinModuleEntries jsonMixinModuleEntries = freshContext.getBean(JsonMixinModuleEntries.class);
      assertThat(jsonMixinModuleEntries).extracting("entries", InstanceOfAssertFactories.MAP)
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
    TestCompiler.forSystem().with(this.generationContext).compile((compiled) -> {
      GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
      ApplicationContextInitializer initializer = compiled
              .getInstance(ApplicationContextInitializer.class, className.toString());
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
    JsonMixinModuleEntries jsonMixinModuleEntries(ApplicationContext applicationContext) {
      this.scanningInvoked = true;
      return JsonMixinModuleEntries.scan(applicationContext, this.packageNames);
    }

  }

}