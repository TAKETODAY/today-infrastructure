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
package cn.taketoday.bytecode.tree;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.FieldVisitor;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.ModuleVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.RecordComponentVisitor;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.TypePath;

/**
 * A node that represents a class.
 *
 * @author Eric Bruneton
 */
public class ClassNode extends ClassVisitor {

  /**
   * The class version. The minor version is stored in the 16 most significant bits, and the major
   * version in the 16 least significant bits.
   */
  public int version;

  /**
   * The class's access flags (see {@link Opcodes}). This field also indicates if
   * the class is deprecated {@link Opcodes#ACC_DEPRECATED} or a record {@link Opcodes#ACC_RECORD}.
   */
  public int access;

  /** The internal name of this class (see {@link Type#getInternalName}). */
  public String name;

  /** The signature of this class. May be {@literal null}. */
  public String signature;

  /**
   * The internal of name of the super class (see {@link Type#getInternalName}).
   * For interfaces, the super class is {@link Object}. May be {@literal null}, but only for the
   * {@link Object} class.
   */
  public String superName;

  /**
   * The internal names of the interfaces directly implemented by this class (see {@link
   * Type#getInternalName}).
   * Map be {@literal null}. there is no interfaces.
   */
  public String[] interfaces;

  /** The name of the source file from which this class was compiled. May be {@literal null}. */
  public String sourceFile;

  /**
   * The correspondence between source and compiled elements of this class. May be {@literal null}.
   */
  public String sourceDebug;

  /** The module stored in this class. May be {@literal null}. */
  public ModuleNode module;

  /** The internal name of the enclosing class of this class. May be {@literal null}. */
  public String outerClass;

  /**
   * The name of the method that contains this class, or {@literal null} if this class is not
   * enclosed in a method.
   */
  public String outerMethod;

  /**
   * The descriptor of the method that contains this class, or {@literal null} if this class is not
   * enclosed in a method.
   */
  public String outerMethodDesc;

  /** The runtime visible annotations of this class. May be {@literal null}. */
  public List<AnnotationNode> visibleAnnotations;

  /** The runtime invisible annotations of this class. May be {@literal null}. */
  public List<AnnotationNode> invisibleAnnotations;

  /** The runtime visible type annotations of this class. May be {@literal null}. */
  public List<TypeAnnotationNode> visibleTypeAnnotations;

  /** The runtime invisible type annotations of this class. May be {@literal null}. */
  public List<TypeAnnotationNode> invisibleTypeAnnotations;

  /** The non standard attributes of this class. May be {@literal null}. */
  public List<Attribute> attrs;

  /** The inner classes of this class. May be {@literal null} if there is not any inner class */
  public List<InnerClassNode> innerClasses;

  /** The internal name of the nest host class of this class. May be {@literal null}. */
  public String nestHostClass;

  /** The internal names of the nest members of this class. May be {@literal null}. */
  public List<String> nestMembers;

  /** The internal names of the permitted subclasses of this class. May be {@literal null}. */
  public List<String> permittedSubclasses;

  /** The record components of this class. May be {@literal null}. */
  public List<RecordComponentNode> recordComponents;

  /** The fields of this class. */
  public ArrayList<FieldNode> fields = new ArrayList<>();

  /** The methods of this class. */
  public ArrayList<MethodNode> methods = new ArrayList<>();

  // -----------------------------------------------------------------------------------------------
  // Implementation of the ClassVisitor abstract class
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visit(
          final int version,
          final int access,
          final String name,
          final String signature,
          final String superName,
          final String[] interfaces) {
    this.version = version;
    this.access = access;
    this.name = name;
    this.signature = signature;
    this.superName = superName;
    this.interfaces = interfaces;
  }

  @Override
  public void visitSource(final String file, final String debug) {
    sourceFile = file;
    sourceDebug = debug;
  }

  @Override
  public ModuleVisitor visitModule(final String name, final int access, final String version) {
    module = new ModuleNode(name, access, version);
    return module;
  }

  @Override
  public void visitNestHost(final String nestHost) {
    this.nestHostClass = nestHost;
  }

  @Override
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    outerClass = owner;
    outerMethod = name;
    outerMethodDesc = descriptor;
  }

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
  public void visitNestMember(final String nestMember) {
    nestMembers = Util.add(nestMembers, nestMember);
  }

  @Override
  public void visitPermittedSubclass(final String permittedSubclass) {
    permittedSubclasses = Util.add(permittedSubclasses, permittedSubclass);
  }

  @Override
  public void visitInnerClass(
          final String name, final String outerName, final String innerName, final int access) {
    InnerClassNode innerClass = new InnerClassNode(name, outerName, innerName, access);
    if (innerClasses == null) {
      innerClasses = new ArrayList<>();
    }
    innerClasses.add(innerClass);
  }

  @Override
  public RecordComponentVisitor visitRecordComponent(
          final String name, final String descriptor, final String signature) {
    RecordComponentNode recordComponent = new RecordComponentNode(name, descriptor, signature);
    recordComponents = Util.add(recordComponents, recordComponent);
    return recordComponent;
  }

  @Override
  public FieldVisitor visitField(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final Object value) {
    FieldNode field = new FieldNode(access, name, descriptor, signature, value);
    fields.add(field);
    return field;
  }

  @Override
  public MethodVisitor visitMethod(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final String[] exceptions) {
    MethodNode method = new MethodNode(access, name, descriptor, signature, exceptions);
    methods.add(method);
    return method;
  }

  @Override
  public void visitEnd() {
    // Nothing to do.
  }

  // -----------------------------------------------------------------------------------------------
  // Accept method
  // -----------------------------------------------------------------------------------------------

  /**
   * Makes the given class visitor visit this class.
   *
   * @param classVisitor a class visitor.
   */
  public void accept(final ClassVisitor classVisitor) {
    // Visit the header.
    classVisitor.visit(version, access, name, signature, superName, interfaces);
    // Visit the source.
    if (sourceFile != null || sourceDebug != null) {
      classVisitor.visitSource(sourceFile, sourceDebug);
    }
    // Visit the module.
    if (module != null) {
      module.accept(classVisitor);
    }
    // Visit the nest host class.
    if (nestHostClass != null) {
      classVisitor.visitNestHost(nestHostClass);
    }
    // Visit the outer class.
    if (outerClass != null) {
      classVisitor.visitOuterClass(outerClass, outerMethod, outerMethodDesc);
    }
    // Visit the annotations.
    if (visibleAnnotations != null) {
      for (AnnotationNode annotation : visibleAnnotations) {
        annotation.accept(classVisitor.visitAnnotation(annotation.desc, true));
      }
    }
    if (invisibleAnnotations != null) {
      for (AnnotationNode annotation : invisibleAnnotations) {
        annotation.accept(classVisitor.visitAnnotation(annotation.desc, false));
      }
    }
    if (visibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : visibleTypeAnnotations) {
        typeAnnotation.accept(
                classVisitor.visitTypeAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, true));
      }
    }
    if (invisibleTypeAnnotations != null) {
      for (TypeAnnotationNode typeAnnotation : invisibleTypeAnnotations) {
        typeAnnotation.accept(
                classVisitor.visitTypeAnnotation(
                        typeAnnotation.typeRef, typeAnnotation.typePath, typeAnnotation.desc, false));
      }
    }
    // Visit the non standard attributes.
    if (attrs != null) {
      for (Attribute attr : attrs) {
        classVisitor.visitAttribute(attr);
      }
    }
    // Visit the nest members.
    if (nestMembers != null) {
      for (String nestMember : nestMembers) {
        classVisitor.visitNestMember(nestMember);
      }
    }
    // Visit the permitted subclasses.
    if (permittedSubclasses != null) {
      for (String permittedSubclass : permittedSubclasses) {
        classVisitor.visitPermittedSubclass(permittedSubclass);
      }
    }
    // Visit the inner classes.
    if (innerClasses != null) {
      for (InnerClassNode innerClass : innerClasses) {
        innerClass.accept(classVisitor);
      }
    }
    // Visit the record components.
    if (recordComponents != null) {
      for (RecordComponentNode recordComponent : recordComponents) {
        recordComponent.accept(classVisitor);
      }
    }
    // Visit the fields.
    for (FieldNode field : fields) {
      field.accept(classVisitor);
    }
    // Visit the methods.
    for (MethodNode method : methods) {
      method.accept(classVisitor);
    }
    classVisitor.visitEnd();
  }
}
