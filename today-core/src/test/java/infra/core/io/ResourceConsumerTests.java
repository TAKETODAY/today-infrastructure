/*
 * Copyright 2017 - 2026 the TODAY authors.
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