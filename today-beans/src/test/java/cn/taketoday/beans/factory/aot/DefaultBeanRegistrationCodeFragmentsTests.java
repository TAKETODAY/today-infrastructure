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

package cn.taketoday.beans.factory.aot;

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.InjectAnnotationBeanPostProcessorTests.StringFactoryBean;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.factory.DummyFactory;
import cn.taketoday.beans.testfixture.beans.factory.aot.GenericFactoryBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.MockBeanRegistrationsCode;
import cn.taketoday.beans.testfixture.beans.factory.aot.NumberFactoryBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBeanConfiguration;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBeanFactoryBean;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.util.ReflectionUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultBeanRegistrationCodeFragments}.
 *
 * @author Stephane Nicoll
 */
class DefaultBeanRegistrationCodeFragmentsTests {

  private final BeanRegistrationsCode beanRegistrationsCode = new MockBeanRegistrationsCode(new TestGenerationContext());

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @Test
  void getTargetOnConstructor() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean,
            SimpleBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToPublicFactoryBean() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean,
            SimpleBeanFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToPublicGenericFactoryBeanExtractTargetFromFactoryBeanType() {
    RegisteredBean registeredBean = registerTestBean(ResolvableType
            .forClassWithGenerics(GenericFactoryBean.class, SimpleBean.class));
    assertTarget(createInstance(registeredBean).getTarget(registeredBean,
            GenericFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToPublicGenericFactoryBeanWithBoundExtractTargetFromFactoryBeanType() {
    RegisteredBean registeredBean = registerTestBean(ResolvableType
            .forClassWithGenerics(NumberFactoryBean.class, Integer.class));
    assertTarget(createInstance(registeredBean).getTarget(registeredBean,
            NumberFactoryBean.class.getDeclaredConstructors()[0]), Integer.class);
  }

  @Test
  void getTargetOnConstructorToPublicGenericFactoryBeanUseBeanTypeAsFallback() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean,
            GenericFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToProtectedFactoryBean() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean,
                    PrivilegedTestBeanFactoryBean.class.getDeclaredConstructors()[0]),
            PrivilegedTestBeanFactoryBean.class);
  }

  @Test
  void getTargetOnMethod() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    Method method = ReflectionUtils.findMethod(SimpleBeanConfiguration.class, "simpleBean");
    assertThat(method).isNotNull();
    assertTarget(createInstance(registeredBean).getTarget(registeredBean, method),
            SimpleBeanConfiguration.class);
  }

  @Test
  void getTargetOnMethodWithInnerBeanInJavaPackage() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            new RootBeanDefinition(String.class));
    Method method = ReflectionUtils.findMethod(getClass(), "createString");
    assertThat(method).isNotNull();
    assertTarget(createInstance(innerBean).getTarget(innerBean, method), getClass());
  }

  @Test
  void getTargetOnConstructorWithInnerBeanInJavaPackage() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean", new RootBeanDefinition(String.class));
    assertTarget(createInstance(innerBean).getTarget(innerBean,
            String.class.getDeclaredConstructors()[0]), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorWithInnerBeanOnTypeInJavaPackage() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            new RootBeanDefinition(StringFactoryBean.class));
    assertTarget(createInstance(innerBean).getTarget(innerBean,
            StringFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
  }

  @Test
  void getTargetOnMethodWithInnerBeanInRegularPackage() {
    RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            new RootBeanDefinition(SimpleBean.class));
    Method method = ReflectionUtils.findMethod(SimpleBeanConfiguration.class, "simpleBean");
    assertThat(method).isNotNull();
    assertTarget(createInstance(innerBean).getTarget(innerBean, method),
            SimpleBeanConfiguration.class);
  }

  @Test
  void getTargetOnConstructorWithInnerBeanInRegularPackage() {
    RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            new RootBeanDefinition(SimpleBean.class));
    assertTarget(createInstance(innerBean).getTarget(innerBean,
            SimpleBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorWithInnerBeanOnFactoryBeanOnTypeInRegularPackage() {
    RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            new RootBeanDefinition(SimpleBean.class));
    assertTarget(createInstance(innerBean).getTarget(innerBean,
            SimpleBeanFactoryBean.class.getDeclaredConstructors()[0]), SimpleBean.class);
  }

  private void assertTarget(ClassName target, Class<?> expected) {
    assertThat(target).isEqualTo(ClassName.get(expected));
  }

  private RegisteredBean registerTestBean(Class<?> beanType) {
    this.beanFactory.registerBeanDefinition("testBean",
            new RootBeanDefinition(beanType));
    return RegisteredBean.of(this.beanFactory, "testBean");
  }

  private RegisteredBean registerTestBean(ResolvableType beanType) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setTargetType(beanType);
    this.beanFactory.registerBeanDefinition("testBean", beanDefinition);
    return RegisteredBean.of(this.beanFactory, "testBean");
  }

  private BeanRegistrationCodeFragments createInstance(RegisteredBean registeredBean) {
    return new DefaultBeanRegistrationCodeFragments(this.beanRegistrationsCode, registeredBean,
            new BeanDefinitionMethodGeneratorFactory(this.beanFactory));
  }

  @SuppressWarnings("unused")
  static String createString() {
    return "Test";
  }

  static class PrivilegedTestBeanFactoryBean implements FactoryBean<SimpleBean> {

    @Override
    public SimpleBean getObject() throws Exception {
      return new SimpleBean();
    }

    @Override
    public Class<?> getObjectType() {
      return SimpleBean.class;
    }
  }

}
