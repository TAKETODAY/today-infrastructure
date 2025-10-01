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

package infra.instrument.classloading;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import infra.core.DecoratingClassLoader;
import infra.core.OverridingClassLoader;
import infra.lang.Assert;
import infra.util.FileCopyUtils;
import infra.util.StringUtils;

/**
 * ClassLoader decorator that shadows an enclosing ClassLoader,
 * applying registered transformers to all affected classes.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Costin Leau
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #addTransformer
 * @see OverridingClassLoader
 * @since 4.0
 */
public class ShadowingClassLoader extends DecoratingClassLoader {

  /** Packages that are excluded by default. */
  public static final String[] DEFAULT_EXCLUDED_PACKAGES = {
          "java.", "javax.", "jdk.", "sun.", "oracle.", "com.sun.", "com.ibm.", "COM.ibm.",
          "org.w3c.", "org.xml.", "org.dom4j.", "org.eclipse", "org.aspectj.", "net.sf.cglib",
          "infra.bytecode", "org.apache.xerces.", "org.apache.commons.logging."
  };

  private final ClassLoader enclosingClassLoader;

  private final ArrayList<ClassFileTransformer> classFileTransformers = new ArrayList<>(1);

  private final HashMap<String, Class<?>> classCache = new HashMap<>();

  /**
   * Create a new ShadowingClassLoader, decorating the given ClassLoader,
   * applying {@link #DEFAULT_EXCLUDED_PACKAGES}.
   *
   * @param enclosingClassLoader the ClassLoader to decorate
   * @see #ShadowingClassLoader(ClassLoader, boolean)
   */
  public ShadowingClassLoader(ClassLoader enclosingClassLoader) {
    this(enclosingClassLoader, true);
  }

  /**
   * Create a new ShadowingClassLoader, decorating the given ClassLoader.
   *
   * @param enclosingClassLoader the ClassLoader to decorate
   * @param defaultExcludes whether to apply {@link #DEFAULT_EXCLUDED_PACKAGES}
   */
  public ShadowingClassLoader(ClassLoader enclosingClassLoader, boolean defaultExcludes) {
    Assert.notNull(enclosingClassLoader, "Enclosing ClassLoader is required");
    this.enclosingClassLoader = enclosingClassLoader;
    if (defaultExcludes) {
      for (String excludedPackage : DEFAULT_EXCLUDED_PACKAGES) {
        excludePackage(excludedPackage);
      }
    }
  }

  /**
   * Add the given ClassFileTransformer to the list of transformers that this
   * ClassLoader will apply.
   *
   * @param transformer the ClassFileTransformer
   */
  public void addTransformer(ClassFileTransformer transformer) {
    Assert.notNull(transformer, "Transformer is required");
    this.classFileTransformers.add(transformer);
  }

  /**
   * Copy all ClassFileTransformers from the given ClassLoader to the list of
   * transformers that this ClassLoader will apply.
   *
   * @param other the ClassLoader to copy from
   */
  public void copyTransformers(ShadowingClassLoader other) {
    Assert.notNull(other, "Other ClassLoader is required");
    this.classFileTransformers.addAll(other.classFileTransformers);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (shouldShadow(name)) {
      Class<?> cls = this.classCache.get(name);
      if (cls != null) {
        return cls;
      }
      return doLoadClass(name);
    }
    else {
      return this.enclosingClassLoader.loadClass(name);
    }
  }

  /**
   * Determine whether the given class should be excluded from shadowing.
   *
   * @param className the name of the class
   * @return whether the specified class should be shadowed
   */
  private boolean shouldShadow(String className) {
    return (!className.equals(getClass().getName()) && !className.endsWith("ShadowingClassLoader") &&
            isEligibleForShadowing(className));
  }

  /**
   * Determine whether the specified class is eligible for shadowing
   * by this class loader.
   *
   * @param className the class name to check
   * @return whether the specified class is eligible
   * @see #isExcluded
   */
  protected boolean isEligibleForShadowing(String className) {
    return !isExcluded(className);
  }

  private Class<?> doLoadClass(String name) throws ClassNotFoundException {
    String internalName = StringUtils.replace(name, ".", "/") + ".class";
    InputStream is = this.enclosingClassLoader.getResourceAsStream(internalName);
    if (is == null) {
      throw new ClassNotFoundException(name);
    }
    try {
      byte[] bytes = FileCopyUtils.copyToByteArray(is);
      bytes = applyTransformers(name, bytes);
      Class<?> cls = defineClass(name, bytes, 0, bytes.length);
      // Additional check for defining the package, if not defined yet.
      if (cls.getPackage() == null) {
        int packageSeparator = name.lastIndexOf('.');
        if (packageSeparator != -1) {
          String packageName = name.substring(0, packageSeparator);
          definePackage(packageName, null, null, null, null, null, null, null);
        }
      }
      this.classCache.put(name, cls);
      return cls;
    }
    catch (IOException ex) {
      throw new ClassNotFoundException("Cannot load resource for class [%s]".formatted(name), ex);
    }
  }

  private byte[] applyTransformers(String name, byte[] bytes) {
    String internalName = StringUtils.replace(name, ".", "/");
    try {
      for (ClassFileTransformer transformer : this.classFileTransformers) {
        byte[] transformed = transformer.transform(this, internalName, null, null, bytes);
        bytes = (transformed != null ? transformed : bytes);
      }
      return bytes;
    }
    catch (IllegalClassFormatException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Nullable
  @Override
  public URL getResource(String name) {
    return this.enclosingClassLoader.getResource(name);
  }

  @Override
  @Nullable
  public InputStream getResourceAsStream(String name) {
    return this.enclosingClassLoader.getResourceAsStream(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    return this.enclosingClassLoader.getResources(name);
  }

}
