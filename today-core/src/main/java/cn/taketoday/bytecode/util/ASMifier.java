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
package cn.taketoday.bytecode.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ConstantDynamic;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.TypePath;
import cn.taketoday.bytecode.TypeReference;

/**
 * A {@link Printer} that prints the ASM code to generate the classes if visits.
 *
 * @author Eric Bruneton
 */
// DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
public class ASMifier extends Printer {

  /** The help message shown when command line arguments are incorrect. */
  private static final String USAGE =
          "Prints the ASM code to generate the given class.\n"
                  + "Usage: ASMifier [-nodebug] <fully qualified class name or class file name>";

  /** A pseudo access flag used to distinguish class access flags. */
  private static final int ACCESS_CLASS = 0x40000;

  /** A pseudo access flag used to distinguish field access flags. */
  private static final int ACCESS_FIELD = 0x80000;

  /** A pseudo access flag used to distinguish inner class flags. */
  private static final int ACCESS_INNER = 0x100000;

  /** A pseudo access flag used to distinguish module requires / exports flags. */
  private static final int ACCESS_MODULE = 0x200000;

  private static final String ANNOTATION_VISITOR = "annotationVisitor";
  private static final String ANNOTATION_VISITOR0 = "annotationVisitor0 = ";
  private static final String COMMA = "\", \"";
  private static final String END_ARRAY = " });\n";
  private static final String END_PARAMETERS = ");\n\n";
  private static final String NEW_OBJECT_ARRAY = ", new Object[] {";
  private static final String VISIT_END = ".visitEnd();\n";

  private static final List<String> FRAME_TYPES =
          List.of("Opcodes.TOP", "Opcodes.INTEGER",
                  "Opcodes.FLOAT", "Opcodes.DOUBLE",
                  "Opcodes.LONG", "Opcodes.NULL",
                  "Opcodes.UNINITIALIZED_THIS");

  private static final Map<Integer, String> CLASS_VERSIONS;

  static {
    CLASS_VERSIONS = Map.ofEntries(
            Map.entry(Opcodes.V1_1, "V1_1"),
            Map.entry(Opcodes.V1_2, "V1_2"),
            Map.entry(Opcodes.V1_3, "V1_3"),
            Map.entry(Opcodes.V1_4, "V1_4"),
            Map.entry(Opcodes.V1_5, "V1_5"),
            Map.entry(Opcodes.V1_6, "V1_6"),
            Map.entry(Opcodes.V1_7, "V1_7"),
            Map.entry(Opcodes.V1_8, "V1_8"),
            Map.entry(Opcodes.V9, "V9"),
            Map.entry(Opcodes.V10, "V10"),
            Map.entry(Opcodes.V11, "V11"),
            Map.entry(Opcodes.V12, "V12"),
            Map.entry(Opcodes.V13, "V13"),
            Map.entry(Opcodes.V14, "V14"),
            Map.entry(Opcodes.V15, "V15"),
            Map.entry(Opcodes.V16, "V16"),
            Map.entry(Opcodes.V17, "V17"),
            Map.entry(Opcodes.V18, "V18"),
            Map.entry(Opcodes.V19, "V19"),
            Map.entry(Opcodes.V20, "V20"),
            Map.entry(Opcodes.V21, "V21")
    );
  }

  /** The name of the visitor variable in the produced code. */
  protected final String name;

  /** The identifier of the annotation visitor variable in the produced code. */
  protected final int id;

  /** The name of the Label variables in the produced code. */
  protected Map<Label, String> labelNames;

  /**
   * Constructs a new {@link ASMifier}.
   */
  public ASMifier() {
    this("classWriter", 0);
  }

  /**
   * Constructs a new {@link ASMifier}.
   *
   * @param visitorVariableName the name of the visitor variable in the produced code.
   * @param annotationVisitorId identifier of the annotation visitor variable in the produced code.
   */
  protected ASMifier(final String visitorVariableName, final int annotationVisitorId) {
    this.name = visitorVariableName;
    this.id = annotationVisitorId;
  }

  /**
   * Prints the ASM source code to generate the given class to the standard output.
   *
   * <p>Usage: ASMifier [-nodebug] &lt;binary class name or class file name&gt;
   *
   * @param args the command line arguments.
   * @throws IOException if the class cannot be found, or if an IOException occurs.
   */
  public static void main(final String[] args) throws IOException {
    main(args, new PrintWriter(System.out, true), new PrintWriter(System.err, true));
  }

  /**
   * Prints the ASM source code to generate the given class to the given output.
   *
   * <p>Usage: ASMifier [-nodebug] &lt;binary class name or class file name&gt;
   *
   * @param args the command line arguments.
   * @param output where to print the result.
   * @param logger where to log errors.
   * @throws IOException if the class cannot be found, or if an IOException occurs.
   */
  static void main(final String[] args, final PrintWriter output, final PrintWriter logger)
          throws IOException {
    main(args, USAGE, new ASMifier(), output, logger);
  }

  // -----------------------------------------------------------------------------------------------
  // Classes
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visit(
          final int version,
          final int access,
          final String name,
          final String signature,
          final String superName,
          final String[] interfaces) {
    String simpleName;
    if (name == null) {
      simpleName = "module-info";
    }
    else {
      int lastSlashIndex = name.lastIndexOf('/');
      if (lastSlashIndex == -1) {
        simpleName = name;
      }
      else {
        text.add("package asm." + name.substring(0, lastSlashIndex).replace('/', '.') + ";\n");
        simpleName = name.substring(lastSlashIndex + 1).replaceAll("[-\\(\\)]", "_");
      }
    }
    text.add("import cn.taketoday.bytecode.AnnotationVisitor;\n");
    text.add("import cn.taketoday.bytecode.Attribute;\n");
    text.add("import cn.taketoday.bytecode.ClassReader;\n");
    text.add("import cn.taketoday.bytecode.ClassWriter;\n");
    text.add("import cn.taketoday.bytecode.ConstantDynamic;\n");
    text.add("import cn.taketoday.bytecode.FieldVisitor;\n");
    text.add("import cn.taketoday.bytecode.Handle;\n");
    text.add("import cn.taketoday.bytecode.Label;\n");
    text.add("import cn.taketoday.bytecode.MethodVisitor;\n");
    text.add("import cn.taketoday.bytecode.Opcodes;\n");
    text.add("import cn.taketoday.bytecode.RecordComponentVisitor;\n");
    text.add("import cn.taketoday.bytecode.Type;\n");
    text.add("import cn.taketoday.bytecode.TypePath;\n");
    text.add("public class " + simpleName + "Dump implements Opcodes {\n\n");
    text.add("public static byte[] dump () throws Exception {\n\n");
    text.add("ClassWriter classWriter = new ClassWriter(0);\n");
    text.add("FieldVisitor fieldVisitor;\n");
    text.add("RecordComponentVisitor recordComponentVisitor;\n");
    text.add("MethodVisitor methodVisitor;\n");
    text.add("AnnotationVisitor annotationVisitor0;\n\n");

    stringBuilder.setLength(0);
    stringBuilder.append("classWriter.visit(");
    String versionString = CLASS_VERSIONS.get(version);
    if (versionString != null) {
      stringBuilder.append(versionString);
    }
    else {
      stringBuilder.append(version);
    }
    stringBuilder.append(", ");
    appendAccessFlags(access | ACCESS_CLASS);
    stringBuilder.append(", ");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(signature);
    stringBuilder.append(", ");
    appendConstant(superName);
    stringBuilder.append(", ");
    if (interfaces != null && interfaces.length > 0) {
      stringBuilder.append("new String[] {");
      for (int i = 0; i < interfaces.length; ++i) {
        stringBuilder.append(i == 0 ? " " : ", ");
        appendConstant(interfaces[i]);
      }
      stringBuilder.append(" }");
    }
    else {
      stringBuilder.append("null");
    }
    stringBuilder.append(END_PARAMETERS);
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitSource(final String file, final String debug) {
    stringBuilder.setLength(0);
    stringBuilder.append("classWriter.visitSource(");
    appendConstant(file);
    stringBuilder.append(", ");
    appendConstant(debug);
    stringBuilder.append(END_PARAMETERS);
    text.add(stringBuilder.toString());
  }

  @Override
  public Printer visitModule(final String name, final int flags, final String version) {
    stringBuilder.setLength(0);
    stringBuilder.append("ModuleVisitor moduleVisitor = classWriter.visitModule(");
    appendConstant(name);
    stringBuilder.append(", ");
    appendAccessFlags(flags | ACCESS_MODULE);
    stringBuilder.append(", ");
    appendConstant(version);
    stringBuilder.append(END_PARAMETERS);
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier("moduleVisitor", 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public void visitNestHost(final String nestHost) {
    stringBuilder.setLength(0);
    stringBuilder.append("classWriter.visitNestHost(");
    appendConstant(nestHost);
    stringBuilder.append(END_PARAMETERS);
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitOuterClass(final String owner, final String name, final String descriptor) {
    stringBuilder.setLength(0);
    stringBuilder.append("classWriter.visitOuterClass(");
    appendConstant(owner);
    stringBuilder.append(", ");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(END_PARAMETERS);
    text.add(stringBuilder.toString());
  }

  @Override
  public ASMifier visitClassAnnotation(final String descriptor, final boolean visible) {
    return visitAnnotation(descriptor, visible);
  }

  @Override
  public ASMifier visitClassTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitClassAttribute(final Attribute attribute) {
    visitAttribute(attribute);
  }

  @Override
  public void visitNestMember(final String nestMember) {
    stringBuilder.setLength(0);
    stringBuilder.append("classWriter.visitNestMember(");
    appendConstant(nestMember);
    stringBuilder.append(END_PARAMETERS);
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitPermittedSubclass(final String permittedSubclass) {
    stringBuilder.setLength(0);
    stringBuilder.append("classWriter.visitPermittedSubclass(");
    appendConstant(permittedSubclass);
    stringBuilder.append(END_PARAMETERS);
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitInnerClass(
          final String name, final String outerName, final String innerName, final int access) {
    stringBuilder.setLength(0);
    stringBuilder.append("classWriter.visitInnerClass(");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(outerName);
    stringBuilder.append(", ");
    appendConstant(innerName);
    stringBuilder.append(", ");
    appendAccessFlags(access | ACCESS_INNER);
    stringBuilder.append(END_PARAMETERS);
    text.add(stringBuilder.toString());
  }

  @Override
  public ASMifier visitRecordComponent(
          final String name, final String descriptor, final String signature) {
    stringBuilder.setLength(0);
    stringBuilder.append("{\n");
    stringBuilder.append("recordComponentVisitor = classWriter.visitRecordComponent(");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(", ");
    appendConstant(signature);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier("recordComponentVisitor", 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public ASMifier visitField(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final Object value) {
    stringBuilder.setLength(0);
    stringBuilder.append("{\n");
    stringBuilder.append("fieldVisitor = classWriter.visitField(");
    appendAccessFlags(access | ACCESS_FIELD);
    stringBuilder.append(", ");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(", ");
    appendConstant(signature);
    stringBuilder.append(", ");
    appendConstant(value);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier("fieldVisitor", 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public ASMifier visitMethod(
          final int access,
          final String name,
          final String descriptor,
          final String signature,
          final String[] exceptions) {
    stringBuilder.setLength(0);
    stringBuilder.append("{\n");
    stringBuilder.append("methodVisitor = classWriter.visitMethod(");
    appendAccessFlags(access);
    stringBuilder.append(", ");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(", ");
    appendConstant(signature);
    stringBuilder.append(", ");
    if (exceptions != null && exceptions.length > 0) {
      stringBuilder.append("new String[] {");
      for (int i = 0; i < exceptions.length; ++i) {
        stringBuilder.append(i == 0 ? " " : ", ");
        appendConstant(exceptions[i]);
      }
      stringBuilder.append(" }");
    }
    else {
      stringBuilder.append("null");
    }
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier("methodVisitor", 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public void visitClassEnd() {
    text.add("classWriter.visitEnd();\n\n");
    text.add("return classWriter.toByteArray();\n");
    text.add("}\n");
    text.add("}\n");
  }

  // -----------------------------------------------------------------------------------------------
  // Modules
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visitMainClass(final String mainClass) {
    stringBuilder.setLength(0);
    stringBuilder.append("moduleVisitor.visitMainClass(");
    appendConstant(mainClass);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitPackage(final String packaze) {
    stringBuilder.setLength(0);
    stringBuilder.append("moduleVisitor.visitPackage(");
    appendConstant(packaze);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitRequire(final String module, final int access, final String version) {
    stringBuilder.setLength(0);
    stringBuilder.append("moduleVisitor.visitRequire(");
    appendConstant(module);
    stringBuilder.append(", ");
    appendAccessFlags(access | ACCESS_MODULE);
    stringBuilder.append(", ");
    appendConstant(version);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitExport(final String packaze, final int access, final String... modules) {
    visitExportOrOpen("moduleVisitor.visitExport(", packaze, access, modules);
  }

  @Override
  public void visitOpen(final String packaze, final int access, final String... modules) {
    visitExportOrOpen("moduleVisitor.visitOpen(", packaze, access, modules);
  }

  private void visitExportOrOpen(
          final String visitMethod, final String packaze, final int access, final String... modules) {
    stringBuilder.setLength(0);
    stringBuilder.append(visitMethod);
    appendConstant(packaze);
    stringBuilder.append(", ");
    appendAccessFlags(access | ACCESS_MODULE);
    if (modules != null && modules.length > 0) {
      stringBuilder.append(", new String[] {");
      for (int i = 0; i < modules.length; ++i) {
        stringBuilder.append(i == 0 ? " " : ", ");
        appendConstant(modules[i]);
      }
      stringBuilder.append(" }");
    }
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitUse(final String service) {
    stringBuilder.setLength(0);
    stringBuilder.append("moduleVisitor.visitUse(");
    appendConstant(service);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitProvide(final String service, final String... providers) {
    stringBuilder.setLength(0);
    stringBuilder.append("moduleVisitor.visitProvide(");
    appendConstant(service);
    stringBuilder.append(",  new String[] {");
    for (int i = 0; i < providers.length; ++i) {
      stringBuilder.append(i == 0 ? " " : ", ");
      appendConstant(providers[i]);
    }
    stringBuilder.append(END_ARRAY);
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitModuleEnd() {
    text.add("moduleVisitor.visitEnd();\n");
  }

  // -----------------------------------------------------------------------------------------------
  // Annotations
  // -----------------------------------------------------------------------------------------------

  // DontCheck(OverloadMethodsDeclarationOrder): overloads are semantically different.
  @Override
  public void visit(final String name, final Object value) {
    stringBuilder.setLength(0);
    stringBuilder.append(ANNOTATION_VISITOR).append(id).append(".visit(");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(value);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitEnum(final String name, final String descriptor, final String value) {
    stringBuilder.setLength(0);
    stringBuilder.append(ANNOTATION_VISITOR).append(id).append(".visitEnum(");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(", ");
    appendConstant(value);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public ASMifier visitAnnotation(final String name, final String descriptor) {
    stringBuilder.setLength(0);
    stringBuilder
            .append("{\n")
            .append("AnnotationVisitor annotationVisitor")
            .append(id + 1)
            .append(" = annotationVisitor");
    stringBuilder.append(id).append(".visitAnnotation(");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier(ANNOTATION_VISITOR, id + 1);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public ASMifier visitArray(final String name) {
    stringBuilder.setLength(0);
    stringBuilder.append("{\n");
    stringBuilder
            .append("AnnotationVisitor annotationVisitor")
            .append(id + 1)
            .append(" = annotationVisitor");
    stringBuilder.append(id).append(".visitArray(");
    appendConstant(name);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier(ANNOTATION_VISITOR, id + 1);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public void visitAnnotationEnd() {
    stringBuilder.setLength(0);
    stringBuilder.append(ANNOTATION_VISITOR).append(id).append(VISIT_END);
    text.add(stringBuilder.toString());
  }

  // -----------------------------------------------------------------------------------------------
  // Record components
  // -----------------------------------------------------------------------------------------------

  @Override
  public ASMifier visitRecordComponentAnnotation(final String descriptor, final boolean visible) {
    return visitAnnotation(descriptor, visible);
  }

  @Override
  public ASMifier visitRecordComponentTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitRecordComponentAttribute(final Attribute attribute) {
    visitAttribute(attribute);
  }

  @Override
  public void visitRecordComponentEnd() {
    visitMemberEnd();
  }

  // -----------------------------------------------------------------------------------------------
  // Fields
  // -----------------------------------------------------------------------------------------------

  @Override
  public ASMifier visitFieldAnnotation(final String descriptor, final boolean visible) {
    return visitAnnotation(descriptor, visible);
  }

  @Override
  public ASMifier visitFieldTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitFieldAttribute(final Attribute attribute) {
    visitAttribute(attribute);
  }

  @Override
  public void visitFieldEnd() {
    visitMemberEnd();
  }

  // -----------------------------------------------------------------------------------------------
  // Methods
  // -----------------------------------------------------------------------------------------------

  @Override
  public void visitParameter(final String parameterName, final int access) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder.append(name).append(".visitParameter(");
    appendString(stringBuilder, parameterName);
    stringBuilder.append(", ");
    appendAccessFlags(access);
    text.add(stringBuilder.append(");\n").toString());
  }

  @Override
  public ASMifier visitAnnotationDefault() {
    stringBuilder.setLength(0);
    stringBuilder
            .append("{\n")
            .append(ANNOTATION_VISITOR0)
            .append(name)
            .append(".visitAnnotationDefault();\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier(ANNOTATION_VISITOR, 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public ASMifier visitMethodAnnotation(final String descriptor, final boolean visible) {
    return visitAnnotation(descriptor, visible);
  }

  @Override
  public ASMifier visitMethodTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation(typeRef, typePath, descriptor, visible);
  }

  @Override
  public ASMifier visitAnnotableParameterCount(final int parameterCount, final boolean visible) {

    stringBuilder.setLength(0);
    stringBuilder
            .append(name)
            .append(".visitAnnotableParameterCount(")
            .append(parameterCount)
            .append(", ")
            .append(visible)
            .append(");\n");
    text.add(stringBuilder.toString());
    return this;
  }

  @Override
  public ASMifier visitParameterAnnotation(
          final int parameter, final String descriptor, final boolean visible) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder
            .append("{\n")
            .append(ANNOTATION_VISITOR0)
            .append(name)
            .append(".visitParameterAnnotation(")
            .append(parameter)
            .append(", ");
    appendConstant(descriptor);
    stringBuilder.append(", ").append(visible).append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier(ANNOTATION_VISITOR, 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public void visitMethodAttribute(final Attribute attribute) {
    visitAttribute(attribute);
  }

  @Override
  public void visitCode() {
    text.add(name + ".visitCode();\n");
  }

  @Override
  public void visitFrame(
          final int type,
          final int numLocal,
          final Object[] local,
          final int numStack,
          final Object[] stack) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    switch (type) {
      case Opcodes.F_NEW, Opcodes.F_FULL -> {
        declareFrameTypes(numLocal, local);
        declareFrameTypes(numStack, stack);
        if (type == Opcodes.F_NEW) {
          stringBuilder.append(name).append(".visitFrame(Opcodes.F_NEW, ");
        }
        else {
          stringBuilder.append(name).append(".visitFrame(Opcodes.F_FULL, ");
        }
        stringBuilder.append(numLocal).append(NEW_OBJECT_ARRAY);
        appendFrameTypes(numLocal, local);
        stringBuilder.append("}, ").append(numStack).append(NEW_OBJECT_ARRAY);
        appendFrameTypes(numStack, stack);
        stringBuilder.append('}');
      }
      case Opcodes.F_APPEND -> {
        declareFrameTypes(numLocal, local);
        stringBuilder
                .append(name)
                .append(".visitFrame(Opcodes.F_APPEND,")
                .append(numLocal)
                .append(NEW_OBJECT_ARRAY);
        appendFrameTypes(numLocal, local);
        stringBuilder.append("}, 0, null");
      }
      case Opcodes.F_CHOP -> stringBuilder
              .append(name)
              .append(".visitFrame(Opcodes.F_CHOP,")
              .append(numLocal)
              .append(", null, 0, null");
      case Opcodes.F_SAME -> stringBuilder.append(name).append(".visitFrame(Opcodes.F_SAME, 0, null, 0, null");
      case Opcodes.F_SAME1 -> {
        declareFrameTypes(1, stack);
        stringBuilder
                .append(name)
                .append(".visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {");
        appendFrameTypes(1, stack);
        stringBuilder.append('}');
      }
      default -> throw new IllegalArgumentException();
    }
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitInsn(final int opcode) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder.append(name).append(".visitInsn(").append(OPCODES[opcode]).append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder
            .append(name)
            .append(".visitIntInsn(")
            .append(OPCODES[opcode])
            .append(", ")
            .append(opcode == Opcodes.NEWARRAY ? TYPES[operand] : Integer.toString(operand))
            .append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder
            .append(name)
            .append(".visitVarInsn(")
            .append(OPCODES[opcode])
            .append(", ")
            .append(var)
            .append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder.append(name).append(".visitTypeInsn(").append(OPCODES[opcode]).append(", ");
    appendConstant(type);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitFieldInsn(
          final int opcode, final String owner, final String name, final String descriptor) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder.append(this.name).append(".visitFieldInsn(").append(OPCODES[opcode]).append(", ");
    appendConstant(owner);
    stringBuilder.append(", ");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitMethodInsn(
          final int opcode,
          final String owner,
          final String name,
          final String descriptor,
          final boolean isInterface) {
    stringBuilder.setLength(0);
    stringBuilder
            .append(this.name)
            .append(".visitMethodInsn(")
            .append(OPCODES[opcode])
            .append(", ");
    appendConstant(owner);
    stringBuilder.append(", ");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(", ");
    stringBuilder.append(isInterface ? "true" : "false");
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitInvokeDynamicInsn(
          final String name,
          final String descriptor,
          final Handle bootstrapMethodHandle,
          final Object... bootstrapMethodArguments) {
    stringBuilder.setLength(0);
    stringBuilder.append(this.name).append(".visitInvokeDynamicInsn(");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(", ");
    appendConstant(bootstrapMethodHandle);
    stringBuilder.append(", new Object[]{");
    for (int i = 0; i < bootstrapMethodArguments.length; ++i) {
      appendConstant(bootstrapMethodArguments[i]);
      if (i != bootstrapMethodArguments.length - 1) {
        stringBuilder.append(", ");
      }
    }
    stringBuilder.append("});\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    stringBuilder.setLength(0);
    declareLabel(label);
    stringBuilder.append(name).append(".visitJumpInsn(").append(OPCODES[opcode]).append(", ");
    appendLabel(label);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitLabel(final Label label) {
    stringBuilder.setLength(0);
    declareLabel(label);
    stringBuilder.append(name).append(".visitLabel(");
    appendLabel(label);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitLdcInsn(final Object value) {
    stringBuilder.setLength(0);
    stringBuilder.append(name).append(".visitLdcInsn(");
    appendConstant(value);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder
            .append(name)
            .append(".visitIincInsn(")
            .append(var)
            .append(", ")
            .append(increment)
            .append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitTableSwitchInsn(
          final int min, final int max, final Label dflt, final Label... labels) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    for (Label label : labels) {
      declareLabel(label);
    }
    declareLabel(dflt);

    stringBuilder
            .append(name)
            .append(".visitTableSwitchInsn(")
            .append(min)
            .append(", ")
            .append(max)
            .append(", ");
    appendLabel(dflt);
    stringBuilder.append(", new Label[] {");
    for (int i = 0; i < labels.length; ++i) {
      stringBuilder.append(i == 0 ? " " : ", ");
      appendLabel(labels[i]);
    }
    stringBuilder.append(END_ARRAY);
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    for (Label label : labels) {
      declareLabel(label);
    }
    declareLabel(dflt);

    stringBuilder.append(name).append(".visitLookupSwitchInsn(");
    appendLabel(dflt);
    stringBuilder.append(", new int[] {");
    for (int i = 0; i < keys.length; ++i) {
      stringBuilder.append(i == 0 ? " " : ", ").append(keys[i]);
    }
    stringBuilder.append(" }, new Label[] {");
    for (int i = 0; i < labels.length; ++i) {
      stringBuilder.append(i == 0 ? " " : ", ");
      appendLabel(labels[i]);
    }
    stringBuilder.append(END_ARRAY);
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder.append(name).append(".visitMultiANewArrayInsn(");
    appendConstant(descriptor);
    stringBuilder.append(", ").append(numDimensions).append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public ASMifier visitInsnAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation("visitInsnAnnotation", typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitTryCatchBlock(
          final Label start, final Label end, final Label handler, final String type) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    declareLabel(start);
    declareLabel(end);
    declareLabel(handler);
    stringBuilder.append(name).append(".visitTryCatchBlock(");
    appendLabel(start);
    stringBuilder.append(", ");
    appendLabel(end);
    stringBuilder.append(", ");
    appendLabel(handler);
    stringBuilder.append(", ");
    appendConstant(type);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public ASMifier visitTryCatchAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation("visitTryCatchAnnotation", typeRef, typePath, descriptor, visible);
  }

  @Override
  public void visitLocalVariable(
          final String name,
          final String descriptor,
          final String signature,
          final Label start,
          final Label end,
          final int index) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder.append(this.name).append(".visitLocalVariable(");
    appendConstant(name);
    stringBuilder.append(", ");
    appendConstant(descriptor);
    stringBuilder.append(", ");
    appendConstant(signature);
    stringBuilder.append(", ");
    appendLabel(start);
    stringBuilder.append(", ");
    appendLabel(end);
    stringBuilder.append(", ").append(index).append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public Printer visitLocalVariableAnnotation(
          final int typeRef,
          final TypePath typePath,
          final Label[] start,
          final Label[] end,
          final int[] index,
          final String descriptor,
          final boolean visible) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder
            .append("{\n")
            .append(ANNOTATION_VISITOR0)
            .append(name)
            .append(".visitLocalVariableAnnotation(")
            .append(typeRef);
    if (typePath == null) {
      stringBuilder.append(", null, ");
    }
    else {
      stringBuilder.append(", TypePath.fromString(\"").append(typePath).append("\"), ");
    }
    stringBuilder.append("new Label[] {");
    for (int i = 0; i < start.length; ++i) {
      stringBuilder.append(i == 0 ? " " : ", ");
      appendLabel(start[i]);
    }
    stringBuilder.append(" }, new Label[] {");
    for (int i = 0; i < end.length; ++i) {
      stringBuilder.append(i == 0 ? " " : ", ");
      appendLabel(end[i]);
    }
    stringBuilder.append(" }, new int[] {");
    for (int i = 0; i < index.length; ++i) {
      stringBuilder.append(i == 0 ? " " : ", ").append(index[i]);
    }
    stringBuilder.append(" }, ");
    appendConstant(descriptor);
    stringBuilder.append(", ").append(visible).append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier(ANNOTATION_VISITOR, 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  @Override
  public void visitLineNumber(final int line, final Label start) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder.append(name).append(".visitLineNumber(").append(line).append(", ");
    appendLabel(start);
    stringBuilder.append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitMaxs(final int maxStack, final int maxLocals) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder
            .append(name)
            .append(".visitMaxs(")
            .append(maxStack)
            .append(", ")
            .append(maxLocals)
            .append(");\n");
    text.add(stringBuilder.toString());
  }

  @Override
  public void visitMethodEnd() {
    visitMemberEnd();
  }

  // -----------------------------------------------------------------------------------------------
  // Common methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Visits a class, field or method annotation.
   *
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a new {@link ASMifier} to visit the annotation values.
   */
  // DontCheck(OverloadMethodsDeclarationOrder): overloads are semantically different.
  public ASMifier visitAnnotation(final String descriptor, final boolean visible) {
    stringBuilder.setLength(0);
    stringBuilder
            .append("{\n")
            .append(ANNOTATION_VISITOR0)
            .append(name)
            .append(".visitAnnotation(");
    appendConstant(descriptor);
    stringBuilder.append(", ").append(visible).append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier(ANNOTATION_VISITOR, 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  /**
   * Visits a class, field or method type annotation.
   *
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   * {@link TypeReference#FIELD}. See {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   * static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   * 'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a new {@link ASMifier} to visit the annotation values.
   */
  public ASMifier visitTypeAnnotation(
          final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
    return visitTypeAnnotation("visitTypeAnnotation", typeRef, typePath, descriptor, visible);
  }

  /**
   * Visits a class, field, method, instruction or try catch block type annotation.
   *
   * @param method the name of the visit method for this type of annotation.
   * @param typeRef a reference to the annotated type. The sort of this type reference must be
   * {@link TypeReference#FIELD}. See {@link TypeReference}.
   * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
   * static inner type within 'typeRef'. May be {@literal null} if the annotation targets
   * 'typeRef' as a whole.
   * @param descriptor the class descriptor of the annotation class.
   * @param visible {@literal true} if the annotation is visible at runtime.
   * @return a new {@link ASMifier} to visit the annotation values.
   */
  public ASMifier visitTypeAnnotation(
          final String method,
          final int typeRef,
          final TypePath typePath,
          final String descriptor,
          final boolean visible) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder
            .append("{\n")
            .append(ANNOTATION_VISITOR0)
            .append(name)
            .append(".")
            .append(method)
            .append("(")
            .append(typeRef);
    if (typePath == null) {
      stringBuilder.append(", null, ");
    }
    else {
      stringBuilder.append(", TypePath.fromString(\"").append(typePath).append("\"), ");
    }
    appendConstant(descriptor);
    stringBuilder.append(", ").append(visible).append(");\n");
    text.add(stringBuilder.toString());
    ASMifier asmifier = createASMifier(ANNOTATION_VISITOR, 0);
    text.add(asmifier.getText());
    text.add("}\n");
    return asmifier;
  }

  /**
   * Visit a class, field or method attribute.
   *
   * @param attribute an attribute.
   */
  public void visitAttribute(final Attribute attribute) {
    StringBuilder stringBuilder = this.stringBuilder;

    stringBuilder.setLength(0);
    stringBuilder.append("// ATTRIBUTE ").append(attribute.type).append('\n');
    if (attribute instanceof ASMifierSupport) {
      if (labelNames == null) {
        labelNames = new HashMap<>();
      }
      stringBuilder.append("{\n");
      ((ASMifierSupport) attribute).asmify(stringBuilder, "attribute", labelNames);
      stringBuilder.append(name).append(".visitAttribute(attribute);\n");
      stringBuilder.append("}\n");
    }
    text.add(stringBuilder.toString());
  }

  /** Visits the end of a field, record component or method. */
  private void visitMemberEnd() {
    stringBuilder.setLength(0);
    stringBuilder.append(name).append(VISIT_END);
    text.add(stringBuilder.toString());
  }

  // -----------------------------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Constructs a new {@link ASMifier}.
   *
   * @param visitorVariableName the name of the visitor variable in the produced code.
   * @param annotationVisitorId identifier of the annotation visitor variable in the produced code.
   * @return a new {@link ASMifier}.
   */
  // DontCheck(AbbreviationAsWordInName): can't be renamed (for backward binary compatibility).
  protected ASMifier createASMifier(
          final String visitorVariableName, final int annotationVisitorId) {
    return new ASMifier(visitorVariableName, annotationVisitorId);
  }

  /**
   * Appends a string representation of the given access flags to {@link #stringBuilder}.
   *
   * @param accessFlags some access flags.
   */
  private void appendAccessFlags(final int accessFlags) {
    StringBuilder stringBuilder = this.stringBuilder;
    boolean isEmpty = true;
    if ((accessFlags & Opcodes.ACC_PUBLIC) != 0) {
      stringBuilder.append("ACC_PUBLIC");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_PRIVATE) != 0) {
      stringBuilder.append("ACC_PRIVATE");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_PROTECTED) != 0) {
      stringBuilder.append("ACC_PROTECTED");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_FINAL) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      if ((accessFlags & ACCESS_MODULE) == 0) {
        stringBuilder.append("ACC_FINAL");
      }
      else {
        stringBuilder.append("ACC_TRANSITIVE");
      }
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_STATIC) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_STATIC");
      isEmpty = false;
    }
    if ((accessFlags & (Opcodes.ACC_SYNCHRONIZED | Opcodes.ACC_SUPER | Opcodes.ACC_TRANSITIVE))
            != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      if ((accessFlags & ACCESS_CLASS) == 0) {
        if ((accessFlags & ACCESS_MODULE) == 0) {
          stringBuilder.append("ACC_SYNCHRONIZED");
        }
        else {
          stringBuilder.append("ACC_TRANSITIVE");
        }
      }
      else {
        stringBuilder.append("ACC_SUPER");
      }
      isEmpty = false;
    }
    if ((accessFlags & (Opcodes.ACC_VOLATILE | Opcodes.ACC_BRIDGE | Opcodes.ACC_STATIC_PHASE))
            != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      if ((accessFlags & ACCESS_FIELD) == 0) {
        if ((accessFlags & ACCESS_MODULE) == 0) {
          stringBuilder.append("ACC_BRIDGE");
        }
        else {
          stringBuilder.append("ACC_STATIC_PHASE");
        }
      }
      else {
        stringBuilder.append("ACC_VOLATILE");
      }
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_VARARGS) != 0
            && (accessFlags & (ACCESS_CLASS | ACCESS_FIELD)) == 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_VARARGS");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_TRANSIENT) != 0 && (accessFlags & ACCESS_FIELD) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_TRANSIENT");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_NATIVE) != 0
            && (accessFlags & (ACCESS_CLASS | ACCESS_FIELD)) == 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_NATIVE");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_ENUM) != 0
            && (accessFlags & (ACCESS_CLASS | ACCESS_FIELD | ACCESS_INNER)) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_ENUM");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_ANNOTATION) != 0
            && (accessFlags & (ACCESS_CLASS | ACCESS_INNER)) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_ANNOTATION");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_ABSTRACT) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_ABSTRACT");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_INTERFACE) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_INTERFACE");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_STRICT) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_STRICT");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_SYNTHETIC");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_DEPRECATED");
      isEmpty = false;
    }
    if ((accessFlags & Opcodes.ACC_RECORD) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      stringBuilder.append("ACC_RECORD");
      isEmpty = false;
    }
    if ((accessFlags & (Opcodes.ACC_MANDATED | Opcodes.ACC_MODULE)) != 0) {
      if (!isEmpty) {
        stringBuilder.append(" | ");
      }
      if ((accessFlags & ACCESS_CLASS) == 0) {
        stringBuilder.append("ACC_MANDATED");
      }
      else {
        stringBuilder.append("ACC_MODULE");
      }
      isEmpty = false;
    }
    if (isEmpty) {
      stringBuilder.append('0');
    }
  }

  /**
   * Appends a string representation of the given constant to {@link #stringBuilder}.
   *
   * @param value a {@link String}, {@link Type}, {@link Handle}, {@link Byte}, {@link Short},
   * {@link Character}, {@link Integer}, {@link Float}, {@link Long} or {@link Double} object,
   * or an array of primitive values. May be {@literal null}.
   */
  protected void appendConstant(final Object value) {
    StringBuilder stringBuilder = this.stringBuilder;
    if (value == null) {
      stringBuilder.append("null");
    }
    else if (value instanceof String) {
      appendString(stringBuilder, (String) value);
    }
    else if (value instanceof Type) {
      stringBuilder.append("Type.forDescriptor(\"");
      stringBuilder.append(((Type) value).getDescriptor());
      stringBuilder.append("\")");
    }
    else if (value instanceof Handle handle) {
      stringBuilder.append("new Handle(");
      stringBuilder.append("Opcodes.").append(HANDLE_TAG[handle.getTag()]).append(", \"");
      stringBuilder.append(handle.getOwner()).append(COMMA);
      stringBuilder.append(handle.getName()).append(COMMA);
      stringBuilder.append(handle.getDesc()).append("\", ");
      stringBuilder.append(handle.isInterface()).append(")");
    }
    else if (value instanceof ConstantDynamic constantDynamic) {
      stringBuilder.append("new ConstantDynamic(\"");
      stringBuilder.append(constantDynamic.getName()).append(COMMA);
      stringBuilder.append(constantDynamic.getDescriptor()).append("\", ");
      appendConstant(constantDynamic.getBootstrapMethod());
      stringBuilder.append(NEW_OBJECT_ARRAY);
      int bootstrapMethodArgumentCount = constantDynamic.getBootstrapMethodArgumentCount();
      for (int i = 0; i < bootstrapMethodArgumentCount; ++i) {
        appendConstant(constantDynamic.getBootstrapMethodArgument(i));
        if (i != bootstrapMethodArgumentCount - 1) {
          stringBuilder.append(", ");
        }
      }
      stringBuilder.append("})");
    }
    else if (value instanceof Byte) {
      stringBuilder.append("new Byte((byte)").append(value).append(')');
    }
    else if (value instanceof Boolean) {
      stringBuilder.append(((Boolean) value).booleanValue() ? "Boolean.TRUE" : "Boolean.FALSE");
    }
    else if (value instanceof Short) {
      stringBuilder.append("new Short((short)").append(value).append(')');
    }
    else if (value instanceof Character) {
      stringBuilder
              .append("new Character((char)")
              .append((int) (Character) value)
              .append(')');
    }
    else if (value instanceof Integer) {
      stringBuilder.append("new Integer(").append(value).append(')');
    }
    else if (value instanceof Float) {
      stringBuilder.append("new Float(\"").append(value).append("\")");
    }
    else if (value instanceof Long) {
      stringBuilder.append("new Long(").append(value).append("L)");
    }
    else if (value instanceof Double) {
      stringBuilder.append("new Double(\"").append(value).append("\")");
    }
    else if (value instanceof byte[] byteArray) {
      stringBuilder.append("new byte[] {");
      for (int i = 0; i < byteArray.length; i++) {
        stringBuilder.append(i == 0 ? "" : ",").append(byteArray[i]);
      }
      stringBuilder.append('}');
    }
    else if (value instanceof boolean[] booleanArray) {
      stringBuilder.append("new boolean[] {");
      for (int i = 0; i < booleanArray.length; i++) {
        stringBuilder.append(i == 0 ? "" : ",").append(booleanArray[i]);
      }
      stringBuilder.append('}');
    }
    else if (value instanceof short[] shortArray) {
      stringBuilder.append("new short[] {");
      for (int i = 0; i < shortArray.length; i++) {
        stringBuilder.append(i == 0 ? "" : ",").append("(short)").append(shortArray[i]);
      }
      stringBuilder.append('}');
    }
    else if (value instanceof char[] charArray) {
      stringBuilder.append("new char[] {");
      for (int i = 0; i < charArray.length; i++) {
        stringBuilder.append(i == 0 ? "" : ",").append("(char)").append((int) charArray[i]);
      }
      stringBuilder.append('}');
    }
    else if (value instanceof int[] intArray) {
      stringBuilder.append("new int[] {");
      for (int i = 0; i < intArray.length; i++) {
        stringBuilder.append(i == 0 ? "" : ",").append(intArray[i]);
      }
      stringBuilder.append('}');
    }
    else if (value instanceof long[] longArray) {
      stringBuilder.append("new long[] {");
      for (int i = 0; i < longArray.length; i++) {
        stringBuilder.append(i == 0 ? "" : ",").append(longArray[i]).append('L');
      }
      stringBuilder.append('}');
    }
    else if (value instanceof float[] floatArray) {
      stringBuilder.append("new float[] {");
      for (int i = 0; i < floatArray.length; i++) {
        stringBuilder.append(i == 0 ? "" : ",").append(floatArray[i]).append('f');
      }
      stringBuilder.append('}');
    }
    else if (value instanceof double[] doubleArray) {
      stringBuilder.append("new double[] {");
      for (int i = 0; i < doubleArray.length; i++) {
        stringBuilder.append(i == 0 ? "" : ",").append(doubleArray[i]).append('d');
      }
      stringBuilder.append('}');
    }
  }

  /**
   * Calls {@link #declareLabel} for each label in the given stack map frame types.
   *
   * @param numTypes the number of stack map frame types in 'frameTypes'.
   * @param frameTypes an array of stack map frame types, in the format described in {@link
   * MethodVisitor#visitFrame}.
   */
  private void declareFrameTypes(final int numTypes, final Object[] frameTypes) {
    for (int i = 0; i < numTypes; ++i) {
      if (frameTypes[i] instanceof Label) {
        declareLabel((Label) frameTypes[i]);
      }
    }
  }

  /**
   * Appends the given stack map frame types to {@link #stringBuilder}.
   *
   * @param numTypes the number of stack map frame types in 'frameTypes'.
   * @param frameTypes an array of stack map frame types, in the format described in {@link
   * MethodVisitor#visitFrame}.
   */
  private void appendFrameTypes(final int numTypes, final Object[] frameTypes) {
    for (int i = 0; i < numTypes; ++i) {
      if (i > 0) {
        stringBuilder.append(", ");
      }
      if (frameTypes[i] instanceof String) {
        appendConstant(frameTypes[i]);
      }
      else if (frameTypes[i] instanceof Integer) {
        stringBuilder.append(FRAME_TYPES.get((Integer) frameTypes[i]));
      }
      else {
        appendLabel((Label) frameTypes[i]);
      }
    }
  }

  /**
   * Appends a declaration of the given label to {@link #stringBuilder}. This declaration is of the
   * form "Label labelXXX = new Label();". Does nothing if the given label has already been
   * declared.
   *
   * @param label a label.
   */
  protected void declareLabel(final Label label) {
    if (labelNames == null) {
      labelNames = new HashMap<>();
    }
    String labelName = labelNames.get(label);
    if (labelName == null) {
      labelName = "label" + labelNames.size();
      labelNames.put(label, labelName);
      stringBuilder.append("Label ").append(labelName).append(" = new Label();\n");
    }
  }

  /**
   * Appends the name of the given label to {@link #stringBuilder}. The given label <i>must</i>
   * already have a name. One way to ensure this is to always call {@link #declareLabel} before
   * calling this method.
   *
   * @param label a label.
   */
  protected void appendLabel(final Label label) {
    stringBuilder.append(labelNames.get(label));
  }
}
