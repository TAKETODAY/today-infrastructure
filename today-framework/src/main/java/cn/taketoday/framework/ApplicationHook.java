/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework;

import cn.taketoday.lang.Nullable;

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

}
