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

package infra.beans.factory.aot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import infra.beans.factory.UnsatisfiedDependencyException;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AutowiredFieldValueResolver}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class AutowiredFieldValueResolverTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  @Test
  void forFieldWhenFieldNameIsEmptyThrowsException() {
    String message = "'fieldName' must not be empty";
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredFieldValueResolver.forField(null))
            .withMessage(message);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredFieldValueResolver.forField(""))
            .withMessage(message);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredFieldValueResolver.forRequiredField(null))
            .withMessage(message);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredFieldValueResolver.forRequiredField(" "))
            .withMessage(message);
  }

  @Test
  void resolveWhenRegisteredBeanIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    AutowiredFieldValueResolver.forField("string").resolve(null))
            .withMessage("'registeredBean' is required");
  }

  @Test
  void resolveWhenFieldIsMissingThrowsException() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredFieldValueResolver.forField("missing")
                    .resolve(registeredBean))
            .withMessage("No field 'missing' found on " + TestBean.class.getName());
  }

  @Test
  void resolveReturnsValue() {
    this.beanFactory.registerSingleton("one", "1");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    Object resolved = AutowiredFieldValueResolver.forField("string")
            .resolve(registeredBean);
    assertThat(resolved).isEqualTo("1");
  }

  @Test
  void resolveWhenRequiredFieldAndBeanReturnsValue() {
    this.beanFactory.registerSingleton("one", "1");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    Object resolved = AutowiredFieldValueResolver.forRequiredField("string")
            .resolve(registeredBean);
    assertThat(resolved).isEqualTo("1");
  }

  @Test
  void resolveWhenRequiredFieldAndNoBeanReturnsNull() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    Object resolved = AutowiredFieldValueResolver.forField("string")
            .resolve(registeredBean);
    assertThat(resolved).isNull();
  }

  @Test
  void resolveWhenRequiredFieldAndNoBeanThrowsException() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredFieldValueResolver resolver = AutowiredFieldValueResolver
            .forRequiredField("string");
    assertThatExceptionOfType(UnsatisfiedDependencyException.class)
            .isThrownBy(() -> resolver.resolve(registeredBean)).satisfies(ex -> {
              assertThat(ex.getBeanName()).isEqualTo("testBean");
              assertThat(ex.getInjectionPoint()).isNotNull();
              assertThat(ex.getInjectionPoint().getField().getName())
                      .isEqualTo("string");
            });
  }

  @Test
  void resolveAndSetWhenInstanceIsNullThrowsException() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredFieldValueResolver.forField("string")
                    .resolveAndSet(registeredBean, null))
            .withMessage("'instance' is required");
  }

  @Test
  void resolveAndSetSetsValue() {
    this.beanFactory.registerSingleton("one", "1");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    TestBean testBean = new TestBean();
    AutowiredFieldValueResolver.forField("string").resolveAndSet(registeredBean,
            testBean);
    assertThat(testBean).extracting("string").isEqualTo("1");
  }

  @Test
  void resolveWithActionWhenActionIsNullThrowsException() {
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> AutowiredFieldValueResolver.forField("string")
                    .resolve(registeredBean, (ThrowingConsumer<Object>) null))
            .withMessage("'action' is required");
  }

  @Test
  void resolveWithActionCallsAction() {
    this.beanFactory.registerSingleton("one", "1");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    List<Object> result = new ArrayList<>();
    AutowiredFieldValueResolver.forField("string").resolve(registeredBean,
            result::add);
    assertThat(result).containsExactly("1");
  }

  @Test
  void resolveWithActionWhenDeducedGenericCallsAction() {
    this.beanFactory.registerSingleton("one", "1");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    TestBean testBean = new TestBean();
    testBean.string = AutowiredFieldValueResolver.forField("string")
            .resolve(registeredBean);
  }

  @Test
  void resolveObjectWhenUsingShortcutInjectsDirectly() {
    StandardBeanFactory beanFactory = new StandardBeanFactory() {

      @Override
      protected Map<String, Object> findAutowireCandidates(String beanName,
              Class<?> requiredType, DependencyDescriptor descriptor) {
        throw new AssertionError("Should be shortcut");
      }

    };
    beanFactory.registerSingleton("one", "1");
    RegisteredBean registeredBean = registerTestBean(beanFactory);
    AutowiredFieldValueResolver resolver = AutowiredFieldValueResolver
            .forField("string");
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> resolver.resolve(registeredBean));
    assertThat(resolver.withShortcut("one").resolveObject(registeredBean))
            .isEqualTo("1");
  }

  @Test
  void resolveRegistersDependantBeans() {
    this.beanFactory.registerSingleton("one", "1");
    RegisteredBean registeredBean = registerTestBean(this.beanFactory);
    AutowiredFieldValueResolver.forField("string").resolve(registeredBean);
    assertThat(this.beanFactory.getDependentBeans("one")).containsExactly("testBean");
  }

  private RegisteredBean registerTestBean(StandardBeanFactory beanFactory) {
    beanFactory.registerBeanDefinition("testBean",
            new RootBeanDefinition(TestBean.class));
    return RegisteredBean.of(beanFactory, "testBean");
  }

  static class TestBean {

    String string;

  }

}
