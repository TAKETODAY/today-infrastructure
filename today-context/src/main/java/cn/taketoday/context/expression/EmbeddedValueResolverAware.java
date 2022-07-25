/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.context.expression;

import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.EmbeddedValueResolver;
import cn.taketoday.core.StringValueResolver;

/**
 * Interface to be implemented by any object that wishes to be notified of a
 * {@code StringValueResolver} for the resolution of embedded definition values.
 *
 * @author Harry Yang
 * @see ConfigurableBeanFactory#resolveEmbeddedValue(String)
 * @see ConfigurableBeanFactory#getBeanExpressionResolver()
 * @see EmbeddedValueResolver
 * @since 4.0
 */
public interface EmbeddedValueResolverAware extends Aware {

  /**
   * Set the StringValueResolver to use for resolving embedded definition values.
   */
  void setEmbeddedValueResolver(StringValueResolver resolver);

}
