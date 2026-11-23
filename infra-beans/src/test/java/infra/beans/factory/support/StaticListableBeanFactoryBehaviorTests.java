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

import java.util.List;
import java.util.Map;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanIsNotAFactoryException;
import infra.beans.factory.BeanNotOfRequiredTypeException;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.SmartFactoryBean;
import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class StaticListableBeanFactoryBehaviorTests {

  @Test
  void exposesRegisteredBeansAndMetadata() {
    StaticListableBeanFactory factory = new StaticListableBeanFactory();
    Product product = new Product("one");
    factory.addBean("product", product);

    assertThat(factory.getBean("product")).isSameAs(product);
    assertThat(factory.getBean(Product.class)).isSameAs(product);
    assertThat(factory.getBean("product", Product.class)).isSameAs(product);
    assertThat(factory.getType("product")).isEqualTo(Product.class);
    assertThat(factory.containsBean("product")).isTrue();
    assertThat(factory.containsBeanDefinition("product")).isTrue();
    assertThat(factory.getBeanDefinitionCount()).isEqualTo(1);
    assertThat(factory.getBeanDefinitionNames()).containsExactly("product");
    assertThat(factory.getAliases("product")).isEmpty();
    assertThat(factory.getBeanDefinitions()).isEmpty();
  }

  @Test
  void resolvesFactoryBeanProductsAndFactoryDereferences() {
    StaticListableBeanFactory factory = new StaticListableBeanFactory();
    Product product = new Product("factory-product");
    ProductFactory productFactory = new ProductFactory(product, true);
    factory.addBean("product", productFactory);

    assertThat(factory.getBean("product")).isSameAs(product);
    assertThat(factory.getBean("&product")).isSameAs(productFactory);
    assertThat(factory.getBean("product", Product.class)).isSameAs(product);
    assertThat(factory.getBean("&product", ProductFactory.class)).isSameAs(productFactory);
    assertThat(factory.getType("product")).isEqualTo(Product.class);
    assertThat(factory.getType("&product")).isEqualTo(ProductFactory.class);
    assertThat(factory.isSingleton("product")).isTrue();
    assertThat(factory.isSingleton("&product")).isTrue();
    assertThat(factory.isPrototype("product")).isFalse();
    assertThat(factory.isTypeMatch("product", Product.class)).isTrue();
    assertThat(factory.isTypeMatch("&product", ProductFactory.class)).isTrue();
    assertThat(factory.getBeanNamesForType(Product.class)).containsExactly("product");
    assertThat(factory.getBeanNamesForType(FactoryBean.class)).containsExactly("product");
    assertThat(factory.getBeansOfType(Product.class)).containsEntry("product", product);
    assertThat(factory.getBeansOfType(FactoryBean.class)).containsEntry("&product", productFactory);
  }

  @Test
  void handlesPrototypeSmartFactoryBeansAndProviders() {
    StaticListableBeanFactory factory = new StaticListableBeanFactory();
    ProductFactory productFactory = new ProductFactory(new Product("prototype"), false);
    factory.addBean("product", productFactory);

    assertThat(factory.isSingleton("product")).isFalse();
    assertThat(factory.isPrototype("product")).isTrue();
    assertThat(factory.getBeanNamesForType(Product.class, false, true)).isEmpty();
    assertThat(factory.getBeanNamesForType(Product.class, true, true)).containsExactly("product");

    ObjectProvider<Product> provider = factory.getBeanProvider(Product.class);
    assertThat(provider.get()).isEqualTo(productFactory.product);
    assertThat(provider.getIfAvailable()).isEqualTo(productFactory.product);
    assertThat(provider.getIfUnique()).isEqualTo(productFactory.product);
    assertThat(provider.stream()).containsExactly(productFactory.product);
  }

  @Test
  void reportsLookupFailuresAndUnsupportedOperations() {
    StaticListableBeanFactory factory = new StaticListableBeanFactory();
    Product product = new Product("one");
    factory.addBean("one", product);
    factory.addBean("two", new Product("two"));

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> factory.getBean("missing"));
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() -> factory.getBean(Integer.class));
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(() -> factory.getBean(Product.class));
    assertThatExceptionOfType(BeanNotOfRequiredTypeException.class)
            .isThrownBy(() -> factory.getBean("one", String.class));
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> factory.getBean("one", new Object()));
    assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> factory.getBean(Product.class, new Object()));
    assertThatExceptionOfType(BeanIsNotAFactoryException.class).isThrownBy(() -> factory.getBean("&one"));
    assertThatExceptionOfType(BeanNotOfRequiredTypeException.class)
            .isThrownBy(() -> factory.getBean("one", new ParameterizedTypeReference<List<String>>() { }));
    assertThat(factory.getBeanProvider(String.class).getIfAvailable()).isNull();
    assertThat(factory.getBeanProvider(Product.class).getIfUnique()).isNull();
    assertThat(product).isNotNull();
  }

  @Test
  void rejectsNullFactoryProductsAndSupportsResolvableTypes() {
    StaticListableBeanFactory factory = new StaticListableBeanFactory();
    factory.addBean("nullProduct", new NullProductFactory());

    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> factory.getBean("nullProduct"));
    assertThat(factory.getBeanNamesForType(ResolvableType.forClass(Product.class))).containsExactly("nullProduct");
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> factory.getBeansOfType(ResolvableType.forClass(Product.class), true, true));
  }

  static class Product {

    final String name;

    Product(String name) {
      this.name = name;
    }
  }

  static class ProductFactory implements SmartFactoryBean<Product> {

    final Product product;

    private final boolean singleton;

    ProductFactory(Product product, boolean singleton) {
      this.product = product;
      this.singleton = singleton;
    }

    @Override
    public Product getObject() {
      return product;
    }

    @Override
    public Class<?> getObjectType() {
      return Product.class;
    }

    @Override
    public boolean isSingleton() {
      return singleton;
    }

    @Override
    public boolean isPrototype() {
      return !singleton;
    }
  }

  static class NullProductFactory implements FactoryBean<Product> {

    @Override
    public Product getObject() {
      return null;
    }

    @Override
    public Class<?> getObjectType() {
      return Product.class;
    }
  }

}
