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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.taketoday.util.ResourceUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY <br>
 * 2019-12-05 23:15
 */
class PathMatchingPatternResourceLoaderTests {

  private PathMatchingPatternResourceLoader resolver = new PathMatchingPatternResourceLoader();
  private static final String[] CLASSES_IN_IO = new String[] { //
          "AbstractResource.class", //
          "ClassPathResource.class", //
          "EncodedResource.class", //
          "DescriptiveResource.class", //
          "InputStreamResource.class", //
          "ByteArrayResource.class", //
          "FileSystemResource.class", //
          "JarEntryResource.class", //
          "JarResource.class", //
          "PathMatchingPatternResourceLoader.class", //
          "InputStreamSource.class", //
          "Resource.class", //
          "ResourceFilter.class", //
          "ResourceLoader.class", //
          "UrlBasedResource.class", //
          "OutputStreamSource.class", //
          "WritableResource.class", //
          "PathMatchingPatternResourceLoaderTests.class", //
          "ResourceTests.class", //
          "ResourceTests$2.class", //
          "ResourceTests$1.class", //
          "EncodedResourceTests.class", //
          "PropertiesUtils.class", //
          "ProtocolResolver.class",
          "DefaultResourceLoader.class",
          "PatternResourceLoader.class",
          "PropertiesUtilsTests.class",
          "ResourcePropertySource.class",
          "PropertiesLoaderSupport.class",
          "PropertySourceFactory.class",
          "ResourceRegion.class",
          "ResourceConsumer.class",
          "FileUrlResource.class",
          "ClassRelativeResourceLoader.class",
          "DefaultResourceLoader$ClassPathContextResource.class",
          "ClassRelativeResourceLoader$ClassRelativeContextResource.class",
          "DefaultPropertySourceFactory.class",
          "JarEntryResource$JarEntryInputStream.class", //
          "ResourceEditor.class",
          "FileSystemResourceLoader.class",
          "ResourceArrayPropertyEditorTests.class",
          "ContextResource.class",
          "PathResource.class",
          "PathResourceTests.class",
          "AbstractFileResolvingResource.class",
          "FileSystemResourceLoader$FileSystemContextResource.class",
          "ResourceArrayPropertyEditor.class",
          "ResourceDecorator.class",
          "WritableResourceDecorator.class",
  };

  private static final String[] CLASSES_IN_JUNIT_RUNNER = new String[] { //
          "BaseTestRunner.class", //
          "TestRunListener.class", //
          "Version.class", //
  };

//    @Test(expected = FileNotFoundException.class)
//    public void invalidPrefixWithPatternElementInIt() throws IOException {
//        resolver.getResources("xx**:**/*.xy");
//    }

  @Test
  public void singleResourceOnFileSystem() throws IOException {
    Resource[] resources = resolver.getResourcesArray("cn/taketoday/core/io/PathMatchingPatternResourceLoaderTests.class");
    assertEquals(1, resources.length);
    assertTrue(resources[0].exists());
    assertProtocolAndFilenames(resources, "file", "PathMatchingPatternResourceLoaderTests.class");

    // ---------------------------------------

    final Resource[] resources2 = ResourceUtils.getResources("cn/taketoday/core/io/PathMatchingPatternResourceLoaderTests.class");
    assertEquals(1, resources2.length);
    assertTrue(resources2[0].exists());
    assertProtocolAndFilenames(resources2, "file", "PathMatchingPatternResourceLoaderTests.class");
  }

  @Test
  public void singleResourceInJar() throws IOException {
    Resource[] resources = resolver.getResourcesArray("org/junit/Assert.class");
    assertEquals(1, resources.length);
    assertProtocolAndFilenames(resources, "jar", "Assert.class");
  }

  @Test
  public void classpathStarWithPatternOnFileSystem() throws IOException {
    Resource[] resources = resolver.getResourcesArray("classpath*:cn/taketoday/core/io/*.class");
    // Have to exclude Clover-generated class files here,
    // as we might be running as part of a Clover test run.
    List<Resource> noCloverResources = new ArrayList<>();
    for (Resource resource : resources) {
      if (!resource.getName().contains("$__CLOVER_")) {
        noCloverResources.add(resource);
      }
    }

    resources = noCloverResources.toArray(Resource.EMPTY_ARRAY);
    assertProtocolAndFilenames(resources, "file", CLASSES_IN_IO);
  }

  @Test
  public void classpathWithPatternInJar() throws IOException {
    Resource[] resources = resolver.getResourcesArray("classpath:junit/runner/*.class");
    assertProtocolAndFilenames(resources, "jar", CLASSES_IN_JUNIT_RUNNER);
  }

  @Test
  public void classpathStarWithPatternInJar() throws IOException {
    Resource[] resources = resolver.getResourcesArray("classpath*:junit/runner/*.class");
    assertProtocolAndFilenames(resources, "jar", CLASSES_IN_JUNIT_RUNNER);
  }

  @Test
  public void rootPatternRetrievalInJarFiles() throws IOException {
    Resource[] resources = resolver.getResourcesArray("classpath*:**/pom.properties");
    boolean found = false;
    for (Resource resource : resources) {
      if (resource.getName().endsWith("pom.properties")) {
        found = true;
      }
    }
    assertTrue(found, "Could not find pom.properties");

    AtomicInteger times = new AtomicInteger();
    resolver.scan("classpath*:**/pom.properties", resource -> {
      times.getAndIncrement();
    });
    System.out.println(times);
    System.out.println(resources.length);
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

  private void assertFilenameIn(Resource resource, String... filenames) {
    String filename = resource.getName();
    assertTrue(Arrays.stream(filenames).anyMatch(filename::endsWith),
            resource + " does not have a filename that matches any of the specified names");
  }

}
