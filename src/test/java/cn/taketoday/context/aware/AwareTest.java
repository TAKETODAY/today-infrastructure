/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.aware;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.core.ConfigurationException;

/**
 * @author Today <br>
 *
 * 2018-07-17 21:35:52
 */
public class AwareTest {

  private long start;

  @Before
  public void start() {
    start = System.currentTimeMillis();
  }

  @After
  public void end() {
    System.out.println("process takes " + (System.currentTimeMillis() - start) + "ms.");
  }

  @Test
  public void test_AwareBean()
          throws BeanDefinitionStoreException, NoSuchBeanDefinitionException, ConfigurationException {

    try (StandardApplicationContext applicationContext = new StandardApplicationContext()) {

      applicationContext.importBeans(AwareBean.class);
      applicationContext.refresh();

      AwareBean bean = applicationContext.getBean(AwareBean.class);
      assert bean.getApplicationContext() != null : "applicationContext == null";
      assert bean.getBeanFactory() != null : "bean factory == null";
      assert bean.getBeanName() != null : "bean name == null";
      assert bean.getEnvironment() != null : "env == null";

      System.out.println(bean);

    }

  }

}
