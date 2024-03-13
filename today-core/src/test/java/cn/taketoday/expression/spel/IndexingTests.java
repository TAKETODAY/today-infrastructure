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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.PropertyAccessor;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static cn.taketoday.expression.spel.SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE;
import static cn.taketoday.expression.spel.SpelMessage.UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SuppressWarnings("rawtypes")
class IndexingTests {

  @Test
  void indexIntoArrays() {
    SpelExpressionParser parser = new SpelExpressionParser();

    // One-dimensional
    this.property = new int[] { 1, 2, 3, 4 };
    Expression expression = parser.parseExpression("property[2]");
    assertThat(expression.getValue(this)).isEqualTo(3);

    // Multi-dimensional
    this.property = new int[][] { { 1, 2 }, { 3, 4 } };
    expression = parser.parseExpression("property[0]");
    assertThat(expression.getValue(this)).isEqualTo(new int[] { 1, 2 });
    expression = parser.parseExpression("property[1][1]");
    assertThat(expression.getValue(this)).isEqualTo(4);
  }

  @Test
  @SuppressWarnings("unchecked")
  void indexIntoGenericPropertyContainingMap() {
    Map<String, String> property = new HashMap<>();
    property.put("foo", "bar");
    this.property = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.HashMap<?, ?>", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this)).isEqualTo(property);
    assertThat(expression.getValue(this, Map.class)).isEqualTo(property);
    expression = parser.parseExpression("property['foo']");
    assertThat(expression.getValue(this)).isEqualTo("bar");
  }

  @Test
  @SuppressWarnings("unchecked")
  void indexIntoGenericPropertyContainingMapObject() {
    Map<String, Map<String, String>> property = new HashMap<>();
    Map<String, String> map = new HashMap<>();
    map.put("foo", "bar");
    property.put("property", map);
    SpelExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();
    context.addPropertyAccessor(new MapAccessor());
    context.setRootObject(property);
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(context)).asString()
            .isEqualTo("java.util.HashMap<?, ?>");
    assertThat(expression.getValue(context)).isEqualTo(map);
    assertThat(expression.getValue(context, Map.class)).isEqualTo(map);
    expression = parser.parseExpression("property['foo']");
    assertThat(expression.getValue(context)).isEqualTo("bar");
  }

  @Test
  void setGenericPropertyContainingMap() {
    Map<String, String> property = new HashMap<>();
    property.put("foo", "bar");
    this.property = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.HashMap<?, ?>", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this)).isEqualTo(property);
    expression = parser.parseExpression("property['foo']");
    assertThat(expression.getValue(this)).isEqualTo("bar");
    expression.setValue(this, "baz");
    assertThat(expression.getValue(this)).isEqualTo("baz");
  }

  @Test
  void setPropertyContainingMap() {
    Map<Integer, Integer> property = new HashMap<>();
    property.put(9, 3);
    this.parameterizedMap = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("parameterizedMap");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("java.util.HashMap<java.lang.Integer, java.lang.Integer>");
    assertThat(expression.getValue(this)).isEqualTo(property);
    expression = parser.parseExpression("parameterizedMap['9']");
    assertThat(expression.getValue(this)).isEqualTo(3);
    expression.setValue(this, "37");
    assertThat(expression.getValue(this)).isEqualTo(37);
  }

  @Test
  void setPropertyContainingMapAutoGrow() {
    SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, false));
    Expression expression = parser.parseExpression("parameterizedMap");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("java.util.Map<java.lang.Integer, java.lang.Integer>");
    assertThat(expression.getValue(this)).isNull();
    expression = parser.parseExpression("parameterizedMap['9']");
    assertThat(expression.getValue(this)).isNull();
    expression.setValue(this, "37");
    assertThat(expression.getValue(this)).isEqualTo(37);
  }

  @Test
  void indexIntoGenericPropertyContainingList() {
    List<String> property = new ArrayList<>();
    property.add("bar");
    this.property = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.ArrayList<?>", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this)).isEqualTo(property);
    expression = parser.parseExpression("property[0]");
    assertThat(expression.getValue(this)).isEqualTo("bar");
  }

  @Test
  void setGenericPropertyContainingList() {
    List<Integer> property = new ArrayList<>();
    property.add(3);
    this.property = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.ArrayList<?>", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this)).isEqualTo(property);
    expression = parser.parseExpression("property[0]");
    assertThat(expression.getValue(this)).isEqualTo(3);
    expression.setValue(this, "4");
    assertThat(expression.getValue(this)).isEqualTo("4");
  }

  @Test
  void setGenericPropertyContainingListAutogrow() {
    List<Integer> property = new ArrayList<>();
    this.property = property;
    SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.ArrayList<?>", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this)).isEqualTo(property);

    Expression indexExpression = parser.parseExpression("property[0]");
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> indexExpression.getValue(this))
            .satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE));
  }

  @Test
  void autoGrowListOfElementsWithoutDefaultConstructor() {
    this.decimals = new ArrayList<>();
    SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
    parser.parseExpression("decimals[0]").setValue(this, "123.4");
    assertThat(decimals).containsExactly(BigDecimal.valueOf(123.4));
  }

  @Test
  void indexIntoPropertyContainingListContainingNullElement() {
    this.decimals = new ArrayList<>();
    this.decimals.add(null);
    this.decimals.add(BigDecimal.ONE);
    SpelExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
    parser.parseExpression("decimals[0]").setValue(this, "9876.5");
    assertThat(decimals).containsExactly(BigDecimal.valueOf(9876.5), BigDecimal.ONE);
  }

  @Test
  void indexIntoPropertyContainingList() {
    List<Integer> property = new ArrayList<>();
    property.add(3);
    this.parameterizedList = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("parameterizedList");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("java.util.ArrayList<java.lang.Integer>");
    assertThat(expression.getValue(this)).isEqualTo(property);
    expression = parser.parseExpression("parameterizedList[0]");
    assertThat(expression.getValue(this)).isEqualTo(3);
  }

  @Test
  void indexIntoPropertyContainingListOfList() {
    List<List<Integer>> property = new ArrayList<>();
    property.add(Arrays.asList(3));
    this.parameterizedListOfList = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("parameterizedListOfList[0]");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("java.util.Arrays$ArrayList<java.lang.Integer>");
    assertThat(expression.getValue(this)).isEqualTo(property.get(0));
    expression = parser.parseExpression("parameterizedListOfList[0][0]");
    assertThat(expression.getValue(this)).isEqualTo(3);
  }

  @Test
  void setPropertyContainingList() {
    List<Integer> property = new ArrayList<>();
    property.add(3);
    this.parameterizedList = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("parameterizedList");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("java.util.ArrayList<java.lang.Integer>");
    assertThat(expression.getValue(this)).isEqualTo(property);
    expression = parser.parseExpression("parameterizedList[0]");
    assertThat(expression.getValue(this)).isEqualTo(3);
    expression.setValue(this, "4");
    assertThat(expression.getValue(this)).isEqualTo(4);
  }

  @Test
  void indexIntoGenericPropertyContainingNullList() {
    SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
    SpelExpressionParser parser = new SpelExpressionParser(configuration);
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.lang.Object", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this)).isNull();

    Expression indexExpression = parser.parseExpression("property[0]");
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> indexExpression.getValue(this))
            .satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(INDEXING_NOT_SUPPORTED_FOR_TYPE));
  }

  @Test
  void indexIntoGenericPropertyContainingGrowingList() {
    List<String> property = new ArrayList<>();
    this.property = property;
    SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
    SpelExpressionParser parser = new SpelExpressionParser(configuration);
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.ArrayList<?>", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this)).isEqualTo(property);

    Expression indexExpression = parser.parseExpression("property[0]");
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> indexExpression.getValue(this))
            .satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE));
  }

  @Test
  void indexIntoGenericPropertyContainingGrowingList2() {
    List<String> property2 = new ArrayList<>();
    this.property2 = property2;
    SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
    SpelExpressionParser parser = new SpelExpressionParser(configuration);
    Expression expression = parser.parseExpression("property2");
    assertThat(expression.getValueTypeDescriptor(this)).asString().isEqualTo("java.util.ArrayList<?>");
    assertThat(expression.getValue(this)).isEqualTo(property2);

    Expression indexExpression = parser.parseExpression("property2[0]");
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> indexExpression.getValue(this))
            .satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE));
  }

  @Test
  void indexIntoGenericPropertyContainingArray() {
    String[] property = { "bar" };
    this.property = property;
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("property");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.lang.String[]", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this)).isEqualTo(property);
    expression = parser.parseExpression("property[0]");
    assertThat(expression.getValue(this)).isEqualTo("bar");
  }

  @Test
  void emptyList() {
    listOfScalarNotGeneric = new ArrayList();
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("listOfScalarNotGeneric");
    assertThat(expression.getValueTypeDescriptor(this)).asString().isEqualTo("java.util.ArrayList<?>");
    assertThat(expression.getValue(this, String.class)).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void resolveCollectionElementType() {
    listNotGeneric = new ArrayList(2);
    listNotGeneric.add(5);
    listNotGeneric.add(6);
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("listNotGeneric");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.ArrayList<?>", FieldAnnotation.class.getCanonicalName());
    assertThat(expression.getValue(this, String.class)).isEqualTo("5,6");
  }

  @Test
  void resolveCollectionElementTypeNull() {
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("listNotGeneric");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.List<?>", FieldAnnotation.class.getCanonicalName());
  }

  @SuppressWarnings("unchecked")
  @Test
  void resolveMapKeyValueTypes() {
    mapNotGeneric = new HashMap();
    mapNotGeneric.put("baseAmount", 3.11);
    mapNotGeneric.put("bonusAmount", 7.17);
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("mapNotGeneric");
    assertThat(expression.getValueTypeDescriptor(this)).asString()
            .isEqualTo("@%s java.util.HashMap<?, ?>", FieldAnnotation.class.getCanonicalName());
  }

  @Test
  @SuppressWarnings("unchecked")
  void listOfScalars() {
    listOfScalarNotGeneric = new ArrayList(1);
    listOfScalarNotGeneric.add("5");
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("listOfScalarNotGeneric[0]");
    assertThat(expression.getValue(this, Integer.class)).isEqualTo(5);
  }

  @Test
  @SuppressWarnings("unchecked")
  void listOfMaps() {
    listOfMapsNotGeneric = new ArrayList();
    Map map = new HashMap();
    map.put("fruit", "apple");
    listOfMapsNotGeneric.add(map);
    SpelExpressionParser parser = new SpelExpressionParser();
    Expression expression = parser.parseExpression("listOfMapsNotGeneric[0]['fruit']");
    assertThat(expression.getValue(this, String.class)).isEqualTo("apple");
  }

  @Target({ ElementType.FIELD })
  @Retention(RetentionPolicy.RUNTIME)
  @interface FieldAnnotation {
  }

  @FieldAnnotation
  public Object property;

  public List property2;

  public Map<Integer, Integer> parameterizedMap;

  public List<BigDecimal> decimals;

  public List<Integer> parameterizedList;

  public List<List<Integer>> parameterizedListOfList;

  @FieldAnnotation
  public List listNotGeneric;

  @FieldAnnotation
  public Map mapNotGeneric;

  public List listOfScalarNotGeneric;

  public List listOfMapsNotGeneric;

  private static class MapAccessor implements PropertyAccessor {

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) {
      return (((Map<?, ?>) target).containsKey(name));
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) {
      return new TypedValue(((Map<?, ?>) target).get(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) {
      return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(EvaluationContext context, Object target, String name, Object newValue) {
      ((Map) target).put(name, newValue);
    }

    @Override
    public Class<?>[] getSpecificTargetClasses() {
      return new Class<?>[] { Map.class };
    }

  }

}
