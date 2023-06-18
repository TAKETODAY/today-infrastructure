/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.test.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import cn.taketoday.util.ClassUtils;

/**
 * {@link JavaFileManager} to create in-memory {@link DynamicClassFileObject
 * ClassFileObjects} when compiling.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 4.0
 */
class DynamicJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

  private final ClassLoader classLoader;

  private final ClassFiles classFiles;

  private final ResourceFiles resourceFiles;

  private final Map<String, DynamicClassFileObject> dynamicClassFiles = Collections.synchronizedMap(new LinkedHashMap<>());

  private final Map<String, DynamicResourceFileObject> dynamicResourceFiles = Collections.synchronizedMap(new LinkedHashMap<>());

  DynamicJavaFileManager(JavaFileManager fileManager, ClassLoader classLoader,
          ClassFiles classFiles, ResourceFiles resourceFiles) {

    super(fileManager);
    this.classFiles = classFiles;
    this.resourceFiles = resourceFiles;
    this.classLoader = classLoader;
  }

  @Override
  public ClassLoader getClassLoader(Location location) {
    return this.classLoader;
  }

  @Override
  public FileObject getFileForOutput(Location location, String packageName,
          String relativeName, FileObject sibling) {
    ResourceFile resourceFile = this.resourceFiles.get(relativeName);
    if (resourceFile != null) {
      return new DynamicResourceFileObject(relativeName, resourceFile.getContent());
    }
    return this.dynamicResourceFiles.computeIfAbsent(relativeName, DynamicResourceFileObject::new);
  }

  @Override
  public JavaFileObject getJavaFileForOutput(Location location, String className,
          JavaFileObject.Kind kind, FileObject sibling) throws IOException {
    if (kind == JavaFileObject.Kind.CLASS) {
      ClassFile classFile = this.classFiles.get(className);
      if (classFile != null) {
        return new DynamicClassFileObject(className, classFile.getContent());
      }
      return this.dynamicClassFiles.computeIfAbsent(className, DynamicClassFileObject::new);
    }
    return super.getJavaFileForOutput(location, className, kind, sibling);
  }

  @Override
  public Iterable<JavaFileObject> list(Location location, String packageName,
          Set<Kind> kinds, boolean recurse) throws IOException {
    List<JavaFileObject> result = new ArrayList<>();
    if (kinds.contains(Kind.CLASS)) {
      for (ClassFile candidate : this.classFiles) {
        String existingPackageName = ClassUtils.getPackageName(candidate.getName());
        if (existingPackageName.equals(packageName) || (recurse && existingPackageName.startsWith(packageName + "."))) {
          result.add(new DynamicClassFileObject(candidate.getName(), candidate.getContent()));
        }
      }
      for (DynamicClassFileObject candidate : this.dynamicClassFiles.values()) {
        String existingPackageName = ClassUtils.getPackageName(candidate.getClassName());
        if (existingPackageName.equals(packageName) || (recurse && existingPackageName.startsWith(packageName + "."))) {
          result.add(candidate);
        }
      }
    }
    super.list(location, packageName, kinds, recurse).forEach(result::add);
    return result;
  }

  @Override
  public String inferBinaryName(Location location, JavaFileObject file) {
    if (file instanceof DynamicClassFileObject dynamicClassFileObject) {
      return dynamicClassFileObject.getClassName();
    }
    return super.inferBinaryName(location, file);
  }

  Map<String, DynamicClassFileObject> getDynamicClassFiles() {
    return this.dynamicClassFiles;
  }

  Map<String, DynamicResourceFileObject> getDynamicResourceFiles() {
    return Collections.unmodifiableMap(this.dynamicResourceFiles);
  }

}
