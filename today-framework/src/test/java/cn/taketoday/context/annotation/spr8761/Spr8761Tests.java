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

package cn.taketoday.context.annotation.spr8761;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.lang.Component;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Tests cornering the regression reported in SPR-8761.
 *
 * @author Chris Beams
 */
public class Spr8761Tests {

  /**
   * Prior to the fix for SPR-8761, this test threw because the nested MyComponent
   * annotation was being falsely considered as a 'lite' Configuration class candidate.
   */
  @Test
  public void repro() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.scan(getClass().getPackage().getName());
    ctx.refresh();
    assertThat(ctx.containsBean("withNestedAnnotation")).isTrue();
  }

}

@Component
class WithNestedAnnotation {

  @Retention(RetentionPolicy.RUNTIME)
  @Component
  public static @interface MyComponent {
  }
}
