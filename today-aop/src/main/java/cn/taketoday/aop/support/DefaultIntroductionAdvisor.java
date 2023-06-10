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

package cn.taketoday.aop.support;

import org.aopalliance.aop.Advice;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.DynamicIntroductionAdvice;
import cn.taketoday.aop.IntroductionAdvisor;
import cn.taketoday.aop.IntroductionInfo;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Simple {@link IntroductionAdvisor} implementation
 * that by default applies to any class.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 20:33
 * @since 3.0
 */
@SuppressWarnings("serial")
public class DefaultIntroductionAdvisor
        extends OrderedSupport implements IntroductionAdvisor, ClassFilter, Ordered, Serializable {

  private final Advice advice;

  private final Set<Class<?>> interfaces = new LinkedHashSet<>();

  /**
   * Create a DefaultIntroductionAdvisor for the given advice.
   *
   * @param advice the Advice to apply (may implement the
   * {@link IntroductionInfo} interface)
   * @see #addInterface
   */
  public DefaultIntroductionAdvisor(Advice advice) {
    this(advice, (advice instanceof IntroductionInfo ? (IntroductionInfo) advice : null));
  }

  /**
   * Create a DefaultIntroductionAdvisor for the given advice.
   *
   * @param advice the Advice to apply
   * @param introductionInfo the IntroductionInfo that describes
   * the interface to introduce (may be {@code null})
   */
  public DefaultIntroductionAdvisor(Advice advice, IntroductionInfo introductionInfo) {
    Assert.notNull(advice, "Advice must not be null");
    this.advice = advice;
    if (introductionInfo != null) {
      Class<?>[] introducedInterfaces = introductionInfo.getInterfaces();
      if (ObjectUtils.isEmpty(introducedInterfaces)) {
        throw new IllegalArgumentException("IntroductionAdviceSupport implements no interfaces");
      }
      for (Class<?> ifc : introducedInterfaces) {
        addInterface(ifc);
      }
    }
  }

  /**
   * Create a DefaultIntroductionAdvisor for the given advice.
   *
   * @param advice the Advice to apply
   * @param ifc the interface to introduce
   */
  public DefaultIntroductionAdvisor(DynamicIntroductionAdvice advice, Class<?> ifc) {
    Assert.notNull(advice, "Advice must not be null");
    this.advice = advice;
    addInterface(ifc);
  }

  /**
   * Add the specified interface to the list of interfaces to introduce.
   *
   * @param ifc the interface to introduce
   */
  public void addInterface(Class<?> ifc) {
    Assert.notNull(ifc, "Interface must not be null");
    if (!ifc.isInterface()) {
      throw new IllegalArgumentException("Specified class [" + ifc.getName() + "] must be an interface");
    }
    this.interfaces.add(ifc);
  }

  @Override
  public Class<?>[] getInterfaces() {
    return ClassUtils.toClassArray(this.interfaces);
  }

  @Override
  public void validateInterfaces() {
    for (Class<?> ifc : this.interfaces) {
      if (this.advice instanceof DynamicIntroductionAdvice &&
              !((DynamicIntroductionAdvice) this.advice).implementsInterface(ifc)) {
        throw new IllegalArgumentException(
                "DynamicIntroductionAdvice [" + this.advice + "] " +
                        "does not implement interface [" + ifc.getName() + "] specified for introduction");
      }
    }
  }

  @Override
  public Advice getAdvice() {
    return this.advice;
  }

  @Override
  public ClassFilter getClassFilter() {
    return this;
  }

  @Override
  public boolean matches(Class<?> clazz) {
    return true;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DefaultIntroductionAdvisor otherAdvisor)) {
      return false;
    }
    return (this.advice.equals(otherAdvisor.advice) && this.interfaces.equals(otherAdvisor.interfaces));
  }

  @Override
  public int hashCode() {
    return this.advice.hashCode() * 13 + this.interfaces.hashCode();
  }

  @Override
  public String toString() {
    return ObjectUtils.toHexString(this) + ": advice [" + this.advice + "]; interfaces "
            + ClassUtils.classNamesToString(this.interfaces);
  }

}
