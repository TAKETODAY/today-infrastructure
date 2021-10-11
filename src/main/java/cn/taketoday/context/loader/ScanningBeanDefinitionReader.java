/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.loader;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.core.ThrowableSupplier;
import cn.taketoday.core.annotation.ClassMetaReader;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.JarEntryResource;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceFilter;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static cn.taketoday.core.Constant.PACKAGE_SEPARATOR;
import static cn.taketoday.core.Constant.PATH_SEPARATOR;

/**
 * @author TODAY 2021/10/2 23:38
 * @since 4.0
 */
public class ScanningBeanDefinitionReader {
  private static final Logger log = LoggerFactory.getLogger(ScanningBeanDefinitionReader.class);

  private final BeanDefinitionRegistry registry;
  /** @since 2.1.7 Scan candidates */
  private final ArrayList<AnnotatedElement> componentScanned = new ArrayList<>();

  private PatternResourceLoader resourceLoader = new PathMatchingPatternResourceLoader();
  private final BeanDefinitionCreationStrategies creationStrategies = new BeanDefinitionCreationStrategies();
  private final BeanDefinitionCreationContext creationContext;

  public ScanningBeanDefinitionReader(BeanDefinitionRegistry registry) {
    this.registry = registry;
    this.creationContext = new BeanDefinitionCreationContext(registry);
  }

  public void setResourceLoader(PatternResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public PatternResourceLoader getResourceLoader() {
    return resourceLoader;
  }

  /**
   * Load {@link BeanDefinition}s from input package locations
   *
   * @param locations
   *         package locations
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @since 4.0
   */
  public void scan(String... locations) throws BeanDefinitionStoreException {
    // Loading candidates components
    log.info("Scanning candidates components");

    for (String location : locations) {
      scan(location);
    }

//    log.info("There are [{}] candidates components in [{}]", candidates.size(), this);
  }

  public void scan(String packageName) {
    String resourceToUse = packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
    if (log.isDebugEnabled()) {
      log.debug("Scanning component candidates from package: [{}]", packageName);
    }
    try {
      Resource[] resources = resourceLoader.getResources(resourceToUse);
      for (Resource resource : resources) {
        scan(resource, packageName);
      }
    }
    catch (IOException e) {
      throw new ApplicationContextException("IO exception occur With Msg: [" + e + ']', e);
    }
  }

  /**
   * Scan class in a {@link Resource}
   *
   * @param resource
   *         {@link Resource} in class maybe a jar file or class directory
   * @param packageName
   *         if {@link Resource} is a directory will use this packageName
   *
   * @throws IOException
   *         if the resource is not available
   * @since 2.1.6
   */
  protected void scan(Resource resource, String packageName) throws IOException {
    if (log.isTraceEnabled()) {
      log.trace("Scanning candidate components in [{}]", resource.getLocation());
    }
    if (resource instanceof FileBasedResource) {
      if (resource.isDirectory()) {
        findInDirectory(resource);
        return;
      }
      if (resource.getName().endsWith(".jar")) {
        scanInJarFile(resource, packageName, () -> new JarFile(resource.getFile()));
      }
    }
    else if (resource instanceof JarEntryResource) {
      scanInJarFile(resource, packageName, ((JarEntryResource) resource)::getJarFile);
    }
  }

  private Predicate<Resource> jarResourceFilter;

  protected void scanInJarFile(Resource resource,
                               String packageName,
                               ThrowableSupplier<JarFile, IOException> jarFileSupplier) throws IOException //
  {
    if (getJarResourceFilter().test(resource)) {
      if (log.isTraceEnabled()) {
        log.trace("Scan in jar file: [{}]", resource.getLocation());
      }
      try (JarFile jarFile = jarFileSupplier.get()) {
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
          loadClassFromJarEntry(jarEntries.nextElement(), packageName);
        }
      }
    }
  }

  /**
   * Load classes from a {@link JarEntry}
   *
   * @param jarEntry
   *         The entry of jar
   */
  public void loadClassFromJarEntry(JarEntry jarEntry, String packageName) {
    if (jarEntry.isDirectory()) {
      return;
    }
    String jarEntryName = jarEntry.getName(); // cn/taketoday/xxx/yyy.class
    if (jarEntryName.endsWith(ClassUtils.CLASS_FILE_SUFFIX)) {
      // fix #10 classes loading from a jar can't be load
      String nameToUse = jarEntryName.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
      if (StringUtils.isEmpty(packageName) || nameToUse.startsWith(packageName)) {
        String className = nameToUse.substring(0, nameToUse.lastIndexOf(PACKAGE_SEPARATOR));
        ClassNode classNode = ClassMetaReader.read(className);
        process(classNode);
      }
    }
  }

  /**
   * <p>
   * Find in directory.
   * </p>
   * Note: don't need packageName
   *
   * @throws IOException
   *         if the resource is not available
   */
  protected void findInDirectory(Resource directory) throws IOException {
    if (!directory.exists()) {
      log.error("The location: [{}] you provided that does not exist", directory.getLocation());
      return;
    }

    if (log.isTraceEnabled()) {
      log.trace("Enter: [{}]", directory.getLocation());
    }

    for (Resource resource : directory.list(CLASS_RESOURCE_FILTER)) {
      if (resource.isDirectory()) { // recursive
        findInDirectory(resource);
      }
      else {
        ClassNode classNode = ClassMetaReader.read(resource);
        process(classNode);
      }
    }
  }

  protected void process(ClassNode classNode) {
    Set<BeanDefinition> beanDefinitions = creationStrategies.loadBeanDefinitions(classNode, creationContext);
    if (CollectionUtils.isNotEmpty(beanDefinitions)) {
      for (BeanDefinition beanDefinition : beanDefinitions) {
        registry.registerBeanDefinition(beanDefinition);
      }
    }
  }

  /** Class resource filter */
  private static final ResourceFilter CLASS_RESOURCE_FILTER = resource ->
          resource.isDirectory()
                  || (resource.getName().endsWith(ClassUtils.CLASS_FILE_SUFFIX)
                  && !resource.getName().startsWith("package-info"));

  public Predicate<Resource> getJarResourceFilter() {
    Predicate<Resource> jarResourceFilter = this.jarResourceFilter;
    if (jarResourceFilter == null) {
      return this.jarResourceFilter = new DefaultJarResourcePredicate(CandidateComponentScanner.getSharedInstance());
    }
    return jarResourceFilter;
  }

  static final class DefaultJarResourcePredicate implements Predicate<Resource> {
    private final CandidateComponentScanner scanner;

    DefaultJarResourcePredicate(CandidateComponentScanner scanner) {
      this.scanner = scanner;
    }

    @Override
    public boolean test(Resource resource) {

      if (scanner.isUseDefaultIgnoreScanJarPrefix()) {

        String fileName = resource.getName();
        for (String ignoreJarName : CandidateComponentScanner.getDefaultIgnoreJarPrefix()) {
          if (fileName.startsWith(ignoreJarName)) {
            return false;
          }
        }
      }

      String[] ignoreScanJarPrefixs = scanner.getIgnoreScanJarPrefixs();
      if (ignoreScanJarPrefixs != null) {
        String fileName = resource.getName();

        for (String ignoreJarName : ignoreScanJarPrefixs) {
          if (fileName.startsWith(ignoreJarName)) {
            return false;
          }
        }
      }

      return true;
    }
  }
}
