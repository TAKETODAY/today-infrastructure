/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.core.test.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link ClassLoader} used to expose dynamically generated content.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DynamicClassLoader extends ClassLoader {

  private final ClassFiles classFiles;

  private final ResourceFiles resourceFiles;

  private final Map<String, DynamicClassFileObject> dynamicClassFiles;

  private final Map<String, DynamicResourceFileObject> dynamicResourceFiles;

  @Nullable
  private final Method defineClassMethod;

  public DynamicClassLoader(ClassLoader parent, ClassFiles classFiles, ResourceFiles resourceFiles,
          Map<String, DynamicClassFileObject> dynamicClassFiles,
          Map<String, DynamicResourceFileObject> dynamicResourceFiles) {

    super(parent);
    this.classFiles = classFiles;
    this.resourceFiles = resourceFiles;
    this.dynamicClassFiles = dynamicClassFiles;
    this.dynamicResourceFiles = dynamicResourceFiles;
    Class<? extends ClassLoader> parentClass = parent.getClass();
    if (parentClass.getName().equals(CompileWithForkedClassLoaderClassLoader.class.getName())) {
      Method setClassResourceLookupMethod = lookupMethod(parentClass,
              "setClassResourceLookup", Function.class);
      ReflectionUtils.makeAccessible(setClassResourceLookupMethod);
      ReflectionUtils.invokeMethod(setClassResourceLookupMethod,
              getParent(), (Function<String, byte[]>) this::findClassBytes);
      this.defineClassMethod = lookupMethod(parentClass,
              "defineDynamicClass", String.class, byte[].class, int.class, int.class);
      ReflectionUtils.makeAccessible(this.defineClassMethod);
      this.dynamicClassFiles.forEach((name, file) -> defineClass(name, file.getBytes()));
    }
    else {
      this.defineClassMethod = null;
    }
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    Class<?> clazz = defineClass(name, findClassBytes(name));
    return (clazz != null ? clazz : super.findClass(name));
  }

  @Nullable
  private Class<?> defineClass(String name, @Nullable byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    if (this.defineClassMethod != null) {
      return (Class<?>) ReflectionUtils.invokeMethod(this.defineClassMethod,
              getParent(), name, bytes, 0, bytes.length);
    }
    return defineClass(name, bytes, 0, bytes.length);
  }

  @Override
  protected Enumeration<URL> findResources(String name) throws IOException {
    URL resource = findResource(name);
    if (resource != null) {
      return new SingletonEnumeration<>(resource);
    }
    return super.findResources(name);
  }

  @Override
  @Nullable
  protected URL findResource(String name) {
    if (name.endsWith(ClassUtils.CLASS_FILE_SUFFIX)) {
      String className = ClassUtils.convertResourcePathToClassName(name.substring(0,
              name.length() - ClassUtils.CLASS_FILE_SUFFIX.length()));
      byte[] classBytes = findClassBytes(className);
      if (classBytes != null) {
        return createResourceUrl(name, () -> classBytes);
      }
    }
    ResourceFile resourceFile = this.resourceFiles.get(name);
    if (resourceFile != null) {
      return createResourceUrl(resourceFile.getPath(), resourceFile::getBytes);
    }
    DynamicResourceFileObject dynamicResourceFile = this.dynamicResourceFiles.get(name);
    if (dynamicResourceFile != null && dynamicResourceFile.getBytes() != null) {
      return createResourceUrl(dynamicResourceFile.getName(), dynamicResourceFile::getBytes);
    }
    return super.findResource(name);
  }

  @Nullable
  private byte[] findClassBytes(String name) {
    ClassFile classFile = this.classFiles.get(name);
    if (classFile != null) {
      return classFile.getContent();
    }
    DynamicClassFileObject dynamicClassFile = this.dynamicClassFiles.get(name);
    return (dynamicClassFile != null ? dynamicClassFile.getBytes() : null);
  }

  @SuppressWarnings("deprecation")  // on JDK 20
  private URL createResourceUrl(String name, Supplier<byte[]> bytesSupplier) {
    try {
      return new URL(null, "resource:///" + name,
              new ResourceFileHandler(bytesSupplier));
    }
    catch (MalformedURLException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private static Method lookupMethod(Class<?> target, String name, Class<?>... parameterTypes) {
    Method method = ReflectionUtils.findMethod(target, name, parameterTypes);
    Assert.notNull(method, () -> "Could not find method '%s' on '%s'".formatted(name, target.getName()));
    return method;
  }

  private static class SingletonEnumeration<E> implements Enumeration<E> {

    @Nullable
    private E element;

    SingletonEnumeration(@Nullable E element) {
      this.element = element;
    }

    @Override
    public boolean hasMoreElements() {
      return this.element != null;
    }

    @Override
    @Nullable
    public E nextElement() {
      E next = this.element;
      this.element = null;
      return next;
    }

  }

  private static class ResourceFileHandler extends URLStreamHandler {

    private final Supplier<byte[]> bytesSupplier;

    ResourceFileHandler(Supplier<byte[]> bytesSupplier) {
      this.bytesSupplier = bytesSupplier;
    }

    @Override
    protected URLConnection openConnection(URL url) {
      return new ResourceFileConnection(url, this.bytesSupplier);
    }

  }

  private static class ResourceFileConnection extends URLConnection {

    private final Supplier<byte[]> bytesSupplier;

    protected ResourceFileConnection(URL url, Supplier<byte[]> bytesSupplier) {
      super(url);
      this.bytesSupplier = bytesSupplier;
    }

    @Override
    public void connect() {
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(this.bytesSupplier.get());
    }

  }

}
