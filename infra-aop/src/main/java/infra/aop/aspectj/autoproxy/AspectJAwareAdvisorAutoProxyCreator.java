/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.aspectj.autoproxy;

import org.aopalliance.aop.Advice;
import org.aspectj.util.PartialOrder;
import org.aspectj.util.PartialOrder.PartialComparable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import infra.aop.Advisor;
import infra.aop.aspectj.AbstractAspectJAdvice;
import infra.aop.aspectj.AspectJPointcutAdvisor;
import infra.aop.aspectj.AspectJProxyUtils;
import infra.aop.aspectj.ShadowMatchUtils;
import infra.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import infra.aop.interceptor.ExposeInvocationInterceptor;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.SmartInitializingSingleton;
import infra.core.Ordered;
import infra.util.ClassUtils;

/**
 * {@link AbstractAdvisorAutoProxyCreator}
 * subclass that exposes AspectJ's invocation context and understands AspectJ's rules
 * for advice precedence when multiple pieces of advice come from the same aspect.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AspectJAwareAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator
        implements SmartInitializingSingleton, DisposableBean {

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
        result.add(pcAdvisor.advisor());
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
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    for (Advisor advisor : candidateAdvisors) {
      if (advisor instanceof AspectJPointcutAdvisor pointcutAdvisor
              && pointcutAdvisor.getAspectName().equals(beanName)) {
        return true;
      }
    }
    return super.shouldSkip(beanClass, beanName);
  }

  @Override
  public void afterSingletonsInstantiated() {
    ShadowMatchUtils.clearCache();
  }

  @Override
  public void destroy() throws Exception {
    ShadowMatchUtils.clearCache();
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

    @Override
    public String toString() {
      Advice advice = this.advisor.getAdvice();
      StringBuilder sb = new StringBuilder(ClassUtils.getShortName(advice.getClass()));
      boolean appended = false;
      if (this.advisor instanceof Ordered ordered) {
        sb.append(": order = ").append(ordered.getOrder());
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
