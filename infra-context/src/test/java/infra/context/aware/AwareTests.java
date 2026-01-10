/*
 * Copyright 2017 - 2026 the TODAY authors.
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
