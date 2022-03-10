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

package cn.taketoday.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Example class used to test {@link AnnotationsScanner} with enclosing classes.
 *
 * @author Phillip Webb
 * @since 4.0
 */
@AnnotationEnclosingClassSample.EnclosedOne
public class AnnotationEnclosingClassSample {

  @EnclosedTwo
  public static class EnclosedStatic {

    @EnclosedThree
    public static class EnclosedStaticStatic {

    }

  }

  @EnclosedTwo
  public class EnclosedInner {

    @EnclosedThree
    public class EnclosedInnerInner {

    }

  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface EnclosedOne {

  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface EnclosedTwo {

  }

  @Retention(RetentionPolicy.RUNTIME)
  public static @interface EnclosedThree {

  }

}
