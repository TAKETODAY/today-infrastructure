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

import org.aopalliance.aop.Advice;
import org.aspectj.util.PartialOrder;
import org.aspectj.util.PartialOrder.PartialComparable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.aspectj.AbstractAspectJAdvice;
import cn.taketoday.aop.aspectj.AspectJPointcutAdvisor;
import cn.taketoday.aop.aspectj.AspectJProxyUtils;
import cn.taketoday.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import cn.taketoday.aop.interceptor.ExposeInvocationInterceptor;
import cn.taketoday.core.Ordered;
import cn.taketoday.util.ClassUtils;

/**
 * {@link cn.taketoday.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}
 * subclass that exposes AspectJ's invocation context and understands AspectJ's rules
 * for advice precedence when multiple pieces of advice come from the same aspect.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

  private static final Comparator<Advisor> DEFAULT_PRECEDENCE_COMPARATOR = new AspectJPrecedenceComparator();

  /**
   * Sort the supplied {@link Advisor} instances according to AspectJ precedence.
   * <p>If two pieces of advice come from the same aspect, they will have the same
   * order. Advice from the same aspect is then further ordered according to the
   * following rules:
   * <ul>
   * <li>If either of the pair is <em>after</em> advice, then the advice declared
   * last gets highest precedence (i.e., runs last).</li>
   * <li>Otherwise the advice declared first gets highest precedence (i.e., runs
   * first).</li>
   * </ul>
   * <p><b>Important:</b> Advisors are sorted in precedence order, from highest
   * precedence to lowest. "On the way in" to a join point, the highest precedence
   * advisor should run first. "On the way out" of a join point, the highest
   * precedence advisor should run last.
   */
  @Override
  protected List<Advisor> sortAdvisors(List<Advisor> advisors) {
    List<PartiallyComparableAdvisorHolder> partiallyComparableAdvisors = new ArrayList<>(advisors.size());
    for (Advisor advisor : advisors) {
      partiallyComparableAdvisors.add(
              new PartiallyComparableAdvisorHolder(advisor, DEFAULT_PRECEDENCE_COMPARATOR));
    }
    List<PartiallyComparableAdvisorHolder> sorted = PartialOrder.sort(partiallyComparableAdvisors);
    if (sorted != null) {
      ArrayList<Advisor> result = new ArrayList<>(advisors.size());
      for (PartiallyComparableAdvisorHolder pcAdvisor : sorted) {
        result.add(pcAdvisor.getAdvisor());
      }
      return result;
    }
    else {
      return super.sortAdvisors(advisors);
    }
  }

  /**
   * Add an {@link ExposeInvocationInterceptor} to the beginning of the advice chain.
   * <p>This additional advice is needed when using AspectJ pointcut expressions
   * and when using AspectJ-style advice.
   */
  @Override
  protected void extendAdvisors(List<Advisor> candidateAdvisors) {
    AspectJProxyUtils.makeAdvisorChainAspectJCapableIfNecessary(candidateAdvisors);
  }

  @Override
  protected boolean shouldSkip(Class<?> beanClass, String beanName) {
    // TODO: Consider optimization by caching the list of the aspect names
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    for (Advisor advisor : candidateAdvisors) {
      if (advisor instanceof AspectJPointcutAdvisor pointcutAdvisor
              && pointcutAdvisor.getAspectName().equals(beanName)) {
        return true;
      }
    }
    return super.shouldSkip(beanClass, beanName);
  }

  /**
   * Implements AspectJ's {@link PartialComparable} interface for defining partial orderings.
   */
  private record PartiallyComparableAdvisorHolder(Advisor advisor, Comparator<Advisor> comparator) implements PartialComparable {

    @Override
    public int compareTo(Object obj) {
      Advisor otherAdvisor = ((PartiallyComparableAdvisorHolder) obj).advisor;
      return this.comparator.compare(this.advisor, otherAdvisor);
    }

    @Override
    public int fallbackCompareTo(Object obj) {
      return 0;
    }

    public Advisor getAdvisor() {
      return this.advisor;
    }

    @Override
    public String toString() {
      Advice advice = this.advisor.getAdvice();
      StringBuilder sb = new StringBuilder(ClassUtils.getShortName(advice.getClass()));
      boolean appended = false;
      if (this.advisor instanceof Ordered) {
        sb.append(": order = ").append(((Ordered) this.advisor).getOrder());
        appended = true;
      }
      if (advice instanceof AbstractAspectJAdvice ajAdvice) {
        sb.append(!appended ? ": " : ", ");
        sb.append("aspect name = ");
        sb.append(ajAdvice.getAspectName());
        sb.append(", declaration order = ");
        sb.append(ajAdvice.getDeclarationOrder());
      }
      return sb.toString();
    }
  }

}
