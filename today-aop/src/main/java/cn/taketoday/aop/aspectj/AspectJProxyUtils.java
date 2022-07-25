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

import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.PointcutAdvisor;
import cn.taketoday.aop.interceptor.ExposeInvocationInterceptor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Utility methods for working with AspectJ proxies.
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AspectJProxyUtils {

  /**
   * Add special advisors if necessary to work with a proxy chain that contains AspectJ advisors:
   * concretely, {@link ExposeInvocationInterceptor} at the beginning of the list.
   * <p>This will expose the current Framework AOP invocation (necessary for some AspectJ pointcut
   * matching) and make available the current AspectJ JoinPoint. The call will have no effect
   * if there are no AspectJ advisors in the advisor chain.
   *
   * @param advisors the advisors available
   * @return {@code true} if an {@link ExposeInvocationInterceptor} was added to the list,
   * otherwise {@code false}
   */
  public static boolean makeAdvisorChainAspectJCapableIfNecessary(List<Advisor> advisors) {
    // Don't add advisors to an empty list; may indicate that proxying is just not required
    if (!advisors.isEmpty()) {
      boolean foundAspectJAdvice = false;
      for (Advisor advisor : advisors) {
        // Be careful not to get the Advice without a guard, as this might eagerly
        // instantiate a non-singleton AspectJ aspect...
        if (isAspectJAdvice(advisor)) {
          foundAspectJAdvice = true;
          break;
        }
      }
      if (foundAspectJAdvice && !advisors.contains(ExposeInvocationInterceptor.ADVISOR)) {
        advisors.add(0, ExposeInvocationInterceptor.ADVISOR);
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether the given Advisor contains an AspectJ advice.
   *
   * @param advisor the Advisor to check
   */
  private static boolean isAspectJAdvice(Advisor advisor) {
    return advisor instanceof InstantiationModelAwarePointcutAdvisor
            || advisor.getAdvice() instanceof AbstractAspectJAdvice
            || (
            advisor instanceof PointcutAdvisor pointcutAdvisor
                    && pointcutAdvisor.getPointcut() instanceof AspectJExpressionPointcut
    );
  }

  static boolean isVariableName(@Nullable String name) {
    if (StringUtils.isEmpty(name)) {
      return false;
    }
    if (!Character.isJavaIdentifierStart(name.charAt(0))) {
      return false;
    }
    int length = name.length();
    for (int i = 1; i < length; i++) {
      if (!Character.isJavaIdentifierPart(name.charAt(i))) {
        return false;
      }
    }
    return true;
  }

}
