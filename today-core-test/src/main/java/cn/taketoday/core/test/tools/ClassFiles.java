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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;

/**
 * An immutable collection of {@link ClassFile} instances.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public final class ClassFiles implements Iterable<ClassFile> {

  private static final ClassFiles NONE = new ClassFiles(Collections.emptyMap());

  private final Map<String, ClassFile> files;

  private ClassFiles(Map<String, ClassFile> files) {
    this.files = files;
  }

  /**
   * Return a {@link ClassFiles} instance with no items.
   *
   * @return the empty instance
   */
  public static ClassFiles none() {
    return NONE;
  }

  /**
   * Factory method that can be used to create a {@link ClassFiles}
   * instance containing the specified classes.
   *
   * @param ClassFiles the classes to include
   * @return a {@link ClassFiles} instance
   */
  public static ClassFiles of(ClassFile... ClassFiles) {
    return none().and(ClassFiles);
  }

  /**
   * Return a new {@link ClassFiles} instance that merges classes from
   * another array of {@link ClassFile} instances.
   *
   * @param classFiles the instances to merge
   * @return a new {@link ClassFiles} instance containing merged content
   */
  public ClassFiles and(ClassFile... classFiles) {
    Map<String, ClassFile> merged = new LinkedHashMap<>(this.files);
    Arrays.stream(classFiles).forEach(file -> merged.put(file.getName(), file));
    return new ClassFiles(Collections.unmodifiableMap(merged));
  }

  /**
   * Return a new {@link ClassFiles} instance that merges classes from another
   * iterable of {@link ClassFiles} instances.
   *
   * @param classFiles the instances to merge
   * @return a new {@link ClassFiles} instance containing merged content
   */
  public ClassFiles and(Iterable<ClassFile> classFiles) {
    Map<String, ClassFile> merged = new LinkedHashMap<>(this.files);
    classFiles.forEach(file -> merged.put(file.getName(), file));
    return new ClassFiles(Collections.unmodifiableMap(merged));
  }

  @Override
  public Iterator<ClassFile> iterator() {
    return this.files.values().iterator();
  }

  /**
   * Stream the {@link ClassFile} instances contained in this collection.
   *
   * @return a stream of classes
   */
  public Stream<ClassFile> stream() {
    return this.files.values().stream();
  }

  /**
   * Returns {@code true} if this collection is empty.
   *
   * @return if this collection is empty
   */
  public boolean isEmpty() {
    return this.files.isEmpty();
  }

  /**
   * Get the {@link ClassFile} with the given class name.
   *
   * @param name the fully qualified name to find
   * @return a {@link ClassFile} instance or {@code null}
   */
  @Nullable
  public ClassFile get(String name) {
    return this.files.get(name);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.files.equals(((ClassFiles) obj).files);
  }

  @Override
  public int hashCode() {
    return this.files.hashCode();
  }

  @Override
  public String toString() {
    return this.files.toString();
  }

}
