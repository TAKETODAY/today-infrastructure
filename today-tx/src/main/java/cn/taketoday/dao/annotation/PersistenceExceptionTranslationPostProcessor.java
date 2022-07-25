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

import java.lang.annotation.Annotation;

import cn.taketoday.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.lang.Assert;
import cn.taketoday.stereotype.Repository;

/**
 * Bean post-processor that automatically applies persistence exception translation to any
 * bean marked with  @{@link cn.taketoday.stereotype.Repository Repository}
 * annotation, adding a corresponding {@link PersistenceExceptionTranslationAdvisor} to
 * the exposed proxy (either an existing AOP proxy or a newly generated proxy that
 * implements all of the target's interfaces).
 *
 * <p>Translates native resource exceptions to
 * {@link cn.taketoday.dao.DataAccessException DataAccessException} hierarchy.
 * Autodetects beans that implement the
 * {@link cn.taketoday.dao.support.PersistenceExceptionTranslator
 * PersistenceExceptionTranslator} interface, which are subsequently asked to translate
 * candidate exceptions.
 *
 * <p>All of  applicable resource factories (e.g.
 * {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean})
 * implement the {@code PersistenceExceptionTranslator} interface out of the box.
 * As a consequence, all that is usually needed to enable automatic exception
 * translation is marking all affected beans (such as Repositories or DAOs)
 * with the {@code @Repository} annotation, along with defining this post-processor
 * as a bean in the application context.
 *
 * <p>{@code PersistenceExceptionTranslator} beans will be sorted according
 * to  dependency ordering rules: see {@link cn.taketoday.core.Ordered}
 * and {@link Order}. Note that such beans will
 * get retrieved from any scope, not just singleton scope
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see PersistenceExceptionTranslationAdvisor
 * @see cn.taketoday.stereotype.Repository
 * @see cn.taketoday.dao.DataAccessException
 * @see cn.taketoday.dao.support.PersistenceExceptionTranslator
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
    Assert.notNull(repositoryAnnotationType, "'repositoryAnnotationType' must not be null");
    this.repositoryAnnotationType = repositoryAnnotationType;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    super.setBeanFactory(beanFactory);
    this.advisor = new PersistenceExceptionTranslationAdvisor(
            beanFactory, this.repositoryAnnotationType);
  }

}
