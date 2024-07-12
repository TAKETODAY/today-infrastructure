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

package cn.taketoday.bytecode.commons;

import cn.taketoday.bytecode.ClassValueHolder;
import cn.taketoday.bytecode.ConstantDynamic;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.signature.SignatureReader;
import cn.taketoday.bytecode.signature.SignatureVisitor;
import cn.taketoday.bytecode.signature.SignatureWriter;

/**
 * A class responsible for remapping types and names.
 *
 * @author Eugene Kuleshov
 */
public abstract class Remapper {

  /**
   * Returns the given descriptor, remapped with {@link #map(String)}.
   *
   * @param descriptor a type descriptor.
   * @return the given descriptor, with its [array element type] internal name remapped with {@link
   * #map(String)} (if the descriptor corresponds to an array or object type, otherwise the
   * descriptor is returned as is).
   */
  public String mapDesc(final String descriptor) {
    return mapType(Type.forDescriptor(descriptor)).getDescriptor();
  }

  /**
   * Returns the given {@link Type}, remapped with {@link #map(String)} or {@link
   * #mapMethodDesc(String)}.
   *
   * @param type a type, which can be a method type.
   * @return the given type, with its [array element type] internal name remapped with {@link
   * #map(String)} (if the type is an array or object type, otherwise the type is returned as
   * is) or, of the type is a method type, with its descriptor remapped with {@link
   * #mapMethodDesc(String)}.
   */
  private Type mapType(final Type type) {
    switch (type.getSort()) {
      case Type.ARRAY:
        String remappedDescriptor = "[".repeat(Math.max(0, type.getDimensions())) + mapType(type.getElementType()).getDescriptor();
        return Type.forDescriptor(remappedDescriptor);
      case Type.OBJECT:
        String remappedInternalName = map(type.getInternalName());
        return remappedInternalName != null ? Type.forInternalName(remappedInternalName) : type;
      case Type.METHOD:
        return Type.forMethod(mapMethodDesc(type.getDescriptor()));
      default:
        return type;
    }
  }

  /**
   * Returns the given internal name, remapped with {@link #map(String)}.
   *
   * @param internalName the internal name (or array type descriptor) of some (array) class.
   * @return the given internal name, remapped with {@link #map(String)}.
   */
  public String mapType(final String internalName) {
    if (internalName == null) {
      return null;
    }
    return mapType(Type.forInternalName(internalName)).getInternalName();
  }

  /**
   * Returns the given internal names, remapped with {@link #map(String)}.
   *
   * @param internalNames the internal names (or array type descriptors) of some (array) classes.
   * @return the given internal name, remapped with {@link #map(String)}.
   */
  public String[] mapTypes(final String[] internalNames) {
    String[] remappedInternalNames = null;
    for (int i = 0; i < internalNames.length; ++i) {
      String internalName = internalNames[i];
      String remappedInternalName = mapType(internalName);
      if (remappedInternalName != null) {
        if (remappedInternalNames == null) {
          remappedInternalNames = internalNames.clone();
        }
        remappedInternalNames[i] = remappedInternalName;
      }
    }
    return remappedInternalNames != null ? remappedInternalNames : internalNames;
  }

  /**
   * Returns the given method descriptor, with its argument and return type descriptors remapped
   * with {@link #mapDesc(String)}.
   *
   * @param methodDescriptor a method descriptor.
   * @return the given method descriptor, with its argument and return type descriptors remapped
   * with {@link #mapDesc(String)}.
   */
  public String mapMethodDesc(final String methodDescriptor) {
    if ("()V".equals(methodDescriptor)) {
      return methodDescriptor;
    }

    StringBuilder stringBuilder = new StringBuilder("(");
    for (Type argumentType : Type.forArgumentTypes(methodDescriptor)) {
      stringBuilder.append(mapType(argumentType).getDescriptor());
    }
    Type returnType = Type.forReturnType(methodDescriptor);
    if (returnType == Type.VOID_TYPE) {
      stringBuilder.append(")V");
    }
    else {
      stringBuilder.append(')').append(mapType(returnType).getDescriptor());
    }
    return stringBuilder.toString();
  }

  /**
   * Returns the given value, remapped with this remapper. Possible values are {@link Boolean},
   * {@link Byte}, {@link Short}, {@link Character}, {@link Integer}, {@link Long}, {@link Double},
   * {@link Float}, {@link String}, {@link Type}, {@link Handle}, {@link ConstantDynamic} or arrays
   * of primitive types .
   *
   * @param value an object. Only {@link Type}, {@link Handle} and {@link ConstantDynamic} values
   * are remapped.
   * @return the given value, remapped with this remapper.
   */
  public Object mapValue(final Object value) {
    if (value instanceof Type) {
      return mapType((Type) value);
    }
    if (value instanceof ClassValueHolder) {
      return mapType(((ClassValueHolder) value).getDescriptor());
    }
    if (value instanceof Handle handle) {
      boolean isFieldHandle = handle.getTag() <= Opcodes.H_PUTSTATIC;
      return new Handle(
              handle.getTag(),
              mapType(handle.getOwner()),
              isFieldHandle ? mapFieldName(handle.getOwner(), handle.getName(), handle.getDesc())
                            : mapMethodName(handle.getOwner(), handle.getName(), handle.getDesc()),
              isFieldHandle ? mapDesc(handle.getDesc())
                            : mapMethodDesc(handle.getDesc()),
              handle.isInterface());
    }
    if (value instanceof ConstantDynamic constantDynamic) {
      int bootstrapMethodArgumentCount = constantDynamic.getBootstrapMethodArgumentCount();
      Object[] remappedBootstrapMethodArguments = new Object[bootstrapMethodArgumentCount];
      for (int i = 0; i < bootstrapMethodArgumentCount; ++i) {
        remappedBootstrapMethodArguments[i] =
                mapValue(constantDynamic.getBootstrapMethodArgument(i));
      }
      String descriptor = constantDynamic.getDescriptor();
      return new ConstantDynamic(
              mapInvokeDynamicMethodName(constantDynamic.getName(), descriptor),
              mapDesc(descriptor),
              (Handle) mapValue(constantDynamic.getBootstrapMethod()),
              remappedBootstrapMethodArguments);
    }
    return value;
  }

  /**
   * Returns the given signature, remapped with the {@link SignatureVisitor} returned by {@link
   * #createSignatureRemapper(SignatureVisitor)}.
   *
   * @param signature a <i>JavaTypeSignature</i>, <i>ClassSignature</i> or <i>MethodSignature</i>.
   * @param typeSignature whether the given signature is a <i>JavaTypeSignature</i>.
   * @return signature the given signature, remapped with the {@link SignatureVisitor} returned by
   * {@link #createSignatureRemapper(SignatureVisitor)}.
   */
  public String mapSignature(final String signature, final boolean typeSignature) {
    if (signature == null) {
      return null;
    }
    SignatureReader signatureReader = new SignatureReader(signature);
    SignatureWriter signatureWriter = new SignatureWriter();
    SignatureVisitor signatureRemapper = createSignatureRemapper(signatureWriter);
    if (typeSignature) {
      signatureReader.acceptType(signatureRemapper);
    }
    else {
      signatureReader.accept(signatureRemapper);
    }
    return signatureWriter.toString();
  }

  /**
   * Constructs a new remapper for signatures. The default implementation of this method returns a
   * new {@link SignatureRemapper}.
   *
   * @param signatureVisitor the SignatureVisitor the remapper must delegate to.
   * @return the newly created remapper.
   */
  protected SignatureVisitor createSignatureRemapper(final SignatureVisitor signatureVisitor) {
    return new SignatureRemapper(signatureVisitor, this);
  }

  /**
   * Maps an annotation attribute name. The default implementation of this method returns the given
   * name, unchanged. Subclasses can override.
   *
   * @param descriptor the descriptor of the annotation class.
   * @param name the name of the annotation attribute.
   * @return the new name of the annotation attribute.
   */
  public String mapAnnotationAttributeName(final String descriptor, final String name) {
    return name;
  }

  /**
   * Maps an inner class name to its new name. The default implementation of this method provides a
   * strategy that will work for inner classes produced by Java, but not necessarily other
   * languages. Subclasses can override.
   *
   * @param name the fully-qualified internal name of the inner class.
   * @param ownerName the internal name of the owner class of the inner class.
   * @param innerName the internal name of the inner class.
   * @return the new inner name of the inner class.
   */
  public String mapInnerClassName(
          final String name, final String ownerName, final String innerName) {
    final String remappedInnerName = this.mapType(name);
    if (remappedInnerName.contains("$")) {
      int index = remappedInnerName.lastIndexOf('$') + 1;
      while (index < remappedInnerName.length()
              && Character.isDigit(remappedInnerName.charAt(index))) {
        index++;
      }
      return remappedInnerName.substring(index);
    }
    else {
      return innerName;
    }
  }

  /**
   * Maps a method name to its new name. The default implementation of this method returns the given
   * name, unchanged. Subclasses can override.
   *
   * @param owner the internal name of the owner class of the method.
   * @param name the name of the method.
   * @param descriptor the descriptor of the method.
   * @return the new name of the method.
   */
  public String mapMethodName(final String owner, final String name, final String descriptor) {
    return name;
  }

  /**
   * Maps an invokedynamic or a constant dynamic method name to its new name. The default
   * implementation of this method returns the given name, unchanged. Subclasses can override.
   *
   * @param name the name of the method.
   * @param descriptor the descriptor of the method.
   * @return the new name of the method.
   */
  public String mapInvokeDynamicMethodName(final String name, final String descriptor) {
    return name;
  }

  /**
   * Maps a record component name to its new name. The default implementation of this method returns
   * the given name, unchanged. Subclasses can override.
   *
   * @param owner the internal name of the owner class of the field.
   * @param name the name of the field.
   * @param descriptor the descriptor of the field.
   * @return the new name of the field.
   */
  public String mapRecordComponentName(
          final String owner, final String name, final String descriptor) {
    return name;
  }

  /**
   * Maps a field name to its new name. The default implementation of this method returns the given
   * name, unchanged. Subclasses can override.
   *
   * @param owner the internal name of the owner class of the field.
   * @param name the name of the field.
   * @param descriptor the descriptor of the field.
   * @return the new name of the field.
   */
  public String mapFieldName(final String owner, final String name, final String descriptor) {
    return name;
  }

  /**
   * Maps a package name to its new name. The default implementation of this method returns the
   * given name, unchanged. Subclasses can override.
   *
   * @param name the fully qualified name of the package (using dots).
   * @return the new name of the package.
   */
  public String mapPackageName(final String name) {
    return name;
  }

  /**
   * Maps a module name to its new name. The default implementation of this method returns the given
   * name, unchanged. Subclasses can override.
   *
   * @param name the fully qualified name (using dots) of a module.
   * @return the new name of the module.
   */
  public String mapModuleName(final String name) {
    return name;
  }

  /**
   * Maps the internal name of a class to its new name. The default implementation of this method
   * returns the given name, unchanged. Subclasses can override.
   *
   * @param internalName the internal name of a class.
   * @return the new internal name.
   */
  public String map(final String internalName) {
    return internalName;
  }
}
