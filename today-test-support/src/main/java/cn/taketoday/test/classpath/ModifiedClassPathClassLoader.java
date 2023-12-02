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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.test.classpath;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Custom {@link URLClassLoader} that modifies the class path.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ModifiedClassPathClassLoader extends URLClassLoader {

  private static final Map<List<AnnotatedElement>, ModifiedClassPathClassLoader> cache = new ConcurrentReferenceHashMap<>();

  private static final Pattern INTELLIJ_CLASSPATH_JAR_PATTERN = Pattern.compile(".*classpath(\\d+)?\\.jar");

  private static final int MAX_RESOLUTION_ATTEMPTS = 5;

  private final Set<String> excludedPackages;

  private final ClassLoader junitLoader;

  ModifiedClassPathClassLoader(URL[] urls, Set<String> excludedPackages,
          ClassLoader parent, ClassLoader junitLoader) {
    super(urls, parent);
    this.excludedPackages = excludedPackages;
    this.junitLoader = junitLoader;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (name.startsWith("org.junit")
            || name.startsWith("org.hamcrest")
            || name.startsWith("io.netty.internal.tcnative")) {
      return Class.forName(name, false, this.junitLoader);
    }
    String packageName = ClassUtils.getPackageName(name);
    if (excludedPackages.contains(packageName)) {
      throw new ClassNotFoundException();
    }
    return super.loadClass(name);
  }

  static ModifiedClassPathClassLoader get(Class<?> testClass, Method testMethod, List<Object> arguments) {
    Set<AnnotatedElement> candidates = new LinkedHashSet<>();
    candidates.add(testClass);
    candidates.add(testMethod);
    candidates.addAll(getAnnotatedElements(arguments.toArray()));
    List<AnnotatedElement> annotatedElements = candidates.stream()
            .filter(ModifiedClassPathClassLoader::hasAnnotation)
            .collect(Collectors.toList());
    if (annotatedElements.isEmpty()) {
      return null;
    }
    return cache.computeIfAbsent(annotatedElements, (key) -> compute(testClass.getClassLoader(), key));
  }

  private static Collection<AnnotatedElement> getAnnotatedElements(Object[] array) {
    Set<AnnotatedElement> result = new LinkedHashSet<>();
    for (Object item : array) {
      if (item instanceof AnnotatedElement) {
        result.add((AnnotatedElement) item);
      }
      else if (ObjectUtils.isArray(item)) {
        result.addAll(getAnnotatedElements(ObjectUtils.toObjectArray(item)));
      }
    }
    return result;
  }

  private static boolean hasAnnotation(AnnotatedElement element) {
    MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY);
    return annotations.isPresent(ForkedClassPath.class)
            || annotations.isPresent(ClassPathOverrides.class)
            || annotations.isPresent(ClassPathExclusions.class);
  }

  private static ModifiedClassPathClassLoader compute(ClassLoader classLoader,
          List<AnnotatedElement> annotatedClasses) {
    List<MergedAnnotations> annotations = annotatedClasses.stream()
            .map(source -> MergedAnnotations.from(source, SearchStrategy.TYPE_HIERARCHY))
            .toList();
    return new ModifiedClassPathClassLoader(processUrls(extractUrls(classLoader), annotations),
            excludedPackages(annotations), classLoader.getParent(), classLoader);
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
    if (classLoader instanceof URLClassLoader urlClassLoader) {
      return Stream.of(urlClassLoader.getURLs());
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

  private static URL[] processUrls(URL[] urls, List<MergedAnnotations> annotations) {
    ClassPathEntryFilter filter = new ClassPathEntryFilter(annotations);
    List<URL> additionalUrls = getAdditionalUrls(annotations);
    List<URL> processedUrls = new ArrayList<>(additionalUrls);
    for (URL url : urls) {
      if (!filter.isExcluded(url)) {
        processedUrls.add(url);
      }
    }
    return processedUrls.toArray(new URL[0]);
  }

  private static List<URL> getAdditionalUrls(List<MergedAnnotations> annotations) {
    LinkedHashSet<URL> urls = new LinkedHashSet<>();
    for (MergedAnnotations candidate : annotations) {
      MergedAnnotation<ClassPathOverrides> annotation = candidate.get(ClassPathOverrides.class);
      if (annotation.isPresent()) {
        urls.addAll(resolveCoordinates(annotation.getStringArray(MergedAnnotation.VALUE)));
      }
    }
    return urls.stream().toList();
  }

  private static List<URL> resolveCoordinates(String[] coordinates) {
    Exception latestFailure = null;
    RepositorySystem repositorySystem = createRepositorySystem();
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    session.setSystemProperties(System.getProperties());
    LocalRepository localRepository = new LocalRepository(System.getProperty("user.home") + "/.m2/repository");
    RemoteRepository remoteRepository = new RemoteRepository.Builder("central", "default",
            "https://repo.maven.apache.org/maven2")
            .build();
    session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));
    for (int i = 0; i < MAX_RESOLUTION_ATTEMPTS; i++) {
      CollectRequest collectRequest = new CollectRequest(null, Collections.singletonList(remoteRepository));
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

  private static RepositorySystem createRepositorySystem() {
    org.eclipse.aether.impl.DefaultServiceLocator serviceLocator = MavenRepositorySystemUtils.newServiceLocator();
    serviceLocator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    serviceLocator.addService(TransporterFactory.class, HttpTransporterFactory.class);
    return serviceLocator.getService(RepositorySystem.class);
  }

  private static List<Dependency> createDependencies(String[] allCoordinates) {
    List<Dependency> dependencies = new ArrayList<>();
    for (String coordinate : allCoordinates) {
      dependencies.add(new Dependency(new DefaultArtifact(coordinate), null));
    }
    return dependencies;
  }

  private static Set<String> excludedPackages(List<MergedAnnotations> annotations) {
    Set<String> excludedPackages = new HashSet<>();
    for (MergedAnnotations candidate : annotations) {
      MergedAnnotation<ClassPathExclusions> annotation = candidate.get(ClassPathExclusions.class);
      if (annotation.isPresent()) {
        excludedPackages.addAll(Arrays.asList(annotation.getStringArray("packages")));
      }
    }
    return excludedPackages;
  }

  /**
   * Filter for class path entries.
   */
  private static final class ClassPathEntryFilter {

    private final List<String> exclusions;

    private final AntPathMatcher matcher = new AntPathMatcher();

    private ClassPathEntryFilter(List<MergedAnnotations> annotations) {
      Set<String> exclusions = new LinkedHashSet<>();
      for (MergedAnnotations candidate : annotations) {
        MergedAnnotation<ClassPathExclusions> annotation = candidate.get(ClassPathExclusions.class);
        if (annotation.isPresent()) {
          exclusions.addAll(Arrays.asList(annotation.getStringArray(MergedAnnotation.VALUE)));
        }
      }
      this.exclusions = exclusions.stream().toList();
    }

    private boolean isExcluded(URL url) {
      if ("file".equals(url.getProtocol())) {
        try {
          URI uri = url.toURI();
          File file = new File(uri);
          String name = (!uri.toString().endsWith("/"))
                        ? file.getName()
                        : file.getParentFile().getParentFile().getName();
          for (String exclusion : this.exclusions) {
            if (this.matcher.match(exclusion, name)) {
              return true;
            }
          }
        }
        catch (URISyntaxException ignored) { }
      }
      return false;
    }

  }

}
