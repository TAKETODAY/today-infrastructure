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

import java.util.List;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.Attribute;
import infra.bytecode.ClassVisitor;
import infra.bytecode.FieldVisitor;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.TypePath;
import infra.lang.Nullable;

/**
 * A node that represents a field.
 *
 * @author Eric Bruneton
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class FieldNode extends FieldVisitor {

  /**
   * The field's access flags (see {@link Opcodes}). This field also indicates if
   * the field is synthetic and/or deprecated.
   */
  public int access;

  /** The field's name. */
  public String name;

  /** The field's descriptor (see {@link Type}). */
  public String desc;

  /** The field's signature. May be {@literal null}. */
  @Nullable
  public String signature;

  /**
   * The field's initial value. This field, which may be {@literal null} if the field does not have
   * an initial value, must be an {@link Integer}, a {@link Float}, a {@link Long}, a {@link Double}
   * or a {@link String}.
   */
  @Nullable
  public Object value;

  /** The runtime visible annotations of this field. May be {@literal null}. */
  @Nullable
  public List<AnnotationNode> visibleAnnotations;

  /** The runtime invisible annotations of this field. May be {@literal null}. */
  @Nullable
  public List<AnnotationNode> invisibleAnnotations;

  /** The runtime visible type annotations of this field. May be {@literal null}. */
  @Nullable
  public List<TypeAnnotationNode> visibleTypeAnnotations;

  /** The runtime invisible type annotations of this field. May be {@literal null}. */
  @Nullable
  public List<TypeAnnotationNode> invisibleTypeAnnotations;

  /** The non standard attributes of this field. * May be {@literal null}. */
  @Nullable
  public List<Attribute> attrs;

  /**
   * Constructs a new {@link FieldNode}.
   *
   * @param access the field's access flags (see {@link Opcodes}). This parameter
   * also indicates if the field is synthetic and/or deprecated.
   * @param name the field's name.
   * @param descriptor the field's descriptor (see {@link Type}).
   * @param signature the field's signature.
   * @param value the field's initial value. This parameter, which may be {@literal null} if the
   * field does not have an initial value, must be an {@link Integer}, a {@link Float}, a {@link
   * Long}, a {@link Double} or a {@link String}.
   */
  public FieldNode(final int access, final String name, final String descriptor,
          @Nullable final String signature, @Nullable final Object value) {
    this.access = access;
    this.name = name;
    this.desc = descriptor;
    this.signature = signature;
    this.value = value;
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
   * Makes the given class visitor visit this field.
   *
   * @param classVisitor a class visitor.
   */
  public void accept(final ClassVisitor classVisitor) {
    FieldVisitor fieldVisitor = classVisitor.visitField(access, name, desc, signature, value);
    if (fieldVisitor == null) {
      return;
    }
    // Visit the annotations.
    if (visibleAnnotations != null) {
      for (AnnotationNode annotation : visibleAnnotations) {
        annotation.accept(fieldVisitor.visitAnnotation(annotation.desc, true));
      }
    }
    if (invisibleAnnotations != null) {
      for (AnnotationNode annotation : invisibleAnnotations) {
        annotation.accept(fieldVisitor.visitAnnotation(annotation.desc, false));
      }
    }
    if (visibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : visibleTypeAnnotations) {
        typeAnnotation.accept(
                fieldVisitor.visitTypeAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
      }
    }
    if (invisibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : invisibleTypeAnnotations) {
        typeAnnotation.accept(
                fieldVisitor.visitTypeAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
      }
    }
    // Visit the non standard attributes.
    if (attrs != null) {
      for (Attribute attr : attrs) {
        fieldVisitor.visitAttribute(attr);
      }
    }
    fieldVisitor.visitEnd();
  }
}
