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

package infra.app.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import infra.app.env.ConfigTreePropertySource;
import infra.core.conversion.ConversionService;
import infra.core.conversion.support.ConfigurableConversionService;
import infra.core.env.StandardEnvironment;
import infra.core.io.InputStreamSource;
import infra.format.support.ApplicationConversionService;
import infra.app.env.ConfigTreePropertySource.Option;
import infra.app.env.ConfigTreePropertySource.Value;
import infra.origin.TextResourceOrigin;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigTreePropertySource}.
 *
 * @author Phillip Webb
 */
class ConfigTreePropertySourceTests {

  @TempDir
  Path directory;

  @Test
  void createWhenNameIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreePropertySource(null, this.directory))
            .withMessageContaining("name must contain");
  }

  @Test
  void createWhenSourceIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreePropertySource("test", null))
            .withMessage("Property source is required");
  }

  @Test
  void createWhenSourceDoesNotExistThrowsException() {
    Path missing = this.directory.resolve("missing");
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreePropertySource("test", missing))
            .withMessage("Directory '" + missing + "' does not exist");
  }

  @Test
  void createWhenSourceIsFileThrowsException() throws Exception {
    Path file = this.directory.resolve("file");
    FileCopyUtils.copy("test".getBytes(StandardCharsets.UTF_8), file.toFile());
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreePropertySource("test", file))
            .withMessage("File '" + file + "' is not a directory");
  }

  @Test
  void getPropertyNamesFromFlatReturnsPropertyNames() throws Exception {
    ConfigTreePropertySource propertySource = getFlatPropertySource();
    assertThat(propertySource.getPropertyNames()).containsExactly("a", "b", "c", "one");
  }

  @Test
  void getPropertyNamesFromNestedReturnsPropertyNames() throws Exception {
    ConfigTreePropertySource propertySource = getNestedPropertySource();
    assertThat(propertySource.getPropertyNames()).containsExactly("c", "fa.a", "fa.b", "fb.a", "fb.fa.a");
  }

  @Test
  void getPropertyNamesFromNestedWithSymlinkInPathReturnsPropertyNames() throws Exception {
    addNested();
    Path symlinkTempDir = Files.createSymbolicLink(this.directory.resolveSibling("symlinkTempDir"), this.directory);
    ConfigTreePropertySource propertySource = new ConfigTreePropertySource("test", symlinkTempDir);
    Files.delete(symlinkTempDir);
    assertThat(propertySource.getPropertyNames()).containsExactly("c", "fa.a", "fa.b", "fb.a", "fb.fa.a");
  }

  @Test
  void getPropertyNamesFromFlatWithSymlinksIgnoresHiddenFiles() throws Exception {
    ConfigTreePropertySource propertySource = getSymlinkedFlatPropertySource();
    assertThat(propertySource.getPropertyNames()).containsExactly("a", "b", "c");
  }

  @Test
  void getPropertyNamesFromNestedWithSymlinksIgnoresHiddenFiles() throws Exception {
    ConfigTreePropertySource propertySource = getSymlinkedNestedPropertySource();
    assertThat(propertySource.getPropertyNames()).containsExactly("aa", "ab", "baa", "c");
  }

  @Test
  void getPropertyNamesWhenLowercaseReturnsPropertyNames() throws Exception {
    addProperty("SpRiNg", "boot");
    ConfigTreePropertySource propertySource = new ConfigTreePropertySource("test", this.directory,
            Option.USE_LOWERCASE_NAMES);
    assertThat(propertySource.getPropertyNames()).containsExactly("spring");
  }

  @Test
  void getPropertyFromFlatReturnsFileContent() throws Exception {
    ConfigTreePropertySource propertySource = getFlatPropertySource();
    assertThat(propertySource.getProperty("b")).hasToString("B");
  }

  @Test
  void getPropertyFromFlatWhenMissingReturnsNull() throws Exception {
    ConfigTreePropertySource propertySource = getFlatPropertySource();
    assertThat(propertySource.getProperty("missing")).isNull();
  }

  @Test
  void getPropertyFromFlatWhenFileDeletedThrowsException() throws Exception {
    ConfigTreePropertySource propertySource = getFlatPropertySource();
    Path b = this.directory.resolve("b");
    Files.delete(b);
    assertThatIllegalStateException().isThrownBy(() -> propertySource.getProperty("b").toString())
            .withMessage("The property file '" + b + "' no longer exists");
  }

  @Test
  void getOriginFromFlatReturnsOrigin() throws Exception {
    ConfigTreePropertySource propertySource = getFlatPropertySource();
    TextResourceOrigin origin = (TextResourceOrigin) propertySource.getOrigin("b");
    assertThat(origin.getResource().getFile()).isEqualTo(this.directory.resolve("b").toFile());
    assertThat(origin.getLocation().getLine()).isZero();
    assertThat(origin.getLocation().getColumn()).isZero();
  }

  @Test
  void getOriginFromFlatWhenMissingReturnsNull() throws Exception {
    ConfigTreePropertySource propertySource = getFlatPropertySource();
    assertThat(propertySource.getOrigin("missing")).isNull();
  }

  @Test
  void getPropertyViaEnvironmentSupportsConversion() throws Exception {
    StandardEnvironment environment = new StandardEnvironment();
    ConversionService conversionService = ApplicationConversionService.getSharedInstance();
    environment.setConversionService((ConfigurableConversionService) conversionService);
    environment.getPropertySources().addFirst(getFlatPropertySource());
    assertThat(environment.getProperty("a")).isEqualTo("A");
    assertThat(environment.getProperty("b")).isEqualTo("B");
    assertThat(environment.getProperty("c", InputStreamSource.class).getInputStream()).hasContent("C");
    assertThat(environment.getProperty("c", byte[].class)).contains('C');
    assertThat(environment.getProperty("one", Integer.class)).isOne();
  }

  @Test
  void getPropertyFromNestedReturnsFileContent() throws Exception {
    ConfigTreePropertySource propertySource = getNestedPropertySource();
    assertThat(propertySource.getProperty("fb.fa.a")).hasToString("BAA");
  }

  @Test
  void getPropertyWhenNotAlwaysReadIgnoresUpdates() throws Exception {
    ConfigTreePropertySource propertySource = getNestedPropertySource();
    Value v1 = propertySource.getProperty("fa.b");
    Value v2 = propertySource.getProperty("fa.b");
    assertThat(v1).isSameAs(v2);
    assertThat(v1).hasToString("AB");
    assertThat(FileCopyUtils.copyToByteArray(v1.getInputStream())).containsExactly('A', 'B');
    addProperty("fa/b", "XX");
    assertThat(v1).hasToString("AB");
    assertThat(FileCopyUtils.copyToByteArray(v1.getInputStream())).containsExactly('A', 'B');
  }

  @Test
  void getPropertyWhenAlwaysReadReflectsUpdates() throws Exception {
    addNested();
    ConfigTreePropertySource propertySource = new ConfigTreePropertySource("test", this.directory,
            Option.ALWAYS_READ);
    Value v1 = propertySource.getProperty("fa.b");
    Value v2 = propertySource.getProperty("fa.b");
    assertThat(v1).isNotSameAs(v2);
    assertThat(v1).hasToString("AB");
    assertThat(FileCopyUtils.copyToByteArray(v1.getInputStream())).containsExactly('A', 'B');
    addProperty("fa/b", "XX");
    assertThat(v1).hasToString("XX");
    assertThat(FileCopyUtils.copyToByteArray(v1.getInputStream())).containsExactly('X', 'X');
    assertThat(propertySource.getProperty("fa.b")).hasToString("XX");
  }

  @Test
  void getPropertyWhenLowercaseReturnsValue() throws Exception {
    addProperty("SpRiNg", "boot");
    ConfigTreePropertySource propertySource = new ConfigTreePropertySource("test", this.directory,
            Option.USE_LOWERCASE_NAMES);
    assertThat(propertySource.getProperty("spring")).hasToString("boot");
  }

  @Test
  void getPropertyAsStringWhenMultiLinePropertyReturnsNonTrimmed() throws Exception {
    addProperty("a", "a\nb\n");
    ConfigTreePropertySource propertySource = new ConfigTreePropertySource("test", this.directory,
            Option.AUTO_TRIM_TRAILING_NEW_LINE);
    assertThat(propertySource.getProperty("a")).hasToString("a\nb\n");
  }

  @Test
  void getPropertyAsStringWhenPropertyEndsWithNewLineReturnsTrimmed() throws Exception {
    addProperty("a", "a\n");
    ConfigTreePropertySource propertySource = new ConfigTreePropertySource("test", this.directory,
            Option.AUTO_TRIM_TRAILING_NEW_LINE);
    assertThat(propertySource.getProperty("a")).hasToString("a");
  }

  @Test
  void getPropertyAsStringWhenPropertyEndsWithWindowsNewLineReturnsTrimmed() throws Exception {
    addProperty("a", "a\r\n");
    ConfigTreePropertySource propertySource = new ConfigTreePropertySource("test", this.directory,
            Option.AUTO_TRIM_TRAILING_NEW_LINE);
    assertThat(propertySource.getProperty("a")).hasToString("a");
  }

  private ConfigTreePropertySource getFlatPropertySource() throws IOException {
    addProperty("a", "A");
    addProperty("b", "B");
    addProperty("c", "C");
    addProperty("one", "1");
    return new ConfigTreePropertySource("test", this.directory);
  }

  private ConfigTreePropertySource getSymlinkedFlatPropertySource() throws IOException {
    addProperty("..hidden-a", "A");
    addProperty("..hidden-b", "B");
    addProperty("..hidden-c", "C");
    createSymbolicLink("a", "..hidden-a");
    createSymbolicLink("b", "..hidden-b");
    createSymbolicLink("c", "..hidden-c");
    return new ConfigTreePropertySource("test", this.directory);
  }

  private ConfigTreePropertySource getNestedPropertySource() throws IOException {
    addNested();
    return new ConfigTreePropertySource("test", this.directory);
  }

  private void addNested() throws IOException {
    addProperty("fa/a", "AA");
    addProperty("fa/b", "AB");
    addProperty("fb/a", "BA");
    addProperty("fb/fa/a", "BAA");
    addProperty("c", "C");
  }

  private ConfigTreePropertySource getSymlinkedNestedPropertySource() throws IOException {
    addProperty("..hidden-a/a", "AA");
    addProperty("..hidden-a/b", "AB");
    addProperty("..hidden-b/fa/a", "BAA");
    addProperty("c", "C");
    createSymbolicLink("aa", "..hidden-a/a");
    createSymbolicLink("ab", "..hidden-a/b");
    createSymbolicLink("baa", "..hidden-b/fa/a");
    return new ConfigTreePropertySource("test", this.directory);
  }

  private void addProperty(String path, String value) throws IOException {
    File file = this.directory.resolve(path).toFile();
    file.getParentFile().mkdirs();
    FileCopyUtils.copy(value.getBytes(StandardCharsets.UTF_8), file);
  }

  private void createSymbolicLink(String link, String target) throws IOException {
    Files.createSymbolicLink(this.directory.resolve(link).toAbsolutePath(),
            this.directory.resolve(target).toAbsolutePath());
  }

}
