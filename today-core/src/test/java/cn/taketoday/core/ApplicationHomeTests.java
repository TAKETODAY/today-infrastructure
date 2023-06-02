/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core;

import net.bytebuddy.ByteBuddy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 23:15
 */
class ApplicationHomeTests {

  @TempDir
  File tempDir;

  @Test
  void whenSourceClassIsProvidedThenApplicationHomeReflectsItsLocation() throws Exception {
    File app = new File(this.tempDir, "app");
    ApplicationHome applicationHome = createApplicationHome(app);
    assertThat(applicationHome.getDir()).isEqualTo(app);
  }

  @Test
  void whenSourceClassIsProvidedWithSpaceInItsPathThenApplicationHomeReflectsItsLocation() throws Exception {
    File app = new File(this.tempDir, "app location");
    ApplicationHome applicationHome = createApplicationHome(app);
    assertThat(applicationHome.getDir()).isEqualTo(app);
  }

  private ApplicationHome createApplicationHome(File location) throws Exception {
    File examplePackage = new File(location, "com/example");
    examplePackage.mkdirs();
    FileCopyUtils.copy(
            new ByteArrayInputStream(
                    new ByteBuddy().subclass(Object.class).name("com.example.Source").make().getBytes()),
            new FileOutputStream(new File(examplePackage, "Source.class")));
    try (URLClassLoader classLoader = new URLClassLoader(new URL[] { location.toURI().toURL() })) {
      Class<?> sourceClass = classLoader.loadClass("com.example.Source");
      // Separate thread to bypass stack-based unit test detection in
      // ApplicationHome
      ExecutorService executor = Executors.newSingleThreadExecutor();
      try {
        return executor.submit(() -> new ApplicationHome(sourceClass)).get();
      }
      finally {
        executor.shutdown();
      }
    }
  }

}
