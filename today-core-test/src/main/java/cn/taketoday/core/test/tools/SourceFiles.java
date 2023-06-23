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

import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * An immutable collection of {@link SourceFile} instances.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public final class SourceFiles implements Iterable<SourceFile> {

  private static final SourceFiles NONE = new SourceFiles(DynamicFiles.none());

  private final DynamicFiles<SourceFile> files;

  private SourceFiles(DynamicFiles<SourceFile> files) {
    this.files = files;
  }

  /**
   * Return a {@link SourceFiles} instance with no items.
   *
   * @return the empty instance
   */
  public static SourceFiles none() {
    return NONE;
  }

  /**
   * Factory method that can be used to create a {@link SourceFiles} instance
   * containing the specified files.
   *
   * @param sourceFiles the files to include
   * @return a {@link SourceFiles} instance
   */
  public static SourceFiles of(SourceFile... sourceFiles) {
    return none().and(sourceFiles);
  }

  /**
   * Return a new {@link SourceFiles} instance that merges files from another
   * array of {@link SourceFile} instances.
   *
   * @param sourceFiles the instances to merge
   * @return a new {@link SourceFiles} instance containing merged content
   */
  public SourceFiles and(SourceFile... sourceFiles) {
    return new SourceFiles(this.files.and(sourceFiles));
  }

  /**
   * Return a new {@link SourceFiles} instance that merges files from another
   * array of {@link SourceFile} instances.
   *
   * @param sourceFiles the instances to merge
   * @return a new {@link SourceFiles} instance containing merged content
   */
  public SourceFiles and(Iterable<SourceFile> sourceFiles) {
    return new SourceFiles(this.files.and(sourceFiles));
  }

  /**
   * Return a new {@link SourceFiles} instance that merges files from another
   * {@link SourceFiles} instance.
   *
   * @param sourceFiles the instance to merge
   * @return a new {@link SourceFiles} instance containing merged content
   */
  public SourceFiles and(SourceFiles sourceFiles) {
    return new SourceFiles(this.files.and(sourceFiles.files));
  }

  @Override
  public Iterator<SourceFile> iterator() {
    return this.files.iterator();
  }

  /**
   * Stream the {@link SourceFile} instances contained in this collection.
   *
   * @return a stream of file instances
   */
  public Stream<SourceFile> stream() {
    return this.files.stream();
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
   * Get the {@link SourceFile} with the given
   * {@linkplain  DynamicFile#getPath() path}.
   *
   * @param path the path to find
   * @return a {@link SourceFile} instance or {@code null}
   */
  @Nullable
  public SourceFile get(String path) {
    return this.files.get(path);
  }

  /**
   * Return the single source file contained in the collection.
   *
   * @return the single file
   * @throws IllegalStateException if the collection doesn't contain exactly
   * one file
   */
  public SourceFile getSingle() throws IllegalStateException {
    return this.files.getSingle();
  }

  /**
   * Return the single matching source file contained in the collection.
   *
   * @return the single file
   * @throws IllegalStateException if the collection doesn't contain exactly
   * one file
   */
  public SourceFile getSingle(String pattern) throws IllegalStateException {
    return getSingle(Pattern.compile(pattern));
  }

  private SourceFile getSingle(Pattern pattern) {
    return this.files.getSingle(
            candidate -> pattern.matcher(candidate.getClassName()).matches());
  }

  /**
   * Return a single source file contained in the specified package.
   *
   * @return the single file
   * @throws IllegalStateException if the collection doesn't contain exactly
   * one file
   */
  public SourceFile getSingleFromPackage(String packageName) {
    return this.files.getSingle(candidate -> Objects.equals(packageName,
            ClassUtils.getPackageName(candidate.getClassName())));
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.files.equals(((SourceFiles) obj).files);
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
