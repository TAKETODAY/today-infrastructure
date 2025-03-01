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

package infra.test.classpath.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Resources}.
 *
 * @author Andy Wilkinson
 */
class ResourcesTests {

  @TempDir
  private Path root;

  @Test
  void whenAddResourceThenResourceIsCreated() {
    new Resources(this.root).addResource("test", "test-content");
    assertThat(this.root.resolve("test")).hasContent("test-content");
  }

  @Test
  void whenAddResourceHasContentReferencingResourceRootThenResourceIsCreatedWithReferenceToRoot() {
    new Resources(this.root).addResource("test", "*** ${resourceRoot} ***");
    assertThat(this.root.resolve("test")).hasContent("*** " + this.root + " ***");
  }

  @Test
  void whenAddResourceWithPathThenResourceIsCreated() {
    new Resources(this.root).addResource("a/b/c/test", "test-content");
    assertThat(this.root.resolve("a/b/c/test")).hasContent("test-content");
  }

  @Test
  void whenAddResourceAndResourceAlreadyExistsThenResourcesIsOverwritten() {
    Resources resources = new Resources(this.root);
    resources.addResource("a/b/c/test", "original-content");
    resources.addResource("a/b/c/test", "new-content");
    assertThat(this.root.resolve("a/b/c/test")).hasContent("new-content");
  }

  @Test
  void whenAddPackageThenNamedResourcesFromPackageAreCreated() {
    new Resources(this.root).addPackage(getClass().getPackage(),
            new String[] { "resource-1.txt", "sub/resource-3.txt" });
    assertThat(this.root.resolve("resource-1.txt")).hasContent("one");
    assertThat(this.root.resolve("resource-2.txt")).doesNotExist();
    assertThat(this.root.resolve("sub/resource-3.txt")).hasContent("three");
  }

  @Test
  void whenAddResourceAndDeleteThenResourceDoesNotExist() {
    Resources resources = new Resources(this.root);
    resources.addResource("test", "test-content");
    assertThat(this.root.resolve("test")).hasContent("test-content");
    resources.delete();
    assertThat(this.root.resolve("test")).doesNotExist();
  }

  @Test
  void whenAddPackageAndDeleteThenResourcesDoNotExist() {
    Resources resources = new Resources(this.root);
    resources.addPackage(getClass().getPackage(),
            new String[] { "resource-1.txt", "resource-2.txt", "sub/resource-3.txt" });
    assertThat(this.root.resolve("resource-1.txt")).hasContent("one");
    assertThat(this.root.resolve("resource-2.txt")).hasContent("two");
    assertThat(this.root.resolve("sub/resource-3.txt")).hasContent("three");
    resources.delete();
    assertThat(this.root.resolve("resource-1.txt")).doesNotExist();
    assertThat(this.root.resolve("resource-2.txt")).doesNotExist();
    assertThat(this.root.resolve("sub/resource-3.txt")).doesNotExist();
    assertThat(this.root.resolve("sub")).doesNotExist();
  }

  @Test
  void whenAddDirectoryThenDirectoryIsCreated() {
    Resources resources = new Resources(this.root);
    resources.addDirectory("dir");
    assertThat(this.root.resolve("dir")).isDirectory();
  }

  @Test
  void whenAddDirectoryWithPathThenDirectoryIsCreated() {
    Resources resources = new Resources(this.root);
    resources.addDirectory("one/two/three/dir");
    assertThat(this.root.resolve("one/two/three/dir")).isDirectory();
  }

  @Test
  void whenAddDirectoryAndDirectoryAlreadyExistsThenDoesNotThrow() {
    Resources resources = new Resources(this.root);
    resources.addDirectory("one/two/three/dir");
    resources.addDirectory("one/two/three/dir");
    assertThat(this.root.resolve("one/two/three/dir")).isDirectory();
  }

  @Test
  void whenAddDirectoryAndResourceAlreadyExistsThenIllegalStateExceptionIsThrown() {
    Resources resources = new Resources(this.root);
    resources.addResource("one/two/three/", "content");
    assertThatIllegalStateException().isThrownBy(() -> resources.addDirectory("one/two/three"));
  }

  @Test
  void whenAddResourceAndDirectoryAlreadyExistsThenIllegalStateExceptionIsThrown() {
    Resources resources = new Resources(this.root);
    resources.addDirectory("one/two/three");
    assertThatIllegalStateException().isThrownBy(() -> resources.addResource("one/two/three", "content"));
  }

}
