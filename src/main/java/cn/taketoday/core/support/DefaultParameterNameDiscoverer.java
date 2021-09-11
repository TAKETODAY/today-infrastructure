/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.core.support;

import cn.taketoday.core.ParameterNameDiscoverer;

/**
 * Default implementation of the {@link ParameterNameDiscoverer} strategy interface,
 * using the Java 8 standard reflection mechanism (if available), and falling back
 * to the ASM-based {@link LocalVariableTableParameterNameDiscoverer} for checking
 * debug information in the class file.
 *
 * <p>Further discoverers may be added through {@link #addDiscoverer(ParameterNameDiscoverer...)}.
 *
 * @author TODAY 2021/9/10 23:08
 * @see LocalVariableTableParameterNameDiscoverer
 * @since 4.0
 */
public class DefaultParameterNameDiscoverer extends CompositeParameterNameDiscoverer {

  public DefaultParameterNameDiscoverer() {
    addDiscoverer(new ReflectiveParameterNameDiscoverer());
    addDiscoverer(new LocalVariableTableParameterNameDiscoverer());
  }

}
