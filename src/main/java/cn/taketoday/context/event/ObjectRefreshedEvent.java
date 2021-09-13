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

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Nullable;

/**
 * Bean Initialized event
 *
 * @author TODAY <br>
 * 2018-09-20 16:48
 */
@SuppressWarnings("serial")
public class ObjectRefreshedEvent extends ApplicationContextEvent {

  /** which bean definition refreshed **/
  private final String name;

  @Nullable
  private BeanDefinition def;

  public ObjectRefreshedEvent(String name, ApplicationContext context) {
    super(context);
    Assert.notNull(name, "name must not be null");
    this.name = name;
  }

  public ObjectRefreshedEvent(BeanDefinition def, ApplicationContext context) {
    super(context);
    Assert.notNull(def, "BeanDefinition must not be null");
    this.name = def.getName();
    this.def = def;
  }

  public final BeanDefinition getBeanDefinition() {
    final BeanDefinition def = this.def;
    if (def == null) {
      return this.def = getApplicationContext().getBeanDefinition(name);
    }
    return def;
  }

}
