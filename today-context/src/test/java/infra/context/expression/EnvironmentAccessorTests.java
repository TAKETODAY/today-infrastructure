/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package infra.context.expression;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.context.support.GenericApplicationContext;
import infra.core.testfixture.env.MockPropertySource;

import static infra.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/11 21:34
 */
class EnvironmentAccessorTests {

  @Test
  public void braceAccess() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("testBean",
            genericBeanDefinition(TestBean.class)
                    .addPropertyValue("name", "#{environment['my.name']}")
                    .getBeanDefinition());

    GenericApplicationContext ctx = new GenericApplicationContext(bf);
    ctx.getEnvironment().getPropertySources().addFirst(new MockPropertySource().withProperty("my.name", "myBean"));
    ctx.refresh();

    assertThat(ctx.getBean(TestBean.class).getName()).isEqualTo("myBean");
    ctx.close();
  }

}
