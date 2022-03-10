/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.aspectj.autoproxy;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 */
public class AspectJAutoProxyCreatorAndLazyInitTargetSourceTests {

  @Test
  public void testAdrian() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

    ITestBean adrian = (ITestBean) ctx.getBean("adrian");
    assertThat(LazyTestBean.instantiations).isEqualTo(0);
    assertThat(adrian).isNotNull();
    adrian.getAge();
    assertThat(adrian.getAge()).isEqualTo(68);
    assertThat(LazyTestBean.instantiations).isEqualTo(1);
  }

}

class LazyTestBean extends TestBean {

  public static int instantiations;

  public LazyTestBean() {
    ++instantiations;
  }

}
