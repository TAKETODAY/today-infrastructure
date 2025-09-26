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

package infra.bytecode.tree;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.Attribute;
import infra.bytecode.ClassVisitor;
import infra.bytecode.RecordComponentVisitor;
import infra.bytecode.Type;
import infra.bytecode.TypePath;

/**
 * A node that represents a record component.
 *
 * @author Remi Forax
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class RecordComponentNode extends RecordComponentVisitor {

  /** The record component name. */
  public String name;

  /** The record component descriptor (see {@link Type}). */
  public String descriptor;

  /** The record component signature. May be {@literal null}. */
  @Nullable
  public String signature;

  /** The runtime visible annotations of this record component. May be {@literal null}. */
  @Nullable
  public List<AnnotationNode> visibleAnnotations;

  /** The runtime invisible annotations of this record component. May be {@literal null}. */
  @Nullable
  public List<AnnotationNode> invisibleAnnotations;

  /** The runtime visible type annotations of this record component. May be {@literal null}. */
  @Nullable
  public List<TypeAnnotationNode> visibleTypeAnnotations;

  /** The runtime invisible type annotations of this record component. May be {@literal null}. */
  @Nullable
  public List<TypeAnnotationNode> invisibleTypeAnnotations;

  /** The non standard attributes of this record component. * May be {@literal null}. */
  @Nullable
  public List<Attribute> attrs;

  /**
   * Constructs a new {@link RecordComponentNode}.
   *
   * @param name the record component name.
   * @param descriptor the record component descriptor (see {@link Type}).
   * @param signature the record component signature.
   */
  public RecordComponentNode(final String name, final String descriptor, @Nullable final String signature) {
    this.name = name;
    this.descriptor = descriptor;
    this.signature = signature;
  }

  // -----------------------------------------------------------------------------------------------
  // Implementation of the FieldVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    AnnotationNode annotation = new AnnotationNode(descriptor);
    if (visible) {
      visibleAnnotations = Util.add(visibleAnnotations, annotation);
    }
    else {
      invisibleAnnotations = Util.add(invisibleAnnotations, annotation);
    }
    return annotation;
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    TypeAnnotationNode typeAnnotation = new TypeAnnotationNode(typeRef, typePath, descriptor);
    if (visible) {
      visibleTypeAnnotations = Util.add(visibleTypeAnnotations, typeAnnotation);
    }
    else {
      invisibleTypeAnnotations = Util.add(invisibleTypeAnnotations, typeAnnotation);
    }
    return typeAnnotation;
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    attrs = Util.add(attrs, attribute);
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Accept methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Makes the given class visitor visit this record component.
   *
   * @param classVisitor a class visitor.
   */
  public void accept(final ClassVisitor classVisitor) {
    RecordComponentVisitor recordComponentVisitor =
            classVisitor.visitRecordComponent(name, descriptor, signature);
    if (recordComponentVisitor == null) {
      return;
    }
    // Visit the annotations.
    if (visibleAnnotations != null) {
      for (AnnotationNode annotation : visibleAnnotations) {
        annotation.accept(recordComponentVisitor.visitAnnotation(annotation.desc, true));
      }
    }
    if (invisibleAnnotations != null) {
      for (AnnotationNode annotation : invisibleAnnotations) {
        annotation.accept(recordComponentVisitor.visitAnnotation(annotation.desc, false));
      }
    }
    if (visibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : visibleTypeAnnotations) {
        typeAnnotation.accept(
                recordComponentVisitor.visitTypeAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
      }
    }
    if (invisibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : invisibleTypeAnnotations) {
        typeAnnotation.accept(
                recordComponentVisitor.visitTypeAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
      }
    }
    // Visit the non standard attributes.
    if (attrs != null) {
      for (Attribute attr : attrs) {
        recordComponentVisitor.visitAttribute(attr);
      }
    }
    recordComponentVisitor.visitEnd();
  }
}
