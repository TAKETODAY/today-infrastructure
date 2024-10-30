/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.context.aot;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.hint.annotation.Reflective;
import cn.taketoday.aot.hint.predicate.ReflectionHintsPredicates;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.annotation.ReflectiveScan;
import cn.taketoday.context.testfixture.context.aot.scan.reflective2.Reflective2OnType;
import cn.taketoday.context.testfixture.context.aot.scan.reflective2.reflective21.Reflective21OnType;
import cn.taketoday.context.testfixture.context.aot.scan.reflective2.reflective22.Reflective22OnType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReflectiveProcessorBeanFactoryInitializationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class ReflectiveProcessorBeanFactoryInitializationAotProcessorTests {

  private final ReflectiveProcessorBeanFactoryInitializationAotProcessor processor = new ReflectiveProcessorBeanFactoryInitializationAotProcessor();

  private final GenerationContext generationContext = new TestGenerationContext();

  @Test
  void processorIsRegistered() {
    assertThat(AotServices.factories(getClass().getClassLoader()).load(BeanFactoryInitializationAotProcessor.class))
            .anyMatch(ReflectiveProcessorBeanFactoryInitializationAotProcessor.class::isInstance);
  }

  @Test
  void shouldProcessAnnotationOnType() {
    process(SampleTypeAnnotatedBean.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(SampleTypeAnnotatedBean.class))
            .accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessAllBeans() throws NoSuchMethodException {
    ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
    process(SampleTypeAnnotatedBean.class, SampleConstructorAnnotatedBean.class);
    Constructor<?> constructor = SampleConstructorAnnotatedBean.class.getDeclaredConstructor(String.class);
    assertThat(reflection.onType(SampleTypeAnnotatedBean.class).and(reflection.onConstructor(constructor)))
            .accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldTriggerScanningIfBeanUsesReflectiveScan() {
    process(SampleBeanWithReflectiveScan.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints().map(TypeHint::getType))
            .containsExactlyInAnyOrderElementsOf(TypeReference.listOf(
                    Reflective2OnType.class, Reflective21OnType.class, Reflective22OnType.class));
  }

  @Test
  void findBasePackagesToScanWhenNoCandidateIsEmpty() {
    Class<?>[] candidates = { String.class };
    assertThat(this.processor.findBasePackagesToScan(candidates)).isEmpty();
  }

  @Test
  void findBasePackagesToScanWithBasePackageClasses() {
    Class<?>[] candidates = { SampleBeanWithReflectiveScan.class };
    assertThat(this.processor.findBasePackagesToScan(candidates))
            .containsOnly(Reflective2OnType.class.getPackageName());
  }

  @Test
  void findBasePackagesToScanWithBasePackages() {
    Class<?>[] candidates = { SampleBeanWithReflectiveScanWithName.class };
    assertThat(this.processor.findBasePackagesToScan(candidates))
            .containsOnly(Reflective2OnType.class.getPackageName());
  }

  @Test
  void findBasePackagesToScanWithBasePackagesAndClasses() {
    Class<?>[] candidates = { SampleBeanWithMultipleReflectiveScan.class };
    assertThat(this.processor.findBasePackagesToScan(candidates))
            .containsOnly(Reflective21OnType.class.getPackageName(), Reflective22OnType.class.getPackageName());
  }

  @Test
  void findBasePackagesToScanWithDuplicatesFiltersThem() {
    Class<?>[] candidates = { SampleBeanWithReflectiveScan.class, SampleBeanWithReflectiveScanWithName.class };
    assertThat(this.processor.findBasePackagesToScan(candidates))
            .containsOnly(Reflective2OnType.class.getPackageName());
  }

  private void process(Class<?>... beanClasses) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    for (Class<?> beanClass : beanClasses) {
      beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
    }
    BeanFactoryInitializationAotContribution contribution = this.processor.processAheadOfTime(beanFactory);
    assertThat(contribution).isNotNull();
    contribution.applyTo(this.generationContext, mock());
  }

  @Reflective
  @SuppressWarnings("unused")
  static class SampleTypeAnnotatedBean {

    private String notManaged;

    public void notManaged() {

    }
  }

  @SuppressWarnings("unused")
  static class SampleConstructorAnnotatedBean {

    @Reflective
    SampleConstructorAnnotatedBean(String name) {

    }

    SampleConstructorAnnotatedBean(Integer nameAsNumber) {

    }

  }

  @ReflectiveScan(basePackageClasses = Reflective2OnType.class)
  static class SampleBeanWithReflectiveScan {
  }

  @ReflectiveScan("cn.taketoday.context.testfixture.context.aot.scan.reflective2")
  static class SampleBeanWithReflectiveScanWithName {
  }

  @ReflectiveScan(basePackageClasses = Reflective22OnType.class,
          basePackages = "cn.taketoday.context.testfixture.context.aot.scan.reflective2.reflective21")
  static class SampleBeanWithMultipleReflectiveScan {
  }

}
