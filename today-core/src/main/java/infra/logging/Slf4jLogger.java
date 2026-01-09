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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2019-11-03 13:55
 */
class Slf4jLogger extends Logger {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String name;

  private final transient org.slf4j.Logger target;

  Slf4jLogger(org.slf4j.Logger target) {
    super(target.isDebugEnabled());
    this.target = target;
    this.name = target.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return debugEnabled && target.isTraceEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return target.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return target.isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return target.isErrorEnabled();
  }

  @Override
  public String getName() {
    return target.getName();
  }

  @Override
  @SuppressWarnings("NullAway")
  protected void logInternal(Level level,@Nullable String format, @Nullable Throwable t, @Nullable Object @Nullable [] args) {
    final String msg = MessageFormatter.format(format, args);
    switch (level) {
      case DEBUG -> target.debug(msg, t);
      case ERROR -> target.error(msg, t);
      case TRACE -> target.trace(msg, t);
      case WARN -> target.warn(msg, t);
      default -> target.info(msg, t);
    }
  }

  @Serial
  protected Object readResolve() {
    return Slf4jLoggerFactory.createLog(this.name);
  }
}
