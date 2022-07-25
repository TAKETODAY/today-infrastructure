/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

/**
 * Superinterface for advisors that perform one or more AOP <b>introductions</b>.
 *
 * <p>This interface cannot be implemented directly; subinterfaces must
 * provide the advice type implementing the introduction.
 *
 * <p>Introduction is the implementation of additional interfaces
 * (not implemented by a target) via AOP advice.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:47
 * @see IntroductionInterceptor
 * @since 3.0
 */
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

  /**
   * Return the filter determining which target classes this introduction
   * should apply to.
   * <p>This represents the class part of a pointcut. Note that method
   * matching doesn't make sense to introductions.
   *
   * @return the class filter
   */
  ClassFilter getClassFilter();

  /**
   * Can the advised interfaces be implemented by the introduction advice?
   * Invoked before adding an IntroductionAdvisor.
   *
   * @throws IllegalArgumentException if the advised interfaces can't be
   * implemented by the introduction advice
   */
  void validateInterfaces();

}
