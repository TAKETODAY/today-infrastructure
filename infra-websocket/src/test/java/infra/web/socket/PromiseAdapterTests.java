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

package infra.web.socket;

import org.junit.jupiter.api.Test;

import infra.util.concurrent.Future;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.ImmediateEventExecutor;

import static org.assertj.core.api.Assertions.assertThat;

class PromiseAdapterTests {

  @Test
  void sourceCancellationRemainsCancellation() {
    DefaultPromise<Void> source = new DefaultPromise<>(ImmediateEventExecutor.INSTANCE);
    Future<Void> result = PromiseAdapter.adapt(source);

    source.cancel(false);

    assertThat(result.isCancelled()).isTrue();
    assertThat(result.isFailure()).isFalse();
    assertThat(result.getCause()).isSameAs(source.cause());
  }

  @Test
  void cancellingAdaptedFutureDoesNotCancelSource() {
    DefaultPromise<Void> source = new DefaultPromise<>(ImmediateEventExecutor.INSTANCE);
    Future<Void> result = PromiseAdapter.adapt(source);

    result.cancel(false);
    source.setSuccess(null);

    assertThat(source.isSuccess()).isTrue();
    assertThat(result.isCancelled()).isTrue();
  }
}
