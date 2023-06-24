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

package cn.taketoday.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.support.AbstractBeanDefinition;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.factory.generator.lifecycle.Destroy;
import cn.taketoday.beans.testfixture.beans.factory.generator.lifecycle.InferredDestroyBean;
import cn.taketoday.beans.testfixture.beans.factory.generator.lifecycle.Init;
import cn.taketoday.beans.testfixture.beans.factory.generator.lifecycle.InitDestroyBean;
import cn.taketoday.beans.testfixture.beans.factory.generator.lifecycle.MultiInitDestroyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Tests for {@link InitDestroyAnnotationBeanPostProcessor}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/24 09:19
 */
class InitDestroyAnnotationBeanPostProcessorTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @Test
  void processAheadOfTimeWhenNoCallbackDoesNotMutateRootBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(NoInitDestroyBean.class);
    processAheadOfTime(beanDefinition);
    RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
    assertThat(mergedBeanDefinition.getInitMethodNames()).isNull();
    assertThat(mergedBeanDefinition.getDestroyMethodNames()).isNull();
  }

  @Test
  void processAheadOfTimeWhenHasInitDestroyAnnotationsAddsMethodNames() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
    processAheadOfTime(beanDefinition);
    RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
    assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("initMethod");
    assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("destroyMethod");
  }

  @Test
  void processAheadOfTimeWhenHasInitDestroyAnnotationsAndCustomDefinedMethodNamesAddsMethodNames() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
    beanDefinition.setInitMethodName("customInitMethod");
    beanDefinition.setDestroyMethodNames("customDestroyMethod");
    processAheadOfTime(beanDefinition);
    RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
    assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("customInitMethod", "initMethod");
    assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("customDestroyMethod", "destroyMethod");
  }

  @Test
  void processAheadOfTimeWhenHasInitDestroyAnnotationsAndOverlappingCustomDefinedMethodNamesFiltersDuplicates() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyBean.class);
    beanDefinition.setInitMethodName("initMethod");
    beanDefinition.setDestroyMethodNames("destroyMethod");
    processAheadOfTime(beanDefinition);
    RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
    assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("initMethod");
    assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("destroyMethod");
  }

  @Test
  void processAheadOfTimeWhenHasInferredDestroyMethodAddsDestroyMethodName() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(InferredDestroyBean.class);
    beanDefinition.setDestroyMethodNames(AbstractBeanDefinition.INFER_METHOD);
    processAheadOfTime(beanDefinition);
    RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
    assertThat(mergedBeanDefinition.getInitMethodNames()).isNull();
    assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("close");
  }

  @Test
  void processAheadOfTimeWhenHasInferredDestroyMethodAndNoCandidateDoesNotMutateRootBeanDefinition() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(NoInitDestroyBean.class);
    beanDefinition.setDestroyMethodNames(AbstractBeanDefinition.INFER_METHOD);
    processAheadOfTime(beanDefinition);
    RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
    assertThat(mergedBeanDefinition.getInitMethodNames()).isNull();
    assertThat(mergedBeanDefinition.getDestroyMethodNames()).isNull();
  }

  @Test
  void processAheadOfTimeWhenHasMultipleInitDestroyAnnotationsAddsAllMethodNames() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(MultiInitDestroyBean.class);
    processAheadOfTime(beanDefinition);
    RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
    assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("initMethod", "anotherInitMethod");
    assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("anotherDestroyMethod", "destroyMethod");
  }

  @Test
  void processAheadOfTimeWithMultipleLevelsOfPublicAndPrivateInitAndDestroyMethods() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(CustomAnnotatedPrivateSameNameInitDestroyBean.class);
    // We explicitly define "afterPropertiesSet" as a "custom init method"
    // to ensure that it will be tracked as such even though it has the same
    // name as InitializingBean#afterPropertiesSet().
    beanDefinition.setInitMethodNames("afterPropertiesSet", "customInit");
    // We explicitly define "destroy" as a "custom destroy method"
    // to ensure that it will be tracked as such even though it has the same
    // name as DisposableBean#destroy().
    beanDefinition.setDestroyMethodNames("destroy", "customDestroy");
    processAheadOfTime(beanDefinition);
    RootBeanDefinition mergedBeanDefinition = getMergedBeanDefinition();
    assertSoftly(softly -> {
      softly.assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly(
              "afterPropertiesSet",
              "customInit",
              CustomAnnotatedPrivateInitDestroyBean.class.getName() + ".privateInit", // fully-qualified private method
              CustomAnnotatedPrivateSameNameInitDestroyBean.class.getName() + ".privateInit" // fully-qualified private method
      );
      softly.assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly(
              "destroy",
              "customDestroy",
              CustomAnnotatedPrivateSameNameInitDestroyBean.class.getName() + ".privateDestroy", // fully-qualified private method
              CustomAnnotatedPrivateInitDestroyBean.class.getName() + ".privateDestroy" // fully-qualified private method
      );
    });
  }

  private void processAheadOfTime(RootBeanDefinition beanDefinition) {
    RegisteredBean registeredBean = registerBean(beanDefinition);
    assertThat(createAotBeanPostProcessor().processAheadOfTime(registeredBean)).isNull();
  }

  private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
    String beanName = "test";
    this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
    return RegisteredBean.of(this.beanFactory, beanName);
  }

  private RootBeanDefinition getMergedBeanDefinition() {
    return (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition("test");
  }

  private InitDestroyAnnotationBeanPostProcessor createAotBeanPostProcessor() {
    InitDestroyAnnotationBeanPostProcessor beanPostProcessor = new InitDestroyAnnotationBeanPostProcessor();
    beanPostProcessor.setInitAnnotationType(Init.class);
    beanPostProcessor.setDestroyAnnotationType(Destroy.class);
    return beanPostProcessor;
  }

  static class NoInitDestroyBean { }

  static class CustomInitDestroyBean {

    public void customInit() {
    }

    public void customDestroy() {
    }
  }

  static class CustomInitializingDisposableBean extends CustomInitDestroyBean
          implements InitializingBean, DisposableBean {

    @Override
    public void afterPropertiesSet() {
    }

    @Override
    public void destroy() {
    }
  }

  static class CustomAnnotatedPrivateInitDestroyBean extends CustomInitializingDisposableBean {

    @Init
    private void privateInit() {
    }

    @Destroy
    private void privateDestroy() {
    }
  }

  static class CustomAnnotatedPrivateSameNameInitDestroyBean extends CustomAnnotatedPrivateInitDestroyBean {

    @Init
    @SuppressWarnings("unused")
    private void privateInit() {
    }

    @Destroy
    @SuppressWarnings("unused")
    private void privateDestroy() {
    }
  }

}