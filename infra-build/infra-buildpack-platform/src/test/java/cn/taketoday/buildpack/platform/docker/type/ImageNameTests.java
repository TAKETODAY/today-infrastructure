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

package cn.taketoday.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ImageName}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ImageNameTests {

  @Test
  void ofWhenNameOnlyCreatesImageName() {
    ImageName imageName = ImageName.of("ubuntu");
    assertThat(imageName).hasToString("docker.io/library/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("docker.io");
    assertThat(imageName.getName()).isEqualTo("library/ubuntu");
  }

  @Test
  void ofWhenSlashedNameCreatesImageName() {
    ImageName imageName = ImageName.of("canonical/ubuntu");
    assertThat(imageName).hasToString("docker.io/canonical/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("docker.io");
    assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
  }

  @Test
  void ofWhenLocalhostNameCreatesImageName() {
    ImageName imageName = ImageName.of("localhost/canonical/ubuntu");
    assertThat(imageName).hasToString("localhost/canonical/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("localhost");
    assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
  }

  @Test
  void ofWhenDomainAndNameCreatesImageName() {
    ImageName imageName = ImageName.of("repo.spring.io/canonical/ubuntu");
    assertThat(imageName).hasToString("repo.spring.io/canonical/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("repo.spring.io");
    assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
  }

  @Test
  void ofWhenDomainNameAndPortCreatesImageName() {
    ImageName imageName = ImageName.of("repo.spring.io:8080/canonical/ubuntu");
    assertThat(imageName).hasToString("repo.spring.io:8080/canonical/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("repo.spring.io:8080");
    assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
  }

  @Test
  void ofWhenSimpleNameAndPortCreatesImageName() {
    ImageName imageName = ImageName.of("repo:8080/ubuntu");
    assertThat(imageName).hasToString("repo:8080/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("repo:8080");
    assertThat(imageName.getName()).isEqualTo("ubuntu");
  }

  @Test
  void ofWhenSimplePathAndPortCreatesImageName() {
    ImageName imageName = ImageName.of("repo:8080/canonical/ubuntu");
    assertThat(imageName).hasToString("repo:8080/canonical/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("repo:8080");
    assertThat(imageName.getName()).isEqualTo("canonical/ubuntu");
  }

  @Test
  void ofWhenNameWithLongPathCreatesImageName() {
    ImageName imageName = ImageName.of("path1/path2/path3/ubuntu");
    assertThat(imageName).hasToString("docker.io/path1/path2/path3/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("docker.io");
    assertThat(imageName.getName()).isEqualTo("path1/path2/path3/ubuntu");
  }

  @Test
  void ofWhenLocalhostDomainCreatesImageName() {
    ImageName imageName = ImageName.of("localhost/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("localhost");
    assertThat(imageName.getName()).isEqualTo("ubuntu");
  }

  @Test
  void ofWhenLocalhostDomainAndPathCreatesImageName() {
    ImageName imageName = ImageName.of("localhost/library/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("localhost");
    assertThat(imageName.getName()).isEqualTo("library/ubuntu");
  }

  @Test
  void ofWhenLegacyDomainUsesNewDomain() {
    ImageName imageName = ImageName.of("index.docker.io/ubuntu");
    assertThat(imageName).hasToString("docker.io/library/ubuntu");
    assertThat(imageName.getDomain()).isEqualTo("docker.io");
    assertThat(imageName.getName()).isEqualTo("library/ubuntu");
  }

  @Test
  void ofWhenNameIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of(null))
            .withMessage("Value must not be empty");
  }

  @Test
  void ofWhenNameIsEmptyThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of("")).withMessage("Value must not be empty");
  }

  @Test
  void ofWhenContainsUppercaseThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of("Test"))
            .withMessageContaining("Unable to parse name")
            .withMessageContaining("Test");
  }

  @Test
  void ofWhenNameIncludesTagThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ImageName.of("ubuntu:latest"))
            .withMessageContaining("Unable to parse name")
            .withMessageContaining(":latest");
  }

  @Test
  void ofWhenNameIncludeDigestThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(
                    () -> ImageName.of("ubuntu@sha256:47bfdb88c3ae13e488167607973b7688f69d9e8c142c2045af343ec199649c09"))
            .withMessageContaining("Unable to parse name")
            .withMessageContaining("@sha256:47b");
  }

  @Test
  void hashCodeAndEquals() {
    ImageName n1 = ImageName.of("ubuntu");
    ImageName n2 = ImageName.of("library/ubuntu");
    ImageName n3 = ImageName.of("docker.io/ubuntu");
    ImageName n4 = ImageName.of("docker.io/library/ubuntu");
    ImageName n5 = ImageName.of("index.docker.io/library/ubuntu");
    ImageName n6 = ImageName.of("alpine");
    assertThat(n1).hasSameHashCodeAs(n2).hasSameHashCodeAs(n3).hasSameHashCodeAs(n4).hasSameHashCodeAs(n5);
    assertThat(n1).isEqualTo(n1).isEqualTo(n2).isEqualTo(n3).isEqualTo(n4).isNotEqualTo(n6);
  }

}
