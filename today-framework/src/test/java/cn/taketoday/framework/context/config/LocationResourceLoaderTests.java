/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cn.taketoday.core.io.DefaultResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.context.config.LocationResourceLoader.ResourceType;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link LocationResourceLoader}.
 *
 * @author Phillip Webb
 */
class LocationResourceLoaderTests {

  private LocationResourceLoader loader = new LocationResourceLoader(new DefaultResourceLoader());

  @TempDir
  File temp;

  @Test
  void isPatternWhenHasAsteriskReturnsTrue() {
    assertThat(this.loader.isPattern("spring/*/boot")).isTrue();
  }

  @Test
  void isPatternWhenNoAsteriskReturnsFalse() {
    assertThat(this.loader.isPattern("spring/boot")).isFalse();
  }

  @Test
  void getResourceWhenPatternThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> this.loader.getResource("spring/boot/*"))
            .withMessage("Location 'spring/boot/*' must not be a pattern");
  }

  @Test
  void getResourceReturnsResource() throws Exception {
    File file = new File(this.temp, "file");
    FileCopyUtils.copy("test".getBytes(), file);
    Resource resource = this.loader.getResource(file.toURI().toString());
    assertThat(resource.getInputStream()).hasContent("test");
  }

  @Test
  void getResourceWhenNotUrlReturnsResource() throws Exception {
    File file = new File(this.temp, "file");
    FileCopyUtils.copy("test".getBytes(), file);
    Resource resource = this.loader.getResource(file.getAbsolutePath());
    assertThat(resource.getInputStream()).hasContent("test");
  }

  @Test
  void getResourceWhenNonCleanPathReturnsResource() throws Exception {
    File file = new File(this.temp, "file");
    FileCopyUtils.copy("test".getBytes(), file);
    Resource resource = this.loader.getResource(this.temp.getAbsolutePath() + "/spring/../file");
    assertThat(resource.getInputStream()).hasContent("test");
  }

  @Test
  void getResourcesWhenNotPatternThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> this.loader.getResources("spring/boot", ResourceType.FILE))
            .withMessage("Location 'spring/boot' must be a pattern");
  }

  @Test
  void getResourcesWhenLocationStartsWithClasspathWildcardThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.loader.getResources("classpath*:spring/boot/*/", ResourceType.FILE))
            .withMessage("Location 'classpath*:spring/boot/*/' cannot use classpath wildcards");
  }

  @Test
  void getResourcesWhenLocationContainsMultipleWildcardsThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.loader.getResources("spring/*/boot/*/", ResourceType.FILE))
            .withMessage("Location 'spring/*/boot/*/' cannot contain multiple wildcards");
  }

  @Test
  void getResourcesWhenPatternDoesNotEndWithAsteriskSlashThrowsException() {
    assertThatIllegalStateException().isThrownBy(() -> this.loader.getResources("spring/boot/*", ResourceType.FILE))
            .withMessage("Location 'spring/boot/*' must end with '*/'");
  }

  @Test
  void getFileResourceReturnsResources() throws Exception {
    createTree();
    List<Resource> resources = this.loader.getResources(this.temp.getAbsolutePath() + "/*/file", ResourceType.FILE);
    assertThat(resources).hasSize(2);
    assertThat(resources.get(0).getInputStream()).hasContent("a");
    assertThat(resources.get(1).getInputStream()).hasContent("b");
  }

  @Test
  void getDirectoryResourceReturnsResources() throws Exception {
    createTree();
    List<Resource> resources = this.loader.getResources(this.temp.getAbsolutePath() + "/*/", ResourceType.DIRECTORY);
    assertThat(resources).hasSize(2);
    assertThat(resources.get(0).getName()).isEqualTo("a");
    assertThat(resources.get(1).getName()).isEqualTo("b");
  }

  @Test
  void getResourcesWhenHasHiddenDirectoriesFiltersResults() throws IOException {
    createTree();
    File hiddenDirectory = new File(this.temp, "..a");
    hiddenDirectory.mkdirs();
    FileCopyUtils.copy("h".getBytes(), new File(hiddenDirectory, "file"));
    List<Resource> resources = this.loader.getResources(this.temp.getAbsolutePath() + "/*/file", ResourceType.FILE);
    assertThat(resources).hasSize(2);
    assertThat(resources.get(0).getInputStream()).hasContent("a");
    assertThat(resources.get(1).getInputStream()).hasContent("b");
  }

  private void createTree() throws IOException {
    File directoryA = new File(this.temp, "a");
    File directoryB = new File(this.temp, "b");
    directoryA.mkdirs();
    directoryB.mkdirs();
    FileCopyUtils.copy("a".getBytes(), new File(directoryA, "file"));
    FileCopyUtils.copy("b".getBytes(), new File(directoryB, "file"));
  }

}
