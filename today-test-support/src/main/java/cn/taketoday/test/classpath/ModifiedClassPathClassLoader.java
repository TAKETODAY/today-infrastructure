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

package cn.taketoday.test.classpath;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Custom {@link URLClassLoader} that modifies the class path.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 */
final class ModifiedClassPathClassLoader extends URLClassLoader {

  private static final ConcurrentReferenceHashMap<Class<?>, ModifiedClassPathClassLoader> cache = new ConcurrentReferenceHashMap<>();

  private static final Pattern INTELLIJ_CLASSPATH_JAR_PATTERN = Pattern.compile(".*classpath(\\d+)?\\.jar");

  private static final int MAX_RESOLUTION_ATTEMPTS = 5;

  private final ClassLoader junitLoader;

  ModifiedClassPathClassLoader(URL[] urls, ClassLoader parent, ClassLoader junitLoader) {
    super(urls, parent);
    this.junitLoader = junitLoader;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (name.startsWith("org.junit") || name.startsWith("org.hamcrest")
            || name.startsWith("io.netty.internal.tcnative")) {
      return Class.forName(name, false, this.junitLoader);
    }
    return super.loadClass(name);
  }

  static ModifiedClassPathClassLoader get(Class<?> testClass) {
    return cache.computeIfAbsent(testClass, ModifiedClassPathClassLoader::compute);
  }

  private static ModifiedClassPathClassLoader compute(Class<?> testClass) {
    ClassLoader classLoader = testClass.getClassLoader();
    MergedAnnotations annotations = MergedAnnotations.from(testClass,
            MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
    if (annotations.isPresent(ForkedClassPath.class) && (annotations.isPresent(ClassPathOverrides.class)
            || annotations.isPresent(ClassPathExclusions.class))) {
      throw new IllegalStateException("@ForkedClassPath is redundant in combination with either "
              + "@ClassPathOverrides or @ClassPathExclusions");
    }
    return new ModifiedClassPathClassLoader(processUrls(extractUrls(classLoader), annotations),
            classLoader.getParent(), classLoader);
  }

  private static URL[] extractUrls(ClassLoader classLoader) {
    List<URL> extractedUrls = new ArrayList<>();
    doExtractUrls(classLoader).forEach((URL url) -> {
      if (isManifestOnlyJar(url)) {
        extractedUrls.addAll(extractUrlsFromManifestClassPath(url));
      }
      else {
        extractedUrls.add(url);
      }
    });
    return extractedUrls.toArray(new URL[0]);
  }

  private static Stream<URL> doExtractUrls(ClassLoader classLoader) {
    if (classLoader instanceof URLClassLoader) {
      return Stream.of(((URLClassLoader) classLoader).getURLs());
    }
    return Stream.of(ManagementFactory.getRuntimeMXBean().getClassPath().split(File.pathSeparator))
            .map(ModifiedClassPathClassLoader::toURL);
  }

  private static URL toURL(String entry) {
    try {
      return new File(entry).toURI().toURL();
    }
    catch (Exception ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  private static boolean isManifestOnlyJar(URL url) {
    return isShortenedIntelliJJar(url);
  }

  private static boolean isShortenedIntelliJJar(URL url) {
    String urlPath = url.getPath();
    boolean isCandidate = INTELLIJ_CLASSPATH_JAR_PATTERN.matcher(urlPath).matches();
    if (isCandidate) {
      try {
        Attributes attributes = getManifestMainAttributesFromUrl(url);
        String createdBy = attributes.getValue("Created-By");
        return createdBy != null && createdBy.contains("IntelliJ");
      }
      catch (Exception ignored) { }
    }
    return false;
  }

  private static List<URL> extractUrlsFromManifestClassPath(URL booterJar) {
    List<URL> urls = new ArrayList<>();
    try {
      for (String entry : getClassPath(booterJar)) {
        urls.add(new URL(entry));
      }
    }
    catch (Exception ex) {
      throw ExceptionUtils.sneakyThrow(ex);
    }
    return urls;
  }

  private static String[] getClassPath(URL booterJar) throws Exception {
    Attributes attributes = getManifestMainAttributesFromUrl(booterJar);
    return StringUtils.delimitedListToStringArray(attributes.getValue(Attributes.Name.CLASS_PATH), " ");
  }

  private static Attributes getManifestMainAttributesFromUrl(URL url) throws Exception {
    try (JarFile jarFile = new JarFile(new File(url.toURI()))) {
      return jarFile.getManifest().getMainAttributes();
    }
  }

  private static URL[] processUrls(URL[] urls, MergedAnnotations annotations) {
    ClassPathEntryFilter filter = new ClassPathEntryFilter(annotations.get(ClassPathExclusions.class));
    List<URL> additionalUrls = getAdditionalUrls(annotations.get(ClassPathOverrides.class));
    List<URL> processedUrls = new ArrayList<>(additionalUrls);
    for (URL url : urls) {
      if (!filter.isExcluded(url)) {
        processedUrls.add(url);
      }
    }
    return processedUrls.toArray(new URL[0]);
  }

  private static List<URL> getAdditionalUrls(MergedAnnotation<ClassPathOverrides> annotation) {
    if (!annotation.isPresent()) {
      return Collections.emptyList();
    }
    return resolveCoordinates(annotation.getStringArray(MergedAnnotation.VALUE));
  }

  private static List<URL> resolveCoordinates(String[] coordinates) {
    Exception latestFailure = null;
    DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
    serviceLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);
    RepositorySystem repositorySystem = serviceLocator.getService(RepositorySystem.class);
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepository = new LocalRepository(System.getProperty("user.home") + "/.m2/repository");
    RemoteRepository remoteRepository = new RemoteRepository.Builder("central", "default",
            "https://repo.maven.apache.org/maven2").build();
    session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));
    for (int i = 0; i < MAX_RESOLUTION_ATTEMPTS; i++) {
      CollectRequest collectRequest = new CollectRequest(null, List.of(remoteRepository));
      collectRequest.setDependencies(createDependencies(coordinates));
      DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
      try {
        DependencyResult result = repositorySystem.resolveDependencies(session, dependencyRequest);
        List<URL> resolvedArtifacts = new ArrayList<>();
        for (ArtifactResult artifact : result.getArtifactResults()) {
          resolvedArtifacts.add(artifact.getArtifact().getFile().toURI().toURL());
        }
        return resolvedArtifacts;
      }
      catch (Exception ex) {
        latestFailure = ex;
      }
    }
    throw new IllegalStateException("Resolution failed after " + MAX_RESOLUTION_ATTEMPTS + " attempts",
            latestFailure);
  }

  private static List<Dependency> createDependencies(String[] allCoordinates) {
    List<Dependency> dependencies = new ArrayList<>();
    for (String coordinate : allCoordinates) {
      dependencies.add(new Dependency(new DefaultArtifact(coordinate), null));
    }
    return dependencies;
  }

  /**
   * Filter for class path entries.
   */
  private static final class ClassPathEntryFilter {

    private final List<String> exclusions;

    private final AntPathMatcher matcher = new AntPathMatcher();

    private ClassPathEntryFilter(MergedAnnotation<ClassPathExclusions> annotation) {
      this.exclusions = new ArrayList<>();
      this.exclusions.add("log4j-*.jar");
      if (annotation.isPresent()) {
        this.exclusions.addAll(Arrays.asList(annotation.getStringArray(MergedAnnotation.VALUE)));
      }
    }

    private boolean isExcluded(URL url) {
      if ("file".equals(url.getProtocol())) {
        try {
          String name = new File(url.toURI()).getName();
          for (String exclusion : this.exclusions) {
            if (this.matcher.match(exclusion, name)) {
              return true;
            }
          }
        }
        catch (URISyntaxException ex) {
        }
      }
      return false;
    }

  }

}
