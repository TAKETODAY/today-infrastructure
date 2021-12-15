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

import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import example.scannable.FooService;
import example.scannable.ServiceInvocationCounter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SimpleScanTests {

  protected String[] getConfigLocations() {
    return new String[] { "simpleScanTests.xml" };
  }

  @Test
  public void testFooService() throws Exception {
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(getConfigLocations(), getClass());

    FooService fooService = (FooService) ctx.getBean("fooServiceImpl");
    ServiceInvocationCounter serviceInvocationCounter = (ServiceInvocationCounter) ctx.getBean("serviceInvocationCounter");

    assertThat(serviceInvocationCounter.getCount()).isEqualTo(0);

    assertThat(fooService.isInitCalled()).isTrue();
    assertThat(serviceInvocationCounter.getCount()).isEqualTo(1);

    String value = fooService.foo(1);
    assertThat(value).isEqualTo("bar");
    assertThat(serviceInvocationCounter.getCount()).isEqualTo(2);

    fooService.foo(1);
    assertThat(serviceInvocationCounter.getCount()).isEqualTo(3);
  }

}
