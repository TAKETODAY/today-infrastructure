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
package infra.core.io;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.FileSystemUtils;
import infra.util.ResourceUtils;
import infra.util.StreamUtils;
import infra.util.StringUtils;

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
          "EncodedResourceTests.class",
          "PathMatchingPatternResourceLoaderTests.class",
          "PropertySourceProcessorTests.class",
          "PropertiesUtilsTests.class",
          "ResourceArrayPropertyEditorTests.class",
          "ModuleResourceTests.class",
  };

  private static final String[] TEST_CLASSES_IN_CORE_IO_SUPPORT = {
          "PathMatchingPatternResourceLoaderTests.class"
  };

  private static final String[] CLASSES_IN_REACTOR_UTIL_ANNOTATION = {
          "Incubating.class", "NonNull.class", "NonNullApi.class", "Nullable.class"
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
      String pattern = "infra/core/io/PathMatchingPatternResourceLoaderTests.class";
      assertExactFilenames(pattern, "PathMatchingPatternResourceLoaderTests.class");
    }

    @Test
    void classpathStarWithPatternOnFileSystem() {
      String pattern = "classpath*:infra/core/i*/*.class";
      String[] expectedFilenames = StringUtils.concatenateStringArrays(
              CLASSES_IN_CORE_IO_SUPPORT, TEST_CLASSES_IN_CORE_IO_SUPPORT);
      assertFilenames(pattern, expectedFilenames);
    }

    @Test
    void usingFileProtocolWithWildcardInPatternAndNonexistentRootPath() throws IOException {
      Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
      String pattern = String.format("file:%s/example/bogus/**", testResourcesDir);
      assertThat(resolver.getResources(pattern)).isEmpty();
      // When the log level for the resolver is set to at least INFO, we should see
      // a log entry similar to the following.
      //
      // [main] INFO  o.s.c.i.s.PathMatchingPatternResourceLoader -
      // Skipping search for files matching pattern [**]: directory
      // [/<...>/today-core/src/test/resources/example/bogus] does not exist
    }

    @Test
    void encodedHashtagInPath() throws IOException {
      Path rootDir = Paths.get("src/test/resources/custom%23root").toAbsolutePath();
      URL root = new URL("file:" + rootDir + "/");
      resolver = new PathMatchingPatternResourceLoader(new DefaultResourceLoader(new URLClassLoader(new URL[] { root })));
      resolver.setUseCaches(false);
      assertExactFilenames("classpath*:scanned/*.txt", "resource#test1.txt", "resource#test2.txt");
    }

    @Nested
    class WithHashtagsInTheirFileNames {

      @Test
      void usingClasspathStarProtocol() {
        String pattern = "classpath*:infra/core/io/**/resource#test*.txt";
        String pathPrefix = ".+infra/core/io/";

        assertExactFilenames(pattern, "resource#test1.txt", "resource#test2.txt");
        assertExactSubPaths(pattern, pathPrefix, "resource#test1.txt", "resource#test2.txt");
      }

      @Test
      void usingClasspathStarProtocolWithWildcardInPatternAndNotEndingInSlash() throws Exception {
        String pattern = "classpath*:infra/core/io/buff*";
        String pathPrefix = ".+infra/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        // We DO find "buffer" if the pattern does NOT end with a slash.
        assertThat(actualSubPaths).containsExactly("buffer");
      }

      @Test
      void usingFileProtocolWithWildcardInPatternAndNotEndingInSlash() throws Exception {
        Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
        String pattern = String.format("file:%s/infra/core/io/buff*", testResourcesDir);
        String pathPrefix = ".+infra/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        // We DO find "buffer" if the pattern does NOT end with a slash.
        assertThat(actualSubPaths).containsExactly("buffer");
      }

      @Test
      void usingClasspathStarProtocolWithWildcardInPatternAndEndingInSlash() throws Exception {
        String pattern = "classpath*:infra/core/io/sup*/";
        String pathPrefix = ".+infra/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        // We do NOT find "support" if the pattern ENDS with a slash.
        assertThat(actualSubPaths).isEmpty();
      }

      @Test
      void usingFileProtocolWithWildcardInPatternAndEndingInSlash() throws Exception {
        Path testResourcesDir = Paths.get("src/test/resources").toAbsolutePath();
        String pattern = String.format("file:%s/infra/core/io/sup*/", testResourcesDir);
        String pathPrefix = ".+infra/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        // We do NOT find "support" if the pattern ENDS with a slash.
        assertThat(actualSubPaths).isEmpty();
      }

      @Test
      void usingClasspathStarProtocolWithWildcardInPatternAndEndingWithSuffixPattern() throws Exception {
        String pattern = "classpath*:infra/core/i*/*.txt";
        String pathPrefix = ".+infra/core/io/";

        List<String> actualSubPaths = getSubPathsIgnoringClassFilesEtc(pattern, pathPrefix);

        assertThat(actualSubPaths)
                .containsExactlyInAnyOrder("resource#test1.txt", "resource#test2.txt");
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

  @Nested
  class ClassPathManifestEntries {

    @TempDir
    Path temp;

    @BeforeAll
    static void suppressJarCaches() {
      URLConnection.setDefaultUseCaches("jar", false);
    }

    @AfterAll
    static void restoreJarCaches() {
      URLConnection.setDefaultUseCaches("jar", true);
    }

    @Test
    void javaDashJarFindsClassPathManifestEntries() throws Exception {
      Path lib = this.temp.resolve("lib");
      Files.createDirectories(lib);
      writeAssetJar(lib.resolve("asset.jar"));
      writeApplicationJar(this.temp.resolve("app.jar"));
      String java = ProcessHandle.current().info().command().get();
      Process process = new ProcessBuilder(java, "-jar", "app.jar")
              .directory(this.temp.toFile())
              .start();
      assertThat(process.waitFor()).isZero();
      String result = StreamUtils.copyToString(process.getInputStream(), StandardCharsets.UTF_8);
      assertThat(result.replace("\\", "/")).contains("!!!!").contains("/lib/asset.jar!/assets/file.txt");
    }

    private void writeAssetJar(Path path) throws Exception {
      try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(path.toFile()))) {
        jar.putNextEntry(new ZipEntry("assets/"));
        jar.closeEntry();
        jar.putNextEntry(new ZipEntry("assets/file.txt"));
        StreamUtils.copy("test", StandardCharsets.UTF_8, jar);
        jar.closeEntry();
      }

      assertThat(new FileSystemResource(path).exists()).isTrue();
      assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR).exists()).isTrue();
      assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/file.txt").exists()).isTrue();
      assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/none.txt").exists()).isFalse();
      assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + "X" + path + ResourceUtils.JAR_URL_SEPARATOR).exists()).isFalse();
      assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + "X" + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/file.txt").exists()).isFalse();
      assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + "X" + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/none.txt").exists()).isFalse();

      Resource resource = new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR + "assets/file.txt");
      try (InputStream is = resource.getInputStream()) {
        assertThat(resource.exists()).isTrue();
        assertThat(resource.createRelative("file.txt").exists()).isTrue();
        assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR).exists()).isTrue();
        is.readAllBytes();
      }
    }

    private void writeApplicationJar(Path path) throws Exception {
      Manifest manifest = new Manifest();
      Attributes mainAttributes = manifest.getMainAttributes();
      mainAttributes.put(Attributes.Name.CLASS_PATH, buildSpringClassPath() + "lib/asset.jar");
      mainAttributes.put(Attributes.Name.MAIN_CLASS, ClassPathManifestEntriesTestApplication.class.getName());
      mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
      try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(path.toFile()), manifest)) {
        String appClassResource = ClassUtils.convertClassNameToResourcePath(
                ClassPathManifestEntriesTestApplication.class.getName()) + ClassUtils.CLASS_FILE_SUFFIX;
        String folder = "";
        for (String name : appClassResource.split("/")) {
          if (!name.endsWith(ClassUtils.CLASS_FILE_SUFFIX)) {
            folder += name + "/";
            jar.putNextEntry(new ZipEntry(folder));
            jar.closeEntry();
          }
          else {
            jar.putNextEntry(new ZipEntry(folder + name));
            try (InputStream in = getClass().getResourceAsStream(name)) {
              in.transferTo(jar);
            }
            jar.closeEntry();
          }
        }
      }
      assertThat(new FileSystemResource(path).exists()).isTrue();
      assertThat(new UrlResource(ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR).exists()).isTrue();
    }

    private String buildSpringClassPath() throws Exception {
      return copyClasses(PathMatchingPatternResourceLoader.class, "today-core")
              + copyClasses(LoggerFactory.class, "commons-logging");
    }

    private String copyClasses(Class<?> sourceClass, String destinationName) throws URISyntaxException, IOException {
      Path destination = this.temp.resolve(destinationName);
      String resourcePath = ClassUtils.convertClassNameToResourcePath(
              sourceClass.getName()) + ClassUtils.CLASS_FILE_SUFFIX;
      URL resource = getClass().getClassLoader().getResource(resourcePath);
      URL url = new URL(resource.toString().replace(resourcePath, ""));
      URLConnection connection = url.openConnection();
      if (connection instanceof JarURLConnection jarUrlConnection) {
        try (JarFile jarFile = jarUrlConnection.getJarFile()) {
          Enumeration<JarEntry> entries = jarFile.entries();
          while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
              Path entryPath = destination.resolve(entry.getName());
              try (InputStream in = jarFile.getInputStream(entry)) {
                Files.createDirectories(entryPath.getParent());
                Files.copy(in, destination.resolve(entry.getName()));
              }
            }
          }
        }
      }
      else {
        File source = new File(url.toURI());
        Files.createDirectories(destination);
        FileSystemUtils.copyRecursively(source, destination.toFile());
      }
      return destinationName + "/ ";
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
