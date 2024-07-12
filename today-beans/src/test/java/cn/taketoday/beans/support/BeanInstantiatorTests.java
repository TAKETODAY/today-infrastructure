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

package cn.taketoday.beans.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.reflect.ReflectionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/12 21:47
 */
class BeanInstantiatorTests {

  @Test
  void forConstructor() {
    assertThat(BeanInstantiator.forConstructor(ArrayList.class).instantiate()).isInstanceOf(ArrayList.class);
    assertThat(BeanInstantiator.forConstructor(HashMap.class).instantiate()).isInstanceOf(HashMap.class);
    assertThat(BeanInstantiator.forConstructor(String[].class).instantiate()).isInstanceOf(String[].class);
    assertThat(BeanInstantiator.forConstructor(String[][].class).instantiate()).isInstanceOf(String[][].class);
    assertThat(BeanInstantiator.forConstructor(TestBean.class).instantiate()).isInstanceOf(TestBean.class);
    assertThat(BeanInstantiator.forConstructor(Void.class).instantiate()).isInstanceOf(Void.class);
    assertThat(BeanInstantiator.forConstructor(Void.class).getConstructor()).isNotNull();

    assertThatThrownBy(() -> BeanInstantiator.forConstructor(ConstructorNotFound.class))
            .isInstanceOf(ReflectionException.class)
            .hasMessageEndingWith("has no default constructor");
  }

  @Test
  void forStaticMethod() throws NoSuchMethodException {
    Method createTestBean = StaticFactoryMethods.class.getMethod("createTestBean");
    assertThat(BeanInstantiator.forStaticMethod(createTestBean).instantiate()).isInstanceOf(TestBean.class)
            .isEqualTo(new TestBean("createTestBean"));

    assertThat(BeanInstantiator.forStaticMethod(createTestBean))
            .hasToString("BeanInstantiator for static method: " + createTestBean);
    assertThat(BeanInstantiator.forStaticMethod(createTestBean).getConstructor()).isNull();
  }

  @Test
  void forClass() throws NoSuchMethodException {
    assertThat(BeanInstantiator.forClass(TestBean.class)).isInstanceOf(ConstructorAccessor.class);
    assertThat(BeanInstantiator.forClass(TestBean.class).instantiate()).isEqualTo(new TestBean());
    assertThat(BeanInstantiator.forClass(TestBean.class).getConstructor()).isEqualTo(TestBean.class.getDeclaredConstructor());

    BeanInstantiator instantiator = BeanInstantiator.forClass(ConstructorNotFound.class);
    assertThatThrownBy(instantiator::instantiate)
            .isInstanceOf(BeanInstantiationException.class)
            .hasRootCauseInstanceOf(NullPointerException.class);

    Object instantiate = instantiator.instantiate(new Object[] { 1 });
    assertThat(instantiate).isInstanceOf(ConstructorNotFound.class);
    assertThat(instantiator.getConstructor()).isNotNull();
  }

  @Test
  void forFunction() {
    var instantiator = BeanInstantiator.forFunction(args -> new TestBean());
    assertThat(instantiator.getConstructor()).isNull();
    assertThat(instantiator).isInstanceOf(FunctionInstantiator.class);
    Object instantiate = instantiator.instantiate();
    assertThat(instantiate).isInstanceOf(TestBean.class);

    assertThat(BeanInstantiator.forFunction(args -> new TestBean(String.valueOf(args[0])))
            .instantiate(new Object[] { "forFunction" }))
            .isEqualTo(new TestBean("forFunction"));

    BeanInstantiator constructor = BeanInstantiator.forFunction(objects -> new BeanConstructorTestsBean(1000));

    Object bean = constructor.instantiate();
    assertThat(bean).isInstanceOf(BeanConstructorTestsBean.class);

    BeanConstructorTestsBean testsBean = (BeanConstructorTestsBean) bean;

    assertThat(testsBean).isNotNull();
    assertThat(testsBean.code).isEqualTo(1000);
    assertThat(testsBean).isNotEqualTo(constructor.instantiate());
  }

  @Test
  void forSupplier() {
    var instantiator = BeanInstantiator.forSupplier(TestBean::new);
    assertThat(instantiator.getConstructor()).isNull();
    assertThat(instantiator).isInstanceOf(SupplierInstantiator.class);
    assertThat(instantiator.instantiate()).isInstanceOf(TestBean.class);

    BeanInstantiator constructor =
            BeanInstantiator.forSupplier(() -> new BeanConstructorTestsBean(1000));

    Object bean = constructor.instantiate();
    assertThat(bean).isInstanceOf(BeanConstructorTestsBean.class);

    BeanConstructorTestsBean testsBean = (BeanConstructorTestsBean) bean;

    assertThat(testsBean).isNotNull();
    assertThat(testsBean.code).isEqualTo(1000);
    assertThat(testsBean).isNotEqualTo(constructor.instantiate());
  }

  @Test
  void forSerialization() {
    BeanInstantiator constructor = BeanInstantiator.forSerialization(BeanConstructorTestsBean.class);
    assertThat(constructor.getConstructor()).isNull();

    Object bean = constructor.instantiate(new Object[] { 1 });
    assertThat(bean).isInstanceOf(BeanConstructorTestsBean.class);

    BeanConstructorTestsBean testsBean = (BeanConstructorTestsBean) bean;

    assertThat(testsBean).isNotNull();
    assertThat(testsBean.code).isEqualTo(0);
    assertThat(testsBean).isNotEqualTo(constructor.instantiate());

    assertThat(constructor.toString()).startsWith("BeanInstantiator use serialization constructor: ");
  }

  @Test
  void forReflective() throws NoSuchMethodException {
    Constructor<TestBean> constructor = TestBean.class.getConstructor();
    var instantiator = BeanInstantiator.forReflective(constructor);
    assertThat(instantiator).isInstanceOf(ReflectiveInstantiator.class);
    assertThat(instantiator.instantiate()).isInstanceOf(TestBean.class);
    assertThat(instantiator.getConstructor()).isSameAs(constructor);
  }

  @Test
  void forMethod() throws NoSuchMethodException {
    Method createTestBean = InstanceFactoryMethods.class.getMethod("createTestBean");
    var instantiator = BeanInstantiator.forMethod(createTestBean, new InstanceFactoryMethods());
    assertThat(instantiator).isInstanceOf(MethodAccessorBeanInstantiator.class);
    assertThat(instantiator).hasToString("BeanInstantiator for instance method: %s", createTestBean);

    assertThat(instantiator.instantiate()).isInstanceOf(TestBean.class);
    assertThat(instantiator.instantiate()).isEqualTo(new TestBean("createTestBean"));

    assertThat(BeanInstantiator.forMethod(createTestBean, InstanceFactoryMethods::new)
            .instantiate()).isEqualTo(new TestBean("createTestBean"));
  }

  static class BeanConstructorTestsBean {
    final int code;

    BeanConstructorTestsBean(int code) {
      this.code = code;
    }
  }

  static class InstanceFactoryMethods {

    public TestBean createTestBean() {
      return new TestBean("createTestBean");
    }

  }

  static class StaticFactoryMethods {

    public static TestBean createTestBean() {
      return new TestBean("createTestBean");
    }

  }

  static class ConstructorNotFound {

    ConstructorNotFound(int i) {

    }
  }
}