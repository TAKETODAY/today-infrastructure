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

package infra.beans.factory.config;

import org.junit.jupiter.api.Test;

import infra.beans.SimpleTypeConverter;
import infra.beans.TypeConverter;
import infra.beans.factory.FactoryBeanNotInitializedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AbstractFactoryBeanTests {

  @Test
  void earlySingletonProxyDelegatesOnlyAfterInitialization() throws Exception {
    var factory = new TestFactoryBean(Service.class);
    factory.setBeanClassLoader(getClass().getClassLoader());

    Service early = factory.getObject();
    assertThat(factory.getObject()).isSameAs(early);
    assertThat(early.equals(early)).isTrue();
    assertThat(early.equals(null)).isFalse();
    assertThat(early.hashCode()).isEqualTo(System.identityHashCode(early));
    assertThat(early.toString()).contains("Early singleton proxy", Service.class.getName());
    assertThatIllegalStateException().isThrownBy(early::value).withMessageContaining("not initialized");

    factory.afterPropertiesSet();

    Service singleton = factory.getObject();
    assertThat(singleton).isNotSameAs(early);
    assertThat(early.value()).isEqualTo("ready");
    assertThat(early.toString()).isEqualTo(singleton.toString());
    assertThatThrownBy(early::fail).isSameAs(factory.failure);

    factory.destroy();
    assertThat(factory.destroyed).isSameAs(singleton);
  }

  @Test
  void rejectsEarlyAccessForConcreteType() {
    var factory = new TestFactoryBean(ServiceImpl.class);

    assertThatExceptionOfType(FactoryBeanNotInitializedException.class).isThrownBy(factory::getObject)
            .withMessageContaining("does not support circular references");
  }

  @Test
  void prototypeProductsAreCreatedOnDemandAndNotDestroyedByFactory() throws Exception {
    var factory = new TestFactoryBean(Service.class);
    factory.setSingleton(false);
    factory.afterPropertiesSet();
    assertThat(factory.creations).isZero();

    assertThat(factory.getObject()).isNotSameAs(factory.getObject());
    assertThat(factory.creations).isEqualTo(2);

    factory.destroy();
    assertThat(factory.destroyed).isNull();
  }

  @Test
  void destroyingAnUninitializedFactoryDoesNotCallTheDestroyHook() throws Exception {
    var factory = new TestFactoryBean(Service.class);
    factory.destroy();

    assertThat(factory.creations).isZero();
    assertThat(factory.destroyed).isNull();
  }

  @Test
  void obtainsTypeConverterFromOwningFactoryWhenAvailable() {
    var factory = new TestFactoryBean(Service.class);
    assertThat(factory.getBeanTypeConverter()).isInstanceOf(SimpleTypeConverter.class);

    ConfigurableBeanFactory owner = mock(ConfigurableBeanFactory.class);
    TypeConverter converter = mock(TypeConverter.class);
    given(owner.getTypeConverter()).willReturn(converter);
    factory.setBeanFactory(owner);

    assertThat(factory.getBeanTypeConverter()).isSameAs(converter);
  }

  interface Service {

    String value();

    void fail();

  }

  record ServiceImpl(IllegalStateException failure) implements Service {

    @Override
    public String value() {
      return "ready";
    }

    @Override
    public void fail() {
      throw this.failure;
    }

  }

  static class TestFactoryBean extends AbstractFactoryBean<Service> {

    final IllegalStateException failure = new IllegalStateException("service failure");

    final Class<?> objectType;

    int creations;

    Service destroyed;

    TestFactoryBean(Class<?> objectType) {
      this.objectType = objectType;
    }

    @Override
    public Class<?> getObjectType() {
      return this.objectType;
    }

    @Override
    protected Service createBeanInstance() {
      this.creations++;
      return new ServiceImpl(this.failure);
    }

    @Override
    protected void destroyInstance(Service instance) {
      this.destroyed = instance;
    }

  }

}
