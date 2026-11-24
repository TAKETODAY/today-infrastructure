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

class StandardBeanFactoryBehaviorTests {

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
