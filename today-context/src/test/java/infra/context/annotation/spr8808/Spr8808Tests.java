/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.context.annotation.spr8808;

import org.junit.jupiter.api.Test;

import infra.context.annotation.ComponentScan;
import infra.context.annotation.Configuration;
import infra.context.annotation.AnnotationConfigApplicationContext;

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
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Config.class);
    ctx.refresh();
  }

}

@Configuration
@ComponentScan(basePackageClasses = Spr8808Tests.class) // scan *this* package
class Config {
}
