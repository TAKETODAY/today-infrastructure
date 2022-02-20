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

package cn.taketoday.framework.web.embedded.tomcat;

import org.apache.catalina.loader.ParallelWebappClassLoader;
import org.apache.tomcat.util.compat.JreCompat;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Extension of Tomcat's {@link ParallelWebappClassLoader} that does not consider the
 * {@link ClassLoader#getSystemClassLoader() system classloader}. This is required to
 * ensure that any custom context class loader is always used (as is the case with some
 * executable archives).
 *
 * @author Phillip Webb
 * @author Andy Clement
 * @since 4.0
 */
public class TomcatEmbeddedWebappClassLoader extends ParallelWebappClassLoader {

  private static final Logger logger = LoggerFactory.getLogger(TomcatEmbeddedWebappClassLoader.class);

  static {
    if (!JreCompat.isGraalAvailable()) {
      ClassLoader.registerAsParallelCapable();
    }
  }

  public TomcatEmbeddedWebappClassLoader() {
  }

  public TomcatEmbeddedWebappClassLoader(ClassLoader parent) {
    super(parent);
  }

  @Override
  public URL findResource(String name) {
    return null;
  }

  @Override
  public Enumeration<URL> findResources(String name) {
    return Collections.emptyEnumeration();
  }

  @Override
  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized(JreCompat.isGraalAvailable() ? this : getClassLoadingLock(name)) {
      Class<?> result = findExistingLoadedClass(name);
      result = (result != null) ? result : doLoadClass(name);
      if (result == null) {
        throw new ClassNotFoundException(name);
      }
      return resolveIfNecessary(result, resolve);
    }
  }

  private Class<?> findExistingLoadedClass(String name) {
    Class<?> resultClass = findLoadedClass0(name);
    resultClass = (resultClass != null || JreCompat.isGraalAvailable()) ? resultClass : findLoadedClass(name);
    return resultClass;
  }

  private Class<?> doLoadClass(String name) {
    if ((this.delegate || filter(name, true))) {
      Class<?> result = loadFromParent(name);
      return (result != null) ? result : findClassIgnoringNotFound(name);
    }
    Class<?> result = findClassIgnoringNotFound(name);
    return (result != null) ? result : loadFromParent(name);
  }

  private Class<?> resolveIfNecessary(Class<?> resultClass, boolean resolve) {
    if (resolve) {
      resolveClass(resultClass);
    }
    return (resultClass);
  }

  @Override
  protected void addURL(URL url) {
    // Ignore URLs added by the Tomcat 8 implementation (see gh-919)
    if (logger.isTraceEnabled()) {
      logger.trace("Ignoring request to add {} to the tomcat classloader", url);
    }
  }

  private Class<?> loadFromParent(String name) {
    if (this.parent == null) {
      return null;
    }
    try {
      return Class.forName(name, false, this.parent);
    }
    catch (ClassNotFoundException ex) {
      return null;
    }
  }

  private Class<?> findClassIgnoringNotFound(String name) {
    try {
      return findClass(name);
    }
    catch (ClassNotFoundException ex) {
      return null;
    }
  }

}
