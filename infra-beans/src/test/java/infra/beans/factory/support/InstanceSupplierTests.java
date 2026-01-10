/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.util.function.ThrowingBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/9 17:27
 */
class InstanceSupplierTests {

  private final RegisteredBean registeredBean = RegisteredBean
          .of(new StandardBeanFactory(), "test");

  @Test
  void getWithoutRegisteredBeanThrowsException() {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    assertThatIllegalStateException().isThrownBy(supplier::get)
            .withMessage("No RegisteredBean parameter provided");
  }

  @Test
  void getWithExceptionWithoutRegisteredBeanThrowsException() {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    assertThatIllegalStateException().isThrownBy(supplier::getWithException)
            .withMessage("No RegisteredBean parameter provided");
  }

  @Test
  void getReturnsResult() throws Throwable {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    assertThat(supplier.get(this.registeredBean)).isEqualTo("test");
  }

  @Test
  void andThenWhenFunctionIsNullThrowsException() {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    ThrowingBiFunction<RegisteredBean, String, String> after = null;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.andThen(after))
            .withMessage("'after' function is required");
  }

  @Test
  void andThenAppliesFunctionToObtainResult() throws Throwable {
    InstanceSupplier<String> supplier = registeredBean -> "bean";
    supplier = supplier.andThen(
            (registeredBean, string) -> registeredBean.getBeanName() + "-" + string);
    assertThat(supplier.get(this.registeredBean)).isEqualTo("test-bean");
  }

  @Test
  void andThenWhenInstanceSupplierHasFactoryMethod() throws Throwable {
    Method factoryMethod = getClass().getDeclaredMethod("andThenWhenInstanceSupplierHasFactoryMethod");
    InstanceSupplier<String> supplier = InstanceSupplier.using(factoryMethod, () -> "bean");
    supplier = supplier.andThen(
            (registeredBean, string) -> registeredBean.getBeanName() + "-" + string);
    assertThat(supplier.get(this.registeredBean)).isEqualTo("test-bean");
    assertThat(supplier.getFactoryMethod()).isSameAs(factoryMethod);
  }

  @Test
  void ofSupplierWhenInstanceSupplierReturnsSameInstance() {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    assertThat(InstanceSupplier.of(supplier)).isSameAs(supplier);
  }

  @Test
  void usingSupplierAdaptsToInstanceSupplier() throws Throwable {
    InstanceSupplier<String> instanceSupplier = InstanceSupplier.using(() -> "test");
    assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
  }

  @Test
  void ofInstanceSupplierAdaptsToInstanceSupplier() throws Throwable {
    InstanceSupplier<String> instanceSupplier = InstanceSupplier
            .of(registeredBean -> "test");
    assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
  }

}
