/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.event;

import java.util.Set;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.factory.BeanReferencePropertyValue;

/**
 * Handled all dependencies
 *
 * @author TODAY <br>
 *
 * 2018-11-10 13:23
 */
@SuppressWarnings("serial")
public class DependenciesHandledEvent extends ApplicationContextEvent {

  private final Set<BeanReferencePropertyValue> dependencies;

  public DependenciesHandledEvent(ApplicationContext source, Set<BeanReferencePropertyValue> dependencies) {
    super(source);
    this.dependencies = dependencies;
  }

  public final Set<BeanReferencePropertyValue> getDependencies() {
    return dependencies;
  }

}
