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

package infra.bytecode.commons;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.RecordComponentVisitor;
import infra.bytecode.TypePath;
import infra.lang.Nullable;

/**
 * A {@link RecordComponentVisitor} that remaps types with a {@link Remapper}.
 *
 * @author Remi Forax
 */
public class RecordComponentRemapper extends RecordComponentVisitor {

  /** The remapper used to remap the types in the visited field. */
  protected final Remapper remapper;

  /**
   * Constructs a new {@link RecordComponentRemapper}.
   *
   * @param recordComponentVisitor the record component visitor this remapper must delegate to.
   * @param remapper the remapper to use to remap the types in the visited record component.
   */
  public RecordComponentRemapper(
          final RecordComponentVisitor recordComponentVisitor, final Remapper remapper) {
    super(recordComponentVisitor);
    this.remapper = remapper;
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitAnnotation(remapper.mapDesc(descriptor), visible);
    return createAnnotationRemapper(descriptor, annotationVisitor);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitTypeAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
    return createAnnotationRemapper(descriptor, annotationVisitor);
  }

  /**
   * Constructs a new remapper for annotations. The default implementation of this method returns a
   * new {@link AnnotationRemapper}.
   *
   * @param descriptor the descriptor sof the visited annotation.
   * @param annotationVisitor the AnnotationVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  @Nullable
  protected AnnotationVisitor createAnnotationRemapper(
          final String descriptor, @Nullable final AnnotationVisitor annotationVisitor) {
    if (annotationVisitor == null) {
      return null;
    }
    return new AnnotationRemapper(descriptor, annotationVisitor, remapper);
  }
}
