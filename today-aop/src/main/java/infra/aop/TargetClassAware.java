/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop;

import org.jspecify.annotations.Nullable;

import infra.aop.framework.Advised;
import infra.aop.support.AopUtils;

/**
 * Minimal interface for exposing the target class behind a proxy.
 *
 * <p>Implemented by AOP proxy objects and proxy factories
 * (via {@link Advised})
 * as well as by {@link TargetSource TargetSources}.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 18:45
 * @see AopUtils#getTargetClass(Object)
 * @since 3.0
 */
public interface TargetClassAware {

  /**
   * Return the target class behind the implementing object
   * (typically a proxy configuration or an actual proxy).
   *
   * @return the target Class, or {@code null} if not known
   */
  @Nullable
  Class<?> getTargetClass();

}
