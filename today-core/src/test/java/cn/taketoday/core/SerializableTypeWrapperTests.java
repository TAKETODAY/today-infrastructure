/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

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