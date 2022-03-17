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

package cn.taketoday.aop.target;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.ProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class LazyCreationTargetSourceTests {

  @Test
  public void testCreateLazy() {
    TargetSource targetSource = new AbstractLazyCreationTargetSource() {
      @Override
      protected Object createObject() {
        return new InitCountingBean();
      }

      @Override
      public Class<?> getTargetClass() {
        return InitCountingBean.class;
      }
    };

    InitCountingBean proxy = (InitCountingBean) ProxyFactory.getProxy(targetSource);
    assertThat(InitCountingBean.initCount).as("Init count should be 0").isEqualTo(0);
    assertThat(targetSource.getTargetClass()).as("Target class incorrect").isEqualTo(InitCountingBean.class);
    assertThat(InitCountingBean.initCount).as("Init count should still be 0 after getTargetClass()").isEqualTo(0);

    proxy.doSomething();
    assertThat(InitCountingBean.initCount).as("Init count should now be 1").isEqualTo(1);

    proxy.doSomething();
    assertThat(InitCountingBean.initCount).as("Init count should still be 1").isEqualTo(1);
  }

  private static class InitCountingBean {

    public static int initCount;

    public InitCountingBean() {
      if (InitCountingBean.class.equals(getClass())) {
        // only increment when creating the actual target - not the proxy
        initCount++;
      }
    }

    public void doSomething() {
      //no-op
    }
  }

}
