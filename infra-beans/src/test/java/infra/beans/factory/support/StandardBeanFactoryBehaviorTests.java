/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import infra.beans.factory.DisposableBean;
import infra.beans.factory.DependenciesBeanPostProcessor;
import infra.beans.factory.config.AutowireCapableBeanFactory;
import infra.beans.factory.config.InstantiationAwareBeanPostProcessor;
import infra.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import infra.beans.PropertyValues;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanPostProcessor;
import infra.beans.factory.config.Scope;
import infra.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class StandardBeanFactoryBehaviorTests {

  @Test
  void instantiationAwareProcessorDefaultsAllowInstantiationAndPopulation() {
    var processor = mock(InstantiationAwareBeanPostProcessor.class, CALLS_REAL_METHODS);
    assertThat(processor.postProcessBeforeInstantiation(String.class, "bean")).isNull();
    assertThat(processor.postProcessAfterInstantiation(new Object(), "bean")).isTrue();
  }

  @Test
  void smartInstantiationProcessorDefaultsPreserveBeanAndType() {
    var processor = mock(SmartInstantiationAwareBeanPostProcessor.class, CALLS_REAL_METHODS);
    var bean = new Object();
    assertThat(processor.predictBeanType(String.class, "bean")).isNull();
    assertThat(processor.determineBeanType(String.class, "bean")).isSameAs(String.class);
    assertThat(processor.determineCandidateConstructors(String.class, "bean")).isNull();
    assertThat(processor.getEarlyBeanReference(bean, "bean")).isSameAs(bean);
  }

  @Test
  void initializationAndDependencyProcessorDefaultsPreserveInputs() {
    var initialization = mock(InitializationBeanPostProcessor.class, CALLS_REAL_METHODS);
    var dependencies = mock(DependenciesBeanPostProcessor.class, CALLS_REAL_METHODS);
    var bean = new Object();
    var properties = new PropertyValues();
    assertThat(initialization.postProcessBeforeInitialization(bean, "bean")).isSameAs(bean);
    assertThat(initialization.postProcessAfterInitialization(bean, "bean")).isSameAs(bean);
    assertThat(dependencies.processDependencies(properties, bean, "bean")).isSameAs(properties);
  }

  @Test
  void autowireDefaultsDelegateWithDocumentedArguments() {
    var factory = mock(AutowireCapableBeanFactory.class, CALLS_REAL_METHODS);
    given(factory.createBean(String.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false)).willReturn("created");
    given(factory.createBean(Integer.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false)).willReturn(42);
    assertThat(factory.createBean(String.class)).isEqualTo("created");
    assertThat(factory.createBean(Integer.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE)).isEqualTo(42);
    var bean = new Object();
    factory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO);
    verify(factory).autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
  }

  @Test
  void mergedDefinitionResetDefaultIsNoOp() {
    var processor = mock(MergedBeanDefinitionPostProcessor.class, CALLS_REAL_METHODS);
    processor.resetBeanDefinition("bean");
    verify(processor).resetBeanDefinition("bean");
    org.mockito.Mockito.verifyNoMoreInteractions(processor);
  }

  @Test
  void createsBeansWithInstanceSupplier() {
    StandardBeanFactory factory = new StandardBeanFactory();
    AtomicInteger invocations = new AtomicInteger();
    RootBeanDefinition definition = new RootBeanDefinition(SuppliedBean.class);
    definition.setInstanceSupplier(() -> {
      invocations.incrementAndGet();
      return new SuppliedBean("supplied");
    });
    factory.registerBeanDefinition("bean", definition);

    SuppliedBean first = factory.getBean("bean", SuppliedBean.class);
    SuppliedBean second = factory.getBean("bean", SuppliedBean.class);

    assertThat(first.value).isEqualTo("supplied");
    assertThat(second).isSameAs(first);
    assertThat(invocations).hasValue(1);
    assertThat(definition.getInstanceSupplier()).isNotNull();
  }

  @Test
  void createsAndDestroysBeansInCustomScope() {
    StandardBeanFactory factory = new StandardBeanFactory();
    RecordingScope scope = new RecordingScope();
    factory.registerScope("request", scope);
    RootBeanDefinition definition = new RootBeanDefinition(ScopedBean.class);
    definition.setScope("request");
    factory.registerBeanDefinition("bean", definition);

    ScopedBean first = factory.getBean("bean", ScopedBean.class);
    ScopedBean second = factory.getBean("bean", ScopedBean.class);

    assertThat(first).isSameAs(second);
    assertThat(scope.getConversationId()).isEqualTo("request-1");
    assertThat(factory.getRegisteredScopeNames()).containsExactly("request");
    assertThat(factory.getRegisteredScope("request")).isSameAs(scope);

    factory.destroyScopedBean("bean");

    assertThat(scope.removedName).isEqualTo("bean");
    assertThat(first.destroyed).isTrue();
    assertThat(scope.callbacks).doesNotContainKey("bean");
  }

  @Test
  void reportsInactiveScopeWithoutCreatingTheBean() {
    var factory = new StandardBeanFactory();
    var scope = mock(Scope.class);
    var failure = new IllegalStateException("No request is bound to this thread");
    given(scope.get(eq("bean"), any())).willThrow(failure);
    factory.registerScope("request", scope);
    var definition = new RootBeanDefinition(ScopedBean.class);
    definition.setScope("request");
    var invocations = new AtomicInteger();
    definition.setInstanceSupplier(() -> {
      invocations.incrementAndGet();
      return new ScopedBean();
    });
    factory.registerBeanDefinition("bean", definition);

    assertThatExceptionOfType(ScopeNotActiveException.class).isThrownBy(() -> factory.getBean("bean"))
            .withMessageContaining("bean", "Scope 'request' is not active")
            .withCause(failure);
    assertThat(invocations).hasValue(0);
  }

  @Test
  void validatesMutableScopeOperations() {
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerBeanDefinition("singleton", new RootBeanDefinition(SuppliedBean.class));
    RootBeanDefinition prototype = new RootBeanDefinition(SuppliedBean.class);
    prototype.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    factory.registerBeanDefinition("prototype", prototype);
    RootBeanDefinition unknown = new RootBeanDefinition(SuppliedBean.class);
    unknown.setScope("unknown");
    factory.registerBeanDefinition("unknown", unknown);

    assertThatIllegalArgumentException().isThrownBy(() -> factory.destroyScopedBean("singleton"));
    assertThatIllegalArgumentException().isThrownBy(() -> factory.destroyScopedBean("prototype"));
    assertThatIllegalStateException().isThrownBy(() -> factory.destroyScopedBean("unknown"));
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> factory.destroyScopedBean("missing"));
    assertThatIllegalArgumentException().isThrownBy(() -> factory.registerScope(BeanDefinition.SCOPE_SINGLETON,
            mock(Scope.class)));
  }

  @Test
  void appliesAndRemovesBeanPostProcessors() {
    StandardBeanFactory factory = new StandardBeanFactory();
    RootBeanDefinition definition = new RootBeanDefinition(SuppliedBean.class);
    definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    factory.registerBeanDefinition("bean", definition);
    AtomicInteger before = new AtomicInteger();
    BeanPostProcessor processor = new InitializationBeanPostProcessor() {
      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        before.incrementAndGet();
        return bean;
      }
    };

    factory.addBeanPostProcessor(processor);
    factory.addBeanPostProcessor(processor);
    assertThat(factory.getBeanPostProcessorCount()).isEqualTo(1);
    factory.getBean("bean");
    factory.removeBeanPostProcessor(processor);
    factory.getBean("bean");

    assertThat(before).hasValue(1);
    assertThatIllegalArgumentException().isThrownBy(() -> factory.addBeanPostProcessor(null));
  }

  @Test
  void findsAnnotationsOnDefinitionsAndManualSingletons() {
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerBeanDefinition("definition", new RootBeanDefinition(AnnotatedBean.class));
    factory.registerSingleton("manual", new AnnotatedBean());

    assertThat(factory.getBeanNamesForAnnotation(Marker.class))
            .containsExactly("definition", "manual");
    MergedAnnotation<Marker> annotation = factory.findAnnotationOnBean("definition", Marker.class);
    assertThat(annotation.isPresent()).isTrue();
    assertThat(factory.findSynthesizedAnnotation("definition", Marker.class)).isNotNull();
    assertThat(factory.findAllAnnotationsOnBean("manual", Marker.class, true)).hasSize(1);
  }

  @Test
  void updatesManualSingletonTypeCachesWhenDestroyed() {
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerSingleton("manual", new SuppliedBean("manual"));
    factory.freezeConfiguration();

    assertThat(factory.getBeanNamesForType(SuppliedBean.class)).containsExactly("manual");
    factory.destroySingleton("manual");

    assertThat(factory.getBeanNamesForType(SuppliedBean.class)).isEmpty();
    assertThat(factory.getBeanNamesForType(Object.class)).isEmpty();
  }

  @Test
  void supportsProviderConsumerOperations() {
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerBeanDefinition("bean", new RootBeanDefinition(SuppliedBean.class));
    ObjectProvider<SuppliedBean> provider = factory.getBeanProvider(SuppliedBean.class);
    AtomicInteger available = new AtomicInteger();
    AtomicInteger unique = new AtomicInteger();

    assertThat(provider.ifAvailable(bean -> available.incrementAndGet())).isTrue();
    assertThat(provider.ifUnique(bean -> unique.incrementAndGet())).isTrue();
    assertThat(available).hasValue(1);
    assertThat(unique).hasValue(1);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Marker {
  }

  @Marker
  static class AnnotatedBean {
  }

  static class SuppliedBean {

    final String value;

    SuppliedBean() {
      this("default");
    }

    SuppliedBean(String value) {
      this.value = value;
    }
  }

  static class ScopedBean implements DisposableBean {

    boolean destroyed;

    @Override
    public void destroy() {
      this.destroyed = true;
    }
  }

  static class RecordingScope implements Scope {

    private final Map<String, Object> beans = new HashMap<>();

    private final Map<String, Runnable> callbacks = new HashMap<>();

    private String removedName;

    @Override
    public Object get(String name, java.util.function.Supplier<?> objectFactory) {
      return this.beans.computeIfAbsent(name, key -> objectFactory.get());
    }

    @Override
    public Object remove(String name) {
      this.removedName = name;
      Runnable callback = this.callbacks.remove(name);
      if (callback != null) {
        callback.run();
      }
      return this.beans.remove(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
      this.callbacks.put(name, callback);
    }

    @Override
    public Object resolveContextualObject(String key) {
      return null;
    }

    @Override
    public String getConversationId() {
      return "request-1";
    }
  }

}
