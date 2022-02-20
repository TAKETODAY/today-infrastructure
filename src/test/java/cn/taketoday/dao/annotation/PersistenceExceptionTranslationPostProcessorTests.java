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

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aop.proxy.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.annotation.Aspect;
import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.aop.support.annotation.Before;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.dao.annotation.PersistenceExceptionTranslationAdvisorTests.RepositoryInterface;
import cn.taketoday.dao.annotation.PersistenceExceptionTranslationAdvisorTests.RepositoryInterfaceImpl;
import cn.taketoday.dao.annotation.PersistenceExceptionTranslationAdvisorTests.StereotypedRepositoryInterfaceImpl;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.lang.Repository;
import jakarta.persistence.PersistenceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class PersistenceExceptionTranslationPostProcessorTests {

  @Test
  @SuppressWarnings("resource")
  public void proxiesCorrectly() {
    GenericApplicationContext gac = new GenericApplicationContext();
    gac.registerBeanDefinition("translator",
            new BeanDefinition(PersistenceExceptionTranslationPostProcessor.class));
    gac.registerBeanDefinition("notProxied", new BeanDefinition(RepositoryInterfaceImpl.class));
    gac.registerBeanDefinition("proxied", new BeanDefinition(StereotypedRepositoryInterfaceImpl.class));
    gac.registerBeanDefinition("classProxied", new BeanDefinition(RepositoryWithoutInterface.class));
    gac.registerBeanDefinition("classProxiedAndAdvised",
            new BeanDefinition(RepositoryWithoutInterfaceAndOtherwiseAdvised.class));
    gac.registerBeanDefinition("myTranslator",
            new BeanDefinition(MyPersistenceExceptionTranslator.class));
    gac.registerBeanDefinition("proxyCreator",
            new BeanDefinition(AspectAutoProxyCreator.class).addPropertyValue("order", 50));
    gac.registerBeanDefinition("logger", new BeanDefinition(LogAllAspect.class));
    gac.refresh();

    RepositoryInterface shouldNotBeProxied = (RepositoryInterface) gac.getBean("notProxied");
    assertThat(AopUtils.isAopProxy(shouldNotBeProxied)).isFalse();
    RepositoryInterface shouldBeProxied = (RepositoryInterface) gac.getBean("proxied");
    assertThat(AopUtils.isAopProxy(shouldBeProxied)).isTrue();
    RepositoryWithoutInterface rwi = (RepositoryWithoutInterface) gac.getBean("classProxied");
    assertThat(AopUtils.isAopProxy(rwi)).isTrue();
    checkWillTranslateExceptions(rwi);

    Additional rwi2 = (Additional) gac.getBean("classProxiedAndAdvised");
    assertThat(AopUtils.isAopProxy(rwi2)).isTrue();
    rwi2.additionalMethod(false);
    checkWillTranslateExceptions(rwi2);
    assertThatExceptionOfType(DataAccessResourceFailureException.class).isThrownBy(() ->
                    rwi2.additionalMethod(true))
            .withMessage("my failure");
  }

  protected void checkWillTranslateExceptions(Object o) {
    assertThat(o).isInstanceOf(Advised.class);
    assertThat(((Advised) o).getAdvisors()).anyMatch(
            PersistenceExceptionTranslationAdvisor.class::isInstance);
  }

  @Repository
  public static class RepositoryWithoutInterface {

    public void nameDoesntMatter() {
    }
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.TYPE })
  public @interface Logging {

    String value() default "";
  }

  public interface Additional {

    @Logging
    void additionalMethod(boolean fail);
  }

  public static class RepositoryWithoutInterfaceAndOtherwiseAdvised extends StereotypedRepositoryInterfaceImpl
          implements Additional {

    @Override
    public void additionalMethod(boolean fail) {
      if (fail) {
        throw new PersistenceException("my failure");
      }
    }
  }

  public static class MyPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
      if (ex instanceof PersistenceException) {
        return new DataAccessResourceFailureException(ex.getMessage());
      }
      return null;
    }
  }

  @Aspect
  public static class LogAllAspect {

    @Before(Logging.class)
    public void log(MethodInvocation jp) {
      System.out.println("Before " + jp.getMethod().getName());
    }
  }

}
