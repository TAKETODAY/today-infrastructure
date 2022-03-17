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

import cn.taketoday.context.annotation.componentscan.cycle.left.LeftConfig;
import cn.taketoday.context.annotation.componentscan.level1.Level1Config;
import cn.taketoday.context.annotation.componentscan.level2.Level2Config;
import cn.taketoday.context.annotation.componentscan.level3.Level3Component;
import cn.taketoday.context.support.StandardApplicationContext;

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
    StandardApplicationContext ctx = new StandardApplicationContext();
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
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(LeftConfig.class); // left scans right, and right scans left
    ctx.refresh();
    ctx.getBean("leftConfig");      // but this is handled gracefully
    ctx.getBean("rightConfig");     // and beans from both packages are available
  }

}
