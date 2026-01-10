/*
 * Copyright 2002-present the original author or authors.
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

package infra.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/1 21:32
 */
class SerializableTypeWrapperTests {

  @Test
  void forField() throws Exception {
    Type type = SerializableTypeWrapper.forField(Fields.class.getField("parameterizedType"));
    assertThat(type.toString()).isEqualTo("java.util.List<java.lang.String>");
    assertSerializable(type);
  }

  @Test
  void forMethodParameter() throws Exception {
    Method method = Methods.class.getDeclaredMethod("method", Class.class, Object.class);
    Type type = SerializableTypeWrapper.forMethodParameter(MethodParameter.forExecutable(method, 0));
    assertThat(type.toString()).isEqualTo("java.lang.Class<T>");
    assertSerializable(type);
  }

  @Test
  void forConstructor() throws Exception {
    Constructor<?> constructor = Constructors.class.getDeclaredConstructor(List.class);
    Type type = SerializableTypeWrapper.forMethodParameter(MethodParameter.forExecutable(constructor, 0));
    assertThat(type.toString()).isEqualTo("java.util.List<java.lang.String>");
    assertSerializable(type);
  }

  @Test
  void classType() throws Exception {
    Type type = SerializableTypeWrapper.forField(Fields.class.getField("classType"));
    assertThat(type.toString()).isEqualTo("class java.lang.String");
    assertSerializable(type);
  }

  @Test
  void genericArrayType() throws Exception {
    GenericArrayType type = (GenericArrayType) SerializableTypeWrapper.forField(Fields.class.getField("genericArrayType"));
    assertThat(type.toString()).isEqualTo("java.util.List<java.lang.String>[]");
    assertSerializable(type);
    assertSerializable(type.getGenericComponentType());
  }

  @Test
  void parameterizedType() throws Exception {
    ParameterizedType type = (ParameterizedType) SerializableTypeWrapper.forField(Fields.class.getField("parameterizedType"));
    assertThat(type.toString()).isEqualTo("java.util.List<java.lang.String>");
    assertSerializable(type);
    assertSerializable(type.getOwnerType());
    assertSerializable(type.getRawType());
    assertSerializable(type.getActualTypeArguments());
    assertSerializable(type.getActualTypeArguments()[0]);
  }

  @Test
  void typeVariableType() throws Exception {
    TypeVariable<?> type = (TypeVariable<?>) SerializableTypeWrapper.forField(Fields.class.getField("typeVariableType"));
    assertThat(type.toString()).isEqualTo("T");
    assertSerializable(type);
    assertSerializable(type.getBounds());
  }

  @Test
  void wildcardType() throws Exception {
    ParameterizedType typeSource = (ParameterizedType) SerializableTypeWrapper.forField(Fields.class.getField("wildcardType"));
    WildcardType type = (WildcardType) typeSource.getActualTypeArguments()[0];
    assertThat(type.toString()).isEqualTo("? extends java.lang.CharSequence");
    assertSerializable(type);
    assertSerializable(type.getLowerBounds());
    assertSerializable(type.getUpperBounds());
  }

  @Test
  void unwrapClassType() throws Exception {
    Field field = Fields.class.getField("classType");
    Type type = SerializableTypeWrapper.forField(field);
    Type unwrapped = SerializableTypeWrapper.unwrap(type);
    assertThat(unwrapped).isEqualTo(String.class);
  }

  @Test
  void unwrapParameterizedType() throws Exception {
    Field field = Fields.class.getField("parameterizedType");
    Type type = SerializableTypeWrapper.forField(field);
    Type unwrapped = SerializableTypeWrapper.unwrap(type);
    assertThat(unwrapped).isInstanceOf(ParameterizedType.class);
    assertThat(((ParameterizedType) unwrapped).getRawType()).isEqualTo(List.class);
  }

  @Test
  void unwrapGenericArrayType() throws Exception {
    Field field = Fields.class.getField("genericArrayType");
    Type type = SerializableTypeWrapper.forField(field);
    Type unwrapped = SerializableTypeWrapper.unwrap(type);
    assertThat(unwrapped).isInstanceOf(GenericArrayType.class);
    Type componentUnwrapped = SerializableTypeWrapper.unwrap(((GenericArrayType) type).getGenericComponentType());
    assertThat(componentUnwrapped.toString()).isEqualTo("java.util.List<java.lang.String>");
  }

  @Test
  void unwrapTypeVariable() throws Exception {
    Field field = Fields.class.getField("typeVariableType");
    Type type = SerializableTypeWrapper.forField(field);
    Type unwrapped = SerializableTypeWrapper.unwrap(type);
    assertThat(unwrapped).isInstanceOf(TypeVariable.class);
    assertThat(((TypeVariable<?>) unwrapped).getName()).isEqualTo("T");
  }

  @Test
  void unwrapWildcardType() throws Exception {
    Field field = Fields.class.getField("wildcardType");
    ParameterizedType type = (ParameterizedType) SerializableTypeWrapper.forField(field);
    WildcardType wildcardType = (WildcardType) type.getActualTypeArguments()[0];
    Type unwrapped = SerializableTypeWrapper.unwrap(wildcardType);
    assertThat(unwrapped).isInstanceOf(WildcardType.class);
    assertThat(((WildcardType) unwrapped).getUpperBounds()[0]).isEqualTo(CharSequence.class);
  }

  @Test
  void methodReturnType() throws Exception {
    Method method = Methods.class.getDeclaredMethod("method", Class.class, Object.class);
    Type type = SerializableTypeWrapper.forMethodParameter(MethodParameter.forExecutable(method, -1));
    assertThat(type.toString()).isEqualTo("java.util.List<T>");
    assertSerializable(type);
  }

  @Test
  void equalsAndHashCode() throws Exception {
    Field field = Fields.class.getField("parameterizedType");
    Type type1 = SerializableTypeWrapper.forField(field);
    Type type2 = SerializableTypeWrapper.forField(field);

    assertThat(type1).isEqualTo(type2);
    assertThat(type1.hashCode()).isEqualTo(type2.hashCode());
  }

  @Test
  void serializesAndDeserializesCorrectly() throws Exception {
    Field field = Fields.class.getField("parameterizedType");
    Type originalType = SerializableTypeWrapper.forField(field);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(originalType);
    oos.close();

    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
    Type deserializedType = (Type) ois.readObject();

    assertThat(deserializedType.toString()).isEqualTo(originalType.toString());
    assertThat(deserializedType).isEqualTo(originalType);
  }

  private void assertSerializable(Object source) throws Exception {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    oos.writeObject(source);
    oos.close();
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
    assertThat(ois.readObject()).isEqualTo(source);
  }

  static class Fields<T> {

    public String classType;

    public List<String>[] genericArrayType;

    public List<String> parameterizedType;

    public T typeVariableType;

    public List<? extends CharSequence> wildcardType;
  }

  interface Methods {

    <T> List<T> method(Class<T> p1, T p2);
  }

  static class Constructors {

    public Constructors(List<String> p) {
    }
  }

}