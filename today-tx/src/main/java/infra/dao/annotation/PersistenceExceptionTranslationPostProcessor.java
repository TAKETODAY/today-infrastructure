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

import java.lang.annotation.Annotation;

import infra.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import infra.beans.factory.BeanFactory;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.lang.Assert;
import infra.stereotype.Repository;

/**
 * Bean post-processor that automatically applies persistence exception translation to any
 * bean marked with  @{@link Repository Repository}
 * annotation, adding a corresponding {@link PersistenceExceptionTranslationAdvisor} to
 * the exposed proxy (either an existing AOP proxy or a newly generated proxy that
 * implements all of the target's interfaces).
 *
 * <p>Translates native resource exceptions to
 * {@link infra.dao.DataAccessException DataAccessException} hierarchy.
 * Autodetects beans that implement the
 * {@link infra.dao.support.PersistenceExceptionTranslator
 * PersistenceExceptionTranslator} interface, which are subsequently asked to translate
 * candidate exceptions.
 *
 * <p>All of  applicable resource factories (e.g.
 * {@link infra.orm.jpa.LocalContainerEntityManagerFactoryBean})
 * implement the {@code PersistenceExceptionTranslator} interface out of the box.
 * As a consequence, all that is usually needed to enable automatic exception
 * translation is marking all affected beans (such as Repositories or DAOs)
 * with the {@code @Repository} annotation, along with defining this post-processor
 * as a bean in the application context.
 *
 * <p>{@code PersistenceExceptionTranslator} beans will be sorted according
 * to  dependency ordering rules: see {@link Ordered}
 * and {@link Order}. Note that such beans will
 * get retrieved from any scope, not just singleton scope
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see PersistenceExceptionTranslationAdvisor
 * @see Repository
 * @see infra.dao.DataAccessException
 * @see infra.dao.support.PersistenceExceptionTranslator
 * @since 4.0
 */
@SuppressWarnings("serial")
public class PersistenceExceptionTranslationPostProcessor extends AbstractBeanFactoryAwareAdvisingPostProcessor {

  private Class<? extends Annotation> repositoryAnnotationType = Repository.class;

  /**
   * Set the 'repository' annotation type.
   * The default repository annotation type is the {@link Repository} annotation.
   * <p>This setter property exists so that developers can provide their own
   * (non-Framework-specific) annotation type to indicate that a class has a
   * repository role.
   *
   * @param repositoryAnnotationType the desired annotation type
   */
  public void setRepositoryAnnotationType(Class<? extends Annotation> repositoryAnnotationType) {
    Assert.notNull(repositoryAnnotationType, "'repositoryAnnotationType' is required");
    this.repositoryAnnotationType = repositoryAnnotationType;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    super.setBeanFactory(beanFactory);
    this.advisor = new PersistenceExceptionTranslationAdvisor(
            beanFactory, this.repositoryAnnotationType);
  }

}
