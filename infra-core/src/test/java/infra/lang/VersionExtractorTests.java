/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.lang;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import jakarta.annotation.PostConstruct;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/21 18:02
 */
class VersionExtractorTests {

  private static final String SAMPLE_CLASS_NAME = VersionExtractorSample.class.getName();

  @Test
  void forClassThrowsForNullClass() {
    assertThatThrownBy(() -> VersionExtractor.forClass(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Class");
  }

  @Test
  void forClassReturnsNullForDirectoryCodeSource() {
    assertThat(VersionExtractor.forClass(VersionExtractorTests.class)).isNull();
  }

  @Test
  void forClassReadsVersionFromPackageOfJarClass() {
    String version = VersionExtractor.forClass(PostConstruct.class);
    assertThat(version).isNotNull();
    assertThat(version).isEqualTo(PostConstruct.class.getPackage().getImplementationVersion());
  }

  @Test
  void forClassUsesPackageImplementationVersion(@TempDir Path tempDir) throws Exception {
    Path jar = createJar(tempDir, "1.2.3");

    try (URLClassLoader classLoader = new URLClassLoader(new URL[] { jar.toUri().toURL() }, null)) {
      Class<?> cls = classLoader.loadClass(SAMPLE_CLASS_NAME);

      // The package is defined from the jar manifest, so the code source is never read
      assertThat(cls.getPackage().getImplementationVersion()).isEqualTo("1.2.3");
      assertThat(VersionExtractor.forClass(cls)).isEqualTo("1.2.3");
    }
  }

  @Test
  void forClassReadsManifestThroughFileCodeSource(@TempDir Path tempDir) throws Exception {
    Path jar = createJar(tempDir, "1.2.3");
    Class<?> cls = defineSampleClass(new CodeSource(jar.toUri().toURL(), (Certificate[]) null));

    // No package version is available, so the manifest has to be read from the code source
    assertThat(cls.getPackage().getImplementationVersion()).isNull();
    assertThat(VersionExtractor.forClass(cls)).isEqualTo("1.2.3");
  }

  @Test
  void forClassReadsManifestThroughJarCodeSource(@TempDir Path tempDir) throws Exception {
    Path jar = createJar(tempDir, "2.0.1");
    URL jarUrl = new URL("jar:" + jar.toUri().toURL() + "!/");
    Class<?> cls = defineSampleClass(new CodeSource(jarUrl, (Certificate[]) null));

    assertThat(cls.getPackage().getImplementationVersion()).isNull();
    assertThat(VersionExtractor.forClass(cls)).isEqualTo("2.0.1");
  }

  @Test
  void forClassReturnsNullWhenManifestHasNoImplementationVersion(@TempDir Path tempDir) throws Exception {
    Path jar = createJar(tempDir, null);
    Class<?> cls = defineSampleClass(new CodeSource(jar.toUri().toURL(), (Certificate[]) null));

    assertThat(VersionExtractor.forClass(cls)).isNull();
  }

  @Test
  void forClassReturnsNullWhenJarHasNoManifest(@TempDir Path tempDir) throws Exception {
    Path jar = createJar(tempDir, null, false);
    Class<?> cls = defineSampleClass(new CodeSource(jar.toUri().toURL(), (Certificate[]) null));

    assertThat(VersionExtractor.forClass(cls)).isNull();
  }

  @Test
  void forClassReturnsNullWhenCodeSourceIsNotAJar(@TempDir Path tempDir) throws Exception {
    Path file = Files.writeString(tempDir.resolve("plain.txt"), "not a jar");
    Class<?> cls = defineSampleClass(new CodeSource(file.toUri().toURL(), (Certificate[]) null));

    assertThat(VersionExtractor.forClass(cls)).isNull();
  }

  @Test
  void forClassReturnsNullWhenCodeSourceDoesNotExist(@TempDir Path tempDir) throws Exception {
    Path missing = tempDir.resolve("missing.jar");
    Class<?> cls = defineSampleClass(new CodeSource(missing.toUri().toURL(), (Certificate[]) null));

    assertThat(VersionExtractor.forClass(cls)).isNull();
  }

  @Test
  void forClassReturnsNullWhenCodeSourceLocationIsNull() throws Exception {
    Class<?> cls = new IsolatedClassLoader().define(
            SAMPLE_CLASS_NAME, readClassBytes(), new ProtectionDomain(null, null));

    assertThat(cls.getProtectionDomain().getCodeSource()).isNull();
    assertThat(VersionExtractor.forClass(cls)).isNull();
  }

  @Test
  void forClassIgnoresUnsupportedProtocol() throws Exception {
    URL url = new URL(null, "memory://example/test.jar", new URLStreamHandler() {
      @Override
      protected URLConnection openConnection(URL u) {
        throw new AssertionError("Should not open a connection for unsupported protocols");
      }
    });
    Class<?> cls = defineSampleClass(new CodeSource(url, (Certificate[]) null));

    assertThat(VersionExtractor.forClass(cls)).isNull();
  }

  // Helpers

  private static Class<?> defineSampleClass(CodeSource codeSource) throws Exception {
    return new IsolatedClassLoader().define(
            SAMPLE_CLASS_NAME, readClassBytes(), new ProtectionDomain(codeSource, null));
  }

  private static byte[] readClassBytes() throws IOException {
    String resource = SAMPLE_CLASS_NAME.replace('.', '/') + ".class";
    try (InputStream in = Objects.requireNonNull(
            VersionExtractorSample.class.getClassLoader().getResourceAsStream(resource))) {
      return in.readAllBytes();
    }
  }

  private static Path createJar(Path directory, @Nullable String implementationVersion)
          throws IOException {
    return createJar(directory, implementationVersion, true);
  }

  private static Path createJar(Path directory, @Nullable String implementationVersion,
          boolean withManifest) throws IOException {
    Path jarPath = Files.createTempFile(directory, "version-extractor", ".jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (implementationVersion != null) {
      manifest.getMainAttributes().put(Attributes.Name.IMPLEMENTATION_VERSION, implementationVersion);
    }

    try (JarOutputStream jar = withManifest
            ? new JarOutputStream(Files.newOutputStream(jarPath), manifest)
            : new JarOutputStream(Files.newOutputStream(jarPath))) {
      jar.putNextEntry(new JarEntry(SAMPLE_CLASS_NAME.replace('.', '/') + ".class"));
      jar.write(readClassBytes());
      jar.closeEntry();
    }
    return jarPath;
  }

  private static final class IsolatedClassLoader extends ClassLoader {

    IsolatedClassLoader() {
      super(null);
    }

    Class<?> define(String name, byte[] bytes, ProtectionDomain protectionDomain) {
      return defineClass(name, bytes, 0, bytes.length, protectionDomain);
    }
  }

}
