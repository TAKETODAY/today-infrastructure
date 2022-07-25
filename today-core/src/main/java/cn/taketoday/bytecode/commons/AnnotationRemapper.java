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

package cn.taketoday.bytecode.commons;

import cn.taketoday.bytecode.AnnotationVisitor;

/**
 * An {@link AnnotationVisitor} that remaps types with a {@link Remapper}.
 *
 * @author Eugene Kuleshov
 */
public class AnnotationRemapper extends AnnotationVisitor {

  /**
   * The descriptor of the visited annotation. May be {@literal null}, for instance for
   * AnnotationDefault.
   */
  protected final String descriptor;

  /** The remapper used to remap the types in the visited annotation. */
  protected final Remapper remapper;

  /**
   * Constructs a new {@link AnnotationRemapper}.
   *
   * @param descriptor the descriptor of the visited annotation. May be {@literal null}.
   * @param annotationVisitor the annotation visitor this remapper must delegate to.
   * @param remapper the remapper to use to remap the types in the visited annotation.
   */
  public AnnotationRemapper(
          final String descriptor,
          final AnnotationVisitor annotationVisitor,
          final Remapper remapper) {
    super(annotationVisitor);
    this.descriptor = descriptor;
    this.remapper = remapper;
  }

  @Override
  public void visit(final String name, final Object value) {
    super.visit(mapAnnotationAttributeName(name), remapper.mapValue(value));
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    super.visitEnum(mapAnnotationAttributeName(name), remapper.mapDesc(descriptor), value);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String name, final String descriptor) {
    AnnotationVisitor annotationVisitor =
            super.visitAnnotation(mapAnnotationAttributeName(name), remapper.mapDesc(descriptor));
    if (annotationVisitor == null) {
      return null;
    }
    else {
      return annotationVisitor == av
             ? this
             : createAnnotationRemapper(descriptor, annotationVisitor);
    }
  }

  @Override
  public AnnotationVisitor visitArray(final String name) {
    AnnotationVisitor annotationVisitor = super.visitArray(mapAnnotationAttributeName(name));
    if (annotationVisitor == null) {
      return null;
    }
    else {
      return annotationVisitor == av
             ? this
             : createAnnotationRemapper(/* descriptor = */ null, annotationVisitor);
    }
  }

  /**
   * Constructs a new remapper for annotations. The default implementation of this method returns a
   * new {@link AnnotationRemapper}.
   *
   * @param descriptor the descriptor of the visited annotation.
   * @param annotationVisitor the AnnotationVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected AnnotationVisitor createAnnotationRemapper(
          final String descriptor, final AnnotationVisitor annotationVisitor) {
    return new AnnotationRemapper(descriptor, annotationVisitor, remapper);
  }

  /**
   * Maps an annotation attribute name with the remapper. Returns the original name unchanged if the
   * internal name of the annotation is {@literal null}.
   *
   * @param name the name of the annotation attribute.
   * @return the new name of the annotation attribute.
   */
  private String mapAnnotationAttributeName(final String name) {
    if (descriptor == null) {
      return name;
    }
    return remapper.mapAnnotationAttributeName(descriptor, name);
  }
}
