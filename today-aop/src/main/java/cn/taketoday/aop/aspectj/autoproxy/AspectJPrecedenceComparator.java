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

package cn.taketoday.aop.aspectj.autoproxy;

import java.util.Comparator;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.aspectj.AspectJAopUtils;
import cn.taketoday.aop.aspectj.AspectJPrecedenceInformation;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Assert;

/**
 * Orders AspectJ advice/advisors by precedence (<i>not</i> invocation order).
 *
 * <p>Given two pieces of advice, {@code A} and {@code B}:
 * <ul>
 * <li>If {@code A} and {@code B} are defined in different aspects, then the advice
 * in the aspect with the lowest order value has the highest precedence.</li>
 * <li>If {@code A} and {@code B} are defined in the same aspect, if one of
 * {@code A} or {@code B} is a form of <em>after</em> advice, then the advice declared
 * last in the aspect has the highest precedence. If neither {@code A} nor {@code B}
 * is a form of <em>after</em> advice, then the advice declared first in the aspect
 * has the highest precedence.</li>
 * </ul>
 *
 * <p>Important: This comparator is used with AspectJ's
 * {@link org.aspectj.util.PartialOrder PartialOrder} sorting utility. Thus, unlike
 * a normal {@link Comparator}, a return value of {@code 0} from this comparator
 * means we don't care about the ordering, not that the two elements must be sorted
 * identically.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 4.0
 */
class AspectJPrecedenceComparator implements Comparator<Advisor> {

  private static final int HIGHER_PRECEDENCE = -1;

  private static final int SAME_PRECEDENCE = 0;

  private static final int LOWER_PRECEDENCE = 1;

  private final Comparator<? super Advisor> advisorComparator;

  /**
   * Create a default {@code AspectJPrecedenceComparator}.
   */
  public AspectJPrecedenceComparator() {
    this.advisorComparator = AnnotationAwareOrderComparator.INSTANCE;
  }

  /**
   * Create an {@code AspectJPrecedenceComparator}, using the given {@link Comparator}
   * for comparing {@link cn.taketoday.aop.Advisor} instances.
   *
   * @param advisorComparator the {@code Comparator} to use for advisors
   */
  public AspectJPrecedenceComparator(Comparator<? super Advisor> advisorComparator) {
    Assert.notNull(advisorComparator, "Advisor comparator is required");
    this.advisorComparator = advisorComparator;
  }

  @Override
  public int compare(Advisor o1, Advisor o2) {
    int advisorPrecedence = this.advisorComparator.compare(o1, o2);
    if (advisorPrecedence == SAME_PRECEDENCE && declaredInSameAspect(o1, o2)) {
      advisorPrecedence = comparePrecedenceWithinAspect(o1, o2);
    }
    return advisorPrecedence;
  }

  private int comparePrecedenceWithinAspect(Advisor advisor1, Advisor advisor2) {
    boolean oneOrOtherIsAfterAdvice =
            (AspectJAopUtils.isAfterAdvice(advisor1) || AspectJAopUtils.isAfterAdvice(advisor2));
    int adviceDeclarationOrderDelta = getAspectDeclarationOrder(advisor1) - getAspectDeclarationOrder(advisor2);

    if (oneOrOtherIsAfterAdvice) {
      // the advice declared last has higher precedence
      if (adviceDeclarationOrderDelta < 0) {
        // advice1 was declared before advice2
        // so advice1 has lower precedence
        return LOWER_PRECEDENCE;
      }
      else if (adviceDeclarationOrderDelta == 0) {
        return SAME_PRECEDENCE;
      }
      else {
        return HIGHER_PRECEDENCE;
      }
    }
    else {
      // the advice declared first has higher precedence
      if (adviceDeclarationOrderDelta < 0) {
        // advice1 was declared before advice2
        // so advice1 has higher precedence
        return HIGHER_PRECEDENCE;
      }
      else if (adviceDeclarationOrderDelta == 0) {
        return SAME_PRECEDENCE;
      }
      else {
        return LOWER_PRECEDENCE;
      }
    }
  }

  private boolean declaredInSameAspect(Advisor advisor1, Advisor advisor2) {
    return (hasAspectName(advisor1) && hasAspectName(advisor2) &&
            getAspectName(advisor1).equals(getAspectName(advisor2)));
  }

  private boolean hasAspectName(Advisor advisor) {
    return (advisor instanceof AspectJPrecedenceInformation ||
            advisor.getAdvice() instanceof AspectJPrecedenceInformation);
  }

  // pre-condition is that hasAspectName returned true
  private String getAspectName(Advisor advisor) {
    AspectJPrecedenceInformation precedenceInfo = AspectJAopUtils.getAspectJPrecedenceInformationFor(advisor);
    Assert.state(precedenceInfo != null, () -> "Unresolvable AspectJPrecedenceInformation for " + advisor);
    return precedenceInfo.getAspectName();
  }

  private int getAspectDeclarationOrder(Advisor advisor) {
    AspectJPrecedenceInformation precedenceInfo = AspectJAopUtils.getAspectJPrecedenceInformationFor(advisor);
    return (precedenceInfo != null ? precedenceInfo.getDeclarationOrder() : 0);
  }

}
