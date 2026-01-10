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

package infra.transaction.support;

import org.junit.jupiter.api.Test;

import infra.transaction.TransactionTimedOutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 21:17
 */
class ResourceHolderSupportTests {

  @Test
  void setSynchronizedWithTransactionSetsFlag() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setSynchronizedWithTransaction(true);
    assertThat(holder.isSynchronizedWithTransaction()).isTrue();
  }

  @Test
  void setRollbackOnlySetsFlag() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setRollbackOnly();
    assertThat(holder.isRollbackOnly()).isTrue();
  }

  @Test
  void resetRollbackOnlyResetsFlag() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setRollbackOnly();
    holder.resetRollbackOnly();
    assertThat(holder.isRollbackOnly()).isFalse();
  }

  @Test
  void setTimeoutInSecondsSetsDeadline() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setTimeoutInSeconds(10);
    assertThat(holder.hasTimeout()).isTrue();
    assertThat(holder.getDeadline()).isNotNull();
  }

  @Test
  void setTimeoutInMillisSetsDeadline() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setTimeoutInMillis(10000);
    assertThat(holder.hasTimeout()).isTrue();
    assertThat(holder.getDeadline()).isNotNull();
  }

  @Test
  void getTimeToLiveInSecondsReturnsCorrectValue() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setTimeoutInSeconds(10);
    assertThat(holder.getTimeToLiveInSeconds()).isGreaterThan(0);
  }

  @Test
  void getTimeToLiveInMillisReturnsCorrectValue() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setTimeoutInMillis(10000);
    assertThat(holder.getTimeToLiveInMillis()).isGreaterThan(0);
  }

  @Test
  void requestedIncreasesReferenceCount() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.requested();
    assertThat(holder.isOpen()).isTrue();
  }

  @Test
  void releasedDecreasesReferenceCount() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.requested();
    holder.released();
    assertThat(holder.isOpen()).isFalse();
  }

  @Test
  void clearResetsTransactionalState() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setSynchronizedWithTransaction(true);
    holder.setRollbackOnly();
    holder.setTimeoutInSeconds(10);
    holder.clear();
    assertThat(holder.isSynchronizedWithTransaction()).isFalse();
    assertThat(holder.isRollbackOnly()).isFalse();
    assertThat(holder.hasTimeout()).isFalse();
  }

  @Test
  void resetResetsAllState() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.setSynchronizedWithTransaction(true);
    holder.setRollbackOnly();
    holder.setTimeoutInSeconds(10);
    holder.requested();
    holder.reset();
    assertThat(holder.isSynchronizedWithTransaction()).isFalse();
    assertThat(holder.isRollbackOnly()).isFalse();
    assertThat(holder.hasTimeout()).isFalse();
    assertThat(holder.isOpen()).isFalse();
  }

  @Test
  void unboundSetsIsVoid() {
    ResourceHolderSupport holder = new ResourceHolderSupport() { };
    holder.unbound();
    assertThat(holder.isVoid()).isTrue();
  }

  @Test
  void timeoutExceptionWhenDeadlineReached() {
    ResourceHolderSupport holder = new ResourceHolderSupport() {};
    holder.setTimeoutInMillis(1);

    // Wait for timeout
    try {
      Thread.sleep(10);
    }
    catch (InterruptedException ignored) {}

    assertThatThrownBy(() -> holder.getTimeToLiveInMillis())
            .isInstanceOf(TransactionTimedOutException.class)
            .hasMessageContaining("Transaction timed out");
    assertThat(holder.isRollbackOnly()).isTrue();
  }

  @Test
  void getTimeToLiveInSecondsThrowsWhenNoTimeout() {
    ResourceHolderSupport holder = new ResourceHolderSupport() {};
    assertThatThrownBy(() -> holder.getTimeToLiveInSeconds())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No timeout specified for this resource holder");
  }

  @Test
  void getTimeToLiveInMillisThrowsWhenNoTimeout() {
    ResourceHolderSupport holder = new ResourceHolderSupport() {};
    assertThatThrownBy(() -> holder.getTimeToLiveInMillis())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No timeout specified for this resource holder");
  }

  @Test
  void referenceCountCannotBeNegative() {
    ResourceHolderSupport holder = new ResourceHolderSupport() {};
    holder.released();
    assertThat(holder.isOpen()).isFalse();
  }

  @Test
  void multipleRequestsIncrementReferenceCount() {
    ResourceHolderSupport holder = new ResourceHolderSupport() {};
    holder.requested();
    holder.requested();
    holder.released();
    assertThat(holder.isOpen()).isTrue();
  }

}