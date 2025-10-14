/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.dao.annotation;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import infra.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import infra.aop.framework.Advised;
import infra.aop.support.AopUtils;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.support.GenericApplicationContext;
import infra.dao.DataAccessException;
import infra.dao.DataAccessResourceFailureException;
import infra.dao.annotation.PersistenceExceptionTranslationAdvisorTests.RepositoryInterface;
import infra.dao.annotation.PersistenceExceptionTranslationAdvisorTests.RepositoryInterfaceImpl;
import infra.dao.annotation.PersistenceExceptionTranslationAdvisorTests.StereotypedRepositoryInterfaceImpl;
import infra.dao.support.PersistenceExceptionTranslator;
import infra.stereotype.Repository;
import jakarta.persistence.PersistenceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
class PersistenceExceptionTranslationPostProcessorTests {

  @Test
  @SuppressWarnings("resource")
  public void proxiesCorrectly() {
    GenericApplicationContext gac = new GenericApplicationContext();
    gac.registerBeanDefinition("translator",
            new RootBeanDefinition(PersistenceExceptionTranslationPostProcessor.class));
    gac.registerBeanDefinition("notProxied", new RootBeanDefinition(RepositoryInterfaceImpl.class));
    gac.registerBeanDefinition("proxied", new RootBeanDefinition(StereotypedRepositoryInterfaceImpl.class));
    gac.registerBeanDefinition("classProxied", new RootBeanDefinition(RepositoryWithoutInterface.class));
    gac.registerBeanDefinition("classProxiedAndAdvised",
            new RootBeanDefinition(RepositoryWithoutInterfaceAndOtherwiseAdvised.class));
    gac.registerBeanDefinition("myTranslator",
            new RootBeanDefinition(MyPersistenceExceptionTranslator.class));
    gac.registerBeanDefinition("proxyCreator",
            BeanDefinitionBuilder.rootBeanDefinition(AnnotationAwareAspectJAutoProxyCreator.class).
                    addPropertyValue("order", 50).getBeanDefinition());
    gac.registerBeanDefinition("logger", new RootBeanDefinition(LogAllAspect.class));
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

  @Test
  void constructorCreatesPostProcessorWithDefaultRepositoryAnnotation() {
    PersistenceExceptionTranslationPostProcessor postProcessor = new PersistenceExceptionTranslationPostProcessor();

    assertThat(postProcessor).isNotNull();
  }

  @Test
  void setRepositoryAnnotationTypeWithValidType() {
    PersistenceExceptionTranslationPostProcessor postProcessor = new PersistenceExceptionTranslationPostProcessor();
    postProcessor.setRepositoryAnnotationType(Repository.class);

    assertThat(postProcessor).isNotNull();
  }

  @Test
  void setRepositoryAnnotationTypeWithNullTypeThrowsException() {
    PersistenceExceptionTranslationPostProcessor postProcessor = new PersistenceExceptionTranslationPostProcessor();

    assertThatThrownBy(() -> postProcessor.setRepositoryAnnotationType(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'repositoryAnnotationType' is required");
  }

  @Test
  void setBeanFactoryInitializesAdvisor() {
    PersistenceExceptionTranslationPostProcessor postProcessor = new PersistenceExceptionTranslationPostProcessor();
    GenericApplicationContext beanFactory = new GenericApplicationContext();

    postProcessor.setBeanFactory(beanFactory);

    assertThat(postProcessor).extracting("advisor").isNotNull();
    assertThat(postProcessor).extracting("advisor").isInstanceOf(PersistenceExceptionTranslationAdvisor.class);
  }

  @Test
  void postProcessorWithCustomRepositoryAnnotation() {
    GenericApplicationContext gac = new GenericApplicationContext();
    PersistenceExceptionTranslationPostProcessor postProcessor = new PersistenceExceptionTranslationPostProcessor();
    postProcessor.setRepositoryAnnotationType(Repository.class);

    gac.registerBeanDefinition("translator",
            new RootBeanDefinition(PersistenceExceptionTranslationPostProcessor.class));
    gac.registerBeanDefinition("proxied",
            new RootBeanDefinition(StereotypedRepositoryInterfaceImpl.class));
    gac.registerBeanDefinition("myTranslator",
            new RootBeanDefinition(MyPersistenceExceptionTranslator.class));
    gac.refresh();

    RepositoryInterface shouldBeProxied = (RepositoryInterface) gac.getBean("proxied");
    assertThat(AopUtils.isAopProxy(shouldBeProxied)).isTrue();
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

  public interface Additional {

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

    @Before("execution(void *.additionalMethod(*))")
    public void log(JoinPoint jp) {
      System.out.println("Before " + jp.getSignature().getName());
    }
  }

}
