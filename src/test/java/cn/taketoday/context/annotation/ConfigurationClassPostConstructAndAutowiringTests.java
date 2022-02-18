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

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests cornering the issue reported in SPR-8080. If the product of a @Bean method
 * was @Autowired into a configuration class while at the same time the declaring
 * configuration class for the @Bean method in question has a @PostConstruct
 * (or other initializer) method, the container would become confused about the
 * 'currently in creation' status of the autowired bean and result in creating multiple
 * instances of the given @Bean, violating container scoping / singleton semantics.
 *
 * <p>This is resolved through no longer relying on 'currently in creation' status, but
 * rather on a thread local that informs the enhanced bean method implementation whether
 * the factory is the caller or not.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/13 21:51
 */
public class ConfigurationClassPostConstructAndAutowiringTests {

  /**
   * Prior to the fix for SPR-8080, this method would succeed due to ordering of
   * configuration class registration.
   */
  @Test
  public void control() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(Config1.class, Config2.class);
    ctx.refresh();

    assertions(ctx);

    Config2 config2 = ctx.getBean(Config2.class);
    assertThat(config2.testBean).isEqualTo(ctx.getBean(TestBean.class));
  }

  /**
   * Prior to the fix for SPR-8080, this method would fail due to ordering of
   * configuration class registration.
   */
  @Test
  public void originalReproCase() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(Config2.class, Config1.class);
    ctx.refresh();

    assertions(ctx);
  }

  private void assertions(StandardApplicationContext ctx) {
    Config1 config1 = ctx.getBean(Config1.class);
    TestBean testBean = ctx.getBean(TestBean.class);
    assertThat(config1.beanMethodCallCount).isEqualTo(1);
    assertThat(testBean.getAge()).isEqualTo(2);
  }

  @Configuration
  static class Config1 {

    int beanMethodCallCount = 0;

    @PostConstruct
    public void init() {
      beanMethod().setAge(beanMethod().getAge() + 1); // age == 2
    }

    @Bean
    public TestBean beanMethod() {
      beanMethodCallCount++;
      TestBean testBean = new TestBean();
      testBean.setAge(1);
      return testBean;
    }
  }

  @Configuration
  static class Config2 {

    TestBean testBean;

    @Autowired
    void setTestBean(TestBean testBean) {
      this.testBean = testBean;
    }
  }

}
