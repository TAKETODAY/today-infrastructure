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

package infra.bytecode.commons;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

import infra.bytecode.ClassValueHolder;
import infra.bytecode.ConstantDynamic;
import infra.bytecode.Handle;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.signature.SignatureReader;
import infra.bytecode.signature.SignatureVisitor;
import infra.bytecode.signature.SignatureWriter;
import infra.lang.Contract;

/**
 * A class responsible for remapping types and names.
 *
 * @author Eugene Kuleshov
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public abstract class Remapper {

  // The class name of LambdaMetafactory.
  private static final String LAMBDA_FACTORY_CLASSNAME = "java/lang/invoke/LambdaMetafactory";

  // The method signature of LambdaMetafactory.metafactory(...).
  private static final String LAMBDA_FACTORY_METAFACTORY =
          "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                  + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;"
                  + "Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";

  // The method signature of LambdaMetafactory.altMetafactory(...).
  private static final String LAMBDA_FACTORY_ALTMETAFACTORY =
          "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                  + "[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;";

  /**
   * Creates a new {@link Remapper}.
   */
  protected Remapper() {
  }

  /**
   * Returns the given descriptor, remapped with {@link #map(String)}.
   *
   * @param descriptor a type descriptor.
   * @return the given descriptor, with its [array element type] internal name remapped with {@link
   * #map(String)} (if the descriptor corresponds to an array or object type, otherwise the
   * descriptor is returned as is). See {@link Type#getInternalName()}.
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
   * #mapMethodDesc(String)}. See {@link Type#getInternalName()}.
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
   * @param internalName the internal name (or array type descriptor) of some (array) class (see
   * {@link Type#getInternalName()}).
   * @return the given internal name, remapped with {@link #map(String)} (see {@link
   * Type#getInternalName()}).
   */
  @Contract("null -> null")
  public @Nullable String mapType(final @Nullable String internalName) {
    if (internalName == null) {
      return null;
    }
    return mapType(Type.forInternalName(internalName)).getInternalName();
  }

  /**
   * Returns the given internal names, remapped with {@link #map(String)}.
   *
   * @param internalNames the internal names (or array type descriptors) of some (array) classes
   * (see {@link Type#getInternalName()}).
   * @return the given internal name, remapped with {@link #map(String)} (see {@link
   * Type#getInternalName()}).
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
      String name = constantDynamic.getName();
      String descriptor = constantDynamic.getDescriptor();
      Handle bootstrapMethod = constantDynamic.getBootstrapMethod();
      int bootstrapMethodArgumentCount = constantDynamic.getBootstrapMethodArgumentCount();
      Object[] bootstrapMethodArguments = new Object[bootstrapMethodArgumentCount];
      Object[] remappedBootstrapMethodArguments = new Object[bootstrapMethodArgumentCount];
      for (int i = 0; i < bootstrapMethodArgumentCount; ++i) {
        bootstrapMethodArguments[i] = constantDynamic.getBootstrapMethodArgument(i);
        remappedBootstrapMethodArguments[i] = mapValue(bootstrapMethodArguments[i]);
      }

      name = mapInvokeDynamicMethodName(name, descriptor, bootstrapMethod, bootstrapMethodArguments);
      return new ConstantDynamic(
              name,
              mapDesc(descriptor),
              (Handle) mapValue(bootstrapMethod),
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
  @Contract("null, _ -> null")
  public @Nullable String mapSignature(final @Nullable String signature, final boolean typeSignature) {
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
   * @deprecated use {@link #createSignatureRemapper} instead.
   */
  @Deprecated(forRemoval = false)
  protected SignatureVisitor createRemappingSignatureAdapter(
          final SignatureVisitor signatureVisitor) {
    return createSignatureRemapper(signatureVisitor);
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
   * @param name the fully-qualified internal name of the inner class (see {@link
   * Type#getInternalName()}).
   * @param ownerName the internal name of the owner class of the inner class (see {@link
   * Type#getInternalName()}).
   * @param innerName the internal name of the inner class (see {@link Type#getInternalName()}).
   * @return the new inner name of the inner class.
   */
  public String mapInnerClassName(
          final String name, final String ownerName, final String innerName) {
    final String remappedInnerName = this.mapType(name);

    if (Objects.equals(remappedInnerName, name)) {
      return innerName;
    }
    else {
      int originSplit = name.lastIndexOf('/');
      int remappedSplit = remappedInnerName.lastIndexOf('/');
      if (originSplit != -1
              && remappedSplit != -1
              && name.substring(originSplit).equals(remappedInnerName.substring(remappedSplit))) {
        // class name not changed
        return innerName;
      }
    }

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
   * @param owner the internal name of the owner class of the method (see {@link
   * Type#getInternalName()}).
   * @param name the name of the method.
   * @param descriptor the descriptor of the method.
   * @return the new name of the method.
   */
  public String mapMethodName(final String owner, final String name, final String descriptor) {
    return name;
  }

  /**
   * Maps an invokedynamic or a constant dynamic method name to its new name. Subclasses can
   * override.
   *
   * <p>The default implementation of this method first performs well-known rule checks (calling
   * {@link #mapWellKnownInvokeDynamicMethodName(String, String, Handle, Object...)}) and then
   * performs basic remapping (calling {@link #mapBasicInvokeDynamicMethodName(String, String,
   * Handle, Object...)}).
   *
   * <p>For most users, only {@link #mapBasicInvokeDynamicMethodName(String, String, Handle,
   * Object...)} needs to be overridden.
   *
   * @param name the name of the method.
   * @param descriptor the descriptor of the method.
   * @param bootstrapMethodHandle the bootstrap method.
   * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
   * an {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link
   * Type}, {@link Handle} or {@link ConstantDynamic} value. This method is allowed to modify
   * the content of the array so a caller should expect that this array may change.
   * @return the new name of the method.
   */
  public String mapInvokeDynamicMethodName(final String name, final String descriptor,
          final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
    String mappedWellKnownName = mapWellKnownInvokeDynamicMethodName(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    if (mappedWellKnownName != null) {
      return mappedWellKnownName;
    }
    return mapBasicInvokeDynamicMethodName(
            name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
  }

  /**
   * Maps well-known invokedynamic (e.g. lambda creation) or const dynamic method names to their new
   * names. This method detects specific invokedynamic method rules and remaps using the
   * corresponding rules. When no rule is matched, returns {@literal null}. When non-null is
   * returned, it means that this invokedynamic method name matches a rule and has been remapped
   * with the relevant rule. Subclasses can override.
   *
   * @param name the name of the method.
   * @param descriptor the descriptor of the method.
   * @param bootstrapMethodHandle the bootstrap method.
   * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
   * an {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link
   * Type}, {@link Handle} or {@link ConstantDynamic} value. This method is allowed to modify
   * the content of the array so a caller should expect that this array may change.
   * @return the new name of the method, or null if no special rule is matched.
   */
  public @Nullable String mapWellKnownInvokeDynamicMethodName(final String name, final String descriptor,
          final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {

    if (LAMBDA_FACTORY_CLASSNAME.equals(bootstrapMethodHandle.getOwner())
            && bootstrapMethodHandle.getTag() == Opcodes.H_INVOKESTATIC) {
      // This is a lambda creation.
      // Note: **if** is reserved for future JDK changes.
      boolean isMetafactory = false;
      isMetafactory |= "metafactory".equals(bootstrapMethodHandle.getName())
              && LAMBDA_FACTORY_METAFACTORY.equals(bootstrapMethodHandle.getDesc());
      isMetafactory |= "altMetafactory".equals(bootstrapMethodHandle.getName())
              && LAMBDA_FACTORY_ALTMETAFACTORY.equals(bootstrapMethodHandle.getDesc());

      if (isMetafactory) {
        // Note:
        // Java lambda instances are created by LambdaMetafactory.metafactory() and
        // LambdaMetafactory.altMetafactory().
        // The specification can be found in the LambdaMetafactory javadoc:
        // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/invoke/LambdaMetafactory.html
        //
        // In short, all the necessary parameters can be obtained from this invokedynamic, including
        // the following three:
        // - Class name: From return type of method descriptor.
        // - Method name: Same as the name of invokedynamic.
        // - Method descriptor: From the first bootstrap argument.
        return mapMethodName(
                Type.forReturnType(descriptor).getInternalName(),
                name,
                bootstrapMethodArguments[0].toString());
      }
    }

    return null;
  }

  /**
   * Maps an invokedynamic or a constant dynamic method name to its new name. The default
   * implementation of this method returns the given name, unchanged. Subclasses can override.
   *
   * @param name the name of the method.
   * @param descriptor the descriptor of the method.
   * @param bootstrapMethodHandle the bootstrap method.
   * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
   * an {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link
   * Type}, {@link Handle} or {@link ConstantDynamic} value. This method is allowed to modify
   * the content of the array so a caller should expect that this array may change.
   * @return the new name of the method.
   */
  public String mapBasicInvokeDynamicMethodName(final String name, final String descriptor,
          final Handle bootstrapMethodHandle, final Object... bootstrapMethodArguments) {
    return name;
  }

  /**
   * Maps a record component name to its new name. The default implementation of this method returns
   * the given name, unchanged. Subclasses can override.
   *
   * @param owner the internal name of the owner class of the field (see {@link
   * Type#getInternalName()}).
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
   * @param owner the internal name of the owner class of the field (see {@link
   * Type#getInternalName()}).
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
   * @param internalName the internal name of a class (see {@link Type#getInternalName()}).
   * @return the new internal name (see {@link Type#getInternalName()}).
   */
  public String map(final String internalName) {
    return internalName;
  }
}
