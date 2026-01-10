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
    TransactionSynchronizationManager.unbindResource(key);
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

  @Test
  void shouldUnbindAtCompletionByDefault() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key);

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.beforeCompletion();
    assertThat(TransactionSynchronizationManager.hasResource(key)).isFalse();
  }

  @Test
  void shouldNotUnbindWhenShouldUnbindAtCompletionReturnsFalse() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldUnbindAtCompletion() {
                return false;
              }
            };

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.beforeCompletion();
    assertThat(TransactionSynchronizationManager.hasResource(key)).isTrue();
    TransactionSynchronizationManager.unbindResource(key);
  }

  @Test
  void shouldReleaseBeforeCompletionByDefault() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean released = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected void releaseResource(TestResourceHolder resourceHolder, String resourceKey) {
                released.set(true);
              }
            };

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.beforeCompletion();
    assertThat(released.get()).isTrue();
  }

  @Test
  void shouldProcessResourceAfterCommitWhenNotReleasedBeforeCompletion() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean processed = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldReleaseBeforeCompletion() {
                return false;
              }

              @Override
              protected void processResourceAfterCommit(TestResourceHolder resourceHolder) {
                processed.set(true);
              }
            };

    sync.afterCommit();
    assertThat(processed.get()).isTrue();
  }

  @Test
  void shouldNotProcessResourceAfterCommitWhenReleasedBeforeCompletion() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean processed = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldReleaseBeforeCompletion() {
                return true;
              }

              @Override
              protected void processResourceAfterCommit(TestResourceHolder resourceHolder) {
                processed.set(true);
              }
            };

    sync.afterCommit();
    assertThat(processed.get()).isFalse();
  }

  @Test
  void shouldReleaseAfterCompletionWhenNotReleasedBeforeCompletion() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean released = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldReleaseBeforeCompletion() {
                return false;
              }

              @Override
              protected void releaseResource(TestResourceHolder resourceHolder, String resourceKey) {
                released.set(true);
              }
            };

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.beforeCompletion(); // Unbinds but doesn't release
    assertThat(released.get()).isFalse();

    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(released.get()).isTrue();
  }

  @Test
  void shouldNotReleaseAfterCompletionWhenAlreadyReleasedBeforeCompletion() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean released = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldReleaseBeforeCompletion() {
                return true;
              }

              @Override
              protected void releaseResource(TestResourceHolder resourceHolder, String resourceKey) {
                released.set(true);
              }
            };

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.beforeCompletion(); // Unbinds and releases
    assertThat(released.get()).isTrue();
    released.set(false); // Reset for verification

    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(released.get()).isFalse();
  }

  @Test
  void shouldCallCleanupResourceWhenNotUnbindingAtCompletion() {
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
                assertThat(committed).isTrue();
              }
            };

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(cleanedUp.get()).isTrue();

    // Clean up
    TransactionSynchronizationManager.unbindResource(key);
  }

  @Test
  void shouldResetResourceHolderInAfterCompletion() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key);

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(holder.isReset()).isTrue();
  }

  @Test
  void shouldCallBeforeCommitWithReadOnlyFlag() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean beforeCommitCalled = new AtomicBoolean();
    boolean readOnlyFlag = true;

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              public void beforeCommit(boolean readOnly) {
                beforeCommitCalled.set(true);
                assertThat(readOnly).isEqualTo(readOnlyFlag);
              }
            };

    sync.beforeCommit(readOnlyFlag);
    assertThat(beforeCommitCalled.get()).isTrue();
  }

  @Test
  void shouldUseCustomShouldReleaseAfterCompletionLogic() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean released = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldReleaseBeforeCompletion() {
                return false;
              }

              @Override
              protected boolean shouldReleaseAfterCompletion(TestResourceHolder resourceHolder) {
                return true; // Override default logic
              }

              @Override
              protected void releaseResource(TestResourceHolder resourceHolder, String resourceKey) {
                released.set(true);
              }
            };

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.beforeCompletion(); // Doesn't release due to shouldReleaseBeforeCompletion returning false
    assertThat(released.get()).isFalse();

    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(released.get()).isTrue();
  }

  @Test
  void shouldHandleMultipleSuspendsAndResumes() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key);

    TransactionSynchronizationManager.bindResource(key, holder);

    // First suspend
    sync.suspend();
    assertThat(TransactionSynchronizationManager.hasResource(key)).isFalse();

    // First resume
    sync.resume();
    assertThat(TransactionSynchronizationManager.hasResource(key)).isTrue();

    // Second suspend
    sync.suspend();
    assertThat(TransactionSynchronizationManager.hasResource(key)).isFalse();

    // Second resume
    sync.resume();
    assertThat(TransactionSynchronizationManager.hasResource(key)).isTrue();

    TransactionSynchronizationManager.unbindResource(key);
  }

  @Test
  void shouldHandleAfterCompletionWithUnknownStatus() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean cleanedUp = new AtomicBoolean();
    int unknownStatus = 999; // Unknown status

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldUnbindAtCompletion() {
                return false;
              }

              @Override
              protected void cleanupResource(TestResourceHolder holder, String key, boolean committed) {
                // With unknown status, should be treated as not committed
                assertThat(committed).isFalse();
                cleanedUp.set(true);
              }
            };

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.afterCompletion(unknownStatus);
    assertThat(cleanedUp.get()).isTrue();

    TransactionSynchronizationManager.unbindResource(key);
  }

  @Test
  void shouldHandleResourceHolderUnbindingInAfterCompletionWhenActive() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key);

    TransactionSynchronizationManager.bindResource(key, holder);
    assertThat(holder.unbound).isFalse();

    sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    assertThat(holder.unbound).isTrue();
    assertThat(sync).extracting("holderActive").isEqualTo(false);
  }

  @Test
  void shouldNotCallProcessResourceAfterCommitWhenShouldReleaseBeforeCompletionIsTrue() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean processed = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldReleaseBeforeCompletion() {
                return true;
              }

              @Override
              protected void processResourceAfterCommit(TestResourceHolder resourceHolder) {
                processed.set(true);
              }
            };

    sync.afterCommit();
    assertThat(processed.get()).isFalse();
  }

  @Test
  void shouldHandleBeforeCompletionWhenShouldUnbindReturnsTrueButShouldReleaseBeforeCompletionReturnsFalse() {
    TestResourceHolder holder = new TestResourceHolder();
    String key = "testKey";
    AtomicBoolean released = new AtomicBoolean();

    ResourceHolderSynchronization<TestResourceHolder, String> sync =
            new TestResourceHolderSync(holder, key) {
              @Override
              protected boolean shouldUnbindAtCompletion() {
                return true;
              }

              @Override
              protected boolean shouldReleaseBeforeCompletion() {
                return false;
              }

              @Override
              protected void releaseResource(TestResourceHolder resourceHolder, String resourceKey) {
                released.set(true);
              }
            };

    TransactionSynchronizationManager.bindResource(key, holder);
    sync.beforeCompletion();

    // Should unbind but not release
    assertThat(TransactionSynchronizationManager.hasResource(key)).isFalse();
    assertThat(released.get()).isFalse();
    assertThat(sync).extracting("holderActive").isEqualTo(false);
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