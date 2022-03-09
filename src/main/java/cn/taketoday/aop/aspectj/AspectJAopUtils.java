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

import org.aopalliance.aop.Advice;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.AfterAdvice;
import cn.taketoday.aop.BeforeAdvice;
import cn.taketoday.lang.Nullable;

/**
 * Utility methods for dealing with AspectJ advisors.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AspectJAopUtils {

  /**
   * Return {@code true} if the advisor is a form of before advice.
   */
  public static boolean isBeforeAdvice(Advisor anAdvisor) {
    AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
    if (precedenceInfo != null) {
      return precedenceInfo.isBeforeAdvice();
    }
    return (anAdvisor.getAdvice() instanceof BeforeAdvice);
  }

  /**
   * Return {@code true} if the advisor is a form of after advice.
   */
  public static boolean isAfterAdvice(Advisor anAdvisor) {
    AspectJPrecedenceInformation precedenceInfo = getAspectJPrecedenceInformationFor(anAdvisor);
    if (precedenceInfo != null) {
      return precedenceInfo.isAfterAdvice();
    }
    return (anAdvisor.getAdvice() instanceof AfterAdvice);
  }

  /**
   * Return the AspectJPrecedenceInformation provided by this advisor or its advice.
   * If neither the advisor nor the advice have precedence information, this method
   * will return {@code null}.
   */
  @Nullable
  public static AspectJPrecedenceInformation getAspectJPrecedenceInformationFor(Advisor anAdvisor) {
    if (anAdvisor instanceof AspectJPrecedenceInformation) {
      return (AspectJPrecedenceInformation) anAdvisor;
    }
    Advice advice = anAdvisor.getAdvice();
    if (advice instanceof AspectJPrecedenceInformation) {
      return (AspectJPrecedenceInformation) advice;
    }
    return null;
  }

}
