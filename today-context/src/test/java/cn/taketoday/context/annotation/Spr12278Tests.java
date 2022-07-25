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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanCreationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Stephane Nicoll
 */
public class Spr12278Tests {

  private AnnotationConfigApplicationContext context;

  @AfterEach
  public void close() {
    if (context != null) {
      context.close();
    }
  }

  @Test
  public void componentSingleConstructor() {
    this.context = new AnnotationConfigApplicationContext(BaseConfiguration.class,
            SingleConstructorComponent.class);
    assertThat(this.context.getBean(SingleConstructorComponent.class).autowiredName).isEqualTo("foo");
  }

  @Test
  public void componentTwoConstructorsNoHint() {
    this.context = new AnnotationConfigApplicationContext(BaseConfiguration.class,
            TwoConstructorsComponent.class);
    assertThat(this.context.getBean(TwoConstructorsComponent.class).name).isEqualTo("fallback");
  }

  @Test
  public void componentTwoSpecificConstructorsNoHint() {
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> new AnnotationConfigApplicationContext(BaseConfiguration.class, TwoSpecificConstructorsComponent.class))
            .withMessageContaining("No default constructor found");
  }

  @Configuration
  static class BaseConfiguration {

    @Bean
    public String autowiredName() {
      return "foo";
    }
  }

  private static class SingleConstructorComponent {

    private final String autowiredName;

    // No @Autowired - implicit wiring
    @SuppressWarnings("unused")
    public SingleConstructorComponent(String autowiredName) {
      this.autowiredName = autowiredName;
    }

  }

  private static class TwoConstructorsComponent {

    private final String name;

    public TwoConstructorsComponent(String name) {
      this.name = name;
    }

    @SuppressWarnings("unused")
    public TwoConstructorsComponent() {
      this("fallback");
    }
  }

  @SuppressWarnings("unused")
  private static class TwoSpecificConstructorsComponent {

    private final Integer counter;

    public TwoSpecificConstructorsComponent(Integer counter) {
      this.counter = counter;
    }

    public TwoSpecificConstructorsComponent(String name) {
      this(Integer.valueOf(name));
    }
  }

}
