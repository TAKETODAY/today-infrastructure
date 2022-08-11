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
package cn.taketoday.bytecode.beans;

import java.beans.PropertyDescriptor;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.core.AbstractClassGenerator;
import cn.taketoday.bytecode.core.CglibReflectUtils;
import cn.taketoday.bytecode.core.ClassEmitter;
import cn.taketoday.bytecode.core.EmitUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class BeanGenerator extends AbstractClassGenerator<Object> {

  record BeanGeneratorKey(String superclass, Map<String, Type> props) {

  }

  private boolean classOnly;
  private Class<?> superclass;
  private final HashMap<String, Type> props = new HashMap<>();

  public BeanGenerator() {
    super(BeanGenerator.class);
  }

  /**
   * Set the class which the generated class will extend. The class must not be
   * declared as final, and must have a non-private no-argument constructor.
   *
   * @param superclass class to extend, or null to extend Object
   */
  public void setSuperclass(Class<?> superclass) {
    if (superclass != null && superclass.equals(Object.class)) {
      superclass = null;
    }
    setNeighbor(superclass);
    this.superclass = superclass;
  }

  public void addProperty(String name, Class<?> type) {
    if (props.containsKey(name)) {
      throw new IllegalArgumentException("Duplicate property name \"" + name + "\"");
    }
    props.put(name, Type.fromClass(type));
  }

  @Override
  protected ClassLoader getDefaultClassLoader() {
    if (superclass != null) {
      return superclass.getClassLoader();
    }
    return null;
  }

  @Override
  protected ProtectionDomain getProtectionDomain() {
    return ReflectionUtils.getProtectionDomain(superclass);
  }

  public Object create() {
    classOnly = false;
    return createHelper();
  }

  public Object createClass() {
    classOnly = true;
    return createHelper();
  }

  private Object createHelper() {
    if (superclass != null) {
      setNamePrefix(superclass.getName());
    }
    String superName = (superclass != null) ? superclass.getName() : "java.lang.Object";
    Object key = new BeanGeneratorKey(superName, props);
    return super.create(key);
  }

  @Override
  public void generateClass(ClassVisitor v) {

    final Map<String, Type> props = this.props;
    int size = props.size();
    final Type[] types = new Type[size];

    int i = 0;
    for (final Entry<String, Type> entry : props.entrySet()) {
      types[i++] = entry.getValue();
    }

    ClassEmitter ce = new ClassEmitter(v);

    ce.beginClass(Opcodes.JAVA_VERSION, Opcodes.ACC_PUBLIC, getClassName(),
            superclass != null ? Type.fromClass(superclass) : Type.TYPE_OBJECT, null);

    EmitUtils.nullConstructor(ce);

    EmitUtils.addProperties(ce, StringUtils.toStringArray(props.keySet()), types);
    ce.endClass();
  }

  @Override
  protected Object firstInstance(Class<Object> type) {
    if (classOnly) {
      return type;
    }
    return ReflectionUtils.newInstance(type);
  }

  @Override
  protected Object nextInstance(Object instance) {
    Class<?> protoclass = (instance instanceof Class) ? (Class<?>) instance : instance.getClass();
    if (classOnly) {
      return protoclass;
    }
    return ReflectionUtils.newInstance(protoclass);
  }

  public static void addProperties(BeanGenerator gen, Map<String, Class<?>> props) {
    props.forEach(gen::addProperty);
  }

  public static void addProperties(BeanGenerator gen, Class<?> type) {
    addProperties(gen, CglibReflectUtils.getBeanProperties(type));
  }

  public static void addProperties(BeanGenerator gen, PropertyDescriptor[] descriptors) {
    for (final PropertyDescriptor descriptor : descriptors) {
      gen.addProperty(descriptor.getName(), descriptor.getPropertyType());
    }
  }
}
