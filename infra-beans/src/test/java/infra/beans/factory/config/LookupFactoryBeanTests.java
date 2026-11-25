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

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import jakarta.inject.Provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class LookupFactoryBeanTests {

  @Test
  void supplierLooksUpPrototypeOnEachCall() throws Exception {
    var count = new AtomicInteger();
    var factory = new SupplierFactoryCreatingFactoryBean();
    factory.setBeanFactory(prototypeFactory(count));
    factory.setTargetBeanName("target");
    factory.afterPropertiesSet();

    assertThat(factory.getObjectType()).isEqualTo(Supplier.class);
    Supplier<Object> supplier = factory.getObject();
    assertThat(factory.getObject()).isSameAs(supplier);
    assertThat(count).hasValue(0);
    assertThat(supplier.get()).isEqualTo(1);
    assertThat(supplier.get()).isEqualTo(2);
  }

  @Test
  void providerLooksUpPrototypeOnEachCall() throws Exception {
    var count = new AtomicInteger();
    var factory = new ProviderCreatingFactoryBean();
    factory.setBeanFactory(prototypeFactory(count));
    factory.setTargetBeanName("target");
    factory.afterPropertiesSet();

    assertThat(factory.getObjectType()).isEqualTo(Provider.class);
    Provider<Object> provider = factory.getObject();
    assertThat(factory.getObject()).isSameAs(provider);
    assertThat(count).hasValue(0);
    assertThat(provider.get()).isEqualTo(1);
    assertThat(provider.get()).isEqualTo(2);
  }

  @Test
  void distinguishesConfigurationErrorsFromDeferredLookupFailure() throws Exception {
    var supplier = new SupplierFactoryCreatingFactoryBean();
    var provider = new ProviderCreatingFactoryBean();
    assertThatIllegalArgumentException().isThrownBy(supplier::afterPropertiesSet).withMessageContaining("targetBeanName");
    assertThatIllegalArgumentException().isThrownBy(provider::afterPropertiesSet).withMessageContaining("targetBeanName");

    supplier.setTargetBeanName("missing");
    provider.setTargetBeanName("missing");
    assertThatIllegalStateException().isThrownBy(supplier::afterPropertiesSet).withMessageContaining("No BeanFactory");
    assertThatIllegalStateException().isThrownBy(provider::afterPropertiesSet).withMessageContaining("No BeanFactory");

    var beanFactory = new StandardBeanFactory();
    supplier.setBeanFactory(beanFactory);
    provider.setBeanFactory(beanFactory);
    supplier.afterPropertiesSet();
    provider.afterPropertiesSet();
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> supplier.getObject().get());
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> provider.getObject().get());
  }

  private static StandardBeanFactory prototypeFactory(AtomicInteger count) {
    var beanFactory = new StandardBeanFactory();
    var definition = new RootBeanDefinition(Integer.class, count::incrementAndGet);
    definition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    beanFactory.registerBeanDefinition("target", definition);
    return beanFactory;
  }

  @Test
  void serviceLocatorResolvesMappedAndUnmappedIdsAndHasIdentitySemantics() {
    var beans = new StandardBeanFactory();
    beans.registerSingleton("primary", "default value");
    beans.registerSingleton("secondary", "other value");
    var factory = locatorFactory(beans);
    var mappings = new Properties();
    mappings.setProperty("", "primary");
    mappings.setProperty("7", "secondary");
    factory.setServiceMappings(mappings);

    var locator = (Locator) factory.getObject();
    assertThat(locator.lookup()).isEqualTo("default value");
    assertThat(locator.lookup(null)).isEqualTo("default value");
    assertThat(locator.lookup(7)).isEqualTo("other value");
    assertThat(locator.lookup("primary")).isEqualTo("default value");
    assertThat(locator.equals(locator)).isTrue();
    assertThat(locator).isNotEqualTo(locatorFactory(beans).getObject());
    assertThat(locator.hashCode()).isEqualTo(System.identityHashCode(locator));
    assertThat(locator.toString()).contains("Service locator:", Locator.class.getName());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(locator::invalid);
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> locator.invalid("a", "b"));
  }

  @Test
  void serviceLocatorFallsBackToTypeAndTranslatesLookupFailure() {
    var beans = new StandardBeanFactory();
    beans.registerSingleton("unique", "resolved by type");
    var factory = locatorFactory(beans);
    factory.setServiceLocatorExceptionClass(LookupFailure.class);
    var locator = (Locator) factory.getObject();

    assertThat(locator.lookup()).isEqualTo("resolved by type");
    assertThatExceptionOfType(LookupFailure.class).isThrownBy(() -> locator.lookup("missing"))
            .withCauseInstanceOf(NoSuchBeanDefinitionException.class);
  }

  private static ServiceLocatorFactoryBean locatorFactory(StandardBeanFactory beans) {
    var factory = new ServiceLocatorFactoryBean();
    factory.setBeanFactory(beans);
    factory.setServiceLocatorInterface(Locator.class);
    factory.afterPropertiesSet();
    return factory;
  }

  interface Locator {

    String lookup();

    String lookup(Object id);

    void invalid();

    String invalid(String one, String two);

  }

  @SuppressWarnings("serial")
  static class LookupFailure extends RuntimeException {

    public LookupFailure(Throwable cause) {
      super(cause);
    }

  }

}
