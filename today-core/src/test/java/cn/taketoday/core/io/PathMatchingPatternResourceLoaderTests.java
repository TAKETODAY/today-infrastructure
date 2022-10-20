/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.io;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY <br>
 * 2019-12-05 23:15
 */
class PathMatchingPatternResourceLoaderTests {

  private PathMatchingPatternResourceLoader resolver = new PathMatchingPatternResourceLoader();

  private static final String[] CLASSES_IN_CORE_IO_SUPPORT = {
          "EncodedResource.class",
          "PathMatchingPatternResourceLoader.class",
          "PropertiesLoaderSupport.class",
          "PropertiesUtils.class",
          "ResourceArrayPropertyEditor.class",
          "PatternResourceLoader.class",
  };

  private static final String[] TEST_CLASSES_IN_CORE_IO_SUPPORT = {
          "PathMatchingPatternResourceLoaderTests.class"
  };

  private static final String[] CLASSES_IN_REACTOR_UTIL_ANNOTATION = {
          "NonNull.class", "NonNullApi.class", "Nullable.class"
  };

  // Fails in a native image -- https://github.com/oracle/graal/issues/5020

  @Test
  void rootPatternRetrievalInJarFiles() throws IOException {
    assertThat(resolver.getResources("classpath*:aspectj*.dtd")).extracting(Resource::getName)
            .as("Could not find aspectj_1_5_0.dtd in the root of the aspectjweaver jar")
            .containsExactly("aspectj_1_5_0.dtd");
  }

  private void assertProtocolAndFilenames(
          Resource[] resources, String protocol, String... filenames) throws IOException {
    if (filenames.length != resources.length) {
      // find which file is forget add
      if (filenames.length < resources.length) {
        Set<String> filenames1 = Set.of(filenames);
        Arrays.stream(resources)
                .map(Resource::getName)
                .filter(Predicate.not(filenames1::contains))
                .forEach(System.err::println);
      }
      else {
        Set<String> less = Arrays.stream(resources)
                .map(Resource::getName)
                .collect(Collectors.toSet());
        Arrays.stream(filenames)
                .filter(Predicate.not(less::contains))
                .forEach(System.err::println);
      }
    }
    assertEquals(filenames.length, resources.length, "Correct number of files found");
    for (Resource resource : resources) {
      String actualProtocol = resource.getURL().getProtocol();
      assertEquals(protocol, actualProtocol);
      assertFilenameIn(resource, filenames);
    }
  }

  @Nested
  class FileSystemResources {

    @Test
    void singleResourceOnFileSystem() {
      String pattern = "cn/taketoday/core/io/PathMatchingPatternResourceLoaderTests.class";
      assertExactFilenames(pattern, "PathMatchingPatternResourceLoaderTests.class");
    }

    @Test
    void classpathStarWithPatternOnFileSystem() {
      String pattern = "classpath*:cn/taketoday/core/i*/*.class";
      String[] expectedFilenames = StringUtils.concatenateStringArrays(
              CLASSES_IN_CORE_IO_SUPPORT, TEST_CLASSES_IN_CORE_IO_SUPPORT);
      assertFilenames(pattern, expectedFilenames);
    }

    @Nested
    class WithHashtagsInTheirFileNames {

      @Test
      void usingClasspathStarProtocol() {
        String pattern = "classpath*:cn/taketoday/core/io/**/resource#test*.txt";
        String pathPrefix = ".+cn/taketoday/core/io/";

        assertExactFilenames(pattern, "resource#test1.txt", "resource#test2.txt");
        assertExactSubPaths(pattern, pathPrefix, "support/resource#test1.txt", "support/resource#test2.txt");
      }

      @Test
      void usingClasspathStarProtocolWithWildcardInPatternAndNotEndingInSlash() throws Exception {
        String pattern = "classpath*:cn/taketoday/core/i*";
        String pathPrefix = ".+cn/taketoday/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        // We DO find "support" if the pattern does NOT end with a slash.
        assertThat(actualSubPaths).containsExactly("support");
      }

      @Test
      void usingFileProtocolWithWildcardInPatternAndNotEndingInSlash() throws Exception {
        Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
        String pattern = String.format("file:%s/cn/taketoday/core/i*", testResourcesDir);
        String pathPrefix = ".+cn/taketoday/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        // We DO find "support" if the pattern does NOT end with a slash.
        assertThat(actualSubPaths).containsExactly("support");
      }

      @Test
      void usingClasspathStarProtocolWithWildcardInPatternAndEndingInSlash() throws Exception {
        String pattern = "classpath*:cn/taketoday/core/io/sup*/";
        String pathPrefix = ".+cn/taketoday/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        // We do NOT find "support" if the pattern ENDS with a slash.
        assertThat(actualSubPaths).isEmpty();
      }

      @Test
      void usingFileProtocolWithWildcardInPatternAndEndingInSlash() throws Exception {
        Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
        String pattern = String.format("file:%s/cn/taketoday/core/io/sup*/", testResourcesDir);
        String pathPrefix = ".+cn/taketoday/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        // We do NOT find "support" if the pattern ENDS with a slash.
        assertThat(actualSubPaths).isEmpty();
      }

      @Test
      void usingClasspathStarProtocolWithWildcardInPatternAndEndingWithSuffixPattern() throws Exception {
        String pattern = "classpath*:cn/taketoday/core/i*/*.txt";
        String pathPrefix = ".+cn/taketoday/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        assertThat(actualSubPaths)
                .containsExactlyInAnyOrder("support/resource#test1.txt", "support/resource#test2.txt");
      }

      private List<String> getSubPathsIgnoringClassFilesEtc(String pattern, String pathPrefix) throws IOException {
        return Arrays.stream(resolver.getResourcesArray(pattern))
                .map(resource -> getPath(resource).replaceFirst(pathPrefix, ""))
                .filter(name -> !name.endsWith(".class"))
                .filter(name -> !name.endsWith(".kt"))
                .filter(name -> !name.endsWith(".factories"))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
      }

      @Test
      void usingFileProtocolWithoutWildcardInPatternAndEndingInSlashStarStar() {
        Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
        String pattern = String.format("file:%s/scanned-resources/**", testResourcesDir);
        String pathPrefix = ".+?resources/";

        // We do NOT find "scanned-resources" if the pattern ENDS with "/**" AND does NOT otherwise contain a wildcard.
        assertExactFilenames(pattern, "resource#test1.txt", "resource#test2.txt");
        assertExactSubPaths(pattern, pathPrefix, "scanned-resources/resource#test1.txt",
                "scanned-resources/resource#test2.txt");
      }

      @Test
      void usingFileProtocolWithWildcardInPatternAndEndingInSlashStarStar() {
        Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
        String pattern = String.format("file:%s/scanned*resources/**", testResourcesDir);
        String pathPrefix = ".+?resources/";

        // We DO find "scanned-resources" if the pattern ENDS with "/**" AND DOES otherwise contain a wildcard.
        assertExactFilenames(pattern, "scanned-resources", "resource#test1.txt", "resource#test2.txt");
        assertExactSubPaths(pattern, pathPrefix, "scanned-resources", "scanned-resources/resource#test1.txt",
                "scanned-resources/resource#test2.txt");
      }

      @Test
      void usingFileProtocolAndAssertingUrlAndUriSyntax() throws Exception {
        Path testResourcesDir = Path.of("src/test/resources").toAbsolutePath();
        String pattern = "file:%s/scanned-resources/**/resource#test1.txt".formatted(testResourcesDir);
        Resource[] resources = resolver.getResourcesArray(pattern);
        assertThat(resources).hasSize(1);
        Resource resource = resources[0];
        assertThat(resource.getName()).isEqualTo("resource#test1.txt");
        // The following assertions serve as regression tests for the lack of the
        // "authority component" (//) in the returned URI/URL. For example, we are
        // expecting file:/my/path (or file:/C:/My/Path) instead of file:///my/path.
        assertThat(resource.getURL().toString()).matches("^file:\\/[^\\/].+test1\\.txt$");
        assertThat(resource.getURI().toString()).matches("^file:\\/[^\\/].+test1\\.txt$");
      }

    }
  }

  @Nested
  class JarResources {

    @Test
    void singleResourceInJar() {
      String pattern = "org/reactivestreams/Publisher.class";
      assertExactFilenames(pattern, "Publisher.class");
    }

    @Test
    void singleResourceInRootOfJar() {
      String pattern = "aspectj_1_5_0.dtd";
      assertExactFilenames(pattern, "aspectj_1_5_0.dtd");
    }

    @Test
    void classpathWithPatternInJar() {
      String pattern = "classpath:reactor/util/annotation/*.class";
      assertExactFilenames(pattern, CLASSES_IN_REACTOR_UTIL_ANNOTATION);
    }

    @Test
    void classpathStarWithPatternInJar() {
      String pattern = "classpath*:reactor/util/annotation/*.class";
      assertExactFilenames(pattern, CLASSES_IN_REACTOR_UTIL_ANNOTATION);
    }

  }

  private void assertFilenameIn(Resource resource, String... filenames) {
    String filename = resource.getName();
    assertTrue(Arrays.stream(filenames).anyMatch(filename::endsWith),
            resource + " does not have a filename that matches any of the specified names");
  }

  private void assertFilenames(String pattern, String... filenames) {
    assertFilenames(pattern, false, filenames);
  }

  private void assertExactFilenames(String pattern, String... filenames) {
    assertFilenames(pattern, true, filenames);
  }

  private void assertFilenames(String pattern, boolean exactly, String... filenames) {
    try {
      Resource[] resources = resolver.getResourcesArray(pattern);
      List<String> actualNames = Arrays.stream(resources)
              .map(Resource::getName)
              .sorted()
              .toList();

      // Uncomment the following if you encounter problems with matching against the file system.
      // List<String> expectedNames = Arrays.stream(filenames).sorted().toList();
      // System.out.println("----------------------------------------------------------------------");
      // System.out.println("Expected: " + expectedNames);
      // System.out.println("Actual: " + actualNames);
      // Arrays.stream(resources).forEach(System.out::println);

      if (exactly) {
        assertThat(actualNames).as("subset of files found").containsExactlyInAnyOrder(filenames);
      }
      else {
        assertThat(actualNames).as("subset of files found").contains(filenames);
      }
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private void assertExactSubPaths(String pattern, String pathPrefix, String... subPaths) {
    try {
      Resource[] resources = resolver.getResourcesArray(pattern);
      List<String> actualSubPaths = Arrays.stream(resources)
              .map(resource -> getPath(resource).replaceFirst(pathPrefix, ""))
              .sorted()
              .toList();
      assertThat(actualSubPaths).containsExactlyInAnyOrder(subPaths);
    }
    catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private String getPath(Resource resource) {
    // Tests fail if we use resouce.getURL().getPath(). They would also fail on Mac OS when
    // using resouce.getURI().getPath() if the resource paths are not Unicode normalized.
    //
    // On the JVM, all tests should pass when using resouce.getFile().getPath(); however,
    // we use FileSystemResource#getPath since this test class is sometimes run within a
    // GraalVM native image which cannot support Path#toFile.
    //
    // See: https://github.com/spring-projects/spring-framework/issues/29243
    return ((FileSystemResource) resource).getPath();
  }

}
