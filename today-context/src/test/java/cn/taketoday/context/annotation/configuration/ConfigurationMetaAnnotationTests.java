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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Ensures that @Configuration is supported properly as a meta-annotation.
 *
 * @author Chris Beams
 */
public class ConfigurationMetaAnnotationTests {

  @Test
  public void customConfigurationStereotype() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(Config.class);
    ctx.refresh();
    assertThat(ctx.containsBean("customName")).isTrue();
    TestBean a = ctx.getBean("a", TestBean.class);
    TestBean b = ctx.getBean("b", TestBean.class);
    assertThat(b).isSameAs(a.getSpouse());
  }

  @TestConfiguration("customName")
  static class Config {
    @Bean
    public TestBean a() {
      TestBean a = new TestBean();
      a.setSpouse(b());
      return a;
    }

    @Bean
    public TestBean b() {
      return new TestBean();
    }
  }

  @Configuration
  @Retention(RetentionPolicy.RUNTIME)
  public @interface TestConfiguration {
    String value() default "";
  }
}
