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
package cn.taketoday.context.annotation4;

import cn.taketoday.beans.Primary;
import cn.taketoday.beans.factory.support.TestBean;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Qualifier;
import cn.taketoday.lang.Value;

/**
 * Class used to test the functionality of factory method bean definitions
 * declared inside a Spring component class.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 */
@Component
public class FactoryMethodComponent {

  private int i;

  public static TestBean nullInstance() {
    return null;
  }

  @Bean
  @Qualifier("public")
  public TestBean publicInstance() {
    return new TestBean("publicInstance");
  }

  // to be ignored
  public TestBean publicInstance(boolean doIt) {
    return new TestBean("publicInstance");
  }

  @Bean
  protected TestBean protectedInstance(
          @Qualifier("public") TestBean spouse, @Value("#{privateInstance.age}") String country) {
    TestBean tb = new TestBean("protectedInstance", 1);
    tb.setSpouse(tb);
    tb.setCountry(country);
    return tb;
  }

  @Bean
  @Scope("prototype")
  private TestBean privateInstance() {
    return new TestBean("privateInstance", i++);
  }

  @Bean
  @Scope(value = "request"/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
  public TestBean requestScopedInstance() {
    return new TestBean("requestScopedInstance", 3);
  }

  @Bean
  @Primary
  public DependencyBean secondInstance() {
    return new DependencyBean();
  }

}
