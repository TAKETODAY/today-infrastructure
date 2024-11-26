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
package infra.bytecode.tree;

import infra.bytecode.TypePath;
import infra.bytecode.TypeReference;
import infra.lang.Nullable;

/**
 * A node that represents a type annotation.
 *
 * @author Eric Bruneton
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class TypeAnnotationNode extends AnnotationNode {

  /** A reference to the annotated type. See {@link TypeReference}. */
  public int typeRef;

  /**
   * The path to the annotated type argument, wildcard bound, array element type, or static outer
   * type within the referenced type. May be {@literal null} if the annotation targets 'typeRef' as
   * a whole.
   */
  @Nullable
  public TypePath typePath;

  /**
   * Constructs a new {@link AnnotationNode}.
   *
   * @param typeRef a reference to the annotated type. See {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   * static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   * 'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   */
  public TypeAnnotationNode(final int typeRef, @Nullable final TypePath typePath, final String descriptor) {
    super(descriptor);
    this.typeRef = typeRef;
    this.typePath = typePath;
  }

}
