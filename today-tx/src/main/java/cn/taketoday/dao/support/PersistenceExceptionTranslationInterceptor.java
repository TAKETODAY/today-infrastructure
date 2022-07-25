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

package cn.taketoday.dao.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

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
 * @see PersistenceExceptionTranslator
 * @since 2.0
 */
public class PersistenceExceptionTranslationInterceptor
        implements MethodInterceptor, BeanFactoryAware, InitializingBean {

  @Nullable
  private volatile PersistenceExceptionTranslator persistenceExceptionTranslator;

  private boolean alwaysTranslate = false;

  @Nullable
  private BeanFactory beanFactory;

  /**
   * Create a new PersistenceExceptionTranslationInterceptor.
   * Needs to be configured with a PersistenceExceptionTranslator afterwards.
   *
   * @see #setPersistenceExceptionTranslator
   */
  public PersistenceExceptionTranslationInterceptor() { }

  /**
   * Create a new PersistenceExceptionTranslationInterceptor
   * for the given PersistenceExceptionTranslator.
   *
   * @param pet the PersistenceExceptionTranslator to use
   */
  public PersistenceExceptionTranslationInterceptor(PersistenceExceptionTranslator pet) {
    Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
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
    Assert.notNull(beanFactory, "BeanFactory must not be null");
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
   * applicable exceptions, independent from the originating method signature.
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
                  "Cannot use PersistenceExceptionTranslator autodetection without BeanFactory");
          translator = detectPersistenceExceptionTranslators(this.beanFactory);
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
    bf.getObjectSupplier(PersistenceExceptionTranslator.class, false)
            .orderedStream()
            .forEach(cpet::addDelegate);
    return cpet;
  }

}
