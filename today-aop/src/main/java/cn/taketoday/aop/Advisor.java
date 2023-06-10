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

package cn.taketoday.aop;

import org.aopalliance.aop.Advice;

/**
 * Base interface holding AOP <b>advice</b> (action to take at a join-point)
 * and a filter determining the applicability of the advice (such as
 * a pointcut). <i>This interface is not for use by  users, but to
 * allow for commonality in support for different types of advice.</i>
 *
 * <p> AOP is based around <b>around advice</b> delivered via method
 * <b>interception</b>, compliant with the AOP Alliance interception API.
 * The Advisor interface allows support for different types of advice,
 * such as <b>before</b> and <b>after</b> advice, which need not be
 * implemented using interception.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 18:07
 * @since 3.0
 */
public interface Advisor {

  /**
   * Common placeholder for an empty {@code Advice} to be returned from
   * {@link #getAdvice()} if no proper advice has been configured (yet).
   */
  Advice EMPTY_ADVICE = new Advice() { };

  /**
   * Return the advice part of this aspect. An advice may be an
   * interceptor, a before advice, a throws advice, etc.
   *
   * @return the advice that should apply if the pointcut matches
   * @see org.aopalliance.intercept.MethodInterceptor
   * @see BeforeAdvice
   * @see AfterAdvice
   * @see ThrowsAdvice
   * @see AfterReturningAdvice
   */
  Advice getAdvice();

  /**
   * Return whether this advice is associated with a particular instance
   * (for example, creating a mixin) or shared with all instances of
   * the advised class obtained from the same bean factory.
   * <p><b>Note that this method is not currently used by the framework.</b>
   * Typical Advisor implementations always return {@code true}.
   * Use singleton/prototype bean definitions or appropriate programmatic
   * proxy creation to ensure that Advisors have the correct lifecycle model.
   *
   * @return whether this advice is associated with a particular target instance
   * @since 4.0
   */
  default boolean isPerInstance() {
    return true;
  }

}
