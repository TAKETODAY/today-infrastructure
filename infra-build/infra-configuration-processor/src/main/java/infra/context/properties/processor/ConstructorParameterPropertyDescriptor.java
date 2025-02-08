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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.TypeKindVisitor8;
import javax.tools.Diagnostic.Kind;

/**
 * A {@link PropertyDescriptor} for a constructor parameter.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConstructorParameterPropertyDescriptor extends PropertyDescriptor<VariableElement> {

  ConstructorParameterPropertyDescriptor(TypeElement ownerElement, ExecutableElement factoryMethod,
          VariableElement source, String name, TypeMirror type, VariableElement field, ExecutableElement getter,
          ExecutableElement setter) {
    super(ownerElement, factoryMethod, source, name, type, field, getter, setter);
  }

  @Override
  protected boolean isProperty(MetadataGenerationEnvironment env) {
    // If it's a constructor parameter, it doesn't matter as we must be able to bind
    // it to build the object.
    return !isNested(env);
  }

  @Override
  protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
    Object defaultValue = getDefaultValueFromAnnotation(environment, getSource());
    if (defaultValue != null) {
      return defaultValue;
    }
    return getSource().asType().accept(DefaultPrimitiveTypeVisitor.INSTANCE, null);
  }

  private Object getDefaultValueFromAnnotation(MetadataGenerationEnvironment environment, Element element) {
    AnnotationMirror annotation = environment.getDefaultValueAnnotation(element);
    List<String> defaultValue = getDefaultValue(environment, annotation);
    if (defaultValue != null) {
      try {
        TypeMirror specificType = determineSpecificType(environment);
        if (defaultValue.size() == 1) {
          return coerceValue(specificType, defaultValue.get(0));
        }
        return defaultValue.stream().map((value) -> coerceValue(specificType, value)).toList();
      }
      catch (IllegalArgumentException ex) {
        environment.getMessager().printMessage(Kind.ERROR, ex.getMessage(), element, annotation);
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private List<String> getDefaultValue(MetadataGenerationEnvironment environment, AnnotationMirror annotation) {
    if (annotation == null) {
      return null;
    }
    Map<String, Object> values = environment.getAnnotationElementValues(annotation);
    return (List<String>) values.get("value");
  }

  private TypeMirror determineSpecificType(MetadataGenerationEnvironment environment) {
    TypeMirror candidate = getSource().asType();
    TypeMirror elementCandidate = environment.getTypeUtils().extractElementType(candidate);
    if (elementCandidate != null) {
      candidate = elementCandidate;
    }
    PrimitiveType primitiveType = environment.getTypeUtils().getPrimitiveType(candidate);
    return (primitiveType != null) ? primitiveType : candidate;
  }

  private Object coerceValue(TypeMirror type, String value) {
    Object coercedValue = type.accept(DefaultValueCoercionTypeVisitor.INSTANCE, value);
    return (coercedValue != null) ? coercedValue : value;
  }

  private static final class DefaultValueCoercionTypeVisitor extends TypeKindVisitor8<Object, String> {

    private static final DefaultValueCoercionTypeVisitor INSTANCE = new DefaultValueCoercionTypeVisitor();

    private <T extends Number> T parseNumber(String value, Function<String, T> parser,
            PrimitiveType primitiveType) {
      try {
        return parser.apply(value);
      }
      catch (NumberFormatException ex) {
        throw new IllegalArgumentException(
                String.format("Invalid %s representation '%s'", primitiveType, value));
      }
    }

    @Override
    public Object visitPrimitiveAsBoolean(PrimitiveType t, String value) {
      return Boolean.parseBoolean(value);
    }

    @Override
    public Object visitPrimitiveAsByte(PrimitiveType t, String value) {
      return parseNumber(value, Byte::parseByte, t);
    }

    @Override
    public Object visitPrimitiveAsShort(PrimitiveType t, String value) {
      return parseNumber(value, Short::parseShort, t);
    }

    @Override
    public Object visitPrimitiveAsInt(PrimitiveType t, String value) {
      return parseNumber(value, Integer::parseInt, t);
    }

    @Override
    public Object visitPrimitiveAsLong(PrimitiveType t, String value) {
      return parseNumber(value, Long::parseLong, t);
    }

    @Override
    public Object visitPrimitiveAsChar(PrimitiveType t, String value) {
      if (value.length() > 1) {
        throw new IllegalArgumentException(String.format("Invalid character representation '%s'", value));
      }
      return value;
    }

    @Override
    public Object visitPrimitiveAsFloat(PrimitiveType t, String value) {
      return parseNumber(value, Float::parseFloat, t);
    }

    @Override
    public Object visitPrimitiveAsDouble(PrimitiveType t, String value) {
      return parseNumber(value, Double::parseDouble, t);
    }

  }

  private static final class DefaultPrimitiveTypeVisitor extends TypeKindVisitor8<Object, Void> {

    private static final DefaultPrimitiveTypeVisitor INSTANCE = new DefaultPrimitiveTypeVisitor();

    @Override
    public Object visitPrimitiveAsBoolean(PrimitiveType t, Void ignore) {
      return false;
    }

    @Override
    public Object visitPrimitiveAsByte(PrimitiveType t, Void ignore) {
      return (byte) 0;
    }

    @Override
    public Object visitPrimitiveAsShort(PrimitiveType t, Void ignore) {
      return (short) 0;
    }

    @Override
    public Object visitPrimitiveAsInt(PrimitiveType t, Void ignore) {
      return 0;
    }

    @Override
    public Object visitPrimitiveAsLong(PrimitiveType t, Void ignore) {
      return 0L;
    }

    @Override
    public Object visitPrimitiveAsChar(PrimitiveType t, Void ignore) {
      return null;
    }

    @Override
    public Object visitPrimitiveAsFloat(PrimitiveType t, Void ignore) {
      return 0F;
    }

    @Override
    public Object visitPrimitiveAsDouble(PrimitiveType t, Void ignore) {
      return 0D;
    }

  }

}
