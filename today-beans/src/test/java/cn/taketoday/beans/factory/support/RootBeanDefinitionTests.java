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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/6/9 23:35
 */
class RootBeanDefinitionTests {

  @Test
  void setInstanceSetResolvedFactoryMethod() {
    InstanceSupplier<?> instanceSupplier = mock();
    Method method = ReflectionUtils.findMethod(String.class, "toString");
    given(instanceSupplier.getFactoryMethod()).willReturn(method);
    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    beanDefinition.setInstanceSupplier(instanceSupplier);
    assertThat(beanDefinition.getResolvedFactoryMethod()).isEqualTo(method);
    verify(instanceSupplier).getFactoryMethod();
  }

  @Test
  void setInstanceDoesNotOverrideResolvedFactoryMethodWithNull() {
    InstanceSupplier<?> instanceSupplier = mock();
    given(instanceSupplier.getFactoryMethod()).willReturn(null);
    Method method = ReflectionUtils.findMethod(String.class, "toString");
    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    beanDefinition.setResolvedFactoryMethod(method);
    beanDefinition.setInstanceSupplier(instanceSupplier);
    assertThat(beanDefinition.getResolvedFactoryMethod()).isEqualTo(method);
    verify(instanceSupplier).getFactoryMethod();
  }

  @Test
  void resolveDestroyMethodWithMatchingCandidateReplacedInferredVaue() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(BeanWithCloseMethod.class);
    beanDefinition.setDestroyMethodName(AbstractBeanDefinition.INFER_METHOD);
    beanDefinition.resolveDestroyMethodIfNecessary();
    assertThat(beanDefinition.getDestroyMethodNames()).containsExactly("close");
  }

  @Test
  void resolveDestroyMethodWithNoCandidateSetDestroyMethodNameToNull() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(BeanWithNoDestroyMethod.class);
    beanDefinition.setDestroyMethodName(AbstractBeanDefinition.INFER_METHOD);
    beanDefinition.resolveDestroyMethodIfNecessary();
    assertThat(beanDefinition.getDestroyMethodNames()).isNull();
  }

  @Test
  void resolveDestroyMethodWithNoResolvableType() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setDestroyMethodName(AbstractBeanDefinition.INFER_METHOD);
    beanDefinition.resolveDestroyMethodIfNecessary();
    assertThat(beanDefinition.getDestroyMethodNames()).isNull();
  }

  static class BeanWithCloseMethod {

    public void close() {
    }

  }

  static class BeanWithNoDestroyMethod {

  }

}
