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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Low-level hook that can be used to attach a {@link ApplicationStartupListener} to a
 * {@link Application} in order to observe or modify its behavior. Hooks are managed
 * on a per-thread basis providing isolation when multiple applications are executed in
 * parallel.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Application#withHook
 * @since 4.0 2022/9/23 23:17
 */
public interface ApplicationHook {

  /**
   * Return the {@link ApplicationStartupListener} that should be hooked into the
   * given {@link Application}.
   *
   * @param application the source {@link Application} instance
   * @return the {@link ApplicationStartupListener} to attach
   */
  @Nullable
  ApplicationStartupListener getStartupListener(Application application);

  // Static Factory Methods

  static ApplicationHook forSingleUse(ApplicationHook delegate) {
    final class SingleUseApplicationHook implements ApplicationHook {
      private final AtomicBoolean used = new AtomicBoolean();

      private final ApplicationHook delegate;

      private SingleUseApplicationHook(ApplicationHook delegate) {
        this.delegate = delegate;
      }

      @Nullable
      @Override
      public ApplicationStartupListener getStartupListener(Application application) {
        return this.used.compareAndSet(false, true) ? this.delegate.getStartupListener(application) : null;
      }

    }
    return new SingleUseApplicationHook(delegate);
  }

}
