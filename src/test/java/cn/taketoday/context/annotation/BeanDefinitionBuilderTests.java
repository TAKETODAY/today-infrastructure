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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.lang.Singleton;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author TODAY 2021/10/3 22:35
 */
class BeanDefinitionBuilderTests {

  @Singleton
  public static class TestBean {

  }

  @Test
  void testBuildBeanDefinitions() throws Exception {

    List<BeanDefinition> beanDefinitions = BeanDefinitionBuilder.from(getClass());

    assert beanDefinitions.size() == 1;

    beanDefinitions = BeanDefinitionBuilder.from(TestBean.class);
    assert beanDefinitions.size() == 1;

    final BeanDefinition beanDefinition = beanDefinitions.get(0);
    beanDefinition.setDestroyMethod(null);
    beanDefinition.setInitMethods((String[]) null);
    beanDefinition.setScope(null);
    beanDefinition.setPropertyValues();
    beanDefinition.setName(null);

    try {
      beanDefinition.validate();
      fail("beanDefinition");
    }
    catch (ConfigurationException e) {
      assert true;
    }

  }

}
