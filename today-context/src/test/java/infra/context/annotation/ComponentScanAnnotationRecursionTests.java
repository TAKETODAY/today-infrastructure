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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import infra.context.annotation.componentscan.cycle.left.LeftConfig;
import infra.context.annotation.componentscan.level1.Level1Config;
import infra.context.annotation.componentscan.level2.Level2Config;
import infra.context.annotation.componentscan.level3.Level3Component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests ensuring that configuration classes marked with @ComponentScan
 * may be processed recursively
 *
 * @author Chris Beams
 * @since 4.0
 */
public class ComponentScanAnnotationRecursionTests {

  @Test
  public void recursion() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(Level1Config.class);
    ctx.refresh();

    // assert that all levels have been detected
    ctx.getBean(Level1Config.class);
    ctx.getBean(Level2Config.class);
    ctx.getBean(Level3Component.class);

    // assert that enhancement is working
    assertThat(ctx.getBean("level1Bean")).isSameAs(ctx.getBean("level1Bean"));
    assertThat(ctx.getBean("level2Bean")).isSameAs(ctx.getBean("level2Bean"));
  }

  public void evenCircularScansAreSupported() {
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.register(LeftConfig.class); // left scans right, and right scans left
    ctx.refresh();
    ctx.getBean("leftConfig");      // but this is handled gracefully
    ctx.getBean("rightConfig");     // and beans from both packages are available
  }

}
