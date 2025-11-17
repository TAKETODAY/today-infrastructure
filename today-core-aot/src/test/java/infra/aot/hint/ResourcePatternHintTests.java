/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aot.hint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ResourcePatternHint}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class ResourcePatternHintTests {

  @Test
  void patternWithLeadingSlashIsRejected() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ResourcePatternHint("/file.properties", null))
            .withMessage("Resource pattern [/file.properties] must not start with a '/' unless it is the root directory");
  }

  @Test
  void rootDirectory() {
    ResourcePatternHint hint = new ResourcePatternHint("/", null);
    assertThat(hint.matches("/")).isTrue();
    assertThat(hint.matches("/com/example")).isFalse();
    assertThat(hint.matches("/file.txt")).isFalse();
  }

  @Test
  void fileAtRoot() {
    ResourcePatternHint hint = new ResourcePatternHint("file.properties", null);
    assertThat(hint.matches("file.properties")).isTrue();
    assertThat(hint.matches("com/example/file.properties")).isFalse();
    assertThat(hint.matches("file.prop")).isFalse();
    assertThat(hint.matches("another-file.properties")).isFalse();
  }

  @Test
  void fileInDirectory() {
    ResourcePatternHint hint = new ResourcePatternHint("com/example/file.properties", null);
    assertThat(hint.matches("com/example/file.properties")).isTrue();
    assertThat(hint.matches("file.properties")).isFalse();
    assertThat(hint.matches("com/file.properties")).isFalse();
    assertThat(hint.matches("com/example/another-file.properties")).isFalse();
  }

  @Test
  void extension() {
    ResourcePatternHint hint = new ResourcePatternHint("**/*.properties", null);
    assertThat(hint.matches("file.properties")).isTrue();
    assertThat(hint.matches("com/example/file.properties")).isTrue();
    assertThat(hint.matches("file.prop")).isFalse();
    assertThat(hint.matches("com/example/file.prop")).isFalse();
  }

  @Test
  void extensionInDirectoryAtAnyDepth() {
    ResourcePatternHint hint = new ResourcePatternHint("com/example/*.properties", null);
    assertThat(hint.matches("com/example/file.properties")).isTrue();
    assertThat(hint.matches("com/example/another/file.properties")).isFalse();
    assertThat(hint.matches("com/file.properties")).isFalse();
    assertThat(hint.matches("file.properties")).isFalse();
  }

  @Test
  void anyFileInDirectoryAtAnyDepth() {
    ResourcePatternHint hint = new ResourcePatternHint("com/example/**", null);
    assertThat(hint.matches("com/example/file.properties")).isTrue();
    assertThat(hint.matches("com/example/another/file.properties")).isTrue();
    assertThat(hint.matches("com/example/another")).isTrue();
    assertThat(hint.matches("file.properties")).isFalse();
    assertThat(hint.matches("com/file.properties")).isFalse();
  }

  @Test
  void patternWithWildcardMatchesMultipleFiles() {
    ResourcePatternHint hint = new ResourcePatternHint("*.xml", null);
    assertThat(hint.matches("config.xml")).isTrue();
    assertThat(hint.matches("application.xml")).isTrue();
    assertThat(hint.matches("config.json")).isFalse();
    assertThat(hint.matches("sub/config.xml")).isFalse();
  }

  @Test
  void patternWithDoubleWildcardMatchesNestedDirectories() {
    ResourcePatternHint hint = new ResourcePatternHint("config/**/*.yaml", null);
    assertThat(hint.matches("config/application.yaml")).isTrue();
    assertThat(hint.matches("config/env/dev.yaml")).isTrue();
    assertThat(hint.matches("config/env/prod/test.yaml")).isTrue();
    assertThat(hint.matches("application.yaml")).isFalse();
    assertThat(hint.matches("config/application.json")).isFalse();
  }

  @Test
  void patternWithPathAndExtension() {
    ResourcePatternHint hint = new ResourcePatternHint("META-INF/*.txt", null);
    assertThat(hint.matches("META-INF/notice.txt")).isTrue();
    assertThat(hint.matches("META-INF/license.txt")).isTrue();
    assertThat(hint.matches("META-INF/sub/readme.txt")).isFalse();
    assertThat(hint.matches("WEB-INF/classes/META-INF/notice.txt")).isFalse();
  }

  @Test
  void equalsAndHashCodeWithSamePatternAndReachableType() {
    TypeReference type = TypeReference.of("com.example.TestClass");
    ResourcePatternHint hint1 = new ResourcePatternHint("*.properties", type);
    ResourcePatternHint hint2 = new ResourcePatternHint("*.properties", type);

    assertThat(hint1).isEqualTo(hint2);
    assertThat(hint1.hashCode()).isEqualTo(hint2.hashCode());
  }

  @Test
  void equalsAndHashCodeWithSamePatternButDifferentReachableType() {
    TypeReference type1 = TypeReference.of("com.example.TestClass1");
    TypeReference type2 = TypeReference.of("com.example.TestClass2");
    ResourcePatternHint hint1 = new ResourcePatternHint("*.properties", type1);
    ResourcePatternHint hint2 = new ResourcePatternHint("*.properties", type2);

    assertThat(hint1).isNotEqualTo(hint2);
    assertThat(hint1.hashCode()).isNotEqualTo(hint2.hashCode());
  }

  @Test
  void equalsAndHashCodeWithDifferentPatternButSameReachableType() {
    TypeReference type = TypeReference.of("com.example.TestClass");
    ResourcePatternHint hint1 = new ResourcePatternHint("*.properties", type);
    ResourcePatternHint hint2 = new ResourcePatternHint("*.xml", type);

    assertThat(hint1).isNotEqualTo(hint2);
    assertThat(hint1.hashCode()).isNotEqualTo(hint2.hashCode());
  }

  @Test
  void equalsWithSameInstance() {
    ResourcePatternHint hint = new ResourcePatternHint("*.properties", null);
    assertThat(hint).isEqualTo(hint);
  }

  @Test
  void equalsWithNull() {
    ResourcePatternHint hint = new ResourcePatternHint("*.properties", null);
    assertThat(hint).isNotEqualTo(null);
  }

  @Test
  void equalsWithDifferentObjectType() {
    ResourcePatternHint hint = new ResourcePatternHint("*.properties", null);
    assertThat(hint).isNotEqualTo("some string");
  }

  @Test
  void getPatternReturnsCorrectValue() {
    ResourcePatternHint hint = new ResourcePatternHint("com/example/*.properties", null);
    assertThat(hint.getPattern()).isEqualTo("com/example/*.properties");
  }

  @Test
  void getReachableTypeReturnsCorrectValue() {
    TypeReference type = TypeReference.of("com.example.TestClass");
    ResourcePatternHint hint = new ResourcePatternHint("*.properties", type);
    assertThat(hint.getReachableType()).isEqualTo(type);
  }

  @Test
  void getReachableTypeReturnsNullWhenNotSet() {
    ResourcePatternHint hint = new ResourcePatternHint("*.properties", null);
    assertThat(hint.getReachableType()).isNull();
  }

}
