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

package infra.context.properties.processor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;

/**
 * Type Utilities.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TypeUtils {

  private static final Map<TypeKind, Class<?>> PRIMITIVE_WRAPPERS;

  static {
    Map<TypeKind, Class<?>> wrappers = new EnumMap<>(TypeKind.class);
    wrappers.put(TypeKind.BOOLEAN, Boolean.class);
    wrappers.put(TypeKind.BYTE, Byte.class);
    wrappers.put(TypeKind.CHAR, Character.class);
    wrappers.put(TypeKind.DOUBLE, Double.class);
    wrappers.put(TypeKind.FLOAT, Float.class);
    wrappers.put(TypeKind.INT, Integer.class);
    wrappers.put(TypeKind.LONG, Long.class);
    wrappers.put(TypeKind.SHORT, Short.class);
    PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(wrappers);
  }

  private static final Map<String, TypeKind> WRAPPER_TO_PRIMITIVE;

  static {
    Map<String, TypeKind> primitives = new HashMap<>();
    PRIMITIVE_WRAPPERS.forEach((kind, wrapperClass) -> primitives.put(wrapperClass.getName(), kind));
    WRAPPER_TO_PRIMITIVE = primitives;
  }

  private final ProcessingEnvironment env;

  private final Types types;

  private final TypeExtractor typeExtractor;

  private final TypeMirror collectionType;

  private final TypeMirror mapType;

  private final Map<TypeElement, TypeDescriptor> typeDescriptors = new HashMap<>();

  TypeUtils(ProcessingEnvironment env) {
    this.env = env;
    this.types = env.getTypeUtils();
    this.typeExtractor = new TypeExtractor(this.types);
    this.collectionType = getDeclaredType(this.types, Collection.class, 1);
    this.mapType = getDeclaredType(this.types, Map.class, 2);
  }

  private TypeMirror getDeclaredType(Types types, Class<?> typeClass, int numberOfTypeArgs) {
    TypeMirror[] typeArgs = new TypeMirror[numberOfTypeArgs];
    Arrays.setAll(typeArgs, (i) -> types.getWildcardType(null, null));
    TypeElement typeElement = this.env.getElementUtils().getTypeElement(typeClass.getName());
    try {
      return types.getDeclaredType(typeElement, typeArgs);
    }
    catch (IllegalArgumentException ex) {
      // Try again without generics for older Java versions
      return types.getDeclaredType(typeElement);
    }
  }

  boolean isSameType(TypeMirror t1, TypeMirror t2) {
    return this.types.isSameType(t1, t2);
  }

  Element asElement(TypeMirror type) {
    return this.types.asElement(type);
  }

  /**
   * Return the qualified name of the specified element.
   *
   * @param element the element to handle
   * @return the fully qualified name of the element, suitable for a call to
   * {@link Class#forName(String)}
   */
  String getQualifiedName(Element element) {
    return this.typeExtractor.getQualifiedName(element);
  }

  /**
   * Return the type of the specified {@link TypeMirror} including all its generic
   * information.
   *
   * @param element the {@link TypeElement} in which this {@code type} is declared
   * @param type the type to handle
   * @return a representation of the type including all its generic information
   */
  String getType(TypeElement element, TypeMirror type) {
    if (type == null) {
      return null;
    }
    return type.accept(this.typeExtractor, resolveTypeDescriptor(element));
  }

  /**
   * Extract the target element type from the specified container type or {@code null}
   * if no element type was found.
   *
   * @param type a type, potentially wrapping an element type
   * @return the element type or {@code null} if no specific type was found
   */
  TypeMirror extractElementType(TypeMirror type) {
    if (!this.env.getTypeUtils().isAssignable(type, this.collectionType)) {
      return null;
    }
    return getCollectionElementType(type);
  }

  private TypeMirror getCollectionElementType(TypeMirror type) {
    if (((TypeElement) this.types.asElement(type)).getQualifiedName().contentEquals(Collection.class.getName())) {
      DeclaredType declaredType = (DeclaredType) type;
      // raw type, just "Collection"
      if (declaredType.getTypeArguments().isEmpty()) {
        return this.types.getDeclaredType(this.env.getElementUtils().getTypeElement(Object.class.getName()));
      }
      // return type argument to Collection<...>
      return declaredType.getTypeArguments().get(0);
    }

    // recursively walk the supertypes, looking for Collection<...>
    for (TypeMirror superType : this.env.getTypeUtils().directSupertypes(type)) {
      if (this.types.isAssignable(superType, this.collectionType)) {
        return getCollectionElementType(superType);
      }
    }
    return null;
  }

  boolean isCollectionOrMap(TypeMirror type) {
    return this.env.getTypeUtils().isAssignable(type, this.collectionType)
            || this.env.getTypeUtils().isAssignable(type, this.mapType);
  }

  String getJavaDoc(Element element) {
    if (element instanceof RecordComponentElement) {
      return getJavaDoc((RecordComponentElement) element);
    }
    String javadoc = (element != null) ? this.env.getElementUtils().getDocComment(element) : null;
    javadoc = (javadoc != null) ? cleanUpJavaDoc(javadoc) : null;
    return (javadoc == null || javadoc.isEmpty()) ? null : javadoc;
  }

  /**
   * Return the {@link PrimitiveType} of the specified type or {@code null} if the type
   * does not represent a valid wrapper type.
   *
   * @param typeMirror a type
   * @return the primitive type or {@code null} if the type is not a wrapper type
   */
  PrimitiveType getPrimitiveType(TypeMirror typeMirror) {
    if (getPrimitiveFor(typeMirror) != null) {
      return this.types.unboxedType(typeMirror);
    }
    return null;
  }

  TypeMirror getWrapperOrPrimitiveFor(TypeMirror typeMirror) {
    Class<?> candidate = getWrapperFor(typeMirror);
    if (candidate != null) {
      return this.env.getElementUtils().getTypeElement(candidate.getName()).asType();
    }
    TypeKind primitiveKind = getPrimitiveFor(typeMirror);
    if (primitiveKind != null) {
      return this.env.getTypeUtils().getPrimitiveType(primitiveKind);
    }
    return null;
  }

  private Class<?> getWrapperFor(TypeMirror type) {
    return PRIMITIVE_WRAPPERS.get(type.getKind());
  }

  private TypeKind getPrimitiveFor(TypeMirror type) {
    return WRAPPER_TO_PRIMITIVE.get(type.toString());
  }

  private TypeDescriptor resolveTypeDescriptor(TypeElement element) {
    if (this.typeDescriptors.containsKey(element)) {
      return this.typeDescriptors.get(element);
    }
    return createTypeDescriptor(element);
  }

  private TypeDescriptor createTypeDescriptor(TypeElement element) {
    TypeDescriptor descriptor = new TypeDescriptor();
    process(descriptor, element.asType());
    this.typeDescriptors.put(element, descriptor);
    return descriptor;
  }

  private void process(TypeDescriptor descriptor, TypeMirror type) {
    if (type.getKind() == TypeKind.DECLARED) {
      DeclaredType declaredType = (DeclaredType) type;
      DeclaredType freshType = (DeclaredType) this.env.getElementUtils()
              .getTypeElement(this.types.asElement(type).toString())
              .asType();
      List<? extends TypeMirror> arguments = declaredType.getTypeArguments();
      for (int i = 0; i < arguments.size(); i++) {
        TypeMirror specificType = arguments.get(i);
        TypeMirror signatureType = freshType.getTypeArguments().get(i);
        descriptor.registerIfNecessary(signatureType, specificType);
      }
      TypeElement element = (TypeElement) this.types.asElement(type);
      process(descriptor, element.getSuperclass());
    }
  }

  private String getJavaDoc(RecordComponentElement recordComponent) {
    String recordJavadoc = this.env.getElementUtils().getDocComment(recordComponent.getEnclosingElement());
    if (recordJavadoc != null) {
      Pattern paramJavadocPattern = paramJavadocPattern(recordComponent.getSimpleName().toString());
      Matcher paramJavadocMatcher = paramJavadocPattern.matcher(recordJavadoc);
      if (paramJavadocMatcher.find()) {
        String paramJavadoc = cleanUpJavaDoc(paramJavadocMatcher.group());
        return paramJavadoc.isEmpty() ? null : paramJavadoc;
      }
    }
    return null;
  }

  private Pattern paramJavadocPattern(String paramName) {
    String pattern = String.format("(?<=@param +%s).*?(?=([\r\n]+ *@)|$)", paramName);
    return Pattern.compile(pattern, Pattern.DOTALL);
  }

  private String cleanUpJavaDoc(String javadoc) {
    StringBuilder result = new StringBuilder(javadoc.length());
    char lastChar = '.';
    for (int i = 0; i < javadoc.length(); i++) {
      char ch = javadoc.charAt(i);
      boolean repeatedSpace = ch == ' ' && lastChar == ' ';
      if (ch != '\r' && ch != '\n' && !repeatedSpace) {
        result.append(ch);
        lastChar = ch;
      }
    }
    return result.toString().trim();
  }

  /**
   * A visitor that extracts the fully qualified name of a type, including generic
   * information.
   */
  private static class TypeExtractor extends SimpleTypeVisitor8<String, TypeDescriptor> {

    private final Types types;

    TypeExtractor(Types types) {
      this.types = types;
    }

    @Override
    public String visitDeclared(DeclaredType type, TypeDescriptor descriptor) {
      TypeElement enclosingElement = getEnclosingTypeElement(type);
      String qualifiedName = determineQualifiedName(type, enclosingElement);
      if (type.getTypeArguments().isEmpty()) {
        return qualifiedName;
      }
      StringBuilder name = new StringBuilder();
      name.append(qualifiedName);
      name.append("<")
              .append(type.getTypeArguments()
                      .stream()
                      .map((t) -> visit(t, descriptor))
                      .collect(Collectors.joining(",")))
              .append(">");
      return name.toString();
    }

    private String determineQualifiedName(DeclaredType type, TypeElement enclosingElement) {
      if (enclosingElement != null) {
        return getQualifiedName(enclosingElement) + "$" + type.asElement().getSimpleName();
      }
      return getQualifiedName(type.asElement());
    }

    @Override
    public String visitTypeVariable(TypeVariable typeVariable, TypeDescriptor descriptor) {
      TypeMirror resolvedGeneric = descriptor.resolveGeneric(typeVariable);
      if (resolvedGeneric != null) {
        if (resolvedGeneric instanceof TypeVariable resolveTypeVariable) {
          // Still unresolved, let's use the upper bound, checking first if
          // a cycle may exist
          if (!hasCycle(resolveTypeVariable)) {
            return visit(resolveTypeVariable.getUpperBound(), descriptor);
          }
        }
        else {
          return visit(resolvedGeneric, descriptor);
        }
      }
      // Fallback to simple representation of the upper bound
      return defaultAction(typeVariable.getUpperBound(), descriptor);
    }

    private boolean hasCycle(TypeVariable variable) {
      TypeMirror upperBound = variable.getUpperBound();
      if (upperBound instanceof DeclaredType declaredType) {
        return declaredType.getTypeArguments().stream().anyMatch((candidate) -> candidate.equals(variable));
      }
      return false;
    }

    @Override
    public String visitArray(ArrayType t, TypeDescriptor descriptor) {
      return t.getComponentType().accept(this, descriptor) + "[]";
    }

    @Override
    public String visitPrimitive(PrimitiveType t, TypeDescriptor descriptor) {
      return this.types.boxedClass(t).getQualifiedName().toString();
    }

    @Override
    protected String defaultAction(TypeMirror t, TypeDescriptor descriptor) {
      return t.toString();
    }

    String getQualifiedName(Element element) {
      if (element == null) {
        return null;
      }
      TypeElement enclosingElement = getEnclosingTypeElement(element.asType());
      if (enclosingElement != null) {
        return getQualifiedName(enclosingElement) + "$"
                + ((DeclaredType) element.asType()).asElement().getSimpleName();
      }
      if (element instanceof TypeElement typeElement) {
        return typeElement.getQualifiedName().toString();
      }
      throw new IllegalStateException("Could not extract qualified name from " + element);
    }

    private TypeElement getEnclosingTypeElement(TypeMirror type) {
      if (type instanceof DeclaredType declaredType) {
        Element enclosingElement = declaredType.asElement().getEnclosingElement();
        if (enclosingElement instanceof TypeElement typeElement) {
          return typeElement;
        }
      }
      return null;
    }

  }

  /**
   * Descriptor for a given type.
   */
  static class TypeDescriptor {

    private final Map<TypeVariable, TypeMirror> generics = new HashMap<>();

    TypeMirror resolveGeneric(TypeVariable typeVariable) {
      TypeMirror resolved = this.generics.get(typeVariable);
      if (resolved != typeVariable && resolved instanceof TypeVariable resolvedTypeVariable) {
        return resolveGeneric(resolvedTypeVariable);
      }
      return resolved;
    }

    private void registerIfNecessary(TypeMirror variable, TypeMirror resolution) {
      if (variable instanceof TypeVariable typeVariable) {
        this.generics.put(typeVariable, resolution);
      }
    }

  }

}
