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

package infra.aop.target;

import org.junit.jupiter.api.Test;

import java.io.NotSerializableException;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.testfixture.io.SerializationTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TargetSourceContractTests {

  @Test
  void singletonAndEmptyTargetSourcesExposeStableContracts() throws Exception {
    var target = new Object();
    var source = new SingletonTargetSource(target);
    assertThat(source.getTargetClass()).isEqualTo(Object.class);
    assertThat(source.getTarget()).isSameAs(target);
    assertThat(source.isStatic()).isTrue();
    source.releaseTarget(new Object());
    assertThat(source).isEqualTo(new SingletonTargetSource(target));
    assertThat(source).isNotEqualTo(EmptyTargetSource.INSTANCE);
    assertThat(source.hashCode()).isEqualTo(target.hashCode());
    assertThat(source.toString()).contains("Object");

    assertThat(EmptyTargetSource.INSTANCE.getTargetClass()).isNull();
    assertThat(EmptyTargetSource.INSTANCE.getTarget()).isNull();
    assertThat(EmptyTargetSource.INSTANCE.isStatic()).isTrue();
    assertThat(EmptyTargetSource.forClass(null)).isSameAs(EmptyTargetSource.INSTANCE);
    assertThat(EmptyTargetSource.forClass(String.class)).isEqualTo(EmptyTargetSource.forClass(String.class));
    var empty = EmptyTargetSource.forClass(String.class);
    assertThat(empty.equals(empty)).isTrue();
    assertThat(empty).isNotEqualTo(String.class);
    assertThat(empty.hashCode()).isEqualTo(EmptyTargetSource.forClass(String.class).hashCode());
    assertThat(EmptyTargetSource.forClass(String.class, false).isStatic()).isFalse();
    assertThat(SerializationTestUtils.serializeAndDeserialize(EmptyTargetSource.INSTANCE))
            .isSameAs(EmptyTargetSource.INSTANCE);
  }

  @Test
  void beanFactoryTargetSourceResolvesTypeAndCopiesConfiguration() {
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("service", new RootBeanDefinition(Service.class));
    var source = new SimpleBeanTargetSource();
    source.setTargetBeanName("service");
    source.setBeanFactory(factory);
    assertThat(source.getBeanFactory()).isSameAs(factory);
    assertThat(source.getTargetClass()).isEqualTo(Service.class);
    assertThat(source.getTargetClass()).isSameAs(source.getTargetClass());
    assertThat(source.getTarget()).isInstanceOf(Service.class);
    assertThat(source.isStatic()).isFalse();
    assertThat(source.toString()).contains("service").contains(Service.class.getName());

    var copy = new SimpleBeanTargetSource();
    copy.copyFrom(source);
    assertThat(source).isEqualTo(copy);
    assertThat(source.equals(source)).isTrue();
    assertThat(source).isNotEqualTo(EmptyTargetSource.INSTANCE);
    assertThat(source.hashCode()).isEqualTo(copy.hashCode());
    assertThat(copy.getTarget()).isSameAs(source.getTarget());
    assertThatIllegalStateException().isThrownBy(() -> new SimpleBeanTargetSource().getTargetBeanName())
            .withMessage("Target bean name not set");
  }

  @Test
  void beanFactoryTargetSourceFallsBackToAnInstanceWhenTypeIsUnknown() {
    BeanFactory factory = mock();
    given(factory.getBean("service")).willReturn(new Service());
    var source = new SimpleBeanTargetSource();
    source.setTargetBeanName("service");
    source.setBeanFactory(factory);

    assertThat(source.getTargetClass()).isEqualTo(Service.class);
    assertThat(source.getTargetClass()).isEqualTo(Service.class);
    verify(factory).getBean("service");

    var missing = new SimpleBeanTargetSource();
    missing.setTargetBeanName("missing");
    missing.setBeanFactory(factory);
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(missing::getTargetClass).withMessageContaining("missing");
  }

  @Test
  void prototypeTargetSourceRejectsSingletonBeans() {
    var factory = new StandardBeanFactory();
    factory.registerBeanDefinition("service", new RootBeanDefinition(Service.class));
    var source = new PrototypeTargetSource();
    source.setTargetBeanName("service");

    assertThatExceptionOfType(BeanDefinitionStoreException.class)
            .isThrownBy(() -> source.setBeanFactory(factory))
            .withMessageContaining("non-prototype bean with name 'service'");
  }

  @Test
  void prototypeTargetSourceInvokesDisposableBeanWithoutAConfigurableFactory() throws Exception {
    BeanFactory factory = mock();
    DisposableBean target = mock();
    given(factory.isPrototype("service")).willReturn(true);
    given(factory.getBean("service")).willReturn(target);
    willThrow(new IllegalStateException("destroy failed")).given(target).destroy();
    var source = new PrototypeTargetSource();
    source.setTargetBeanName("service");
    source.setBeanFactory(factory);

    source.releaseTarget(source.getTarget());

    verify(target).destroy();
  }

  @Test
  void prototypeTargetSourceReleasesDisposableBeanWithoutAConfigurableFactory() throws Exception {
    BeanFactory factory = mock();
    DisposableBean target = mock();
    given(factory.isPrototype("service")).willReturn(true);
    given(factory.getBean("service")).willReturn(target);
    var source = new PrototypeTargetSource();
    source.setTargetBeanName("service");
    source.setBeanFactory(factory);

    source.releaseTarget(source.getTarget());

    verify(target).destroy();
  }

  @Test
  void prototypeTargetSourceSerializationReportsTargetLookupFailure() {
    BeanFactory factory = mock();
    given(factory.isPrototype("service")).willReturn(true);
    given(factory.getBean("service")).willThrow(new NoSuchBeanDefinitionException("service"));
    var source = new PrototypeTargetSource();
    source.setTargetBeanName("service");
    source.setBeanFactory(factory);

    assertThatExceptionOfType(NotSerializableException.class)
            .isThrownBy(() -> SerializationTestUtils.serializeAndDeserialize(source))
            .withMessageContaining("Cannot get target for disconnecting TargetSource", "service");
  }

  @Test
  void prototypeTargetSourceDisconnectsToAnEmptySourceWhenNoTargetCanBeResolved() throws Exception {
    var factory = mock(BeanFactory.class);
    given(factory.isPrototype("service")).willReturn(true);
    willReturn(Service.class).given(factory).getType("service");
    var source = new PrototypeTargetSource();
    source.setTargetBeanName("service");
    source.setBeanFactory(factory);

    Object disconnected = SerializationTestUtils.serializeAndDeserialize(source);

    assertThat(disconnected).isEqualTo(EmptyTargetSource.forClass(Service.class));
  }

  @Test
  void threadLocalTargetSourceCreatesAndReusesAThreadBoundPrototype() throws Exception {
    var factory = new StandardBeanFactory();
    var definition = new RootBeanDefinition(Service.class);
    definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    factory.registerBeanDefinition("service", definition);
    var source = new ThreadLocalTargetSource();
    source.setTargetBeanName("service");
    source.setBeanFactory(factory);

    try {
      Object first = source.getTarget();
      assertThat(source.getTarget()).isSameAs(first);
      assertThat(source.getInvocationCount()).isEqualTo(2);
      assertThat(source.getHitCount()).isOne();
      source.destroy();
      assertThat(source.getObjectCount()).isZero();
      assertThat(source.getTarget()).isNotSameAs(first);
    }
    finally {
      source.destroy();
    }
  }

  @Test
  void swappableTargetSourceValidatesAndReportsState() {
    assertThatIllegalArgumentException().isThrownBy(() -> new HotSwappableTargetSource(null));
    var source = new HotSwappableTargetSource("one");
    assertThat(source.getTargetClass()).isEqualTo(String.class);
    assertThat(source.isStatic()).isFalse();
    assertThat(source.swap("two")).isEqualTo("one");
    assertThat(source.getTarget()).isEqualTo("two");
    assertThatIllegalArgumentException().isThrownBy(() -> source.swap(null));
    assertThat(source).isEqualTo(new HotSwappableTargetSource("two"));
    assertThat(source.hashCode()).isEqualTo(new HotSwappableTargetSource("two").hashCode());
    assertThat(source.toString()).contains("two");
  }

  @Test
  void refreshableTargetSourceRefreshesAndTracksTimes() {
    BeanFactory factory = mock();
    given(factory.getBean("one")).willReturn("first", "second");
    var source = new BeanFactoryRefreshableTargetSource(factory, "one");
    assertThat(source.getTarget()).isEqualTo("first");
    assertThat(source.getRefreshCount()).isOne();
    assertThat(source.getLastRefreshTime()).isPositive();
    source.refresh();
    assertThat(source.getTarget()).isEqualTo("second");
    assertThat(source.getRefreshCount()).isEqualTo(2);
    assertThat(source.isStatic()).isFalse();
    assertThat(source.requiresRefresh()).isTrue();
  }

  static class Service {
  }

}
