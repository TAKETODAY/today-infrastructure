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

import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.env.Environment;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutowiredMethodArgumentsResolver}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AutowiredMethodArgumentsResolverTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @Test
  void forMethodWhenMethodNameIsEmptyThrowsException() {
    String message = "'methodName' must not be empty";
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredMethodArgumentsResolver.forMethod(null))
            .withMessage(message);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredMethodArgumentsResolver.forMethod(""))
            .withMessage(message);
    assertThatIllegalArgumentException()
            .isThrownBy(
                    () -> AutowiredMethodArgumentsResolver.forRequiredMethod(null))
            .withMessage(message);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredMethodArgumentsResolver.forRequiredMethod(" "))
            .withMessage(message);
  }

  @Test
  void resolveWhenRegisteredBeanIsNullThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredMethodArgumentsResolver
                    .forMethod("injectString", String.class).resolve(null))
            .withMessage("'registeredBean' is required");
  }

  @Test
  void resolveWhenMethodIsMissingThrowsException() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver.forMethod("missing", InputStream.class);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> resolver.resolve(registeredBean))
            .withMessage("Method 'missing' with parameter types [java.io.InputStream] declared on %s could not be found.",
                    TestBean.class.getName());
  }

  @Test
  void resolveRequiredWithSingleDependencyReturnsValue() {
    this.beanFactory.registerSingleton("test", "testValue");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
            .forRequiredMethod("injectString", String.class);
    AutowiredArguments resolved = resolver.resolve(registeredBean);
    assertThat(resolved.toArray()).containsExactly("testValue");
  }

  @Test
  void resolveRequiredWhenNoSuchBeanThrowsUnsatisfiedDependencyException() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
            .forRequiredMethod("injectString", String.class);
    assertThatExceptionOfType(UnsatisfiedDependencyException.class)
            .isThrownBy(() -> resolver.resolve(registeredBean)).satisfies(ex -> {
              assertThat(ex.getBeanName()).isEqualTo("testBean");
              assertThat(ex.getInjectionPoint()).isNotNull();
              assertThat(ex.getInjectionPoint().getMember().getName())
                      .isEqualTo("injectString");
            });
  }

  @Test
  void resolveNonRequiredWhenNoSuchBeanReturnsNull() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
            .forMethod("injectString", String.class);
    assertThat(resolver.resolve(registeredBean)).isNull();
  }

  @Test
  void resolveRequiredWithMultipleDependenciesReturnsValue() {
    Environment environment = mock();
    this.beanFactory.registerSingleton("test", "testValue");
    this.beanFactory.registerSingleton("environment", environment);
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
            .forRequiredMethod("injectStringAndEnvironment", String.class,
                    Environment.class);
    AutowiredArguments resolved = resolver.resolve(registeredBean);
    assertThat(resolved.toArray()).containsExactly("testValue", environment);
  }

  @Test
  void resolveAndInvokeWhenInstanceIsNullThrowsException() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredMethodArgumentsResolver
                    .forMethod("injectString", String.class)
                    .resolveAndInvoke(registeredBean, null))
            .withMessage("'instance' is required");
  }

  @Test
  void resolveAndInvokeInvokesMethod() {
    this.beanFactory.registerSingleton("test", "testValue");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
            .forRequiredMethod("injectString", String.class);
    TestBean instance = new TestBean();
    resolver.resolveAndInvoke(registeredBean, instance);
    assertThat(instance.getString()).isEqualTo("testValue");
  }

  @Test
  void resolveWithActionWhenActionIsNullThrowsException() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredMethodArgumentsResolver
                    .forMethod("injectString", String.class)
                    .resolve(registeredBean, null))
            .withMessage("'action' is required");
  }

  @Test
  void resolveWithActionCallsAction() {
    this.beanFactory.registerSingleton("test", "testValue");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    List<Object> result = new ArrayList<>();
    AutowiredMethodArgumentsResolver.forMethod("injectString", String.class)
            .resolve(registeredBean, result::add);
    assertThat(result).hasSize(1);
    assertThat(((AutowiredArguments) result.get(0)).toArray())
            .containsExactly("testValue");
  }

  @Test
  void resolveWhenUsingShortcutsInjectsDirectly() {
    StandardBeanFactory beanFactory = new StandardBeanFactory() {

      @Override
      protected Map<String, Object> findAutowireCandidates(String beanName,
              Class<?> requiredType, DependencyDescriptor descriptor) {
        throw new AssertionError("Should be shortcut");
      }

    };
    beanFactory.registerSingleton("test", "testValue");
    RegisteredBean registeredBean = registerTestBean(beanFactory);
    AutowiredMethodArgumentsResolver resolver = AutowiredMethodArgumentsResolver
            .forRequiredMethod("injectString", String.class);
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> resolver.resolve(registeredBean));
    assertThat(resolver.withShortcut("test").resolve(registeredBean).getObject(0))
            .isEqualTo("testValue");
  }

  @Test
  void resolveRegistersDependantBeans() {
    this.beanFactory.registerSingleton("test", "testValue");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredMethodArgumentsResolver.forMethod("injectString", String.class)
            .resolve(registeredBean);
    assertThat(this.beanFactory.getDependentBeans("test"))
            .containsExactly("testBean");
  }

  private RegisteredBean registerTestBean(StandardBeanFactory beanFactory) {
    beanFactory.registerBeanDefinition("testBean",
            new RootBeanDefinition(TestBean.class));
    return RegisteredBean.of(beanFactory, "testBean");
  }

  @SuppressWarnings("unused")
  static class TestBean {

    private String string;

    void injectString(String string) {
      this.string = string;
    }

    void injectStringAndEnvironment(String string, Environment environment) {
    }

    String getString() {
      return this.string;
    }

  }

}
