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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/8/28 20:48
 */
class BeanConstructorTests {

  static class BeanConstructorTestsBean {
    final int code;

    BeanConstructorTestsBean(int code) {
      this.code = code;
    }
  }

  @Test
  void fromFunction() {
    FunctionInstantiator constructor
            = BeanInstantiator.forFunction(objects -> new BeanConstructorTestsBean(1000));

    Object bean = constructor.instantiate();
    assertThat(bean).isInstanceOf(BeanConstructorTestsBean.class);

    BeanConstructorTestsBean testsBean = (BeanConstructorTestsBean) bean;

    assertThat(testsBean).isNotNull();
    assertThat(testsBean.code).isEqualTo(1000);
    assertThat(testsBean).isNotEqualTo(constructor.instantiate());
  }

  @Test
  void fromSupplier() {

    SupplierInstantiator constructor =
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

    Object bean = constructor.instantiate(new Object[] { 1 });
    assertThat(bean).isInstanceOf(BeanConstructorTestsBean.class);

    BeanConstructorTestsBean testsBean = (BeanConstructorTestsBean) bean;

    assertThat(testsBean).isNotNull();
    assertThat(testsBean.code).isEqualTo(0);
    assertThat(testsBean).isNotEqualTo(constructor.instantiate());
  }

}
