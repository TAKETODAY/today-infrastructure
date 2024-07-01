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

package cn.taketoday.beans.factory.aot;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.function.UnaryOperator;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.InjectAnnotationBeanPostProcessorTests.StringFactoryBean;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.factory.DummyFactory;
import cn.taketoday.beans.testfixture.beans.factory.aot.DefaultSimpleBeanContract;
import cn.taketoday.beans.testfixture.beans.factory.aot.GenericFactoryBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.MockBeanRegistrationCode;
import cn.taketoday.beans.testfixture.beans.factory.aot.MockBeanRegistrationsCode;
import cn.taketoday.beans.testfixture.beans.factory.aot.NumberFactoryBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBeanArrayFactoryBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBeanConfiguration;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBeanContract;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBeanFactoryBean;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultBeanRegistrationCodeFragments}.
 *
 * @author Stephane Nicoll
 */
class DefaultBeanRegistrationCodeFragmentsTests {

  private final BeanRegistrationsCode beanRegistrationsCode = new MockBeanRegistrationsCode(new TestGenerationContext());

  private final GenerationContext generationContext = new TestGenerationContext();

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @Test
  public void getTargetWithInstanceSupplier() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleBean.class);
    beanDefinition.setInstanceSupplier(SimpleBean::new);
    RegisteredBean registeredBean = registerTestBean(beanDefinition);
    BeanRegistrationCodeFragments codeFragments = createInstance(registeredBean);
    assertThatExceptionOfType(AotBeanProcessingException.class)
            .isThrownBy(() -> codeFragments.getTarget(registeredBean))
            .withMessageContaining("Error processing bean with name 'testBean': instance supplier is not supported");
  }

  @Test
  public void getTargetWithInstanceSupplierAndResourceDescription() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleBean.class);
    beanDefinition.setInstanceSupplier(SimpleBean::new);
    beanDefinition.setResourceDescription("my test resource");
    RegisteredBean registeredBean = registerTestBean(beanDefinition);
    BeanRegistrationCodeFragments codeFragments = createInstance(registeredBean);
    assertThatExceptionOfType(AotBeanProcessingException.class)
            .isThrownBy(() -> codeFragments.getTarget(registeredBean))
            .withMessageContaining("Error processing bean with name 'testBean' defined in my test resource: "
                    + "instance supplier is not supported");
  }

  @Test
  void getTargetOnConstructor() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class,
            SimpleBean.class.getDeclaredConstructors()[0]);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToPublicFactoryBean() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class,
            SimpleBeanFactoryBean.class.getDeclaredConstructors()[0]);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToPublicFactoryBeanProducingArray() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean[].class,
            SimpleBeanArrayFactoryBean.class.getDeclaredConstructors()[0]);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToPublicGenericFactoryBeanExtractTargetFromFactoryBeanType() {
    ResolvableType beanType = ResolvableType.forClassWithGenerics(
            GenericFactoryBean.class, SimpleBean.class);
    RegisteredBean registeredBean = registerTestBean(beanType,
            GenericFactoryBean.class.getDeclaredConstructors()[0]);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToPublicGenericFactoryBeanWithBoundExtractTargetFromFactoryBeanType() {
    ResolvableType beanType = ResolvableType.forClassWithGenerics(
            NumberFactoryBean.class, Integer.class);
    RegisteredBean registeredBean = registerTestBean(beanType,
            NumberFactoryBean.class.getDeclaredConstructors()[0]);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean), Integer.class);
  }

  @Test
  void getTargetOnConstructorToPublicGenericFactoryBeanUseBeanTypeAsFallback() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class,
            GenericFactoryBean.class.getDeclaredConstructors()[0]);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorToProtectedFactoryBean() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class,
            PrivilegedTestBeanFactoryBean.class.getDeclaredConstructors()[0]);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean),
            PrivilegedTestBeanFactoryBean.class);
  }

  @Test
  void getTargetOnMethod() {
    Method method = ReflectionUtils.findMethod(SimpleBeanConfiguration.class, "simpleBean");
    assertThat(method).isNotNull();
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class, method);
    assertTarget(createInstance(registeredBean).getTarget(registeredBean),
            SimpleBeanConfiguration.class);
  }

  @Test
  void getTargetOnMethodFromInterface() {
    this.beanFactory.registerBeanDefinition("configuration",
            new RootBeanDefinition(DefaultSimpleBeanContract.class));
    Method method = ReflectionUtils.findMethod(SimpleBeanContract.class, "simpleBean");
    assertThat(method).isNotNull();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleBean.class);
    applyConstructorOrFactoryMethod(beanDefinition, method);
    beanDefinition.setFactoryBeanName("configuration");
    this.beanFactory.registerBeanDefinition("testBean", beanDefinition);
    RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, "testBean");
    assertTarget(createInstance(registeredBean).getTarget(registeredBean),
            DefaultSimpleBeanContract.class);
  }

  @Test
  void getTargetOnMethodWithInnerBeanInJavaPackage() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    Method method = ReflectionUtils.findMethod(getClass(), "createString");
    assertThat(method).isNotNull();
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            applyConstructorOrFactoryMethod(new RootBeanDefinition(String.class), method));
    assertTarget(createInstance(innerBean).getTarget(innerBean), getClass());
  }

  @Test
  void getTargetOnConstructorWithInnerBeanInJavaPackage() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    RootBeanDefinition innerBeanDefinition = applyConstructorOrFactoryMethod(
            new RootBeanDefinition(String.class), String.class.getDeclaredConstructors()[0]);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            innerBeanDefinition);
    assertTarget(createInstance(innerBean).getTarget(innerBean), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorWithInnerBeanOnTypeInJavaPackage() {
    RegisteredBean registeredBean = registerTestBean(SimpleBean.class);
    RootBeanDefinition innerBeanDefinition = applyConstructorOrFactoryMethod(
            new RootBeanDefinition(StringFactoryBean.class),
            StringFactoryBean.class.getDeclaredConstructors()[0]);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            innerBeanDefinition);
    assertTarget(createInstance(innerBean).getTarget(innerBean), SimpleBean.class);
  }

  @Test
  void getTargetOnMethodWithInnerBeanInRegularPackage() {
    RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
    Method method = ReflectionUtils.findMethod(SimpleBeanConfiguration.class, "simpleBean");
    assertThat(method).isNotNull();
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            applyConstructorOrFactoryMethod(new RootBeanDefinition(SimpleBean.class), method));
    assertTarget(createInstance(innerBean).getTarget(innerBean),
            SimpleBeanConfiguration.class);
  }

  @Test
  void getTargetOnConstructorWithInnerBeanInRegularPackage() {
    RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
    RootBeanDefinition innerBeanDefinition = applyConstructorOrFactoryMethod(
            new RootBeanDefinition(SimpleBean.class), SimpleBean.class.getDeclaredConstructors()[0]);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            innerBeanDefinition);
    assertTarget(createInstance(innerBean).getTarget(innerBean), SimpleBean.class);
  }

  @Test
  void getTargetOnConstructorWithInnerBeanOnFactoryBeanOnTypeInRegularPackage() {
    RegisteredBean registeredBean = registerTestBean(DummyFactory.class);
    RootBeanDefinition innerBeanDefinition = applyConstructorOrFactoryMethod(
            new RootBeanDefinition(SimpleBean.class),
            SimpleBeanFactoryBean.class.getDeclaredConstructors()[0]);
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(registeredBean, "innerTestBean",
            innerBeanDefinition);
    assertTarget(createInstance(innerBean).getTarget(innerBean), SimpleBean.class);
  }

  @Test
  void customizedGetTargetDoesNotResolveInstantiationDescriptor() {
    RegisteredBean registeredBean = spy(registerTestBean(SimpleBean.class));
    BeanRegistrationCodeFragments customCodeFragments = createCustomCodeFragments(registeredBean, codeFragments -> new BeanRegistrationCodeFragmentsDecorator(codeFragments) {
      @Override
      public ClassName getTarget(RegisteredBean registeredBean) {
        return ClassName.get(String.class);
      }
    });
    assertTarget(customCodeFragments.getTarget(registeredBean), String.class);
    verify(registeredBean, never()).resolveInstantiationDescriptor();
  }

  @Test
  void customizedGenerateInstanceSupplierCodeDoesNotResolveInstantiationDescriptor() {
    RegisteredBean registeredBean = spy(registerTestBean(SimpleBean.class));
    BeanRegistrationCodeFragments customCodeFragments = createCustomCodeFragments(registeredBean, codeFragments -> new BeanRegistrationCodeFragmentsDecorator(codeFragments) {
      @Override
      public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
              BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
        return CodeBlock.of("// Hello");
      }
    });
    assertThat(customCodeFragments.generateInstanceSupplierCode(this.generationContext,
            new MockBeanRegistrationCode(this.generationContext), false)).hasToString("// Hello");
    verify(registeredBean, never()).resolveInstantiationDescriptor();
  }

  private BeanRegistrationCodeFragments createCustomCodeFragments(RegisteredBean registeredBean, UnaryOperator<BeanRegistrationCodeFragments> customFragments) {
    BeanRegistrationAotContribution aotContribution = BeanRegistrationAotContribution.
            withCustomCodeFragments(customFragments);
    BeanRegistrationCodeFragments defaultCodeFragments = createInstance(registeredBean);
    return aotContribution.customizeBeanRegistrationCodeFragments(
            this.generationContext, defaultCodeFragments);
  }

  private void assertTarget(ClassName target, Class<?> expected) {
    assertThat(target).isEqualTo(ClassName.get(expected));
  }

  private RegisteredBean registerTestBean(Class<?> beanType) {
    return registerTestBean(beanType, null);
  }

  private RegisteredBean registerTestBean(Class<?> beanType,
          @Nullable Executable constructorOrFactoryMethod) {
    this.beanFactory.registerBeanDefinition("testBean", applyConstructorOrFactoryMethod(
            new RootBeanDefinition(beanType), constructorOrFactoryMethod));
    return RegisteredBean.of(this.beanFactory, "testBean");
  }

  private RegisteredBean registerTestBean(ResolvableType beanType,
          @Nullable Executable constructorOrFactoryMethod) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setTargetType(beanType);
    return registerTestBean(applyConstructorOrFactoryMethod(
            beanDefinition, constructorOrFactoryMethod));
  }

  private RegisteredBean registerTestBean(RootBeanDefinition beanDefinition) {
    this.beanFactory.registerBeanDefinition("testBean", beanDefinition);
    return RegisteredBean.of(this.beanFactory, "testBean");
  }

  private RootBeanDefinition applyConstructorOrFactoryMethod(RootBeanDefinition beanDefinition,
          @Nullable Executable constructorOrFactoryMethod) {

    if (constructorOrFactoryMethod instanceof Method method) {
      beanDefinition.setResolvedFactoryMethod(method);
    }
    else if (constructorOrFactoryMethod instanceof Constructor<?> constructor) {
      beanDefinition.setAttribute(RootBeanDefinition.PREFERRED_CONSTRUCTORS_ATTRIBUTE, constructor);
    }
    return beanDefinition;
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
    public SimpleBean getObject() {
      return new SimpleBean();
    }

    @Override
    public Class<?> getObjectType() {
      return SimpleBean.class;
    }
  }

}
