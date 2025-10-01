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

package infra.app.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Supplier;
import java.util.jar.Manifest;

import infra.app.loader.net.protocol.jar.JarUrlClassLoader;
import org.jspecify.annotations.Nullable;

/**
 * {@link ClassLoader} used by the {@link Launcher}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class LaunchedClassLoader extends JarUrlClassLoader {

  private static final String JAR_MODE_PACKAGE_PREFIX = "infra.app.loader.jarmode.";

  private static final String JAR_MODE_RUNNER_CLASS_NAME = JarModeRunner.class.getName();

  static {
    ClassLoader.registerAsParallelCapable();
  }

  private final boolean exploded;

  @Nullable
  private final Archive rootArchive;

  private final Object definePackageLock = new Object();

  @Nullable
  private volatile DefinePackageCallType definePackageCallType;

  /**
   * Create a new {@link LaunchedClassLoader} instance.
   *
   * @param exploded if the underlying archive is exploded
   * @param urls the URLs from which to load classes and resources
   * @param parent the parent class loader for delegation
   */
  public LaunchedClassLoader(boolean exploded, URL[] urls, ClassLoader parent) {
    this(exploded, null, urls, parent);
  }

  /**
   * Create a new {@link LaunchedClassLoader} instance.
   *
   * @param exploded if the underlying archive is exploded
   * @param rootArchive the root archive or {@code null}
   * @param urls the URLs from which to load classes and resources
   * @param parent the parent class loader for delegation
   */
  public LaunchedClassLoader(boolean exploded, @Nullable Archive rootArchive, URL[] urls, ClassLoader parent) {
    super(urls, parent);
    this.exploded = exploded;
    this.rootArchive = rootArchive;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (name.startsWith(JAR_MODE_PACKAGE_PREFIX) || name.equals(JAR_MODE_RUNNER_CLASS_NAME)) {
      try {
        Class<?> result = loadClassInLaunchedClassLoader(name);
        if (resolve) {
          resolveClass(result);
        }
        return result;
      }
      catch (ClassNotFoundException ex) {
        // Ignore
      }
    }
    return super.loadClass(name, resolve);
  }

  private Class<?> loadClassInLaunchedClassLoader(String name) throws ClassNotFoundException {
    try {
      String internalName = name.replace('.', '/') + ".class";
      try (InputStream inputStream = getParent().getResourceAsStream(internalName);
              ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        if (inputStream == null) {
          throw new ClassNotFoundException(name);
        }
        inputStream.transferTo(outputStream);
        byte[] bytes = outputStream.toByteArray();
        Class<?> definedClass = defineClass(name, bytes, 0, bytes.length);
        definePackageIfNecessary(name);
        return definedClass;
      }
    }
    catch (IOException ex) {
      throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
    }
  }

  @Override
  protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
    return (!this.exploded) ? super.definePackage(name, man, url) : definePackageForExploded(name, man, url);
  }

  private Package definePackageForExploded(String name, Manifest man, URL url) {
    synchronized(this.definePackageLock) {
      return definePackage(DefinePackageCallType.MANIFEST, () -> super.definePackage(name, man, url));
    }
  }

  @Override
  protected Package definePackage(String name, String specTitle, String specVersion, String specVendor,
          String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
    if (!this.exploded) {
      return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor,
              sealBase);
    }
    return definePackageForExploded(name, sealBase, () -> super.definePackage(name, specTitle, specVersion,
            specVendor, implTitle, implVersion, implVendor, sealBase));
  }

  private Package definePackageForExploded(String name, URL sealBase, Supplier<Package> call) {
    synchronized(this.definePackageLock) {
      if (this.definePackageCallType == null) {
        // We're not part of a call chain which means that the URLClassLoader
        // is trying to define a package for our exploded JAR. We use the
        // manifest version to ensure package attributes are set
        Manifest manifest = getManifest(this.rootArchive);
        if (manifest != null) {
          return definePackage(name, manifest, sealBase);
        }
      }
      return definePackage(DefinePackageCallType.ATTRIBUTES, call);
    }
  }

  private <T> T definePackage(DefinePackageCallType type, Supplier<T> call) {
    DefinePackageCallType existingType = this.definePackageCallType;
    try {
      this.definePackageCallType = type;
      return call.get();
    }
    finally {
      this.definePackageCallType = existingType;
    }
  }

  @Nullable
  private Manifest getManifest(@Nullable Archive archive) {
    try {
      return (archive != null) ? archive.getManifest() : null;
    }
    catch (IOException ex) {
      return null;
    }
  }

  /**
   * The different types of call made to define a package. We track these for exploded
   * jars so that we can detect packages that should have manifest attributes applied.
   */
  private enum DefinePackageCallType {

    /**
     * A define package call from a resource that has a manifest.
     */
    MANIFEST,

    /**
     * A define package call with a direct set of attributes.
     */
    ATTRIBUTES

  }

}
