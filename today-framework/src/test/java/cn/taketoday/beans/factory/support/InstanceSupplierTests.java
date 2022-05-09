/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import cn.taketoday.util.function.ThrowingBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.*;

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
    assertThatIllegalStateException().isThrownBy(() -> supplier.get())
            .withMessage("No RegisteredBean parameter provided");
  }

  @Test
  void getWithExceptionWithoutRegisteredBeanThrowsException() {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    assertThatIllegalStateException().isThrownBy(() -> supplier.getWithException())
            .withMessage("No RegisteredBean parameter provided");
  }

  @Test
  void getReturnsResult() throws Exception {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    assertThat(supplier.get(this.registeredBean)).isEqualTo("test");
  }

  @Test
  void andThenWithBiFunctionWhenFunctionIsNullThrowsException() {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    ThrowingBiFunction<RegisteredBean, String, String> after = null;
    assertThatIllegalArgumentException().isThrownBy(() -> supplier.andThen(after))
            .withMessage("After must not be null");
  }

  @Test
  void andThenWithBiFunctionAppliesFunctionToObtainResult() throws Exception {
    InstanceSupplier<String> supplier = registeredBean -> "bean";
    supplier = supplier.andThen(
            (registeredBean, string) -> registeredBean.getBeanName() + "-" + string);
    assertThat(supplier.get(this.registeredBean)).isEqualTo("test-bean");
  }

  @Test
  void ofSupplierWhenInstanceSupplierReturnsSameInstance() {
    InstanceSupplier<String> supplier = registeredBean -> "test";
    assertThat(InstanceSupplier.of(supplier)).isSameAs(supplier);
  }

  @Test
  void usingSupplierAdaptsToInstanceSupplier() throws Exception {
    InstanceSupplier<String> instanceSupplier = InstanceSupplier.using(() -> "test");
    assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
  }

  @Test
  void ofInstanceSupplierAdaptsToInstanceSupplier() throws Exception {
    InstanceSupplier<String> instanceSupplier = InstanceSupplier
            .of(registeredBean -> "test");
    assertThat(instanceSupplier.get(this.registeredBean)).isEqualTo("test");
  }

}