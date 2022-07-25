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

import java.util.List;

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.FieldVisitor;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.ModuleVisitor;
import cn.taketoday.bytecode.RecordComponentVisitor;
import cn.taketoday.bytecode.TypePath;
import cn.taketoday.lang.Nullable;

/**
 * A {@link ClassVisitor} that remaps types with a {@link Remapper}.
 *
 * <p><i>This visitor has several limitations</i>. A non-exhaustive list is the following:
 *
 * <ul>
 *   <li>it cannot remap type names in dynamically computed strings (remapping of type names in
 *       static values is supported).
 *   <li>it cannot remap values derived from type names at compile time, such as
 *       <ul>
 *         <li>type name hashcodes used by some Java compilers to implement the string switch
 *             statement.
 *         <li>some compound strings used by some Java compilers to implement lambda
 *             deserialization.
 *       </ul>
 * </ul>
 *
 * @author Eugene Kuleshov
 */
public class ClassRemapper extends ClassVisitor {

  /** The remapper used to remap the types in the visited class. */
  protected final Remapper remapper;

  /** The internal name of the visited class. */
  protected String className;

  /**
   * Constructs a new {@link ClassRemapper}.
   *
   * @param classVisitor the class visitor this remapper must delegate to.
   * @param remapper the remapper to use to remap the types in the visited class.
   */
  protected ClassRemapper(final ClassVisitor classVisitor, final Remapper remapper) {
    super(classVisitor);
    this.remapper = remapper;
  }

  @Override
  public void visit(
          final int version,
          final int access,
          final String name,
          final String signature,
          final String superName,
          final String[] interfaces) {
    this.className = name;
    Remapper remapper = this.remapper;
    super.visit(
            version,
            access,
            remapper.mapType(name),
            remapper.mapSignature(signature, false),
            remapper.mapType(superName),
            interfaces == null ? null : remapper.mapTypes(interfaces));
  }

  @Override
  public ModuleVisitor visitModule(final String name, final int flags, final String version) {
    ModuleVisitor moduleVisitor = super.visitModule(remapper.mapModuleName(name), flags, version);
    return moduleVisitor == null ? null : createModuleRemapper(moduleVisitor);
  }

  @Override
  public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitAnnotation(remapper.mapDesc(descriptor), visible);
    return annotationVisitor == null
           ? null
           : createAnnotationRemapper(descriptor, annotationVisitor);
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    AnnotationVisitor annotationVisitor =
            super.visitTypeAnnotation(typeRef, typePath, remapper.mapDesc(descriptor), visible);
    return annotationVisitor == null
           ? null
           : createAnnotationRemapper(descriptor, annotationVisitor);
  }

  @Override
  public void visitAttribute(final Attribute attribute) {
    if (attribute instanceof ModuleHashesAttribute moduleHashesAttribute) {
      List<String> modules = moduleHashesAttribute.modules;
      int size = modules.size();
      for (int i = 0; i < size; ++i) {
        modules.set(i, remapper.mapModuleName(modules.get(i)));
      }
    }
    super.visitAttribute(attribute);
  }

  @Override
  public RecordComponentVisitor visitRecordComponent(
          final String name, final String descriptor, final String signature) {
    Remapper remapper = this.remapper;

    RecordComponentVisitor recordComponentVisitor =
            super.visitRecordComponent(
                    remapper.mapRecordComponentName(className, name, descriptor),
                    remapper.mapDesc(descriptor),
                    remapper.mapSignature(signature, true));
    return createRecordComponentRemapper(recordComponentVisitor);
  }

  @Override
  public FieldVisitor visitField(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final Object value) {
    Remapper remapper = this.remapper;
    FieldVisitor fieldVisitor =
            super.visitField(
                    access,
                    remapper.mapFieldName(className, name, descriptor),
                    remapper.mapDesc(descriptor),
                    remapper.mapSignature(signature, true),
                    (value == null) ? null : remapper.mapValue(value));
    return fieldVisitor == null ? null : createFieldRemapper(fieldVisitor);
  }

  @Override
  public MethodVisitor visitMethod(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final String[] exceptions) {
    Remapper remapper = this.remapper;

    String remappedDescriptor = remapper.mapMethodDesc(descriptor);
    MethodVisitor methodVisitor =
            super.visitMethod(
                    access,
                    remapper.mapMethodName(className, name, descriptor),
                    remappedDescriptor,
                    remapper.mapSignature(signature, false),
                    exceptions == null ? null : remapper.mapTypes(exceptions));
    return methodVisitor == null ? null : createMethodRemapper(methodVisitor);
  }

  @Override
  public void visitInnerClass(
          final String name, final String outerName, final String innerName, final int access) {
    Remapper remapper = this.remapper;

    super.visitInnerClass(
            remapper.mapType(name),
            outerName == null ? null : remapper.mapType(outerName),
            innerName == null ? null : remapper.mapInnerClassName(name, outerName, innerName),
            access);
  }

  @Override
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    Remapper remapper = this.remapper;

    super.visitOuterClass(
            remapper.mapType(owner),
            name == null ? null : remapper.mapMethodName(owner, name, descriptor),
            descriptor == null ? null : remapper.mapMethodDesc(descriptor));
  }

  @Override
  public void visitNestHost(final String nestHost) {
    super.visitNestHost(remapper.mapType(nestHost));
  }

  @Override
  public void visitNestMember(final String nestMember) {
    super.visitNestMember(remapper.mapType(nestMember));
  }

  @Override
  public void visitPermittedSubclass(final String permittedSubclass) {
    super.visitPermittedSubclass(remapper.mapType(permittedSubclass));
  }

  /**
   * Constructs a new remapper for fields. The default implementation of this method returns a new
   * {@link FieldRemapper}.
   *
   * @param fieldVisitor the FieldVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected FieldVisitor createFieldRemapper(final FieldVisitor fieldVisitor) {
    return new FieldRemapper(fieldVisitor, remapper);
  }

  /**
   * Constructs a new remapper for methods. The default implementation of this method returns a new
   * {@link MethodRemapper}.
   *
   * @param methodVisitor the MethodVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected MethodVisitor createMethodRemapper(final MethodVisitor methodVisitor) {
    return new MethodRemapper(methodVisitor, remapper);
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
   * Constructs a new remapper for modules. The default implementation of this method returns a new
   * {@link ModuleRemapper}.
   *
   * @param moduleVisitor the ModuleVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected ModuleVisitor createModuleRemapper(final ModuleVisitor moduleVisitor) {
    return new ModuleRemapper(moduleVisitor, remapper);
  }

  /**
   * Constructs a new remapper for record components. The default implementation of this method
   * returns a new {@link RecordComponentRemapper}.
   *
   * @param recordComponentVisitor the RecordComponentVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  @Nullable
  protected RecordComponentVisitor createRecordComponentRemapper(
          @Nullable final RecordComponentVisitor recordComponentVisitor) {
    if (recordComponentVisitor == null) {
      return null;
    }
    return new RecordComponentRemapper(recordComponentVisitor, remapper);
  }
}
