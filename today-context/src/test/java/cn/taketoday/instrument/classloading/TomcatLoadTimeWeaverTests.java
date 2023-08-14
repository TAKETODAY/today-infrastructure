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

package cn.taketoday.instrument.classloading;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.classpath.ClassPathOverrides;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/14 14:42
 */

class TomcatLoadTimeWeaverTests {

  @Test
  @ClassPathOverrides("org.apache.tomcat.embed:tomcat-embed-core:10.1.11")
  void tomcatLoadTimeWeaver() {
    SimpleInstrumentableClassLoader classLoader = new SimpleInstrumentableClassLoader(ClassUtils.getDefaultClassLoader());
    assertThatThrownBy(() -> new TomcatLoadTimeWeaver(classLoader))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not initialize TomcatLoadTimeWeaver because Tomcat API classes are not available");

    assertThatThrownBy(TomcatLoadTimeWeaver::new)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Could not initialize TomcatLoadTimeWeaver because Tomcat API classes are not available");

  }

}