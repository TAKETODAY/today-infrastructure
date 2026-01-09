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

package infra.logging;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * @author TODAY 2021/11/5 22:53
 * @since 4.0
 */
public class NoOpLogger extends Logger {

  @Serial
  private static final long serialVersionUID = 1L;

  public NoOpLogger() {
    super(false);
  }

  @Override
  public String getName() {
    return "NoOpLogger";
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public boolean isInfoEnabled() {
    return false;
  }

  @Override
  public boolean isWarnEnabled() {
    return false;
  }

  @Override
  public boolean isErrorEnabled() {
    return false;
  }

  @Override
  protected void logInternal(Level level, @Nullable Object msg, @Nullable Throwable t) {
  }

  @Override
  protected void logInternal(Level level, @Nullable String msg, @Nullable Throwable t, @Nullable Object @Nullable [] args) {
  }

}
