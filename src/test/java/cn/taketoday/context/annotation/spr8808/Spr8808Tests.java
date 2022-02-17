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

package cn.taketoday.context.annotation.spr8808;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;

/**
 * Tests cornering the bug in which @Configuration classes that @ComponentScan themselves
 * would result in a ConflictingBeanDefinitionException.
 *
 * @author Chris Beams
 * @since 4.0
 */
public class Spr8808Tests {

  /**
   * This test failed with ConflictingBeanDefinitionException prior to fixes for
   * SPR-8808.
   */
  @Test
  public void repro() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(Config.class);
    ctx.refresh();
  }

}

@Configuration
@ComponentScan(basePackageClasses = Spr8808Tests.class) // scan *this* package
class Config {
}
