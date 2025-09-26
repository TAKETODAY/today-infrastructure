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
