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

import org.aspectj.lang.JoinPoint;

/**
 * Aspect used as part of before advice binding tests and
 * serves as base class for a number of more specialized test aspects.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
class AdviceBindingTestAspect {

  protected AdviceBindingCollaborator collaborator;

  public void setCollaborator(AdviceBindingCollaborator aCollaborator) {
    this.collaborator = aCollaborator;
  }

  // "advice" methods

  public void oneIntArg(int age) {
    this.collaborator.oneIntArg(age);
  }

  public void oneObjectArg(Object bean) {
    this.collaborator.oneObjectArg(bean);
  }

  public void oneIntAndOneObject(int x, Object o) {
    this.collaborator.oneIntAndOneObject(x, o);
  }

  public void needsJoinPoint(JoinPoint tjp) {
    this.collaborator.needsJoinPoint(tjp.getSignature().getName());
  }

  public void needsJoinPointStaticPart(JoinPoint.StaticPart tjpsp) {
    this.collaborator.needsJoinPointStaticPart(tjpsp.getSignature().getName());
  }

  /**
   * Collaborator interface that makes it easy to test this aspect is
   * working as expected through mocking.
   */
  public interface AdviceBindingCollaborator {

    void oneIntArg(int x);

    void oneObjectArg(Object o);

    void oneIntAndOneObject(int x, Object o);

    void needsJoinPoint(String s);

    void needsJoinPointStaticPart(String s);
  }

}
