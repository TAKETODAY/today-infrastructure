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

package infra.util;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.core.Ordered;
import infra.core.OverridingClassLoader;
import infra.core.annotation.Order;
import infra.util.Instantiator.FailureHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/18 10:55
 */
class InstantiatorTests {

  private final ParamA paramA = new ParamA();

  private final ParamB paramB = new ParamB();

  private ParamC paramC;

  @Test
  void instantiateWhenOnlyDefaultConstructorCreatesInstance() {
    WithDefaultConstructor instance = createInstance(WithDefaultConstructor.class);
    assertThat(instance).isInstanceOf(WithDefaultConstructor.class);
  }

  @Test
  void instantiateWhenMultipleConstructorPicksMostArguments() {
    WithMultipleConstructors instance = createInstance(WithMultipleConstructors.class);
    assertThat(instance).isInstanceOf(WithMultipleConstructors.class);
  }

  @Test
  void instantiateWhenAdditionalConstructorPicksMostSuitable() {
    WithAdditionalConstructor instance = createInstance(WithAdditionalConstructor.class);
    assertThat(instance).isInstanceOf(WithAdditionalConstructor.class);
  }

  @Test
  void instantiateOrdersInstances() {
    List<Object> instances = createInstantiator(Object.class).instantiate(
            Arrays.asList(WithMultipleConstructors.class.getName(), WithAdditionalConstructor.class.getName()));
    assertThat(instances).hasSize(2);
    assertThat(instances.get(0)).isInstanceOf(WithAdditionalConstructor.class);
    assertThat(instances.get(1)).isInstanceOf(WithMultipleConstructors.class);

    Object e = new Object();
    instances.add(e);
    assertThat(instances).hasSize(3);
    assertThat(instances.get(2)).isSameAs(e);
  }

  @Test
  void instantiateWithFactory() {
    assertThat(this.paramC).isNull();
    WithFactory instance = createInstance(WithFactory.class);
    assertThat(instance.getParamC()).isEqualTo(this.paramC);
  }

  @Test
  void instantiateTypesCreatesInstance() {
    WithDefaultConstructor instance = createInstantiator(WithDefaultConstructor.class)
            .instantiateTypes(Collections.singleton(WithDefaultConstructor.class))
            .get(0);
    assertThat(instance).isInstanceOf(WithDefaultConstructor.class);
  }

  @Test
  void instantiateWithClassLoaderCreatesInstance() {
    OverridingClassLoader classLoader = new OverridingClassLoader(getClass().getClassLoader()) {

      @Override
      protected boolean isEligibleForOverriding(String className) {
        return super.isEligibleForOverriding(className)
                && className.equals(WithDefaultConstructorSubclass.class.getName());
      }

    };
    WithDefaultConstructor instance = createInstantiator(WithDefaultConstructor.class)
            .instantiate(classLoader, Collections.singleton(WithDefaultConstructorSubclass.class.getName()))
            .get(0);
    assertThat(instance.getClass().getClassLoader()).isSameAs(classLoader);
  }

  @Test
  void createWhenWrongTypeThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> createInstantiator(WithDefaultConstructor.class)
                    .instantiate(Collections.singleton(WithAdditionalConstructor.class.getName())))
            .withMessageContaining("Unable to instantiate");
  }

  @Test
  void createWithFailureHandlerInvokesFailureHandler() {
    assertThatIllegalStateException()
            .isThrownBy(() -> new Instantiator<>(WithDefaultConstructor.class, (availableParameters) -> {
            }, new CustomFailureHandler())
                    .instantiate(Collections.singleton(WithAdditionalConstructor.class.getName())))
            .withMessageContaining("custom failure handler message");
  }

  @Test
  void instantiateWithSingleNameCreatesInstance() {
    WithDefaultConstructor instance = createInstantiator(WithDefaultConstructor.class)
            .instantiate(WithDefaultConstructor.class.getName());
    assertThat(instance).isInstanceOf(WithDefaultConstructor.class);
  }

  @Test
  void getArgReturnsArg() {
    Instantiator<?> instantiator = createInstantiator(WithMultipleConstructors.class);
    assertThat(instantiator.getArg(ParamA.class)).isSameAs(this.paramA);
    assertThat(instantiator.getArg(ParamB.class)).isSameAs(this.paramB);
    assertThat(instantiator.getArg(ParamC.class)).isInstanceOf(ParamC.class);
  }

  @Test
  void getArgWhenUnknownThrowsException() {
    Instantiator<?> instantiator = createInstantiator(WithMultipleConstructors.class);
    assertThatIllegalArgumentException().isThrownBy(() -> instantiator.getArg(InputStream.class))
            .withMessageStartingWith("Unknown argument type");
  }

  private <T> T createInstance(Class<T> type) {
    return createInstantiator(type).instantiate(type.getName());
  }

  private <T> Instantiator<T> createInstantiator(Class<T> type) {
    return new Instantiator<>(type, (availableParameters) -> {
      availableParameters.add(ParamA.class, this.paramA);
      availableParameters.add(ParamB.class, this.paramB);
      availableParameters.add(ParamC.class, ParamC::new);
    });
  }

  static class WithDefaultConstructorSubclass extends WithDefaultConstructor {

  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  static class WithMultipleConstructors {

    WithMultipleConstructors() {
      throw new IllegalStateException();
    }

    WithMultipleConstructors(ParamA paramA) {
      throw new IllegalStateException();
    }

    WithMultipleConstructors(ParamA paramA, ParamB paramB) {
    }

  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class WithAdditionalConstructor {

    WithAdditionalConstructor(ParamA paramA, ParamB paramB) {
    }

    WithAdditionalConstructor(ParamA paramA, ParamB paramB, String extra) {
      throw new IllegalStateException();
    }

  }

  static class WithFactory {

    private final ParamC paramC;

    WithFactory(ParamC paramC) {
      this.paramC = paramC;
    }

    ParamC getParamC() {
      return this.paramC;
    }

  }

  class ParamA {

  }

  class ParamB {

  }

  class ParamC {

    ParamC(Class<?> type) {
      InstantiatorTests.this.paramC = this;
    }

  }

  class CustomFailureHandler implements FailureHandler {

    @Override
    public void handleFailure(Class<?> type, String implementationName, Throwable failure) {
      throw new IllegalStateException("custom failure handler message");
    }

  }

}
