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

package cn.taketoday.aop.aspectj.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link AspectJAwareAdvisorAutoProxyCreator} subclass that processes all AspectJ
 * annotation aspects in the current application context, as well as Framework Advisors.
 *
 * <p>Any AspectJ annotated classes will automatically be recognized, and their
 * advice applied if Framework AOP's proxy-based model is capable of applying it.
 * This covers method execution joinpoints.
 *
 * <p>If the &lt;aop:include&gt; element is used, only @AspectJ beans with names matched by
 * an include pattern will be considered as defining aspects to use for Framework auto-proxying.
 *
 * <p>Processing of Framework Advisors follows the rules established in
 * {@link cn.taketoday.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AspectJAdvisorFactory
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AnnotationAwareAspectJAutoProxyCreator extends AspectJAwareAdvisorAutoProxyCreator {

  @Nullable
  private List<Pattern> includePatterns;

  @Nullable
  private AspectJAdvisorFactory aspectJAdvisorFactory;

  @Nullable
  private BeanFactoryAspectJAdvisorsBuilder aspectJAdvisorsBuilder;

  /**
   * Set a list of regex patterns, matching eligible @AspectJ bean names.
   * <p>Default is to consider all @AspectJ beans as eligible.
   */
  public void setIncludePatterns(List<String> patterns) {
    this.includePatterns = new ArrayList<>(patterns.size());
    for (String patternText : patterns) {
      this.includePatterns.add(Pattern.compile(patternText));
    }
  }

  public void setAspectJAdvisorFactory(AspectJAdvisorFactory aspectJAdvisorFactory) {
    Assert.notNull(aspectJAdvisorFactory, "AspectJAdvisorFactory must not be null");
    this.aspectJAdvisorFactory = aspectJAdvisorFactory;
  }

  @Override
  protected void initBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.initBeanFactory(beanFactory);
    if (this.aspectJAdvisorFactory == null) {
      this.aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory(beanFactory);
    }
    this.aspectJAdvisorsBuilder =
            new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);
  }

  @Override
  protected List<Advisor> findCandidateAdvisors() {
    // Add all the Framework advisors found according to superclass rules.
    List<Advisor> advisors = super.findCandidateAdvisors();
    // Build Advisors for all AspectJ aspects in the bean factory.
    if (aspectJAdvisorsBuilder != null) {
      advisors.addAll(aspectJAdvisorsBuilder.buildAspectJAdvisors());
    }
    return advisors;
  }

  @Override
  protected boolean isInfrastructureClass(Class<?> beanClass) {
    // Previously we setProxyTargetClass(true) in the constructor, but that has too
    // broad an impact. Instead we now override isInfrastructureClass to avoid proxying
    // aspects. I'm not entirely happy with that as there is no good reason not
    // to advise aspects, except that it causes advice invocation to go through a
    // proxy, and if the aspect implements e.g the Ordered interface it will be
    // proxied by that interface and fail at runtime as the advice method is not
    // defined on the interface. We could potentially relax the restriction about
    // not advising aspects in the future.
    return super.isInfrastructureClass(beanClass)
            || (aspectJAdvisorFactory != null && aspectJAdvisorFactory.isAspect(beanClass));
  }

  /**
   * Check whether the given aspect bean is eligible for auto-proxying.
   * <p>If no &lt;aop:include&gt; elements were used then "includePatterns" will be
   * {@code null} and all beans are included. If "includePatterns" is non-null,
   * then one of the patterns must match.
   */
  protected boolean isEligibleAspectBean(String beanName) {
    if (this.includePatterns == null) {
      return true;
    }
    else {
      for (Pattern pattern : this.includePatterns) {
        if (pattern.matcher(beanName).matches()) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Subclass of BeanFactoryAspectJAdvisorsBuilderAdapter that delegates to
   * surrounding AnnotationAwareAspectJAutoProxyCreator facilities.
   */
  private class BeanFactoryAspectJAdvisorsBuilderAdapter extends BeanFactoryAspectJAdvisorsBuilder {

    public BeanFactoryAspectJAdvisorsBuilderAdapter(
            BeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {

      super(beanFactory, advisorFactory);
    }

    @Override
    protected boolean isEligibleBean(String beanName) {
      return AnnotationAwareAspectJAutoProxyCreator.this.isEligibleAspectBean(beanName);
    }
  }

}
