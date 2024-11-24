/*
 * Copyright 2017 - 2023 the original author or authors.
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
package infra.context.aware;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Today <br>
 *
 * 2018-07-17 21:35:52
 */
class AwareTests {

  @Test
  void awareBean() throws BeanDefinitionStoreException, NoSuchBeanDefinitionException {

    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    applicationContext.register(AwareBean.class);
    applicationContext.refresh();

    AwareBean bean = applicationContext.getBean(AwareBean.class);
    assert bean.getApplicationContext() != null : "applicationContext == null";
    assert bean.getBeanFactory() != null : "bean factory == null";
    assert bean.getBeanName() != null : "bean name == null";
    assert bean.getEnvironment() != null : "env == null";

    applicationContext.close();

  }

}
