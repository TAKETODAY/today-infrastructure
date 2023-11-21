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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.BeanFactoryPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/9 17:27
 */
class RegisteredBeanTests {

  private StandardBeanFactory beanFactory;

  @BeforeEach
  void setup() {
    this.beanFactory = new StandardBeanFactory();
    this.beanFactory.registerBeanDefinition("bd",
            new RootBeanDefinition(TestBean.class));
    this.beanFactory.registerSingleton("sb", new TestBean());
  }

  @Test
  void ofWhenBeanFactoryIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RegisteredBean.of(null, "bd"))
            .withMessage("'beanFactory' is required");
  }

  @Test
  void ofWhenBeanNameIsEmptyThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RegisteredBean.of(this.beanFactory, null))
            .withMessage("'beanName' must not be empty");
  }

  @Test
  void ofInnerBeanWhenInnerBeanIsNullThrowsException() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    assertThatIllegalArgumentException().isThrownBy(
                    () -> RegisteredBean.ofInnerBean(parent, (BeanDefinitionHolder) null))
            .withMessage("'innerBean' is required");
  }

  @Test
  void ofInnerBeanWhenParentIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RegisteredBean.ofInnerBean(null,
                    new RootBeanDefinition(TestInnerBean.class)))
            .withMessage("'parent' is required");
  }

  @Test
  void ofInnerBeanWhenInnerBeanDefinitionIsNullThrowsException() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> RegisteredBean.ofInnerBean(parent, "ib", null))
            .withMessage("'innerBeanDefinition' is required");
  }

  @Test
  void getBeanNameReturnsBeanName() {
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
    assertThat(registeredBean.getBeanName()).isEqualTo("bd");
  }

  @Test
  void getBeanNameWhenNamedInnerBeanReturnsBeanName() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent, "ib",
            new RootBeanDefinition(TestInnerBean.class));
    assertThat(registeredBean.getBeanName()).isEqualTo("ib");
  }

  @Test
  void getBeanNameWhenUnnamedInnerBeanReturnsBeanName() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
            new RootBeanDefinition(TestInnerBean.class));
    assertThat(registeredBean.getBeanName()).startsWith("(inner bean)#");
  }

  @Test
  void getBeanClassReturnsBeanClass() {
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
    assertThat(registeredBean.getBeanClass()).isEqualTo(TestBean.class);
  }

  @Test
  void getBeanTypeReturnsBeanType() {
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
    assertThat(registeredBean.getBeanType().toClass()).isEqualTo(TestBean.class);
  }

  @Test
  void getBeanTypeWhenHasInstanceBackedByLambdaDoesNotReturnLambdaType() {
    this.beanFactory.registerBeanDefinition("bfpp", new RootBeanDefinition(
            BeanFactoryPostProcessor.class, RegisteredBeanTests::getBeanFactoryPostProcessorLambda));
    this.beanFactory.getBean("bfpp");
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bfpp");
    assertThat(registeredBean.getBeanType().toClass()).isEqualTo(BeanFactoryPostProcessor.class);
  }

  static BeanFactoryPostProcessor getBeanFactoryPostProcessorLambda() {
    return bf -> { };
  }

  @Test
  void getMergedBeanDefinitionReturnsMergedBeanDefinition() {
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
    assertThat(registeredBean.getMergedBeanDefinition().getBeanClass())
            .isEqualTo(TestBean.class);
  }

  @Test
  void getMergedBeanDefinitionWhenSingletonThrowsException() {
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "sb");
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(registeredBean::getMergedBeanDefinition);
  }

  @Test
  void getMergedBeanDefinitionWhenInnerBeanReturnsMergedBeanDefinition() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
            new RootBeanDefinition(TestInnerBean.class));
    assertThat(registeredBean.getMergedBeanDefinition().getBeanClass())
            .isEqualTo(TestInnerBean.class);
  }

  @Test
  void isInnerBeanWhenInnerBeanReturnsTrue() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
            new RootBeanDefinition(TestInnerBean.class));
    assertThat(registeredBean.isInnerBean()).isTrue();
  }

  @Test
  void isInnerBeanWhenNotInnerBeanReturnsTrue() {
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
    assertThat(registeredBean.isInnerBean()).isFalse();
  }

  @Test
  void getParentWhenInnerBeanReturnsParent() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
            new RootBeanDefinition(TestInnerBean.class));
    assertThat(registeredBean.getParent()).isSameAs(parent);
  }

  @Test
  void getParentWhenNotInnerBeanReturnsNull() {
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
    assertThat(registeredBean.getParent()).isNull();
  }

  @Test
  void isGeneratedBeanNameWhenInnerBeanWithoutNameReturnsTrue() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
            new RootBeanDefinition(TestInnerBean.class));
    assertThat(registeredBean.isGeneratedBeanName()).isTrue();
  }

  @Test
  void isGeneratedBeanNameWhenInnerBeanWithNameReturnsFalse() {
    RegisteredBean parent = RegisteredBean.of(this.beanFactory, "bd");
    RegisteredBean registeredBean = RegisteredBean.ofInnerBean(parent,
            new BeanDefinitionHolder(new RootBeanDefinition(TestInnerBean.class),
                    "test"));
    assertThat(registeredBean.isGeneratedBeanName()).isFalse();
  }

  @Test
  void isGeneratedBeanNameWhenNotInnerBeanReturnsFalse() {
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "bd");
    assertThat(registeredBean.isGeneratedBeanName()).isFalse();
  }

  static class TestBean {

  }

  static class TestInnerBean {

  }

}
