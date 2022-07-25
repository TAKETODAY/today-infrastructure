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

package cn.taketoday.aop.aspectj;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aop.testfixture.interceptor.CallCountingInterceptor;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class AspectJExpressionPointcutAdvisorTests {

  private ITestBean testBean;

  private CallCountingInterceptor interceptor;

  @BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    testBean = (ITestBean) ctx.getBean("testBean");
    interceptor = (CallCountingInterceptor) ctx.getBean("interceptor");
  }

  @Test
  public void testPointcutting() {
    assertThat(interceptor.getCount()).as("Count should be 0").isEqualTo(0);
    testBean.getSpouses();
    assertThat(interceptor.getCount()).as("Count should be 1").isEqualTo(1);
    testBean.getSpouse();
    assertThat(interceptor.getCount()).as("Count should be 1").isEqualTo(1);
  }

}


