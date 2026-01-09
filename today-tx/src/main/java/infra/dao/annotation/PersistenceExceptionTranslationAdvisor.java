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

package infra.dao.annotation;

import org.aopalliance.aop.Advice;

import java.lang.annotation.Annotation;

import infra.aop.Pointcut;
import infra.aop.support.AbstractPointcutAdvisor;
import infra.aop.support.annotation.AnnotationMatchingPointcut;
import infra.beans.factory.BeanFactory;
import infra.dao.support.PersistenceExceptionTranslationInterceptor;
import infra.dao.support.PersistenceExceptionTranslator;

/**
 * Framework AOP exception translation aspect for use at Repository or DAO layer level.
 * Translates native persistence exceptions into  DataAccessException hierarchy,
 * based on a given PersistenceExceptionTranslator.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see infra.dao.DataAccessException
 * @see infra.dao.support.PersistenceExceptionTranslator
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
