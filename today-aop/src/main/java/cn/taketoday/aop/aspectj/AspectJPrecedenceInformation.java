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

package cn.taketoday.aop.aspectj;

import cn.taketoday.core.Ordered;

/**
 * Interface to be implemented by types that can supply the information
 * needed to sort advice/advisors by AspectJ's precedence rules.
 *
 * @author Adrian Colyer
 * @see cn.taketoday.aop.aspectj.autoproxy.AspectJPrecedenceComparator
 * @since 4.0
 */
public interface AspectJPrecedenceInformation extends Ordered {

  // Implementation note:
  // We need the level of indirection this interface provides as otherwise the
  // AspectJPrecedenceComparator must ask an Advisor for its Advice in all cases
  // in order to sort advisors. This causes problems with the
  // InstantiationModelAwarePointcutAdvisor which needs to delay creating
  // its advice for aspects with non-singleton instantiation models.

  /**
   * Return the name of the aspect (bean) in which the advice was declared.
   */
  String getAspectName();

  /**
   * Return the declaration order of the advice member within the aspect.
   */
  int getDeclarationOrder();

  /**
   * Return whether this is a before advice.
   */
  boolean isBeforeAdvice();

  /**
   * Return whether this is an after advice.
   */
  boolean isAfterAdvice();

}
