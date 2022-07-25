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
package cn.taketoday.bytecode;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * A Java class file, whose content can be returned as a verbose, human "readable" string. As an
 * example, the string representation of the HelloWorld class, obtained with the {@link #toString()}
 * method, is:
 *
 * <pre>
 * magic: -889275714
 * minor_version: 0
 * major_version: 49
 * access_flags: 33
 * this_class: ConstantClassInfo HelloWorld
 * super_class: ConstantClassInfo java/lang/Object
 * interfaces_count: 0
 * fields_count: 0
 * methods_count: 2
 * access_flags: 1
 * name_index: &lt;init&gt;
 * descriptor_index: ()V
 * attributes_count: 1
 * attribute_name_index: Code
 * max_stack: 1
 * max_locals: 1
 * 0: 25 0
 * 1: 183 ConstantMethodRefInfo java/lang/Object.&lt;init&gt;()V
 * 2: 177
 * exception_table_length: 0
 * attributes_count: 2
 * attribute_name_index: LineNumberTable
 * line_number_table_length: 1
 * start_pc: &lt;0&gt;
 * line_number: 31
 * attribute_name_index: LocalVariableTable
 * local_variable_table_length: 1
 * start_pc: &lt;0&gt;
 * length: &lt;3&gt;
 * name_index: this
 * descriptor_index: LHelloWorld;
 * index: 0
 * access_flags: 9
 * name_index: main
 * descriptor_index: ([Ljava/lang/String;)V
 * attributes_count: 1
 * attribute_name_index: Code
 * max_stack: 2
 * max_locals: 1
 * 0: 178 ConstantFieldRefInfo java/lang/System.outLjava/io/PrintStream;
 * 1: 18 ConstantStringInfo Hello, world!
 * 2: 182 ConstantMethodRefInfo java/io/PrintStream.println(Ljava/lang/String;)V
 * 3: 177
 * exception_table_length: 0
 * attributes_count: 2
 * attribute_name_index: LineNumberTable
 * line_number_table_length: 2
 * start_pc: &lt;0&gt;
 * line_number: 33
 * start_pc: &lt;3&gt;
 * line_number: 34
 * attribute_name_index: LocalVariableTable
 * local_variable_table_length: 1
 * start_pc: &lt;0&gt;
 * length: &lt;4&gt;
 * name_index: args
 * descriptor_index: [Ljava/lang/String;
 * index: 0
 * attributes_count: 1
 * attribute_name_index: SourceFile
 * sourcefile_index: HelloWorld.java
 * </pre>
 *
 * <p>This class is used to compare classes in unit tests. Its source code is as close as possible
 * to the Java Virtual Machine specification for ease of reference. The constant pool and bytecode
 * offsets are abstracted away so that two classes which differ only by their constant pool or low
 * level byte code instruction representation (e.g. a ldc vs. a ldc_w) are still considered equal.
 * Likewise, attributes (resp. type annotations) are re-ordered into alphabetical order, so that two
 * classes which differ only via the ordering of their attributes (resp. type annotations) are still
 * considered equal.
 *
 * @author Eric Bruneton
 */
public class ClassFile {

  /** The name of JDK9 module classes. */
  static final String MODULE_INFO = "module-info";

  /** The binary content of a Java class file. */
  private final byte[] classBytes;

  /** The name of the class contained in this class file, lazily computed. */
  private String className;

  /** The dump of the constant pool of {@link #classBytes}, lazily computed. */
  private String constantPoolDump;

  /** The dump of {@link #classBytes}, lazily computed. */
  private String dump;

  /**
   * Constructs a new ClassFile instance.
   *
   * @param classBytes the binary content of a Java class file.
   */
  public ClassFile(final byte[] classBytes) {
    this.classBytes = classBytes;
  }

  /**
   * Returns a string representation of the constant pool of the class contained in this class file.
   *
   * @return a string representation of the constant pool of the class contained in this class file.
   */
  public String getConstantPoolDump() {
    if (constantPoolDump == null) {
      computeNameAndDumps();
    }
    return constantPoolDump;
  }

  /**
   * Returns a new instance of the class contained in this class file. The class is loaded in a new
   * class loader.
   *
   * @return a new instance of the class, or {@literal null} if the class is abstract, is an enum,
   * or a module info.
   * @throws ReflectiveOperationException if the class is invalid or if an error occurs in its
   * constructor.
   */
  public Object newInstance() throws ReflectiveOperationException {
    if (className == null) {
      computeNameAndDumps();
    }
    return newInstance(className, classBytes);
  }

  /**
   * Returns a new instance of the given class. The class is loaded in a new class loader.
   *
   * @param className the name of the class to load.
   * @param classContent the content of the class to load.
   * @return a new instance of the class, or {@literal null} if the class is abstract, is an enum,
   * or a module info.
   * @throws ReflectiveOperationException if the class is invalid or if an error occurs in its
   * constructor.
   */
  static Object newInstance(final String className, final byte[] classContent)
          throws ReflectiveOperationException {
    if (className.endsWith(MODULE_INFO)) {
      if (Util.getMajorJavaVersion() < 9) {
        throw new UnsupportedClassVersionError("Module info is not supported before JDK 9");
      }
      else {
        return null;
      }
    }
    ByteClassLoader byteClassLoader = new ByteClassLoader(className, classContent);
    Class<?> clazz = byteClassLoader.loadClass(className);
    // Make sure the class is loaded from the given byte array, excluding any other source.
    if (!byteClassLoader.classLoaded()) {
      // This should never happen, given the implementation of ByteClassLoader.
      throw new AssertionError("Class " + className + " loaded from wrong source");
    }
    if (!clazz.isEnum() && (clazz.getModifiers() & Modifier.ABSTRACT) == 0) {
      Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
      ArrayList<Object> arguments = new ArrayList<>();
      for (Class<?> parameterType : constructor.getParameterTypes()) {
        arguments.add(Array.get(Array.newInstance(parameterType, 1), 0));
      }
      constructor.setAccessible(true);
      return constructor.newInstance(arguments.toArray(new Object[0]));
    }
    return null;
  }

  /**
   * Returns whether the given class file is the same as this one.
   *
   * @return true if 'other' is a {@link ClassFile} with the same string representation.
   * @throws ClassFormatException if the class content can't be parsed.
   */
  @Override
  public boolean equals(final Object other) {
    if (other instanceof ClassFile) {
      return toString().equals(other.toString());
    }
    return false;
  }

  /**
   * Returns the hashcode of this class file.
   *
   * @return the hashcode of the string representation of this class file.
   * @throws ClassFormatException if the class content can't be parsed.
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * Returns a string representation of this class file.
   *
   * @return a string representation of this class file (see the class comments for more details).
   * @throws ClassFormatException if the class content can't be parsed.
   */
  @Override
  public String toString() {
    if (dump == null) {
      computeNameAndDumps();
    }
    return dump;
  }

  /**
   * Computes the name and the string representation of the class (and of its constant pool)
   * contained in this class file.
   *
   * @throws ClassFormatException if the class content can't be parsed.
   */
  private void computeNameAndDumps() {
    try {
      Builder builder = new Builder("ClassFile", /* parent = */ null);
      Builder constantPoolBuilder = new Builder("ConstantPool", /* parent = */ null);
      ConstantClassInfo classInfo =
              dumpClassFile(new Parser(classBytes), builder, constantPoolBuilder);
      className = classInfo.dump().replace('/', '.');
      StringBuilder stringBuilder = new StringBuilder();
      builder.build(stringBuilder);
      dump = stringBuilder.toString();
      StringBuilder constantPoolStringBuilder = new StringBuilder();
      constantPoolBuilder.build(constantPoolStringBuilder);
      constantPoolDump = constantPoolStringBuilder.toString();
    }
    catch (IOException e) {
      throw new ClassFormatException(e.getMessage(), e);
    }
  }

  /**
   * Parses and dumps the high level structure of the class.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @param constantPoolBuilder a dump builder for the constant pool.
   * @return the ConstantClassInfo corresponding to the parsed class.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.1">JVMS
   * 4.1</a>
   */
  private static ConstantClassInfo dumpClassFile(
          final Parser parser, final Builder builder, final Builder constantPoolBuilder)
          throws IOException {
    builder.add("magic: ", parser.u4());
    builder.add("minor_version: ", parser.u2());
    int majorVersion = parser.u2();
    if (majorVersion > /* V15 = */ 59) {
      throw new ClassFormatException("Unsupported class version");
    }
    builder.add("major_version: ", majorVersion);
    int constantPoolCount = parser.u2();
    int cpIndex = 1;
    while (cpIndex < constantPoolCount) {
      CpInfo cpInfo = parseCpInfo(parser, builder);
      builder.putCpInfo(cpIndex, cpInfo);
      constantPoolBuilder.putCpInfo(cpIndex, cpInfo);
      constantPoolBuilder.addCpInfo("constant_pool: ", cpIndex);
      cpIndex += cpInfo.size();
    }
    builder.add("access_flags: ", parser.u2());
    int thisClass = parser.u2();
    builder.addCpInfo("this_class: ", thisClass);
    builder.addCpInfo("super_class: ", parser.u2());
    int interfaceCount = builder.add("interfaces_count: ", parser.u2());
    for (int i = 0; i < interfaceCount; ++i) {
      builder.addCpInfo("interface: ", parser.u2());
    }
    int fieldCount = builder.add("fields_count: ", parser.u2());
    for (int i = 0; i < fieldCount; ++i) {
      dumpFieldInfo(parser, builder);
    }
    int methodCount = builder.add("methods_count: ", parser.u2());
    for (int i = 0; i < methodCount; ++i) {
      dumpMethodInfo(parser, builder);
    }
    dumpAttributeList(parser, builder);
    return builder.getCpInfo(thisClass, ConstantClassInfo.class);
  }

  /**
   * Parses and dumps a list of attributes.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.1">JVMS
   * 4.1</a>
   */
  private static void dumpAttributeList(final Parser parser, final Builder builder)
          throws IOException {
    int attributeCount = builder.add("attributes_count: ", parser.u2());
    SortedBuilder sortedBuilder = builder.addSortedBuilder();
    for (int i = 0; i < attributeCount; ++i) {
      dumpAttributeInfo(parser, sortedBuilder);
    }
  }

  /**
   * Parses a cp_info structure.
   *
   * @param parser a class parser.
   * @param classContext a context to lookup constant pool items from their index.
   * @return the parsed constant pool item.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4">JVMS
   * 4.4</a>
   */
  private static CpInfo parseCpInfo(final Parser parser, final ClassContext classContext)
          throws IOException {
    int tag = parser.u1();
    switch (tag) {
      case 7:
        return new ConstantClassInfo(parser, classContext);
      case 9:
        return new ConstantFieldRefInfo(parser, classContext);
      case 10:
        return new ConstantMethodRefInfo(parser, classContext);
      case 11:
        return new ConstantInterfaceMethodRefInfo(parser, classContext);
      case 8:
        return new ConstantStringInfo(parser, classContext);
      case 3:
        return new ConstantIntegerInfo(parser);
      case 4:
        return new ConstantFloatInfo(parser);
      case 5:
        return new ConstantLongInfo(parser);
      case 6:
        return new ConstantDoubleInfo(parser);
      case 12:
        return new ConstantNameAndTypeInfo(parser, classContext);
      case 1:
        return new ConstantUtf8Info(parser);
      case 15:
        return new ConstantMethodHandleInfo(parser, classContext);
      case 16:
        return new ConstantMethodTypeInfo(parser, classContext);
      case 17:
        return new ConstantDynamicInfo(parser, classContext);
      case 18:
        return new ConstantInvokeDynamicInfo(parser, classContext);
      case 19:
        return new ConstantModuleInfo(parser, classContext);
      case 20:
        return new ConstantPackageInfo(parser, classContext);
      default:
        throw new ClassFormatException("Invalid constant pool item tag " + tag);
    }
  }

  /**
   * Parses and dumps a field_info structure.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5">JVMS
   * 4.5</a>
   */
  private static void dumpFieldInfo(final Parser parser, final Builder builder) throws IOException {
    builder.add("access_flags: ", parser.u2());
    builder.addCpInfo("name_index: ", parser.u2());
    builder.addCpInfo("descriptor_index: ", parser.u2());
    dumpAttributeList(parser, builder);
  }

  /**
   * Parses and dumps a method_info structure.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.6">JVMS
   * 4.6</a>
   */
  private static void dumpMethodInfo(final Parser parser, final Builder builder)
          throws IOException {
    // method_info has the same top level structure as field_info.
    dumpFieldInfo(parser, builder);
  }

  /**
   * Parses and dumps an attribute_info structure.
   *
   * @param parser a class parser.
   * @param sortedBuilder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7">JVMS
   * 4.7</a>
   */
  private static void dumpAttributeInfo(final Parser parser, final SortedBuilder sortedBuilder)
          throws IOException {
    String attributeName = sortedBuilder.getCpInfo(parser.u2()).toString();
    int attributeLength = parser.u4();
    Builder builder = sortedBuilder.addBuilder(attributeName);
    builder.add("attribute_name_index: ", attributeName);
    if (attributeName.equals("ConstantValue")) {
      dumpConstantValueAttribute(parser, builder);
    }
    else if (attributeName.equals("Code")) {
      dumpCodeAttribute(parser, builder);
    }
    else if (attributeName.equals("StackMapTable")) {
      dumpStackMapTableAttribute(parser, builder);
    }
    else if (attributeName.equals("Exceptions")) {
      dumpExceptionsAttribute(parser, builder);
    }
    else if (attributeName.equals("InnerClasses")) {
      dumpInnerClassesAttribute(parser, builder);
    }
    else if (attributeName.equals("EnclosingMethod")) {
      dumpEnclosingMethodAttribute(parser, builder);
    }
    else if (attributeName.equals("Synthetic")) {
      dumpSyntheticAttribute();
    }
    else if (attributeName.equals("Signature")) {
      dumpSignatureAttribute(parser, builder);
    }
    else if (attributeName.equals("SourceFile")) {
      dumpSourceFileAttribute(parser, builder);
    }
    else if (attributeName.equals("SourceDebugExtension")) {
      dumpSourceDebugAttribute(attributeLength, parser, builder);
    }
    else if (attributeName.equals("LineNumberTable")) {
      dumpLineNumberTableAttribute(parser, builder);
    }
    else if (attributeName.equals("LocalVariableTable")) {
      dumpLocalVariableTableAttribute(parser, builder);
    }
    else if (attributeName.equals("LocalVariableTypeTable")) {
      dumpLocalVariableTypeTableAttribute(parser, builder);
    }
    else if (attributeName.equals("Deprecated")) {
      dumpDeprecatedAttribute();
    }
    else if (attributeName.equals("RuntimeVisibleAnnotations")) {
      dumpRuntimeVisibleAnnotationsAttribute(parser, builder);
    }
    else if (attributeName.equals("RuntimeInvisibleAnnotations")) {
      dumpRuntimeInvisibleAnnotationsAttribute(parser, builder);
    }
    else if (attributeName.equals("RuntimeVisibleParameterAnnotations")) {
      dumpRuntimeVisibleParameterAnnotationsAttribute(parser, builder);
    }
    else if (attributeName.equals("RuntimeInvisibleParameterAnnotations")) {
      dumpRuntimeInvisibleParameterAnnotationsAttribute(parser, builder);
    }
    else if (attributeName.equals("RuntimeVisibleTypeAnnotations")) {
      dumpRuntimeVisibleTypeAnnotationsAttribute(parser, builder);
    }
    else if (attributeName.equals("RuntimeInvisibleTypeAnnotations")) {
      dumpRuntimeInvisibleTypeAnnotationsAttribute(parser, builder);
    }
    else if (attributeName.equals("AnnotationDefault")) {
      dumpAnnotationDefaultAttribute(parser, builder);
    }
    else if (attributeName.equals("BootstrapMethods")) {
      dumpBootstrapMethodsAttribute(parser, builder);
    }
    else if (attributeName.equals("MethodParameters")) {
      dumpMethodParametersAttribute(parser, builder);
    }
    else if (attributeName.equals("Module")) {
      dumpModuleAttribute(parser, builder);
    }
    else if (attributeName.equals("ModulePackages")) {
      dumpModulePackagesAttribute(parser, builder);
    }
    else if (attributeName.equals("ModuleMainClass")) {
      dumpModuleMainClassAttribute(parser, builder);
    }
    else if (attributeName.equals("NestHost")) {
      dumpNestHostAttribute(parser, builder);
    }
    else if (attributeName.equals("NestMembers")) {
      dumpNestMembersAttribute(parser, builder);
    }
    else if (attributeName.equals("PermittedSubclasses")) {
      dumpPermittedSubclassesAttribute(parser, builder);
    }
    else if (attributeName.equals("Record")) {
      dumpRecordAttribute(parser, builder);
    }
    else if (attributeName.equals("StackMap")) {
      dumpStackMapAttribute(parser, builder);
    }
    else if (!attributeName.equals("CodeComment") && !attributeName.equals("Comment")) {
      // Not a standard attribute nor one the of empty non-standard attributes used for tests.
      throw new ClassFormatException("Unknown attribute " + attributeName);
    }
  }

  /**
   * Parses and dumps a ConstantValue attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.2">JVMS
   * 4.7.2</a>
   */
  private static void dumpConstantValueAttribute(final Parser parser, final Builder builder)
          throws IOException {
    builder.addCpInfo("constantvalue_index: ", parser.u2());
  }

  /**
   * Parses and dumps a Code attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.3">JVMS
   * 4.7.3</a>
   */
  private static void dumpCodeAttribute(final Parser parser, final Builder builder)
          throws IOException {
    builder.add("max_stack: ", parser.u2());
    builder.add("max_locals: ", parser.u2());
    int codeLength = parser.u4();
    dumpInstructions(codeLength, parser, builder);
    int exceptionCount = builder.add("exception_table_length: ", parser.u2());
    for (int i = 0; i < exceptionCount; ++i) {
      builder.addInsnIndex("start_pc: ", parser.u2());
      builder.addInsnIndex("end_pc: ", parser.u2());
      builder.addInsnIndex("handler_pc: ", parser.u2());
      builder.addCpInfo("catch_type: ", parser.u2());
    }
    dumpAttributeList(parser, builder);
  }

  /**
   * Parses and dumps the bytecode instructions of a method.
   *
   * @param codeLength the number of bytes to parse.
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5">JVMS
   * 6.5</a>
   */
  private static void dumpInstructions(
          final int codeLength, final Parser parser, final Builder builder) throws IOException {
    int bytecodeOffset = 0; // Number of bytes parsed so far.
    int insnIndex = 0; // Number of instructions parsed so far.
    while (bytecodeOffset < codeLength) {
      builder.putInsnIndex(bytecodeOffset, insnIndex);
      int opcode = parser.u1();
      int startOffset = bytecodeOffset++;
      // Instructions are in alphabetical order of their opcode name, as
      // in the specification. This leads to some duplicated code, but is
      // done on purpose for ease of reference.
      switch (opcode) {
        case 0x32: // aaload
        case 0x53: // aastore
        case 0x01: // aconst_null
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x19: // aload
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x2A: // aload_0
        case 0x2B: // aload_1
        case 0x2C: // aload_2
        case 0x2D: // aload_3
          builder.addInsn(insnIndex, 0x19, opcode - 0x2A);
          break;
        case 0xBD: // anewarray
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()));
          bytecodeOffset += 2;
          break;
        case 0xB0: // areturn
        case 0xBE: // arraylength
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x3A: // astore
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x4B: // astore_0
        case 0x4C: // astore_1
        case 0x4D: // astore_2
        case 0x4E: // astore_3
          builder.addInsn(insnIndex, 0x3A, opcode - 0x4B);
          break;
        case 0xBF: // athrow
        case 0x33: // baload
        case 0x54: // bastore
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x10: // bipush
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x34: // caload
        case 0x55: // castore
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xC0: // checkcast
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()));
          bytecodeOffset += 2;
          break;
        case 0x90: // d2f
        case 0x8E: // d2i
        case 0x8F: // d2l
        case 0x63: // dadd
        case 0x31: // daload
        case 0x52: // dastore
        case 0x98: // dcmpg
        case 0x97: // dcmpl
        case 0x0E: // dconst_0
        case 0x0F: // dconst_1
        case 0x6F: // ddiv
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x18: // dload
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x26: // dload_0
        case 0x27: // dload_1
        case 0x28: // dload_2
        case 0x29: // dload_3
          builder.addInsn(insnIndex, 0x18, opcode - 0x26);
          break;
        case 0x6B: // dmul
        case 0x77: // dneg
        case 0x73: // drem
        case 0xAF: // dreturn
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x39: // dstore
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x47: // dstore_0
        case 0x48: // dstore_1
        case 0x49: // dstore_2
        case 0x4A: // dstore_3
          builder.addInsn(insnIndex, 0x39, opcode - 0x47);
          break;
        case 0x67: // dsub
        case 0x59: // dup
        case 0x5A: // dup_x1
        case 0x5B: // dup_x2
        case 0x5C: // dup2
        case 0x5D: // dup2_x1
        case 0x5E: // dup2_x2
        case 0x8D: // f2d
        case 0x8B: // f2i
        case 0x8C: // f2l
        case 0x62: // fadd
        case 0x30: // faload
        case 0x51: // fastore
        case 0x96: // fcmpg
        case 0x95: // fcmpl
        case 0x0B: // fconst_0
        case 0x0C: // fconst_1
        case 0x0D: // fconst_2
        case 0x6E: // fdiv
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x17: // fload
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x22: // fload_0
        case 0x23: // fload_1
        case 0x24: // fload_2
        case 0x25: // fload_3
          builder.addInsn(insnIndex, 0x17, opcode - 0x22);
          break;
        case 0x6A: // fmul
        case 0x76: // fneg
        case 0x72: // frem
        case 0xAE: // freturn
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x38: // fstore
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x43: // fstore_0
        case 0x44: // fstore_1
        case 0x45: // fstore_2
        case 0x46: // fstore_3
          builder.addInsn(insnIndex, 0x38, opcode - 0x43);
          break;
        case 0x66: // fsub
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xB4: // getfield
        case 0xB2: // getstatic
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()));
          bytecodeOffset += 2;
          break;
        case 0xA7: // goto
          builder.addInsn(
                  insnIndex, opcode, new InstructionIndex(startOffset + parser.s2(), builder));
          bytecodeOffset += 2;
          break;
        case 0xC8: // goto_w
          builder.addInsn(
                  insnIndex, 0xA7, new InstructionIndex(startOffset + parser.u4(), builder));
          bytecodeOffset += 4;
          break;
        case 0x91: // i2b
        case 0x92: // i2c
        case 0x87: // i2d
        case 0x86: // i2f
        case 0x85: // i2l
        case 0x93: // i2s
        case 0x60: // iadd
        case 0x2E: // iaload
        case 0x7E: // iand
        case 0x4F: // iastore
        case 0x02: // iconst_m1
        case 0x03: // iconst_0
        case 0x04: // iconst_1
        case 0x05: // iconst_2
        case 0x06: // iconst_3
        case 0x07: // iconst_4
        case 0x08: // iconst_5
        case 0x6C: // idiv
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xA5: // if_acmpeq
        case 0xA6: // if_acmpne
        case 0x9F: // if_icmpeq
        case 0xA0: // if_icmpne
        case 0xA1: // if_icmplt
        case 0xA2: // if_icmpge
        case 0xA3: // if_icmpgt
        case 0xA4: // if_icmple
        case 0x99: // ifeq
        case 0x9A: // ifne
        case 0x9B: // iflt
        case 0x9C: // ifge
        case 0x9D: // ifgt
        case 0x9E: // ifle
        case 0xC7: // ifnonnull
        case 0xC6: // ifnull
          builder.addInsn(
                  insnIndex, opcode, new InstructionIndex(startOffset + parser.s2(), builder));
          bytecodeOffset += 2;
          break;
        case 0x84: // iinc
          builder.addInsn(insnIndex, opcode, parser.u1(), parser.s1());
          bytecodeOffset += 2;
          break;
        case 0x15: // iload
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x1A: // iload_0
        case 0x1B: // iload_1
        case 0x1C: // iload_2
        case 0x1D: // iload_3
          builder.addInsn(insnIndex, 0x15, opcode - 0x1A);
          break;
        case 0x68: // imul
        case 0x74: // ineg
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xC1: // instanceof
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()));
          bytecodeOffset += 2;
          break;
        case 0xBA: // invokedynamic
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()));
          parser.u2();
          bytecodeOffset += 4;
          break;
        case 0xB9: // invokeinterface
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()), parser.u1());
          parser.u1();
          bytecodeOffset += 4;
          break;
        case 0xB7: // invokespecial
        case 0xB8: // invokestatic
        case 0xB6: // invokevirtual
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()));
          bytecodeOffset += 2;
          break;
        case 0x80: // ior
        case 0x70: // irem
        case 0xAC: // ireturn
        case 0x78: // ishl
        case 0x7A: // ishr
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x36: // istore
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x3B: // istore_0
        case 0x3C: // istore_1
        case 0x3D: // istore_2
        case 0x3E: // istore_3
          builder.addInsn(insnIndex, 0x36, opcode - 0x3B);
          break;
        case 0x64: // isub
        case 0x7C: // iushr
        case 0x82: // ixor
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xA8: // jsr
          builder.addInsn(
                  insnIndex, opcode, new InstructionIndex(startOffset + parser.s2(), builder));
          bytecodeOffset += 2;
          break;
        case 0xC9: // jsr_w
          builder.addInsn(
                  insnIndex, 0xA8, new InstructionIndex(startOffset + parser.u4(), builder));
          bytecodeOffset += 4;
          break;
        case 0x8A: // l2d
        case 0x89: // l2f
        case 0x88: // l2i
        case 0x61: // ladd
        case 0x2F: // laload
        case 0x7F: // land
        case 0x50: // lastore
        case 0x94: // lcmp
        case 0x09: // lconst_0
        case 0x0A: // lconst_1
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x12: // ldc
          builder.addInsn(insnIndex, 0x12, builder.getCpInfo(parser.u1()));
          bytecodeOffset += 1;
          break;
        case 0x13: // ldc_w
        case 0x14: // ldc2_w
          builder.addInsn(insnIndex, opcode == 0x13 ? 0x12 : 0x14, builder.getCpInfo(parser.u2()));
          bytecodeOffset += 2;
          break;
        case 0x6D: // ldiv
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x16: // lload
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x1E: // lload_0
        case 0x1F: // lload_1
        case 0x20: // lload_2
        case 0x21: // lload_3
          builder.addInsn(insnIndex, 0x16, opcode - 0x1E);
          break;
        case 0x69: // lmul
        case 0x75: // lneg
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xAB: // lookupswitch
          builder.addInsn(insnIndex, opcode);
          while (bytecodeOffset % 4 != 0) {
            parser.u1();
            bytecodeOffset++;
          }
          builder.addInsnIndex("default: ", startOffset + parser.u4());
          int pairCount = builder.add("npairs: ", parser.u4());
          bytecodeOffset += 8;
          for (int i = 0; i < pairCount; ++i) {
            builder.addInsnIndex(parser.u4() + ": ", startOffset + parser.u4());
            bytecodeOffset += 8;
          }
          break;
        case 0x81: // lor
        case 0x71: // lrem
        case 0xAD: // lreturn
        case 0x79: // lshl
        case 0x7B: // lshr
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x37: // lstore
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x3F: // lstore_0
        case 0x40: // lstore_1
        case 0x41: // lstore_2
        case 0x42: // lstore_3
          builder.addInsn(insnIndex, 0x37, opcode - 0x3F);
          break;
        case 0x65: // lsub
        case 0x7D: // lushr
        case 0x83: // lxor
        case 0xC2: // monitorenter
        case 0xC3: // monitorexit
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xC5: // multianewarray
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()), parser.u1());
          bytecodeOffset += 3;
          break;
        case 0xBB: // new
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()));
          bytecodeOffset += 2;
          break;
        case 0xBC: // newarray
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0x00: // nop
        case 0x57: // pop
        case 0x58: // pop2
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xB5: // putfield
        case 0xB3: // putstatic
          builder.addInsn(insnIndex, opcode, builder.getCpInfo(parser.u2()));
          bytecodeOffset += 2;
          break;
        case 0xA9: // ret
          builder.addInsn(insnIndex, opcode, parser.u1());
          bytecodeOffset += 1;
          break;
        case 0xB1: // return
        case 0x35: // saload
        case 0x56: // sastore
          builder.addInsn(insnIndex, opcode);
          break;
        case 0x11: // sipush
          builder.addInsn(insnIndex, opcode, parser.s2());
          bytecodeOffset += 2;
          break;
        case 0x5F: // swap
          builder.addInsn(insnIndex, opcode);
          break;
        case 0xAA: // tableswitch
          builder.addInsn(insnIndex, opcode);
          while (bytecodeOffset % 4 != 0) {
            parser.u1();
            bytecodeOffset++;
          }
          builder.addInsnIndex("default: ", startOffset + parser.u4());
          int low = builder.add("low: ", parser.u4());
          int high = builder.add("high: ", parser.u4());
          bytecodeOffset += 12;
          for (int i = low; i <= high; ++i) {
            builder.addInsnIndex(i + ": ", startOffset + parser.u4());
            bytecodeOffset += 4;
          }
          break;
        case 0xC4: // wide
          opcode = parser.u1();
          bytecodeOffset += 1;
          switch (opcode) {
            case 0x15: // iload
            case 0x17: // fload
            case 0x19: // aload
            case 0x16: // lload
            case 0x18: // dload
            case 0x36: // istore
            case 0x38: // fstore
            case 0x3A: // astore
            case 0x37: // lstore
            case 0x39: // dstore
            case 0xA9: // ret
              builder.addInsn(insnIndex, opcode, parser.u2());
              bytecodeOffset += 2;
              break;
            case 0x84: // iinc
              builder.addInsn(insnIndex, opcode, parser.u2(), parser.s2());
              bytecodeOffset += 4;
              break;
            default:
              throw new ClassFormatException("Unknown wide opcode: " + opcode);
          }
          break;
        default:
          throw new ClassFormatException("Unknown opcode: " + opcode);
      }
      insnIndex++;
    }
    builder.putInsnIndex(bytecodeOffset, insnIndex);
  }

  /**
   * Parses and dumps a StackMapTable attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.4">JVMS
   * 4.7.4</a>
   */
  private static void dumpStackMapTableAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int entryCount = builder.add("number_of_entries: ", parser.u2());
    int bytecodeOffset = -1;
    for (int i = 0; i < entryCount; ++i) {
      int frameType = parser.u1();
      if (frameType < 64) {
        int offsetDelta = frameType;
        bytecodeOffset += offsetDelta + 1;
        builder.addInsnIndex("SAME ", bytecodeOffset);
      }
      else if (frameType < 128) {
        int offsetDelta = frameType - 64;
        bytecodeOffset += offsetDelta + 1;
        builder.addInsnIndex("SAME_LOCALS_1_STACK_ITEM ", bytecodeOffset);
        dumpVerificationTypeInfo(parser, builder);
      }
      else if (frameType < 247) {
        throw new ClassFormatException("Unknown frame type " + frameType);
      }
      else if (frameType == 247) {
        int offsetDelta = parser.u2();
        bytecodeOffset += offsetDelta + 1;
        builder.addInsnIndex("SAME_LOCALS_1_STACK_ITEM ", bytecodeOffset);
        dumpVerificationTypeInfo(parser, builder);
      }
      else if (frameType < 251) {
        int offsetDelta = parser.u2();
        bytecodeOffset += offsetDelta + 1;
        builder.addInsnIndex("CHOP_" + (251 - frameType) + " ", bytecodeOffset);
      }
      else if (frameType == 251) {
        int offsetDelta = parser.u2();
        bytecodeOffset += offsetDelta + 1;
        builder.addInsnIndex("SAME ", bytecodeOffset);
      }
      else if (frameType < 255) {
        int offsetDelta = parser.u2();
        bytecodeOffset += offsetDelta + 1;
        builder.addInsnIndex("APPEND_" + (frameType - 251) + " ", bytecodeOffset);
        for (int j = 0; j < frameType - 251; ++j) {
          dumpVerificationTypeInfo(parser, builder);
        }
      }
      else {
        int offsetDelta = parser.u2();
        bytecodeOffset += offsetDelta + 1;
        builder.addInsnIndex("FULL ", bytecodeOffset);
        int numberOfLocals = builder.add("number_of_locals: ", parser.u2());
        for (int j = 0; j < numberOfLocals; ++j) {
          dumpVerificationTypeInfo(parser, builder);
        }
        int numberOfStackItems = builder.add("number_of_stack_items: ", parser.u2());
        for (int j = 0; j < numberOfStackItems; ++j) {
          dumpVerificationTypeInfo(parser, builder);
        }
      }
    }
  }

  /**
   * Parses and dumps a verification_type_info structure.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.2">JVMS
   * 4.7.2</a>
   */
  private static void dumpVerificationTypeInfo(final Parser parser, final Builder builder)
          throws IOException {
    int tag = builder.add("tag: ", parser.u1());
    if (tag > 8) {
      throw new ClassFormatException("Unknown verification_type_info tag: " + tag);
    }
    if (tag == 7) {
      builder.addCpInfo("cpool_index: ", parser.u2());
    }
    else if (tag == 8) {
      builder.addInsnIndex("offset: ", parser.u2());
    }
  }

  /**
   * Parses and dumps an Exception attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.5">JVMS
   * 4.7.5</a>
   */
  private static void dumpExceptionsAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int exceptionCount = builder.add("number_of_exceptions: ", parser.u2());
    for (int i = 0; i < exceptionCount; ++i) {
      builder.addCpInfo("exception_index: ", parser.u2());
    }
  }

  /**
   * Parses and dumps an InnerClasses attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.6">JVMS
   * 4.7.6</a>
   */
  private static void dumpInnerClassesAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int classCount = builder.add("number_of_classes: ", parser.u2());
    for (int i = 0; i < classCount; ++i) {
      builder.addCpInfo("inner_class_info_index: ", parser.u2());
      builder.addCpInfo("outer_class_info_index: ", parser.u2());
      builder.addCpInfo("inner_name_index: ", parser.u2());
      builder.add("inner_class_access_flags: ", parser.u2());
    }
  }

  /**
   * Parses and dumps an EnclosingMethod attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.7">JVMS
   * 4.7.7</a>
   */
  private static void dumpEnclosingMethodAttribute(final Parser parser, final Builder builder)
          throws IOException {
    builder.addCpInfo("class_index: ", parser.u2());
    builder.addCpInfo("method_index: ", parser.u2());
  }

  /**
   * Parses and dumps a Synthetic attribute.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.8">JVMS
   * 4.7.8</a>
   */
  private static void dumpSyntheticAttribute() {
    // Nothing to parse.
  }

  /**
   * Parses and dumps a Signature attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9">JVMS
   * 4.7.9</a>
   */
  private static void dumpSignatureAttribute(final Parser parser, final Builder builder)
          throws IOException {
    builder.addCpInfo("signature_index: ", parser.u2());
  }

  /**
   * Parses and dumps a SourceFile attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.10">JVMS
   * 4.7.10</a>
   */
  private static void dumpSourceFileAttribute(final Parser parser, final Builder builder)
          throws IOException {
    builder.addCpInfo("sourcefile_index: ", parser.u2());
  }

  /**
   * Parses and dumps a SourceDebug attribute.
   *
   * @param attributeLength the length of the SourceDebug attribute (excluding its 6 header bytes).
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.11">JVMS
   * 4.7.11</a>
   */
  private static void dumpSourceDebugAttribute(
          final int attributeLength, final Parser parser, final Builder builder) throws IOException {
    byte[] attributeData = parser.bytes(attributeLength);
    StringBuilder stringBuilder = new StringBuilder();
    for (byte data : attributeData) {
      stringBuilder.append(data).append(',');
    }
    builder.add("debug_extension: ", stringBuilder.toString());
  }

  /**
   * Parses and dumps a LineNumberTable attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.12">JVMS
   * 4.7.12</a>
   */
  private static void dumpLineNumberTableAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int lineNumberCount = builder.add("line_number_table_length: ", parser.u2());
    for (int i = 0; i < lineNumberCount; ++i) {
      builder.addInsnIndex("start_pc: ", parser.u2());
      builder.add("line_number: ", parser.u2());
    }
  }

  /**
   * Parses and dumps a LocalVariableTable attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.13">JVMS
   * 4.7.13</a>
   */
  private static void dumpLocalVariableTableAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int localVariableCount = builder.add("local_variable_table_length: ", parser.u2());
    for (int i = 0; i < localVariableCount; ++i) {
      int startPc = builder.addInsnIndex("start_pc: ", parser.u2());
      builder.addInsnIndex("length: ", startPc + parser.u2());
      builder.addCpInfo("name_index: ", parser.u2());
      builder.addCpInfo("descriptor_index: ", parser.u2());
      builder.add("index: ", parser.u2());
    }
  }

  /**
   * Parses and dumps a LocalVariableTypeTable attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.14">JVMS
   * 4.7.14</a>
   */
  private static void dumpLocalVariableTypeTableAttribute(
          final Parser parser, final Builder builder) throws IOException {
    int localVariableCount = builder.add("local_variable_type_table_length: ", parser.u2());
    for (int i = 0; i < localVariableCount; ++i) {
      int startPc = builder.addInsnIndex("start_pc: ", parser.u2());
      builder.addInsnIndex("length: ", startPc + parser.u2());
      builder.addCpInfo("name_index: ", parser.u2());
      builder.addCpInfo("signature_index: ", parser.u2());
      builder.add("index: ", parser.u2());
    }
  }

  /**
   * Parses and dumps a Deprecated attribute.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.15">JVMS
   * 4.7.15</a>
   */
  private static void dumpDeprecatedAttribute() {
    // Nothing to parse.
  }

  /**
   * Parses and dumps a RuntimeVisibleAnnotations attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16">JVMS
   * 4.7.16</a>
   */
  private static void dumpRuntimeVisibleAnnotationsAttribute(
          final Parser parser, final Builder builder) throws IOException {
    int annotationCount = builder.add("num_annotations: ", parser.u2());
    for (int i = 0; i < annotationCount; ++i) {
      dumpAnnotation(parser, builder);
    }
  }

  /**
   * Parses and dumps an annotations structure.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16">JVMS
   * 4.7.16</a>
   */
  private static void dumpAnnotation(final Parser parser, final Builder builder)
          throws IOException {
    builder.addCpInfo("type_index: ", parser.u2());
    int elementValuePairCount = builder.add("num_element_value_pairs: ", parser.u2());
    for (int i = 0; i < elementValuePairCount; ++i) {
      builder.addCpInfo("element_name_index: ", parser.u2());
      dumpElementValue(parser, builder);
    }
  }

  /**
   * Parses and dumps an element_value structure.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a
   * href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1">JVMS
   * 4.7.16.1</a>
   */
  private static void dumpElementValue(final Parser parser, final Builder builder)
          throws IOException {
    int tag = parser.u1();
    switch (tag) {
      case 'B':
      case 'C':
      case 'D':
      case 'F':
      case 'I':
      case 'J':
      case 'S':
      case 'Z':
      case 's':
        builder.addCpInfo(((char) tag) + ": ", parser.u2());
        return;
      case 'e':
        builder.addCpInfo("e: ", parser.u2());
        builder.addCpInfo("const_name_index: ", parser.u2());
        return;
      case 'c':
        builder.addCpInfo(((char) tag) + ": ", parser.u2());
        return;
      case '@':
        builder.add("@: ", "");
        dumpAnnotation(parser, builder);
        return;
      case '[':
        int valueCount = builder.add("[: ", parser.u2());
        for (int i = 0; i < valueCount; ++i) {
          dumpElementValue(parser, builder);
        }
        return;
      default:
        throw new ClassFormatException("Unknown element_type tag: " + tag);
    }
  }

  /**
   * Parses and dumps a RuntimeInvisibleAnnotations attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.17">JVMS
   * 4.7.17</a>
   */
  private static void dumpRuntimeInvisibleAnnotationsAttribute(
          final Parser parser, final Builder builder) throws IOException {
    dumpRuntimeVisibleAnnotationsAttribute(parser, builder);
  }

  /**
   * Parses and dumps a RuntimeVisibleParameterAnnotations attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18">JVMS
   * 4.7.18</a>
   */
  private static void dumpRuntimeVisibleParameterAnnotationsAttribute(
          final Parser parser, final Builder builder) throws IOException {
    int parameterCount = builder.add("num_parameters: ", parser.u1());
    for (int i = 0; i < parameterCount; ++i) {
      int annotationCount = builder.add("num_annotations: ", parser.u2());
      for (int j = 0; j < annotationCount; ++j) {
        dumpAnnotation(parser, builder);
      }
    }
  }

  /**
   * Parses and dumps a RuntimeInvisibleParameterAnnotations attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.19">JVMS
   * 4.7.19</a>
   */
  private static void dumpRuntimeInvisibleParameterAnnotationsAttribute(
          final Parser parser, final Builder builder) throws IOException {
    dumpRuntimeVisibleParameterAnnotationsAttribute(parser, builder);
  }

  /**
   * Parses and dumps a RuntimeVisibleTypeAnnotations attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20">JVMS
   * 4.7.20</a>
   */
  private static void dumpRuntimeVisibleTypeAnnotationsAttribute(
          final Parser parser, final Builder builder) throws IOException {
    int annotationCount = builder.add("num_annotations: ", parser.u2());
    SortedBuilder sortedBuilder = builder.addSortedBuilder();
    for (int i = 0; i < annotationCount; ++i) {
      dumpTypeAnnotation(parser, sortedBuilder);
    }
  }

  /**
   * Parses and dumps a type_annotation structure.
   *
   * @param parser a class parser.
   * @param sortedBuilder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20">JVMS
   * 4.7.20</a>
   */
  private static void dumpTypeAnnotation(final Parser parser, final SortedBuilder sortedBuilder)
          throws IOException {
    int targetType = parser.u1();
    Builder builder = sortedBuilder.addBuilder(String.valueOf(targetType));
    builder.add("target_type: ", targetType);
    switch (targetType) {
      case 0x00:
      case 0x01:
        // type_parameter_target
        builder.add("type_parameter_index: ", parser.u1());
        break;
      case 0x10:
        // supertype_target
        builder.add("supertype_index: ", parser.u2());
        break;
      case 0x11:
      case 0x12:
        // type_parameter_bound_target
        builder.add("type_parameter_index: ", parser.u1());
        builder.add("bound_index: ", parser.u1());
        break;
      case 0x13:
      case 0x14:
      case 0x15:
        // empty_target
        // Nothing to parse.
        break;
      case 0x16:
        // formal_parameter_target
        builder.add("formal_parameter_index: ", parser.u1());
        break;
      case 0x17:
        // throws_target
        builder.add("throws_type_index: ", parser.u2());
        break;
      case 0x40:
      case 0x41:
        // localvar_target
        int tableLength = builder.add("table_length: ", parser.u2());
        for (int i = 0; i < tableLength; ++i) {
          int startPc = builder.addInsnIndex("start_pc: ", parser.u2());
          builder.addInsnIndex("length: ", startPc + parser.u2());
          builder.add("index: ", parser.u2());
        }
        break;
      case 0x42:
        // catch_target
        builder.add("exception_table_index: ", parser.u2());
        break;
      case 0x43:
      case 0x44:
      case 0x45:
      case 0x46:
        // offset_target
        builder.addInsnIndex("offset: ", parser.u2());
        break;
      case 0x47:
      case 0x48:
      case 0x49:
      case 0x4A:
      case 0x4B:
        // type_argument_target
        builder.addInsnIndex("offset: ", parser.u2());
        builder.add("type_argument_index: ", parser.u1());
        break;
      default:
        throw new ClassFormatException("Unknown target_type: " + targetType);
    }
    dumpTypePath(parser, builder);
    // Sort type annotations based on the full target_info and type_path (excluding the annotation
    // content), instead of only on their target_type.
    builder.sortByContent();
    dumpAnnotation(parser, builder);
  }

  /**
   * Parses and dumps a type_path structure.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a
   * href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20.2">JVMS
   * 4.7.20.2</a>
   */
  private static void dumpTypePath(final Parser parser, final Builder builder) throws IOException {
    int pathLength = builder.add("path_length: ", parser.u1());
    for (int i = 0; i < pathLength; ++i) {
      builder.add("type_path_kind: ", parser.u1());
      builder.add("type_argument_index: ", parser.u1());
    }
  }

  /**
   * Parses and dumps a RuntimeInvisibleTypeAnnotations attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.21">JVMS
   * 4.7.21</a>
   */
  private static void dumpRuntimeInvisibleTypeAnnotationsAttribute(
          final Parser parser, final Builder builder) throws IOException {
    dumpRuntimeVisibleTypeAnnotationsAttribute(parser, builder);
  }

  /**
   * Parses and dumps an AnnotationDefault attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.22">JVMS
   * 4.7.22</a>
   */
  private static void dumpAnnotationDefaultAttribute(final Parser parser, final Builder builder)
          throws IOException {
    dumpElementValue(parser, builder);
  }

  /**
   * Parses and dumps a BootstrapMethods attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.23">JVMS
   * 4.7.23</a>
   */
  private static void dumpBootstrapMethodsAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int bootstrapMethodCount = builder.add("num_bootstrap_methods: ", parser.u2());
    for (int i = 0; i < bootstrapMethodCount; ++i) {
      builder.addCpInfo("bootstrap_method_ref: ", parser.u2());
      int bootstrapArgumentCount = builder.add("num_bootstrap_arguments: ", parser.u2());
      for (int j = 0; j < bootstrapArgumentCount; ++j) {
        builder.addCpInfo("bootstrap_argument: ", parser.u2());
      }
    }
  }

  /**
   * Parses and dumps a MethodParameters attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.24">JVMS
   * 4.7.24</a>
   */
  private static void dumpMethodParametersAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int parameterCount = builder.add("parameters_count: ", parser.u1());
    for (int i = 0; i < parameterCount; ++i) {
      builder.addCpInfo("name_index: ", parser.u2());
      builder.add("access_flags: ", parser.u2());
    }
  }

  /**
   * Parses and dumps a Module attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.25">JVMS
   * 4.7.25</a>
   */
  private static void dumpModuleAttribute(final Parser parser, final Builder builder)
          throws IOException {
    builder.addCpInfo("name: ", parser.u2());
    builder.add("access: ", parser.u2());
    builder.addCpInfo("version: ", parser.u2());
    int requireCount = builder.add("require_count: ", parser.u2());
    for (int i = 0; i < requireCount; ++i) {
      builder.addCpInfo("name: ", parser.u2());
      builder.add("access: ", parser.u2());
      builder.addCpInfo("version: ", parser.u2());
    }
    int exportCount = builder.add("export_count: ", parser.u2());
    for (int i = 0; i < exportCount; ++i) {
      builder.addCpInfo("name: ", parser.u2());
      builder.add("access: ", parser.u2());
      int exportToCount = builder.add("export_to_count: ", parser.u2());
      for (int j = 0; j < exportToCount; ++j) {
        builder.addCpInfo("to: ", parser.u2());
      }
    }
    int openCount = builder.add("open_count: ", parser.u2());
    for (int i = 0; i < openCount; ++i) {
      builder.addCpInfo("name: ", parser.u2());
      builder.add("access: ", parser.u2());
      int openToCount = builder.add("open_to_count: ", parser.u2());
      for (int j = 0; j < openToCount; ++j) {
        builder.addCpInfo("to: ", parser.u2());
      }
    }
    int useCount = builder.add("use_count: ", parser.u2());
    for (int i = 0; i < useCount; ++i) {
      builder.addCpInfo("use: ", parser.u2());
    }
    int provideCount = builder.add("provide_count: ", parser.u2());
    for (int i = 0; i < provideCount; ++i) {
      builder.addCpInfo("provide: ", parser.u2());
      int provideWithCount = builder.add("provide_with_count: ", parser.u2());
      for (int j = 0; j < provideWithCount; ++j) {
        builder.addCpInfo("with: ", parser.u2());
      }
    }
  }

  /**
   * Parses and dumps a ModulePackages attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.26">JVMS
   * 4.7.26</a>
   */
  private static void dumpModulePackagesAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int packageCount = builder.add("package_count: ", parser.u2());
    for (int i = 0; i < packageCount; ++i) {
      builder.addCpInfo("package: ", parser.u2());
    }
  }

  /**
   * Parses and dumps a ModuleMainClass attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.27">JVMS
   * 4.7.27</a>
   */
  private static void dumpModuleMainClassAttribute(final Parser parser, final Builder builder)
          throws IOException {
    builder.addCpInfo("main_class: ", parser.u2());
  }

  /**
   * Parses and dumps a NestHost attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.7.28">JVMS
   * 4.7.28</a>
   */
  private static void dumpNestHostAttribute(final Parser parser, final Builder builder)
          throws IOException {
    builder.addCpInfo("host_class: ", parser.u2());
  }

  /**
   * Parses and dumps a NestMembers attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.7.29">JVMS
   * 4.7.29</a>
   */
  private static void dumpNestMembersAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int numberOfClasses = builder.add("number_of_classes: ", parser.u2());
    for (int i = 0; i < numberOfClasses; ++i) {
      builder.addCpInfo("class: ", parser.u2());
    }
  }

  /**
   * Parses and dumps a PermittedSubclasses attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://openjdk.java.net/jeps/360">JEP 360</a>
   */
  private static void dumpPermittedSubclassesAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int permittedSubclassesCount = builder.add("permitted_subclasses_count: ", parser.u2());
    for (int i = 0; i < permittedSubclassesCount; ++i) {
      builder.addCpInfo("class: ", parser.u2());
    }
  }

  /**
   * Parses and dumps a Record attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a href="https://openjdk.java.net/jeps/360">JEP 360</a>
   */
  private static void dumpRecordAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int numberOfComponentRecords = builder.add("number_of_component_records: ", parser.u2());
    for (int i = 0; i < numberOfComponentRecords; ++i) {
      builder.addCpInfo("record_component_name: ", parser.u2());
      builder.addCpInfo("record_component_descriptor: ", parser.u2());
      dumpAttributeList(parser, builder);
    }
  }

  /**
   * Parses and dumps a StackMap attribute.
   *
   * @param parser a class parser.
   * @param builder a dump builder.
   * @throws IOException if the class can't be parsed.
   * @see <a
   * href="http://docs.oracle.com/javame/config/cldc/opt-pkgs/api/cldc/api/Appendix1-verifier.pdf">CLDC</a>
   */
  private static void dumpStackMapAttribute(final Parser parser, final Builder builder)
          throws IOException {
    int entryCount = builder.add("number_of_entries: ", parser.u2());
    for (int i = 0; i < entryCount; ++i) {
      builder.addInsnIndex("offset: ", parser.u2());
      int numberOfLocals = builder.add("number_of_locals: ", parser.u2());
      for (int j = 0; j < numberOfLocals; ++j) {
        dumpVerificationTypeInfo(parser, builder);
      }
      int numberOfStackItems = builder.add("number_of_stack_items: ", parser.u2());
      for (int j = 0; j < numberOfStackItems; ++j) {
        dumpVerificationTypeInfo(parser, builder);
      }
    }
  }

  /** An abstract constant pool item. */
  private abstract static class CpInfo {
    /** The dump of this item. */
    private String dump;
    /** The context to use to get the referenced constant pool items. */
    private final ClassContext classContext;

    /**
     * Constructs a CpInfo for an item without references to other items.
     *
     * @param dump the dump of this item.
     */
    CpInfo(final String dump) {
      this.dump = dump;
      this.classContext = null;
    }

    /**
     * Constructs a CpInfo for an item with references to other items.
     *
     * @param classContext a context to lookup constant pool items from their index.
     */
    CpInfo(final ClassContext classContext) {
      this.classContext = classContext;
    }

    /**
     * Returns the number of entries used by this item in constant_pool.
     *
     * @return the number of entries used by this item in constant_pool.
     */
    int size() {
      return 1;
    }

    /**
     * Returns the constant pool item with the given index.
     *
     * @param <C> a CpInfo subclass.
     * @param cpIndex a constant pool entry index.
     * @param cpInfoType the expected type of the constant pool entry.
     * @return the constant pool item with the given index.
     */
    <C extends CpInfo> C getCpInfo(final int cpIndex, final Class<C> cpInfoType) {
      return classContext.getCpInfo(cpIndex, cpInfoType);
    }

    /**
     * Returns the dump of this item.
     *
     * @return the dump of this item.
     */
    String dump() {
      return dump;
    }

    @Override
    public String toString() {
      if (dump == null) {
        dump = getClass().getSimpleName() + " " + dump();
      }
      return dump;
    }
  }

  /**
   * A CONSTANT_Class_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.1">JVMS
   * 4.4.1</a>
   */
  private static class ConstantClassInfo extends CpInfo {
    private final int nameIndex;

    /**
     * Parses a CONSTANT_Class_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantClassInfo(final Parser parser, final ClassContext classContext) throws IOException {
      super(classContext);
      this.nameIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(nameIndex, ConstantUtf8Info.class).dump();
    }
  }

  /**
   * A CONSTANT_Fieldref_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.2">JVMS
   * 4.4.2</a>
   */
  private static class ConstantFieldRefInfo extends CpInfo {
    private final int classIndex;
    private final int nameAndTypeIndex;

    /**
     * Parses a CONSTANT_Fieldref_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantFieldRefInfo(final Parser parser, final ClassContext classContext) throws IOException {
      super(classContext);
      this.classIndex = parser.u2();
      this.nameAndTypeIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(classIndex, ConstantClassInfo.class).dump()
              + "."
              + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class).dump();
    }
  }

  /**
   * A CONSTANT_Methodref_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.2">JVMS
   * 4.4.2</a>
   */
  private static class ConstantMethodRefInfo extends CpInfo {
    private final int classIndex;
    private final int nameAndTypeIndex;

    /**
     * Parses a CONSTANT_Methodref_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantMethodRefInfo(final Parser parser, final ClassContext classContext) throws IOException {
      super(classContext);
      this.classIndex = parser.u2();
      this.nameAndTypeIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(classIndex, ConstantClassInfo.class).dump()
              + "."
              + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class).dump();
    }
  }

  /**
   * A CONSTANT_InterfaceMethodref_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.2">JVMS
   * 4.4.2</a>
   */
  private static class ConstantInterfaceMethodRefInfo extends CpInfo {
    private final int classIndex;
    private final int nameAndTypeIndex;

    /**
     * Parses a CONSTANT_InterfaceMethodref_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantInterfaceMethodRefInfo(final Parser parser, final ClassContext classContext)
            throws IOException {
      super(classContext);
      this.classIndex = parser.u2();
      this.nameAndTypeIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(classIndex, ConstantClassInfo.class).dump()
              + "."
              + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class).dump();
    }
  }

  /**
   * A CONSTANT_String_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.3">JVMS
   * 4.4.3</a>
   */
  private static class ConstantStringInfo extends CpInfo {
    final int stringIndex;

    /**
     * Parses a CONSTANT_String_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantStringInfo(final Parser parser, final ClassContext classContext) throws IOException {
      super(classContext);
      this.stringIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(stringIndex, ConstantUtf8Info.class).dump();
    }
  }

  /**
   * A CONSTANT_Integer_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.4">JVMS
   * 4.4.4</a>
   */
  private static class ConstantIntegerInfo extends CpInfo {

    /**
     * Parses a CONSTANT_Integer_info item.
     *
     * @param parser a class parser.
     * @throws IOException if the class can't be parsed.
     */
    ConstantIntegerInfo(final Parser parser) throws IOException {
      super(Integer.toString(parser.u4()));
    }
  }

  /**
   * A CONSTANT_Float_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.4">JVMS
   * 4.4.4</a>
   */
  private static class ConstantFloatInfo extends CpInfo {

    /**
     * Parses a CONSTANT_Float_info item.
     *
     * @param parser a class parser.
     * @throws IOException if the class can't be parsed.
     */
    ConstantFloatInfo(final Parser parser) throws IOException {
      super(Float.toString(Float.intBitsToFloat(parser.u4())));
    }
  }

  /**
   * A CONSTANT_Long_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.5">JVMS
   * 4.4.5</a>
   */
  private static class ConstantLongInfo extends CpInfo {

    /**
     * Parses a CONSTANT_Long_info item.
     *
     * @param parser a class parser.
     * @throws IOException if the class can't be parsed.
     */
    ConstantLongInfo(final Parser parser) throws IOException {
      super(Long.toString(parser.s8()));
    }

    @Override
    int size() {
      return 2;
    }
  }

  /**
   * A CONSTANT_Double_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.5">JVMS
   * 4.4.5</a>
   */
  private static class ConstantDoubleInfo extends CpInfo {

    /**
     * Parses a CONSTANT_Double_info item.
     *
     * @param parser a class parser.
     * @throws IOException if the class can't be parsed.
     */
    ConstantDoubleInfo(final Parser parser) throws IOException {
      super(Double.toString(Double.longBitsToDouble(parser.s8())));
    }

    @Override
    int size() {
      return 2;
    }
  }

  /**
   * A CONSTANT_NameAndType_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.6">JVMS
   * 4.4.6</a>
   */
  private static class ConstantNameAndTypeInfo extends CpInfo {
    private final int nameIndex;
    private final int descriptorIndex;

    /**
     * Parses a CONSTANT_NameAndType_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantNameAndTypeInfo(final Parser parser, final ClassContext classContext)
            throws IOException {
      super(classContext);
      this.nameIndex = parser.u2();
      this.descriptorIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(nameIndex, ConstantUtf8Info.class).dump()
              + getCpInfo(descriptorIndex, ConstantUtf8Info.class).dump();
    }
  }

  /**
   * A CONSTANT_Utf8_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.7">JVMS
   * 4.4.7</a>
   */
  private static class ConstantUtf8Info extends CpInfo {

    /**
     * Parses a CONSTANT_Utf8_info item.
     *
     * @param parser a class parser.
     * @throws IOException if the class can't be parsed.
     */
    ConstantUtf8Info(final Parser parser) throws IOException {
      super(parser.utf8());
    }
  }

  /**
   * A CONSTANT_MethodHandle_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.8">JVMS
   * 4.4.8</a>
   */
  private static class ConstantMethodHandleInfo extends CpInfo {
    private final int referenceKind;
    private final int referenceIndex;

    /**
     * Parses a CONSTANT_MethodHandle_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantMethodHandleInfo(final Parser parser, final ClassContext classContext)
            throws IOException {
      super(classContext);
      this.referenceKind = parser.u1();
      this.referenceIndex = parser.u2();
    }

    @Override
    String dump() {
      return referenceKind + "." + getCpInfo(referenceIndex, CpInfo.class);
    }
  }

  /**
   * A CONSTANT_MethodType_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.9">JVMS
   * 4.4.9</a>
   */
  private static class ConstantMethodTypeInfo extends CpInfo {
    private final int descriptorIndex;

    /**
     * Parses a CONSTANT_MethodType_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantMethodTypeInfo(final Parser parser, final ClassContext classContext)
            throws IOException {
      super(classContext);
      this.descriptorIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(descriptorIndex, ConstantUtf8Info.class).dump();
    }
  }

  /**
   * A CONSTANT_InvokeDynamic_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.10">JVMS
   * 4.4.10</a>
   */
  private static class ConstantInvokeDynamicInfo extends CpInfo {
    private final int bootstrapMethodAttrIndex;
    private final int nameAndTypeIndex;

    /**
     * Parses a CONSTANT_InvokeDynamic_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantInvokeDynamicInfo(final Parser parser, final ClassContext classContext)
            throws IOException {
      super(classContext);
      this.bootstrapMethodAttrIndex = parser.u2();
      this.nameAndTypeIndex = parser.u2();
    }

    @Override
    String dump() {
      return bootstrapMethodAttrIndex
              + "."
              + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class).dump();
    }
  }

  /**
   * A CONSTANT_Module_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.11">JVMS
   * 4.4.11</a>
   */
  private static class ConstantModuleInfo extends CpInfo {
    private final int descriptorIndex;

    /**
     * Parses a CONSTANT_Module_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantModuleInfo(final Parser parser, final ClassContext classContext) throws IOException {
      super(classContext);
      this.descriptorIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(descriptorIndex, ConstantUtf8Info.class).dump();
    }
  }

  /**
   * A CONSTANT_Package_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.12">JVMS
   * 4.4.12</a>
   */
  private static class ConstantPackageInfo extends CpInfo {
    private final int descriptorIndex;

    /**
     * Parses a CONSTANT_Package_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantPackageInfo(final Parser parser, final ClassContext classContext) throws IOException {
      super(classContext);
      this.descriptorIndex = parser.u2();
    }

    @Override
    String dump() {
      return getCpInfo(descriptorIndex, ConstantUtf8Info.class).dump();
    }
  }

  /**
   * A CONSTANT_Dynamic_info item.
   *
   * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.13">JVMS
   * 4.4.13</a>
   */
  private static class ConstantDynamicInfo extends CpInfo {
    private final int bootstrapMethodAttrIndex;
    private final int nameAndTypeIndex;

    /**
     * Parses a CONSTANT_Dynamic_info item.
     *
     * @param parser a class parser.
     * @param classContext a context to lookup constant pool items from their index.
     * @throws IOException if the class can't be parsed.
     */
    ConstantDynamicInfo(final Parser parser, final ClassContext classContext) throws IOException {
      super(classContext);
      this.bootstrapMethodAttrIndex = parser.u2();
      this.nameAndTypeIndex = parser.u2();
    }

    @Override
    String dump() {
      return bootstrapMethodAttrIndex
              + "."
              + getCpInfo(nameAndTypeIndex, ConstantNameAndTypeInfo.class).dump();
    }
  }

  /**
   * The index of a bytecode instruction. This index is computed in {@link #toString}, from the
   * bytecode offset of the instruction, after the whole class has been parsed. Indeed, due to
   * forward references, the index of an instruction might not be known when its offset is used.
   *
   * <p>Dumps use instruction indices instead of bytecode offsets in order to abstract away the low
   * level byte code instruction representation details (e.g. an ldc vs. an ldc_w).
   */
  private static class InstructionIndex {
    /** An offset in bytes from the start of the bytecode of a method. */
    private final int bytecodeOffset;
    /** The context to use to find the index from the bytecode offset. */
    private final MethodContext methodContext;

    InstructionIndex(final int bytecodeOffset, final MethodContext methodContext) {
      this.bytecodeOffset = bytecodeOffset;
      this.methodContext = methodContext;
    }

    int getBytecodeOffset() {
      return bytecodeOffset;
    }

    @Override
    public String toString() {
      return "<" + methodContext.getInsnIndex(bytecodeOffset) + ">";
    }
  }

  /**
   * A simple byte array parser. The method names reflect the type names used in the Java Virtual
   * Machine Specification for ease of reference.
   */
  private static class Parser {
    private final DataInputStream dataInputStream;

    Parser(final byte[] data) {
      this.dataInputStream = new DataInputStream(new ByteArrayInputStream(data));
    }

    int u1() throws IOException {
      return dataInputStream.readUnsignedByte();
    }

    int s1() throws IOException {
      return dataInputStream.readByte();
    }

    int u2() throws IOException {
      return dataInputStream.readUnsignedShort();
    }

    int s2() throws IOException {
      return dataInputStream.readShort();
    }

    int u4() throws IOException {
      return dataInputStream.readInt();
    }

    long s8() throws IOException {
      long highBytes = dataInputStream.readInt();
      long lowBytes = dataInputStream.readInt() & 0xFFFFFFFFL;
      return (highBytes << 32) | lowBytes;
    }

    String utf8() throws IOException {
      return dataInputStream.readUTF();
    }

    byte[] bytes(final int length) throws IOException {
      if (length > dataInputStream.available()) {
        throw new ClassFormatException("Invalid length: " + length);
      }
      byte[] bytes = new byte[length];
      dataInputStream.readFully(bytes);
      return bytes;
    }
  }

  /** A context to lookup constant pool items from their index. */
  private interface ClassContext {
    <C extends CpInfo> C getCpInfo(int cpIndex, Class<C> cpInfoType);
  }

  /** A context to lookup instruction indices from their bytecode offset. */
  private interface MethodContext {
    int getInsnIndex(int bytecodeOffset);
  }

  /**
   * A helper class to build the dump of a class file. The dump can't be output fully sequentially,
   * as the input class is parsed, in particular due to the re-ordering of attributes and
   * annotations. Instead, a tree is constructed first, then its nodes are sorted and finally the
   * tree is parsed in Depth First Search order to build the dump. This class is the super class of
   * the internal nodes of the tree.
   *
   * <p>Each internal node is a context that can store a mapping between constant pool indices and
   * constant pool items and between bytecode offsets and instructions indices. This can be used to
   * resolve references to such objects. Contexts inherit from their parent, i.e. if a lookup fails
   * in some builder, the lookup continues in the parent, and so on until the root is reached.
   */
  private abstract static class AbstractBuilder<T> implements ClassContext, MethodContext {
    /** Flag used to distinguish CpInfo keys in {@link #context}. */
    private static final int CP_INFO_KEY = 0xF0000000;
    /** The parent node of this node. May be {@literal null}. */
    private final AbstractBuilder<?> parent;
    /** The children of this builder. */
    final ArrayList<T> children;
    /** The map used to implement the Context interfaces. */
    private final HashMap<Integer, Object> context;

    AbstractBuilder(final AbstractBuilder<?> parent) {
      this.parent = parent;
      this.children = new ArrayList<>();
      this.context = new HashMap<>();
    }

    /**
     * Lookup constant pool items from their index.
     *
     * @param cpIndex a constant pool item index.
     * @return the constant pool item at the given index.
     */
    CpInfo getCpInfo(final int cpIndex) {
      return getCpInfo(cpIndex, CpInfo.class);
    }

    @Override
    public <C extends CpInfo> C getCpInfo(final int cpIndex, final Class<C> cpInfoType) {
      Object cpInfo = get(CP_INFO_KEY | cpIndex);
      if (cpInfo == null) {
        throw new ClassFormatException("Invalid constant pool index: " + cpIndex);
      }
      else if (!cpInfoType.isInstance(cpInfo)) {
        throw new ClassFormatException(
                "Invalid constant pool type: "
                        + cpInfo.getClass().getName()
                        + " should be "
                        + cpInfoType.getName());
      }
      return cpInfoType.cast(cpInfo);
    }

    @Override
    public int getInsnIndex(final int bytecodeOffset) {
      Integer insnIndex = (Integer) get(bytecodeOffset);
      if (insnIndex == null) {
        throw new ClassFormatException("Invalid bytecode offset: " + bytecodeOffset);
      }
      return insnIndex;
    }

    /**
     * Registers the CpInfo for the given constant pool index.
     *
     * @param cpIndex a constant pool item index.
     * @param cpInfo a constant pool item.
     */
    void putCpInfo(final int cpIndex, final CpInfo cpInfo) {
      context.put(CP_INFO_KEY | cpIndex, cpInfo);
    }

    /**
     * Registers the instruction index for the given bytecode offset.
     *
     * @param bytecodeOffset a bytecode offset.
     * @param instructionIndex the index of a bytecode instruction.
     */
    void putInsnIndex(final int bytecodeOffset, final int instructionIndex) {
      context.put(bytecodeOffset, instructionIndex);
    }

    /**
     * Recursively appends the builder's children to the given string.
     *
     * @param stringBuilder a string builder.
     */
    void build(final StringBuilder stringBuilder) {
      for (Object child : children) {
        if (child instanceof AbstractBuilder<?>) {
          ((AbstractBuilder<?>) child).build(stringBuilder);
        }
        else {
          stringBuilder.append(child);
        }
      }
    }

    /**
     * Returns the value associated with the given key.
     *
     * @param key a context key.
     * @return the value associated with the given key in this context or, if not found, in the
     * parent context (recursively).
     */
    private Object get(final int key) {
      Object value = context.get(key);
      if (value != null) {
        return value;
      }
      return parent == null ? null : parent.get(key);
    }
  }

  /** An {@link AbstractBuilder} with concrete methods to add children. */
  private static class Builder extends AbstractBuilder<Object> implements Comparable<Builder> {
    /** The name of this builder, for sorting in {@link SortedBuilder}. */
    private String name;

    Builder(final String name, final AbstractBuilder<?> parent) {
      super(parent);
      this.name = name;
    }

    /**
     * Appends name and value to children and returns value.
     *
     * @param <T> a value type.
     * @param name a name.
     * @param value a value.
     * @return value
     */
    <T> T add(final String name, final T value) {
      children.add(name);
      children.add(value);
      children.add("\n");
      return value;
    }

    /**
     * Appends name and the instruction index corresponding to bytecodeOffset to children, and
     * returns bytecodeOffset.
     *
     * @param name a name.
     * @param bytecodeOffset the offset of a bytecode instruction.
     * @return bytecodeOffset.
     */
    int addInsnIndex(final String name, final int bytecodeOffset) {
      add(name, new InstructionIndex(bytecodeOffset, this));
      return bytecodeOffset;
    }

    /**
     * Appends the given arguments to children.
     *
     * @param insnIndex the index of a bytecode instruction.
     * @param opcode a bytecode instruction opcode.
     * @param arguments the bytecode instruction arguments.
     */
    void addInsn(final int insnIndex, final int opcode, final Object... arguments) {
      children.add(insnIndex);
      children.add(": ");
      children.add(opcode);
      for (Object argument : arguments) {
        children.add(" ");
        children.add(argument);
      }
      children.add("\n");
    }

    /**
     * Appends name and the CpInfo corresponding to cpIndex to children.
     *
     * @param name a name.
     * @param cpIndex a constant pool item index.
     */
    void addCpInfo(final String name, final int cpIndex) {
      add(name, cpIndex == 0 ? 0 : getCpInfo(cpIndex));
    }

    /**
     * Appends a new {@link SortedBuilder} to children and returns it.
     *
     * @return a new {@link SortedBuilder}.
     */
    SortedBuilder addSortedBuilder() {
      SortedBuilder sortedBuilder = new SortedBuilder(this);
      children.add(sortedBuilder);
      return sortedBuilder;
    }

    /** Use the content of this builder, instead of its name, to sort it in a SortedBuilder. */
    void sortByContent() {
      StringBuilder stringBuilder = new StringBuilder();
      for (Object child : children) {
        if (child instanceof InstructionIndex) {
          // Instruction index might not be known at this point, use bytecodeOffset instead.
          stringBuilder.append(((InstructionIndex) child).getBytecodeOffset());
        }
        else {
          stringBuilder.append(child.toString());
        }
      }
      name = stringBuilder.toString();
    }

    @Override
    public int compareTo(final Builder builder) {
      return name.compareTo(builder.name);
    }

    @Override
    public boolean equals(final Object other) {
      return (other instanceof Builder) && name.equals(((Builder) other).name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }
  }

  /** An {@link AbstractBuilder} which sorts its children by name before building. */
  private static class SortedBuilder extends AbstractBuilder<Builder> {
    SortedBuilder(final Builder parent) {
      super(parent);
    }

    /**
     * Appends a new {@link Builder} to children and returns it.
     *
     * @param name the name of the new builder.
     * @return the new builder.
     */
    Builder addBuilder(final String name) {
      Builder builder = new Builder(name, this);
      children.add(builder);
      return builder;
    }

    @Override
    void build(final StringBuilder stringBuilder) {
      Collections.sort(children);
      super.build(stringBuilder);
    }
  }

  /** A simple ClassLoader to test that a class can be loaded in the JVM. */
  private static class ByteClassLoader extends ClassLoader {
    private final String className;
    private final byte[] classContent;
    private boolean classLoaded;

    ByteClassLoader(final String className, final byte[] classContent) {
      this.className = className;
      this.classContent = classContent;
    }

    boolean classLoaded() {
      return classLoaded;
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
            throws ClassNotFoundException {
      if (name.equals(className)) {
        classLoaded = true;
        return defineClass(className, classContent, 0, classContent.length);
      }
      else {
        return super.loadClass(name, resolve);
      }
    }
  }
}
