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

package infra.bytecode;

import infra.lang.Nullable;

/**
 * A visitor to visit a Java class. The methods of this class must be called in the following order:
 * {@code visit} [ {@code visitSource} ] [ {@code visitModule} ][ {@code visitNestHost} ][ {@code
 * visitOuterClass} ] ( {@code visitAnnotation} | {@code visitTypeAnnotation} | {@code
 * visitAttribute} )* ( {@code visitNestMember} | [ {@code * visitPermittedSubclass} ] | {@code
 * visitInnerClass} | {@code visitRecordComponent} | {@code visitField} | {@code visitMethod} )*
 * {@code visitEnd}.
 *
 * @author Eric Bruneton
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public abstract class ClassVisitor {

  /**
   * The class visitor to which this visitor must delegate method calls. May be
   * null.
   */
  @Nullable
  protected ClassVisitor cv;

  /**
   * Constructs a new {@link ClassVisitor}.
   */
  public ClassVisitor() {
    this(null);
  }

  /**
   * Constructs a new {@link ClassVisitor}.
   *
   * @param classVisitor the class visitor to which this visitor must delegate method
   * calls. May be null.
   */
  public ClassVisitor(@Nullable ClassVisitor classVisitor) {
    this.cv = classVisitor;
  }

  /**
   * Visits the header of the class.
   *
   * @param version the class version. The minor version is stored in the 16 most significant bits,
   * and the major version in the 16 least significant bits.
   * @param access the class's access flags (see {@link Opcodes}). This parameter also indicates if
   * the class is deprecated {@link Opcodes#ACC_DEPRECATED} or a record {@link
   * Opcodes#ACC_RECORD}.
   * @param name the internal name of the class (see {@link Type#getInternalName()}).
   * @param signature the signature of this class. May be {@literal null} if the class is not a
   * generic one, and does not extend or implement generic classes or interfaces.
   * @param superName the internal of name of the super class (see {@link Type#getInternalName()}).
   * For interfaces, the super class is {@link Object}. May be {@literal null}, but only for the
   * {@link Object} class.
   * @param interfaces the internal names of the class's interfaces (see {@link
   * Type#getInternalName()}). May be {@literal null}.
   */
  public void visit(
          final int version,
          final int access,
          final String name,
          final String signature,
          final String superName,
          final String[] interfaces) {
    if (cv != null) {
      cv.visit(version, access, name, signature, superName, interfaces);
    }
  }

  /**
   * Visits the source of the class.
   *
   * @param source the name of the source file from which the class was compiled. May be {@literal
   * null}.
   * @param debug additional debug information to compute the correspondence between source and
   * compiled elements of the class. May be {@literal null}.
   */
  public void visitSource(final String source, final String debug) {
    if (cv != null) {
      cv.visitSource(source, debug);
    }
  }

  /**
   * Visit the module corresponding to the class.
   *
   * @param name the fully qualified name (using dots) of the module.
   * @param access the module access flags, among {@code ACC_OPEN}, {@code ACC_SYNTHETIC} and {@code
   * ACC_MANDATED}.
   * @param version the module version, or {@literal null}.
   * @return a visitor to visit the module values, or {@literal null} if this visitor is not
   * interested in visiting this module.
   */
  @Nullable
  public ModuleVisitor visitModule(final String name, final int access, @Nullable String version) {
    if (cv != null) {
      return cv.visitModule(name, access, version);
    }
    return null;
  }

  /**
   * Visits the nest host class of the class. A nest is a set of classes of the same package that
   * share access to their private members. One of these classes, called the host, lists the other
   * members of the nest, which in turn should link to the host of their nest. This method must be
   * called only once and only if the visited class is a non-host member of a nest. A class is
   * implicitly its own nest, so it's invalid to call this method with the visited class name as
   * argument.
   *
   * @param nestHost the internal name of the host class of the nest.
   */
  public void visitNestHost(final String nestHost) {
    if (cv != null) {
      cv.visitNestHost(nestHost);
    }
  }

  /**
   * Visits the enclosing class of the class. This method must be called only if the class has an
   * enclosing class.
   *
   * @param owner internal name of the enclosing class of the class.
   * @param name the name of the method that contains the class, or {@literal null} if the class is
   * not enclosed in a method of its enclosing class.
   * @param descriptor the descriptor of the method that contains the class, or {@literal null} if
   * the class is not enclosed in a method of its enclosing class.
   */
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    if (cv != null) {
      cv.visitOuterClass(owner, name, descriptor);
    }
  }

  /**
   * Visits an annotation of the class.
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   * interested in visiting this annotation.
   */
  @Nullable
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    if (cv != null) {
      return cv.visitAnnotation(descriptor, visible);
    }
    return null;
  }

  /**
   * Visits an annotation on a type in the class signature.
   *
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   * {@link TypeReference#CLASS_TYPE_PARAMETER}, {@link
   * TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link TypeReference#CLASS_EXTENDS}. See
   * {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   * static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   * 'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
   * interested in visiting this annotation.
   */
  @Nullable
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    if (cv != null) {
      return cv.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }
    return null;
  }

  /**
   * Visits a non standard attribute of the class.
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    if (cv != null) {
      cv.visitAttribute(attribute);
    }
  }

  /**
   * Visits a member of the nest. A nest is a set of classes of the same package that share access
   * to their private members. One of these classes, called the host, lists the other members of the
   * nest, which in turn should link to the host of their nest. This method must be called only if
   * the visited class is the host of a nest. A nest host is implicitly a member of its own nest, so
   * it's invalid to call this method with the visited class name as argument.
   *
   * @param nestMember the internal name of a nest member.
   */
  public void visitNestMember(final String nestMember) {
    if (cv != null) {
      cv.visitNestMember(nestMember);
    }
  }

  /**
   * Visits a permitted subclasses. A permitted subclass is one of the allowed subclasses of the
   * current class.
   *
   * @param permittedSubclass the internal name of a permitted subclass.
   */
  public void visitPermittedSubclass(final String permittedSubclass) {
    if (cv != null) {
      cv.visitPermittedSubclass(permittedSubclass);
    }
  }

  /**
   * Visits information about an inner class. This inner class is not necessarily a member of the
   * class being visited.
   *
   * @param name the internal name of an inner class (see {@link Type#getInternalName()}).
   * @param outerName the internal name of the class to which the inner class belongs (see {@link
   * Type#getInternalName()}). May be {@literal null} for not member classes.
   * @param innerName the (simple) name of the inner class inside its enclosing class. May be
   * {@literal null} for anonymous inner classes.
   * @param access the access flags of the inner class as originally declared in the enclosing
   * class.
   */
  public void visitInnerClass(
          final String name, final String outerName, final String innerName, final int access) {
    if (cv != null) {
      cv.visitInnerClass(name, outerName, innerName, access);
    }
  }

  /**
   * Visits a record component of the class.
   *
   * @param name the record component name.
   * @param descriptor the record component descriptor (see {@link Type}).
   * @param signature the record component signature. May be {@literal null} if the record component
   * type does not use generic types.
   * @return a visitor to visit this record component annotations and attributes, or {@literal null}
   * if this class visitor is not interested in visiting these annotations and attributes.
   */
  @Nullable
  public RecordComponentVisitor visitRecordComponent(
          final String name, final String descriptor, final String signature) {
    if (cv != null) {
      return cv.visitRecordComponent(name, descriptor, signature);
    }
    return null;
  }

  /**
   * Visits a field of the class.
   *
   * @param access the field's access flags (see {@link Opcodes}). This parameter also indicates if
   * the field is synthetic and/or deprecated.
   * @param name the field's name.
   * @param descriptor the field's descriptor (see {@link Type}).
   * @param signature the field's signature. May be {@literal null} if the field's type does not use
   * generic types.
   * @param value the field's initial value. This parameter, which may be {@literal null} if the
   * field does not have an initial value, must be an {@link Integer}, a {@link Float}, a {@link
   * Long}, a {@link Double} or a {@link String} (for {@code int}, {@code float}, {@code long}
   * or {@code String} fields respectively). <i>This parameter is only used for static
   * fields</i>. Its value is ignored for non static fields, which must be initialized through
   * bytecode instructions in constructors or methods.
   * @return a visitor to visit field annotations and attributes, or {@literal null} if this class
   * visitor is not interested in visiting these annotations and attributes.
   */
  @Nullable
  public FieldVisitor visitField(final int access, final String name,
          final String descriptor, final String signature, final Object value) {
    if (cv != null) {
      return cv.visitField(access, name, descriptor, signature, value);
    }
    return null;
  }

  /**
   * Visits a method of the class. This method <i>must</i> return a new {@link MethodVisitor}
   * instance (or {@literal null}) each time it is called, i.e., it should not return a previously
   * returned visitor.
   *
   * @param access the method's access flags (see {@link Opcodes}). This parameter also indicates if
   * the method is synthetic and/or deprecated.
   * @param name the method's name.
   * @param descriptor the method's descriptor (see {@link Type}).
   * @param signature the method's signature. May be {@literal null} if the method parameters,
   * return type and exceptions do not use generic types.
   * @param exceptions the internal names of the method's exception classes (see {@link
   * Type#getInternalName()}). May be {@literal null}.
   * @return an object to visit the byte code of the method, or {@literal null} if this class
   * visitor is not interested in visiting the code of this method.
   */
  @Nullable
  public MethodVisitor visitMethod(final int access, final String name,
          final String descriptor, final String signature, final String[] exceptions) {
    if (cv != null) {
      return cv.visitMethod(access, name, descriptor, signature, exceptions);
    }
    return null;
  }

  /**
   * Visits the end of the class. This method, which is the last one to be called, is used to inform
   * the visitor that all the fields and methods of the class have been visited.
   */
  public void visitEnd() {
    if (cv != null) {
      cv.visitEnd();
    }
  }
}
