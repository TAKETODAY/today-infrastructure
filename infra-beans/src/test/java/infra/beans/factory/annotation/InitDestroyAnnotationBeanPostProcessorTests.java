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

package infra.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.DependencyInjector;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.factory.generator.lifecycle.Destroy;
import infra.beans.testfixture.beans.factory.generator.lifecycle.InferredDestroyBean;
import infra.beans.testfixture.beans.factory.generator.lifecycle.Init;
import infra.beans.testfixture.beans.factory.generator.lifecycle.InitDestroyBean;
import infra.beans.testfixture.beans.factory.generator.lifecycle.MultiInitDestroyBean;

import static infra.core.testfixture.io.SerializationTestUtils.serializeAndDeserialize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InitDestroyAnnotationBeanPostProcessor}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/24 09:19
 */
class InitDestroyAnnotationBeanPostProcessorTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @Test
  void invokesInheritedCallbacksInLifecycleOrderWithoutDuplicatingConfiguredMethods() {
    var processor = createLifecycleProcessor();
    processor.addInitAnnotationType(ExtraInit.class);
    processor.addInitAnnotationType(null);
    processor.addDestroyAnnotationType(ExtraDestroy.class);
    processor.addDestroyAnnotationType(null);
    processor.setBeanFactory(this.beanFactory);
    this.beanFactory.addBeanPostProcessor(processor);
    var definition = new RootBeanDefinition(ChildLifecycleBean.class);
    definition.setInitMethodName("initialize");
    definition.setDestroyMethodName("release");
    registerBean(definition);

    var bean = this.beanFactory.getBean("test", ChildLifecycleBean.class);
    assertThat(bean.events).containsExactly("parent init", "child init");

    this.beanFactory.destroySingletons();
    assertThat(bean.events).containsExactly("parent init", "child init", "child destroy", "parent destroy");
  }

  @Test
  void invokesOverriddenAnnotatedCallbacksOnlyOnce() {
    var processor = createLifecycleProcessor();
    var bean = new OverridingLifecycleBean();
    var definition = new RootBeanDefinition(OverridingLifecycleBean.class);

    processor.postProcessMergedBeanDefinition(definition, OverridingLifecycleBean.class, "test");
    processor.postProcessBeforeInitialization(bean, "test");
    processor.postProcessBeforeDestruction(bean, "test");

    assertThat(bean.events).containsExactly("overridden init", "overridden destroy");
  }

  @Test
  void skipsCallbacksAlreadyManagedByAnotherProcessor() {
    var processor = createLifecycleProcessor();
    var definition = new RootBeanDefinition(ParentLifecycleBean.class);
    definition.registerExternallyManagedInitMethod("initialize");
    definition.registerExternallyManagedDestroyMethod("release");
    var bean = new ParentLifecycleBean();

    processor.postProcessMergedBeanDefinition(definition, ParentLifecycleBean.class, "test");
    assertThat(processor.postProcessBeforeInitialization(bean, "test")).isSameAs(bean);
    assertThat(processor.requiresDestruction(bean)).isFalse();
    processor.postProcessBeforeDestruction(bean, "test");

    assertThat(bean.events).isEmpty();
  }

  @Test
  void leavesBeansWithoutCallbacksUntouched() {
    var processor = createLifecycleProcessor();
    var bean = "plain value";
    var definition = new RootBeanDefinition(String.class);
    processor.postProcessMergedBeanDefinition(definition, String.class, "test");

    assertThat(processor.postProcessBeforeInitialization(bean, "test")).isSameAs(bean);
    assertThat(processor.requiresDestruction(bean)).isFalse();
    processor.postProcessBeforeDestruction(bean, "test");
    assertThat(definition.getExternallyManagedInitMethods()).isEmpty();
    assertThat(definition.getExternallyManagedDestroyMethods()).isEmpty();
  }

  @Test
  void rediscoversDestroyCallbackAfterSerialization() throws Exception {
    var processor = createLifecycleProcessor();
    assertThat(processor.requiresDestruction(new ParentLifecycleBean())).isTrue();
    var restored = serializeAndDeserialize(processor);
    var bean = new ParentLifecycleBean();

    assertThat(restored.requiresDestruction(bean)).isTrue();
    restored.postProcessBeforeDestruction(bean, "test");

    assertThat(bean.events).containsExactly("parent destroy");
  }

  @Test
  void rejectsAnnotatedCallbacksWithParameters() {
    var processor = createLifecycleProcessor();
    var bean = new InvalidLifecycleBean();

    assertThatIllegalStateException().isThrownBy(() -> processor.postProcessBeforeInitialization(bean, "test"))
            .withMessageContainingAll("requires a no-arg method", "initialize");
  }

  @Test
  void preservesInitCallbackCauseAndSuppressesDestroyCallbackFailure() {
    var processor = createLifecycleProcessor();
    var bean = new FailingLifecycleBean();

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> processor.postProcessBeforeInitialization(bean, "test"))
            .withMessageContaining("Invocation of init method failed").withCause(bean.failure);
    assertThat(processor.requiresDestruction(bean)).isTrue();
    assertThatCode(() -> processor.postProcessBeforeDestruction(bean, "test")).doesNotThrowAnyException();
    assertThat(bean.destroyCalled).isTrue();
  }

  @Test
  void reportsPreparationFailureWithoutInvokingCallbacks() throws Exception {
    var processor = createLifecycleProcessor();
    var bean = new ParentLifecycleBean();
    var factory = mock(BeanFactory.class);
    var injector = mock(DependencyInjector.class);
    given(factory.getInjector()).willReturn(injector);
    processor.setBeanFactory(factory);
    Method initialize = ParentLifecycleBean.class.getDeclaredMethod("initialize");
    Method release = ParentLifecycleBean.class.getDeclaredMethod("release");
    var failure = new IllegalStateException("cannot prepare callback");
    given(injector.resolveArguments(initialize)).willThrow(failure);
    given(injector.resolveArguments(release)).willThrow(failure);

    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> processor.postProcessBeforeInitialization(bean, "test"))
            .withMessageContaining("Failed to invoke init method").withCause(failure);
    assertThatCode(() -> processor.postProcessBeforeDestruction(bean, "test")).doesNotThrowAnyException();
    assertThat(bean.events).isEmpty();
  }

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
    assertThat(mergedBeanDefinition.getInitMethodNames()).containsExactly("initMethod", "customInitMethod");
    assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly("destroyMethod", "customDestroyMethod");
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
              CustomAnnotatedPrivateInitDestroyBean.class.getName() + ".privateInit", // fully-qualified private method
              CustomAnnotatedPrivateSameNameInitDestroyBean.class.getName() + ".privateInit", // fully-qualified private method
              "afterPropertiesSet",
              "customInit"
      );
      softly.assertThat(mergedBeanDefinition.getDestroyMethodNames()).containsExactly(
              CustomAnnotatedPrivateSameNameInitDestroyBean.class.getName() + ".privateDestroy", // fully-qualified private method
              CustomAnnotatedPrivateInitDestroyBean.class.getName() + ".privateDestroy", // fully-qualified private method
              "destroy",
              "customDestroy"
      );
    });
  }

  private void processAheadOfTime(RootBeanDefinition beanDefinition) {
    RegisteredBean registeredBean = registerBean(beanDefinition);
    assertThat(createLifecycleProcessor().processAheadOfTime(registeredBean)).isNull();
  }

  private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
    String beanName = "test";
    this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
    return RegisteredBean.of(this.beanFactory, beanName);
  }

  private RootBeanDefinition getMergedBeanDefinition() {
    return (RootBeanDefinition) this.beanFactory.getMergedBeanDefinition("test");
  }

  private InitDestroyAnnotationBeanPostProcessor createLifecycleProcessor() {
    InitDestroyAnnotationBeanPostProcessor beanPostProcessor = new InitDestroyAnnotationBeanPostProcessor();
    beanPostProcessor.setInitAnnotationType(Init.class);
    beanPostProcessor.setDestroyAnnotationType(Destroy.class);
    return beanPostProcessor;
  }

  static class NoInitDestroyBean { }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ExtraInit { }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ExtraDestroy { }

  static class ParentLifecycleBean {

    final List<String> events = new ArrayList<>();

    @Init
    public void initialize() {
      this.events.add("parent init");
    }

    @Destroy
    public void release() {
      this.events.add("parent destroy");
    }

  }

  static class ChildLifecycleBean extends ParentLifecycleBean {

    @ExtraInit
    private void initializeChild() {
      this.events.add("child init");
    }

    @ExtraDestroy
    private void releaseChild() {
      this.events.add("child destroy");
    }

  }

  static class InvalidLifecycleBean {

    @Init
    void initialize(String value) { }

  }

  static class OverridingLifecycleBean extends ParentLifecycleBean {

    @Init
    @Override
    public void initialize() {
      this.events.add("overridden init");
    }

    @Destroy
    @Override
    public void release() {
      this.events.add("overridden destroy");
    }

  }

  static class FailingLifecycleBean {

    final IllegalStateException failure = new IllegalStateException("callback failed");

    boolean destroyCalled;

    @Init
    void initialize() {
      throw this.failure;
    }

    @Destroy
    void release() {
      this.destroyCalled = true;
      throw this.failure;
    }

  }

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
