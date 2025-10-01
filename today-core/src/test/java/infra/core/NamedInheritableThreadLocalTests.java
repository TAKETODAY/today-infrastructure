/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 18:04
 */
class NamedInheritableThreadLocalTests {

  @Test
  void emptyNameThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new NamedInheritableThreadLocal<>(""))
            .withMessage("Name must not be empty");
  }

  @Test
  void nullNameThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new NamedInheritableThreadLocal<>(null))
            .withMessage("Name must not be empty");
  }

  @Test
  void nameIsReturnedInToString() {
    NamedInheritableThreadLocal<String> local = new NamedInheritableThreadLocal<>("testName");
    assertThat(local.toString()).isEqualTo("testName");
  }

  @Test
  void withInitialReturnsSuppliedValue() {
    NamedInheritableThreadLocal<String> local = NamedInheritableThreadLocal.withInitial("testName", () -> "test");
    assertThat(local.get()).isEqualTo("test");
    assertThat(local.toString()).isEqualTo("testName");
  }

  @Test
  void valueInheritedByChildThread() throws InterruptedException {
    NamedInheritableThreadLocal<String> local = new NamedInheritableThreadLocal<>("testName");
    local.set("parentValue");

    Thread childThread = new Thread(() -> assertThat(local.get()).isEqualTo("parentValue"));
    childThread.start();
    childThread.join();
  }

  @Test
  void withInitialNullSupplierThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> NamedInheritableThreadLocal.withInitial("testName", null));
  }

  @Test
  void multipleThreadsGetIndependentValues() throws InterruptedException {
    NamedInheritableThreadLocal<String> local = new NamedInheritableThreadLocal<>("testName");
    local.set("parentValue");

    Thread thread1 = new Thread(() -> {
      local.set("thread1Value");
      assertThat(local.get()).isEqualTo("thread1Value");
    });

    Thread thread2 = new Thread(() -> {
      local.set("thread2Value");
      assertThat(local.get()).isEqualTo("thread2Value");
    });

    thread1.start();
    thread2.start();
    thread1.join();
    thread2.join();

    assertThat(local.get()).isEqualTo("parentValue");
  }

  @Test
  void removeValueFromThreadLocal() {
    NamedInheritableThreadLocal<String> local = new NamedInheritableThreadLocal<>("testName");
    local.set("testValue");
    assertThat(local.get()).isEqualTo("testValue");

    local.remove();
    assertThat(local.get()).isNull();
  }

  @Test
  void withInitialSupplierReturnsDifferentInstancesPerThread() throws InterruptedException {
    NamedInheritableThreadLocal<Object> local = NamedInheritableThreadLocal.withInitial("testName", Object::new);
    Object parentValue = local.get();

    Thread childThread = new Thread(() -> {
      Object childValue = local.get();
      assertThat(childValue).isNotSameAs(parentValue);
    });

    childThread.start();
    childThread.join();
  }

}