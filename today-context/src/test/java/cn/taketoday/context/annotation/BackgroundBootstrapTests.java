/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;

import static cn.taketoday.stereotype.Component.Bootstrap.BACKGROUND;

/**
 * @author Juergen Hoeller
 */
class BackgroundBootstrapTests {

  @Test
  @Timeout(5)
  void bootstrapWithCustomExecutor() {
    var ctx = new AnnotationConfigApplicationContext(CustomExecutorBeanConfig.class);
    ctx.getBean("testBean1", TestBean.class);
    ctx.getBean("testBean2", TestBean.class);
    ctx.getBean("testBean3", TestBean.class);
    ctx.close();
  }

  @Configuration
  static class CustomExecutorBeanConfig {

    @Bean
    public ThreadPoolTaskExecutor bootstrapExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setThreadNamePrefix("Custom-");
      executor.setCorePoolSize(2);
      executor.initialize();
      return executor;
    }

    @Bean(bootstrap = BACKGROUND)
    @DependsOn("testBean3")
    public TestBean testBean1(TestBean testBean3) throws InterruptedException {
      Thread.sleep(3000);
      return new TestBean();
    }

    @Bean(bootstrap = BACKGROUND)
    @Lazy
    public TestBean testBean2() throws InterruptedException {
      Thread.sleep(3000);
      return new TestBean();
    }

    @Bean
    @Lazy
    public TestBean testBean3() {
      return new TestBean();
    }

    @Bean
    public String dependent(@Lazy TestBean testBean1, @Lazy TestBean testBean2, @Lazy TestBean testBean3) {
      return "";
    }
  }

}
