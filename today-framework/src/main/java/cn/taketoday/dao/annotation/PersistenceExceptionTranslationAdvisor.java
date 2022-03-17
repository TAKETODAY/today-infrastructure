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

package cn.taketoday.dao.annotation;

import org.aopalliance.aop.Advice;

import java.lang.annotation.Annotation;

import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.support.AbstractPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AnnotationMatchingPointcut;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.dao.support.PersistenceExceptionTranslationInterceptor;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;

/**
 * Framework AOP exception translation aspect for use at Repository or DAO layer level.
 * Translates native persistence exceptions into  DataAccessException hierarchy,
 * based on a given PersistenceExceptionTranslator.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see cn.taketoday.dao.DataAccessException
 * @see cn.taketoday.dao.support.PersistenceExceptionTranslator
 * @since 4.0
 */
@SuppressWarnings("serial")
public class PersistenceExceptionTranslationAdvisor extends AbstractPointcutAdvisor {

  private final PersistenceExceptionTranslationInterceptor advice;

  private final AnnotationMatchingPointcut pointcut;

  /**
   * Create a new PersistenceExceptionTranslationAdvisor.
   *
   * @param persistenceExceptionTranslator the PersistenceExceptionTranslator to use
   * @param repositoryAnnotationType the annotation type to check for
   */
  public PersistenceExceptionTranslationAdvisor(
          PersistenceExceptionTranslator persistenceExceptionTranslator,
          Class<? extends Annotation> repositoryAnnotationType) {

    this.advice = new PersistenceExceptionTranslationInterceptor(persistenceExceptionTranslator);
    this.pointcut = new AnnotationMatchingPointcut(repositoryAnnotationType, true);
  }

  /**
   * Create a new PersistenceExceptionTranslationAdvisor.
   *
   * @param beanFactory the BeanFactory to obtaining all
   * PersistenceExceptionTranslators from
   * @param repositoryAnnotationType the annotation type to check for
   */
  PersistenceExceptionTranslationAdvisor(
          BeanFactory beanFactory, Class<? extends Annotation> repositoryAnnotationType) {

    this.advice = new PersistenceExceptionTranslationInterceptor(beanFactory);
    this.pointcut = new AnnotationMatchingPointcut(repositoryAnnotationType, true);
  }

  @Override
  public Advice getAdvice() {
    return this.advice;
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

}
