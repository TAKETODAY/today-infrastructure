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

package infra.transaction.support;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 21:12
 */
class ResourceHolderSynchronizationTests {

  @Test
  void suspendUnbindsResourceAndResumeRebinds() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key);

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.suspend();
    assertThat(TransactionSynchronizationManager.hasResource(key)).isFalse();

    sync.resume();
    assertThat(TransactionSynchronizationManager.hasResource(key)).isTrue();
  }

  @Test
  void afterCommitProcessesResourceWhenNotReleasedBeforeCompletion() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldReleaseBeforeCompletion() {
                return false;
              }

              @Override
              protected void processResourceAfterCommit(TestResourceHolder resourceHolder) {
                resourceHolder.processed = true;
              }
            };

    sync.afterCommit();
    assertThat(holder.processed).isTrue();
  }

  @Test
  void resourceCleanupIsPerformedWhenNotUnbinding() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean cleanedUp = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldUnbindAtCompletion() {
                return false;
              }

              @Override
              protected void cleanupResource(TestResourceHolder holder, String key, boolean committed) {
                cleanedUp.set(true);
              }
            };

    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(cleanedUp).isTrue();
  }

  @Test
  void flushResourceInvokesFlushCallback() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean flushed = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected void flushResource(TestResourceHolder resourceHolder) {
                flushed.set(true);
              }
            };

    sync.flush();
    assertThat(flushed.get()).isTrue();
  }

  @Test
  void afterCompletionWithRollbackStatus() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean cleanedUp = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldUnbindAtCompletion() {
                return false;
              }

              @Override
              protected void cleanupResource(TestResourceHolder holder, String key, boolean committed) {
                assertThat(committed).isFalse();
                cleanedUp.set(true);
              }
            };

    sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
    assertThat(cleanedUp).isTrue();
  }

  private static class TestResourceHolder implements ResourceHolder {
    private boolean reset;
    private boolean processed;
    private boolean unbound;

    @Override
    public void reset() {
      this.reset = true;
    }

    @Override
    public void unbound() {
      this.unbound = true;
    }

    @Override
    public boolean isVoid() {
      return false;
    }

    public boolean isReset() {
      return reset;
    }
  }

  private static class TestResourceHolderSync
          extends ResourceHolderSynchronization<TestResourceHolder, String> {

    TestResourceHolderSync(TestResourceHolder holder, String key) {
      super(holder, key);
    }
  }

}