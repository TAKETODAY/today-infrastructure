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

import java.util.List;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class Spr11310Tests {

  @Test
  public void orderedList() {
    ApplicationContext context = new StandardApplicationContext(Config.class);
    StringHolder holder = context.getBean(StringHolder.class);
    assertThat(holder.itemsList.get(0)).isEqualTo("second");
    assertThat(holder.itemsList.get(1)).isEqualTo("first");
    assertThat(holder.itemsList.get(2)).isEqualTo("unknownOrder");
  }

  @Test
  public void orderedArray() {
    ApplicationContext context = new StandardApplicationContext(Config.class);
    StringHolder holder = context.getBean(StringHolder.class);
    assertThat(holder.itemsArray[0]).isEqualTo("second");
    assertThat(holder.itemsArray[1]).isEqualTo("first");
    assertThat(holder.itemsArray[2]).isEqualTo("unknownOrder");
  }

  @Configuration
  static class Config {

    @Bean
    @Order(50)
    public String first() {
      return "first";
    }

    @Bean
    public String unknownOrder() {
      return "unknownOrder";
    }

    @Bean
    @Order(5)
    public String second() {
      return "second";
    }

    @Bean
    public StringHolder stringHolder() {
      return new StringHolder();
    }

  }

  private static class StringHolder {
    @Autowired
    private List<String> itemsList;

    @Autowired
    private String[] itemsArray;

  }
}
