/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.properties.processor.fieldvalues.javac;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import infra.context.properties.processor.fieldvalues.FieldValuesParser;
import infra.context.properties.processor.fieldvalues.javac.ExpressionTree.Member;
import infra.context.properties.processor.support.ConventionUtils;

/**
 * {@link FieldValuesParser} implementation for the standard Java compiler.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JavaCompilerFieldValuesParser implements FieldValuesParser {

  private final Trees trees;

  public JavaCompilerFieldValuesParser(ProcessingEnvironment env) throws Exception {
    this.trees = Trees.instance(env);
  }

  @Override
  public Map<String, Object> getFieldValues(TypeElement element) throws Exception {
    Tree tree = this.trees.getTree(element);
    if (tree != null) {
      FieldCollector fieldCollector = new FieldCollector();
      tree.accept(fieldCollector);
      return fieldCollector.getFieldValues();
    }
    return Collections.emptyMap();
  }

  /**
   * {@link TreeVisitor} to collect fields.
   */
  private static final class FieldCollector implements TreeVisitor {

    private static final Map<String, Class<?>> WRAPPER_TYPES = Map.of(
            "boolean", Boolean.class,
            Boolean.class.getName(), Boolean.class,
            "byte", Byte.class,
            Byte.class.getName(), Byte.class,
            "short", Short.class,
            Short.class.getName(), Short.class,
            "int", Integer.class,
            Integer.class.getName(), Integer.class,
            "long", Long.class,
            Long.class.getName(), Long.class
    );

    private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

    static {
      Map<Class<?>, Object> values = new HashMap<>();
      values.put(Boolean.class, false);
      values.put(Byte.class, (byte) 0);
      values.put(Short.class, (short) 0);
      values.put(Integer.class, 0);
      values.put(Long.class, (long) 0);
      DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
    }

    private static final Map<String, Object> WELL_KNOWN_STATIC_FINALS = Map.of(
            "Boolean.TRUE", true,
            "Boolean.FALSE", false,
            "StandardCharsets.ISO_8859_1", "ISO-8859-1",
            "StandardCharsets.UTF_8", "UTF-8",
            "StandardCharsets.UTF_16", "UTF-16",
            "StandardCharsets.US_ASCII", "US-ASCII",
            "Duration.ZERO", 0,
            "Period.ZERO", 0
    );

    private static final String DURATION_OF = "Duration.of";

    private static final Map<String, String> DURATION_SUFFIX = Map.of(
            "Nanos", "ns",
            "Millis", "ms",
            "Seconds", "s",
            "Minutes", "m",
            "Hours", "h",
            "Days", "d"
    );

    private static final String PERIOD_OF = "Period.of";

    private static final Map<String, String> PERIOD_SUFFIX = Map.of(
            "Days", "d",
            "Weeks", "w",
            "Months", "m",
            "Years", "y"
    );

    private static final String DATA_SIZE_OF = "DataSize.of";

    private static final Map<String, String> DATA_SIZE_SUFFIX = Map.of(
            "Bytes", "B",
            "Kilobytes", "KB",
            "Megabytes", "MB",
            "Gigabytes", "GB",
            "Terabytes", "TB"
    );

    private final Map<String, Object> fieldValues = new HashMap<>();

    private final Map<String, Object> staticFinals = new HashMap<>();

    @Override
    public void visitVariable(VariableTree variable) throws Exception {
      Set<Modifier> flags = variable.getModifierFlags();
      if (flags.contains(Modifier.STATIC) && flags.contains(Modifier.FINAL)) {
        this.staticFinals.put(variable.getName(), getValue(variable));
      }
      if (!flags.contains(Modifier.FINAL)) {
        this.fieldValues.put(variable.getName(), getValue(variable));
      }
    }

    private Object getValue(VariableTree variable) throws Exception {
      ExpressionTree initializer = variable.getInitializer();
      Class<?> wrapperType = WRAPPER_TYPES.get(variable.getType());
      Object defaultValue = DEFAULT_TYPE_VALUES.get(wrapperType);
      if (initializer != null) {
        return getValue(variable.getType(), initializer, defaultValue);
      }
      return defaultValue;
    }

    private Object getValue(String variableType, ExpressionTree expression, Object defaultValue) throws Exception {
      Object literalValue = expression.getLiteralValue();
      if (literalValue != null) {
        return literalValue;
      }
      Object factoryValue = expression.getFactoryValue();
      if (factoryValue != null) {
        return getFactoryValue(expression, factoryValue);
      }
      List<? extends ExpressionTree> arrayValues = expression.getArrayExpression();
      if (arrayValues != null) {
        Object[] result = new Object[arrayValues.size()];
        for (int i = 0; i < arrayValues.size(); i++) {
          Object value = getValue(variableType, arrayValues.get(i), null);
          if (value == null) { // One of the elements could not be resolved
            return defaultValue;
          }
          result[i] = value;
        }
        return result;
      }
      if (expression.getKind().equals("IDENTIFIER")) {
        return this.staticFinals.get(expression.toString());
      }
      if (expression.getKind().equals("MEMBER_SELECT")) {
        Object value = WELL_KNOWN_STATIC_FINALS.get(expression.toString());
        if (value != null) {
          return value;
        }
        Member selectedMember = expression.getSelectedMember();
        // Type matching the expression, assuming an enum
        if (selectedMember != null && selectedMember.expression().equals(variableType)) {
          return ConventionUtils.toDashedCase(selectedMember.identifier().toLowerCase(Locale.ENGLISH));
        }
        return null;
      }
      return null;
    }

    private Object getFactoryValue(ExpressionTree expression, Object factoryValue) {
      Object durationValue = getFactoryValue(expression, factoryValue, DURATION_OF, DURATION_SUFFIX);
      if (durationValue != null) {
        return durationValue;
      }
      Object dataSizeValue = getFactoryValue(expression, factoryValue, DATA_SIZE_OF, DATA_SIZE_SUFFIX);
      if (dataSizeValue != null) {
        return dataSizeValue;
      }
      Object periodValue = getFactoryValue(expression, factoryValue, PERIOD_OF, PERIOD_SUFFIX);
      if (periodValue != null) {
        return periodValue;
      }
      return factoryValue;
    }

    private Object getFactoryValue(ExpressionTree expression, Object factoryValue, String prefix,
            Map<String, String> suffixMapping) {
      Object instance = expression.getInstance();
      if (instance != null && instance.toString().startsWith(prefix)) {
        String type = instance.toString();
        type = type.substring(prefix.length(), type.indexOf('('));
        String suffix = suffixMapping.get(type);
        return (suffix != null) ? factoryValue + suffix : null;
      }
      return null;
    }

    Map<String, Object> getFieldValues() {
      return this.fieldValues;
    }

  }

}
