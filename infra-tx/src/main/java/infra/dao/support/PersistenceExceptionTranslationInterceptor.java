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

package infra.dao.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.BeanCreationNotAllowedException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.InitializingBean;
import infra.lang.Assert;
import infra.util.ReflectionUtils;

/**
 * AOP Alliance MethodInterceptor that provides persistence exception translation
 * based on a given PersistenceExceptionTranslator.
 *
 * <p>Delegates to the given {@link PersistenceExceptionTranslator} to translate
 * a RuntimeException thrown into  DataAccessException hierarchy
 * (if appropriate). If the RuntimeException in question is declared on the
 * target method, it is always propagated as-is (with no translation applied).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see PersistenceExceptionTranslator
 * @since 2.0
 */
public class PersistenceExceptionTranslationInterceptor
        implements MethodInterceptor, BeanFactoryAware, InitializingBean {

  private boolean alwaysTranslate = false;

  private @Nullable BeanFactory beanFactory;

  private volatile @Nullable PersistenceExceptionTranslator persistenceExceptionTranslator;

  /**
   * Create a new PersistenceExceptionTranslationInterceptor.
   * Needs to be configured with a PersistenceExceptionTranslator afterwards.
   *
   * @see #setPersistenceExceptionTranslator
   */
  public PersistenceExceptionTranslationInterceptor() {
  }

  /**
   * Create a new PersistenceExceptionTranslationInterceptor
   * for the given PersistenceExceptionTranslator.
   *
   * @param pet the PersistenceExceptionTranslator to use
   */
  public PersistenceExceptionTranslationInterceptor(PersistenceExceptionTranslator pet) {
    Assert.notNull(pet, "PersistenceExceptionTranslator is required");
    this.persistenceExceptionTranslator = pet;
  }

  /**
   * Create a new PersistenceExceptionTranslationInterceptor, autodetecting
   * PersistenceExceptionTranslators in the given BeanFactory.
   *
   * @param beanFactory the BeanFactory to obtaining all
   * PersistenceExceptionTranslators from
   */
  public PersistenceExceptionTranslationInterceptor(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanFactory = beanFactory;
  }

  /**
   * Specify the PersistenceExceptionTranslator to use.
   * <p>Default is to autodetect all PersistenceExceptionTranslators
   * in the containing BeanFactory, using them in a chain.
   *
   * @see #detectPersistenceExceptionTranslators
   */
  public void setPersistenceExceptionTranslator(PersistenceExceptionTranslator pet) {
    this.persistenceExceptionTranslator = pet;
  }

  /**
   * Specify whether to always translate the exception ("true"), or whether throw the
   * raw exception when declared, i.e. when the originating method signature's exception
   * declarations allow for the raw exception to be thrown ("false").
   * <p>Default is "false". Switch this flag to "true" in order to always translate
   * applicable exceptions, independent of the originating method signature.
   * <p>Note that the originating method does not have to declare the specific exception.
   * Any base class will do as well, even {@code throws Exception}: As long as the
   * originating method does explicitly declare compatible exceptions, the raw exception
   * will be rethrown. If you would like to avoid throwing raw exceptions in any case,
   * switch this flag to "true".
   */
  public void setAlwaysTranslate(boolean alwaysTranslate) {
    this.alwaysTranslate = alwaysTranslate;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    if (this.persistenceExceptionTranslator == null) {
      // No explicit exception translator specified - perform autodetection.
      this.beanFactory = beanFactory;
    }
  }

  @Override
  public void afterPropertiesSet() {
    if (this.persistenceExceptionTranslator == null && this.beanFactory == null) {
      throw new IllegalArgumentException("Property 'persistenceExceptionTranslator' is required");
    }
  }

  @Override
  @Nullable
  public Object invoke(MethodInvocation mi) throws Throwable {
    try {
      return mi.proceed();
    }
    catch (RuntimeException ex) {
      // Let it throw raw if the type of the exception is on the throws clause of the method.
      if (!this.alwaysTranslate && ReflectionUtils.declaresException(mi.getMethod(), ex.getClass())) {
        throw ex;
      }
      else {
        PersistenceExceptionTranslator translator = this.persistenceExceptionTranslator;
        if (translator == null) {
          Assert.state(this.beanFactory != null,
                  "Cannot use PersistenceExceptionTranslator auto-detection without ListableBeanFactory");
          try {
            translator = detectPersistenceExceptionTranslators(this.beanFactory);
          }
          catch (BeanCreationNotAllowedException ex2) {
            // Cannot create PersistenceExceptionTranslator bean on shutdown:
            // fall back to rethrowing original exception without translation
            throw ex;
          }
          this.persistenceExceptionTranslator = translator;
        }
        throw DataAccessUtils.translateIfNecessary(ex, translator);
      }
    }
  }

  /**
   * Detect all PersistenceExceptionTranslators in the given BeanFactory.
   *
   * @param bf the BeanFactory to obtain PersistenceExceptionTranslators from
   * @return a chained PersistenceExceptionTranslator, combining all
   * PersistenceExceptionTranslators found in the given bean factory
   * @see ChainedPersistenceExceptionTranslator
   */
  protected PersistenceExceptionTranslator detectPersistenceExceptionTranslators(BeanFactory bf) {
    // Find all translators, being careful not to activate FactoryBeans.
    ChainedPersistenceExceptionTranslator cpet = new ChainedPersistenceExceptionTranslator();
    for (var translator : bf.getBeanProvider(PersistenceExceptionTranslator.class, false)) {
      cpet.addDelegate(translator);
    }
    return cpet;
  }

}
