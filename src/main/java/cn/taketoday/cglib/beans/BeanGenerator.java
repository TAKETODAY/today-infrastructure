/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.beans;

import java.beans.PropertyDescriptor;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Type;
import cn.taketoday.cglib.core.AbstractClassGenerator;
import cn.taketoday.cglib.core.CglibReflectUtils;
import cn.taketoday.cglib.core.ClassEmitter;
import cn.taketoday.cglib.core.EmitUtils;
import cn.taketoday.cglib.core.KeyFactory;

import static cn.taketoday.asm.Opcodes.ACC_PUBLIC;
import static cn.taketoday.asm.Opcodes.JAVA_VERSION;
import static cn.taketoday.core.Constant.TYPE_OBJECT;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
public class BeanGenerator extends AbstractClassGenerator<Object> {

  private static final BeanGeneratorKey KEY_FACTORY = KeyFactory.create(BeanGeneratorKey.class);

  interface BeanGeneratorKey {
    Object newInstance(String superclass, Map<String, Type> props);
  }

  private boolean classOnly;
  private Class<?> superclass;
  private Map<String, Type> props = new HashMap<>();

  public BeanGenerator() {
    super(BeanGenerator.class);
  }

  /**
   * Set the class which the generated class will extend. The class must not be
   * declared as final, and must have a non-private no-argument constructor.
   *
   * @param superclass
   *         class to extend, or null to extend Object
   */
  public void setSuperclass(Class<?> superclass) {
    if (superclass != null && superclass.equals(Object.class)) {
      superclass = null;
    }
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
    return CglibReflectUtils.getProtectionDomain(superclass);
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
    Object key = KEY_FACTORY.newInstance(superName, props);
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

    ce.beginClass(JAVA_VERSION, ACC_PUBLIC, getClassName(),
                  superclass != null ? Type.fromClass(superclass) : TYPE_OBJECT, null);

    EmitUtils.nullConstructor(ce);

    final Set<String> keySet = props.keySet();
    EmitUtils.addProperties(ce, keySet.toArray(new String[keySet.size()]), types);

    ce.endClass();
  }

  @Override
  protected Object firstInstance(Class<Object> type) {
    if (classOnly) {
      return type;
    }
    return CglibReflectUtils.newInstance(type);
  }

  @Override
  protected Object nextInstance(Object instance) {
    Class<?> protoclass = (instance instanceof Class) ? (Class<?>) instance : instance.getClass();
    if (classOnly) {
      return protoclass;
    }
    return CglibReflectUtils.newInstance(protoclass);
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
