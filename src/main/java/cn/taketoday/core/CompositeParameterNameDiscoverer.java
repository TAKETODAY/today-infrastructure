/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core;

import java.lang.reflect.Executable;
import java.util.ArrayList;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * {@link ParameterNameDiscoverer} implementation that tries several discoverer
 * delegates in succession. Those added first in the {@code addDiscoverer} method
 * have high-est priority. If one returns {@code null}, the next will be tried.
 *
 * <p>The default behavior is to return {@code null} if no discoverer matches.
 *
 * @author TODAY 2021/9/10 23:02
 * @since 4.0
 */
public class CompositeParameterNameDiscoverer extends ParameterNameDiscoverer implements ArraySizeTrimmer {

  private final ArrayList<ParameterNameDiscoverer> discoverers = new ArrayList<>();

  /**
   * add ParameterNameDiscoverer
   *
   * @param discoverer ParameterNameDiscoverers
   */
  public void addDiscoverer(@Nullable ParameterNameDiscoverer... discoverer) {
    CollectionUtils.addAll(discoverers, discoverer);
    trimToSize();
  }

  @Nullable
  @Override
  public String[] getParameterNames(Executable executable) {
    for (ParameterNameDiscoverer discoverer : discoverers) {
      String[] parameterNames = discoverer.getParameterNames(executable);
      if (parameterNames != null) {
        return parameterNames;
      }
    }
    // cannot resolve
    return null;
  }

  @Override
  public void trimToSize() {
    discoverers.trimToSize();
  }

}
