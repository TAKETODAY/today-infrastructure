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

package infra.core.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.util.ResourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 15:45
 */
class ResourceConsumerTests {

  @Test
  void acceptExecutesOperation() throws IOException {
    AtomicBoolean executed = new AtomicBoolean();
    ResourceConsumer consumer = resource -> executed.set(true);
    consumer.accept(ResourceUtils.getResource("infra/core/io/test.properties"));
    assertThat(executed).isTrue();
  }

  @Test
  void andThenExecutesOperationsInSequence() throws IOException {
    List<String> operations = new ArrayList<>();
    ResourceConsumer first = resource -> operations.add("first");
    ResourceConsumer second = resource -> operations.add("second");

    ResourceConsumer combined = first.andThen(second);
    combined.accept(ResourceUtils.getResource("infra/core/io/test.properties"));

    assertThat(operations).containsExactly("first", "second");
  }

  @Test
  void andThenWithNullAfterConsumerThrowsException() {
    ResourceConsumer consumer = resource -> { };
    assertThatNullPointerException()
            .isThrownBy(() -> consumer.andThen(null));
  }

  @Test
  void firstOperationFailureSkipsSecondOperation() {
    ResourceConsumer first = resource -> { throw new IOException("First failed"); };
    ResourceConsumer second = resource -> { throw new IOException("Should not execute"); };

    ResourceConsumer combined = first.andThen(second);
    assertThatExceptionOfType(IOException.class)
            .isThrownBy(() -> combined.accept(ResourceUtils.getResource("test.properties")))
            .withMessage("First failed");
  }

  @Test
  void acceptWithNullResourceThrowsException() {
    ResourceConsumer consumer = Resource::toString;
    assertThatNullPointerException()
            .isThrownBy(() -> consumer.accept(null));
  }

}