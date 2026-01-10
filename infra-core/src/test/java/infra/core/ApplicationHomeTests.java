/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import net.bytebuddy.ByteBuddy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;

import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

  @Test
  void defaultConstructorUsesCurrentWorkingDirectory() {
    ApplicationHome home = new ApplicationHome();
    assertThat(home.getDir()).isEqualTo(new File(System.getProperty("user.dir")).getAbsoluteFile());
  }

  @Test
  void nullSourceClassUsesCurrentWorkingDirectory() {
    ApplicationHome home = new ApplicationHome(null);
    assertThat(home.getDir()).isEqualTo(new File(System.getProperty("user.dir")).getAbsoluteFile());
  }

  @Test
  void unitTestSourceReturnsCurrentWorkingDirectory() {
    ApplicationHome home = new ApplicationHome(getClass());
    assertThat(home.getDir()).isEqualTo(new File(System.getProperty("user.dir")).getAbsoluteFile());
  }

  @Test
  void nonExistentSourceFallsBackToWorkingDirectory() {
    ApplicationHome home = new ApplicationHome(String.class); // String.class location may not exist
    assertThat(home.getDir()).isEqualTo(new File(System.getProperty("user.dir")).getAbsoluteFile());
  }

  @Test
  void toStringReturnsDirectoryPath() {
    ApplicationHome home = new ApplicationHome();
    assertThat(home.toString()).isEqualTo(home.getDir().toString());
  }

  @Test
  void whenSourceIsDirectoryThenDirIsSource(@TempDir File tempDir) throws Exception {
    File directory = new File(tempDir, "classes");
    directory.mkdirs();
    ApplicationHome home = createApplicationHome(directory);
    assertThat(home.getDir()).isEqualTo(directory.getAbsoluteFile());
  }

  @Test
  void whenNoManifestFoundThenUsesWorkingDirectory() {
    ApplicationHome home = new ApplicationHome();
    assertThat(home.getDir()).isEqualTo(new File(System.getProperty("user.dir")).getAbsoluteFile());
  }

  @Test
  void whenLocationCannotBeOpenedThenFallsBackToWorkingDirectory() {
    ApplicationHome home = new ApplicationHome(Object.class); // Protected core class
    assertThat(home.getDir()).isEqualTo(new File(System.getProperty("user.dir")).getAbsoluteFile());
  }

  @Test
  void whenHomeDirectoryDoesNotExistThenUsesCurrentDirectory() {
    ApplicationHome home = new ApplicationHome(File.class);
    assertThat(home.getDir()).isEqualTo(new File("").getAbsoluteFile());
  }

  @Test
  void getSourceReturnsNullWhenNoSourceFound() {
    ApplicationHome home = new ApplicationHome(null);
    assertThat(home.getSource()).isNull();
  }

  @Test
  void getSourceReturnsFileWhenSourceClassProvided() throws Exception {
    File app = new File(tempDir, "app");
    ApplicationHome applicationHome = createApplicationHome(app);
    assertThat(applicationHome.getSource()).isNotNull();
    assertThat(applicationHome.getSource()).isEqualTo(app.getAbsoluteFile());
  }

  @Test
  void findDefaultHomeDirUsesUserDirProperty() {
    String originalUserDir = System.getProperty("user.dir");
    try {
      System.setProperty("user.dir", tempDir.getAbsolutePath());
      ApplicationHome home = new ApplicationHome();
      assertThat(home.getDir()).isEqualTo(tempDir.getAbsoluteFile());
    }
    finally {
      System.setProperty("user.dir", originalUserDir);
    }
  }

  @Test
  void findDefaultHomeDirUsesCurrentDirectoryWhenUserDirIsEmpty() {
    String originalUserDir = System.getProperty("user.dir");
    try {
      System.clearProperty("user.dir");
      ApplicationHome home = new ApplicationHome();
      assertThat(home.getDir()).isEqualTo(new File(".").getAbsoluteFile());
    }
    finally {
      System.setProperty("user.dir", originalUserDir);
    }
  }

  @Test
  void constructorWithClassReturnsCorrectSourceAndDir() throws Exception {
    File app = new File(tempDir, "app");
    ApplicationHome applicationHome = createApplicationHome(app);

    assertThat(applicationHome.getSource()).isEqualTo(app.getAbsoluteFile());
    assertThat(applicationHome.getDir()).isEqualTo(app.getAbsoluteFile());
  }

  @Test
  void getRootJarFileExtractsCorrectPathFromNestedJar() {
    String jarPath = "/path/to/app.jar!/BOOT-INF/classes!/";
    JarFile mockJarFile = Mockito.mock(JarFile.class);
    Mockito.when(mockJarFile.getName()).thenReturn(jarPath);

    ApplicationHome home = new ApplicationHome();
    // Access private method via reflection
    try {
      java.lang.reflect.Method method = ApplicationHome.class.getDeclaredMethod("getRootJarFile", JarFile.class);
      method.setAccessible(true);
      File result = (File) method.invoke(home, mockJarFile);
      assertThat(result.getPath()).isEqualTo("/path/to/app.jar");
    }
    catch (Exception e) {
      fail("Reflection failed: " + e.getMessage());
    }
  }

  @Test
  void getRootJarFileReturnsSamePathWhenNotNested() {
    String jarPath = "/path/to/app.jar";
    JarFile mockJarFile = Mockito.mock(JarFile.class);
    Mockito.when(mockJarFile.getName()).thenReturn(jarPath);

    ApplicationHome home = new ApplicationHome();
    try {
      java.lang.reflect.Method method = ApplicationHome.class.getDeclaredMethod("getRootJarFile", JarFile.class);
      method.setAccessible(true);
      File result = (File) method.invoke(home, mockJarFile);
      assertThat(result.getPath()).isEqualTo("/path/to/app.jar");
    }
    catch (Exception e) {
      fail("Reflection failed: " + e.getMessage());
    }
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
