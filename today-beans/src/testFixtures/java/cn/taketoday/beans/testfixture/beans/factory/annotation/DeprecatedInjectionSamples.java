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

package cn.taketoday.beans.testfixture.beans.factory.annotation;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.env.Environment;

public abstract class DeprecatedInjectionSamples {

  @Deprecated
  public static class DeprecatedEnvironment { }

  @Deprecated
  public static class DeprecatedSample {

    @Autowired
    Environment environment;

  }

  public static class DeprecatedFieldInjectionPointSample {

    @Autowired
    @Deprecated
    Environment environment;

  }

  public static class DeprecatedFieldInjectionTypeSample {

    @Autowired
    DeprecatedEnvironment environment;
  }

  public static class DeprecatedPrivateFieldInjectionTypeSample {

    @Autowired
    private DeprecatedEnvironment environment;
  }

  public static class DeprecatedMethodInjectionPointSample {

    @Autowired
    @Deprecated
    void setEnvironment(Environment environment) { }
  }

  public static class DeprecatedMethodInjectionTypeSample {

    @Autowired
    void setEnvironment(DeprecatedEnvironment environment) { }
  }

  public static class DeprecatedPrivateMethodInjectionTypeSample {

    @Autowired
    private void setEnvironment(DeprecatedEnvironment environment) { }
  }

}
