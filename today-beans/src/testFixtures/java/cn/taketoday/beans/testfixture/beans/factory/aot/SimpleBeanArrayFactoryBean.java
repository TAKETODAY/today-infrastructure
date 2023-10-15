/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.testfixture.beans.factory.aot;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.config.AbstractFactoryBean;

/**
 * A public {@link FactoryBean} that produces an array of objects.
 *
 * @author Stephane Nicoll
 */
public class SimpleBeanArrayFactoryBean extends AbstractFactoryBean<SimpleBean[]> {

  @Override
  public Class<?> getObjectType() {
    return SimpleBean[].class;
  }

  @Override
  protected SimpleBean[] createBeanInstance() throws Exception {
    return new SimpleBean[0];
  }

}
