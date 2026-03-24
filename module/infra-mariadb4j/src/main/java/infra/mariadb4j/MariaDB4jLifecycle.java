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

package infra.mariadb4j;

import org.jspecify.annotations.Nullable;

import ch.vorburger.exec.ManagedProcessException;
import infra.context.Lifecycle;

/**
 * Lifecycle implementation for managing an embedded MariaDB instance using MariaDB4j.
 * <p>This component wraps a {@link MariaDB} instance to provide start, stop, and status checking
 * capabilities within the Infra lifecycle management system.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/3/22 09:22
 */
public class MariaDB4jLifecycle implements Lifecycle {

  @Nullable MariaDB mariaDb;

  private ManagedProcessException lastException;

  public MariaDB4jLifecycle(@Nullable MariaDB mariaDb) {
    this.mariaDb = mariaDb;
  }

  /** {@inheritDoc} */
  @Override
  public void start() {
    try {
      if (mariaDb != null) {
        mariaDb.start();
      }
    }
    catch (ManagedProcessException e) {
      lastException = e;
      throw new IllegalStateException("MariaDB4j start() failed", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void stop() {
    try {
      if (mariaDb != null) {
        mariaDb.stop();
      }
      mariaDb = null;
    }
    catch (ManagedProcessException e) {
      lastException = e;
      throw new IllegalStateException("MariaDB4j stop() failed", e);
    }
  }

  @Override
  public boolean isRunning() {
    return mariaDb != null;
  }

  public ManagedProcessException getLastException() {
    return lastException;
  }

}
