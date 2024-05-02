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

package cn.taketoday.app.loader;

import org.junit.jupiter.api.Test;

import java.net.URL;

import cn.taketoday.app.loader.jarmode.JarMode;
import cn.taketoday.app.loader.zip.AssertFileChannelDataBlocksClosed;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LaunchedClassLoader}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@AssertFileChannelDataBlocksClosed
class LaunchedClassLoaderTests {

  @Test
  void loadClassWhenJarModeClassLoadsInLaunchedClassLoader() throws Exception {
    try (LaunchedClassLoader classLoader = new LaunchedClassLoader(false, new URL[] {},
            getClass().getClassLoader())) {
      Class<?> jarModeClass = classLoader.loadClass(JarMode.class.getName());
      Class<?> jarModeRunnerClass = classLoader.loadClass(JarModeRunner.class.getName());
      assertThat(jarModeClass.getClassLoader()).isSameAs(classLoader);
      assertThat(jarModeRunnerClass.getClassLoader()).isSameAs(classLoader);
    }
  }

}
