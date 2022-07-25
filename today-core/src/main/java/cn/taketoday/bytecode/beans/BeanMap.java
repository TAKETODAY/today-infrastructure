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

import java.security.ProtectionDomain;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.core.AbstractClassGenerator;
import cn.taketoday.bytecode.core.KeyFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ReflectionUtils;

/**
 * A <code>Map</code>-based view of a JavaBean. The default set of keys is the
 * union of all property names (getters or setters). An attempt to set a
 * read-only property will be ignored, and write-only properties will be
 * returned as <code>null</code>. Removal of objects is not a supported (the key
 * set is fixed).
 *
 * @author Chris Nokleberg
 * @see cn.taketoday.beans.support.BeanMapping
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class BeanMap extends AbstractMap<String, Object> implements Map<String, Object> {
  /**
   * Limit the properties reflected in the key set of the map to readable
   * properties.
   *
   * @see BeanMap.Generator#setRequire
   */
  public static final int REQUIRE_GETTER = 1;

  /**
   * Limit the properties reflected in the key set of the map to writable
   * properties.
   *
   * @see BeanMap.Generator#setRequire
   */
  public static final int REQUIRE_SETTER = 2;

  /**
   * Helper method to create a new <code>BeanMap</code>. For finer control over
   * the generated instance, use a new instance of <code>BeanMap.Generator</code>
   * instead of this static method.
   *
   * @param bean the JavaBean underlying the map
   * @return a new <code>BeanMap</code> instance
   */
  public static BeanMap create(Object bean) {
    Generator gen = new Generator();
    gen.setBean(bean);
    return gen.create();
  }

  final static class Generator extends AbstractClassGenerator {
    private static final BeanMapKey KEY_FACTORY = KeyFactory.create(BeanMapKey.class, KeyFactory.CLASS_BY_NAME);

    interface BeanMapKey {
      Object newInstance(Class type, int require);
    }

    private Object bean;
    private Class beanClass;
    private int require;

    public Generator() {
      super(BeanMap.class);
    }

    /**
     * Set the bean that the generated map should reflect. The bean may be swapped
     * out for another bean of the same type using {@link #setBean(Object)}. Calling this
     * method overrides any value previously set using {@link #setBeanClass}. You
     * must call either this method or {@link #setBeanClass} before {@link #create}.
     *
     * @param bean the initial bean
     */
    public void setBean(Object bean) {
      this.bean = bean;
      if (bean != null) {
        beanClass = bean.getClass();
        setNeighbor(beanClass);
      }
      else {
        setNeighbor(null);
      }
    }

    /**
     * Set the class of the bean that the generated map should support. You must
     * call either this method or {@link #setBeanClass} before {@link #create}.
     *
     * @param beanClass the class of the bean
     */
    public void setBeanClass(Class beanClass) {
      this.beanClass = beanClass;
      setNeighbor(beanClass);
    }

    /**
     * Limit the properties reflected by the generated map.
     *
     * @param require any combination of {@link #REQUIRE_GETTER} and
     * {@link #REQUIRE_SETTER}; default is zero (any property allowed)
     */
    public void setRequire(int require) {
      this.require = require;
    }

    protected ClassLoader getDefaultClassLoader() {
      return beanClass.getClassLoader();
    }

    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(beanClass);
    }

    /**
     * Create a new instance of the <code>BeanMap</code>. An existing generated
     * class will be reused if possible.
     */
    public BeanMap create() {
      Assert.notNull(beanClass, "Class of bean unknown");
      setNamePrefix(beanClass.getName());
      return (BeanMap) super.create(KEY_FACTORY.newInstance(beanClass, require));
    }

    public void generateClass(ClassVisitor v) throws Exception {
      new BeanMapEmitter(v, getClassName(), beanClass, require);
    }

    protected Object firstInstance(Class type) {
      return ((BeanMap) ReflectionUtils.newInstance(type)).newInstance(bean);
    }

    protected Object nextInstance(Object instance) {
      return ((BeanMap) instance).newInstance(bean);
    }
  }

  /**
   * Create a new <code>BeanMap</code> instance using the specified bean. This is
   * faster than using the {@link #create} static method.
   *
   * @param bean the JavaBean underlying the map
   * @return a new <code>BeanMap</code> instance
   */
  abstract public BeanMap newInstance(Object bean);

  /**
   * Get the type of a property.
   *
   * @param name the name of the JavaBean property
   * @return the type of the property, or null if the property does not exist
   */
  abstract public Class getPropertyType(String name);

  protected Object bean;

  protected BeanMap() { }

  protected BeanMap(Object bean) {
    setBean(bean);
  }

  @Override
  public Object get(Object key) {
    return get(bean, key);
  }

  @Override
  public Object put(String key, Object value) {
    return put(bean, key, value);
  }

  /**
   * Get the property of a bean. This allows a <code>BeanMap</code> to be used
   * statically for multiple beans--the bean instance tied to the map is ignored
   * and the bean passed to this method is used instead.
   *
   * @param bean the bean to query; must be compatible with the type of this
   * <code>BeanMap</code>
   * @param key must be a String
   * @return the current value, or null if there is no matching property
   */
  abstract public Object get(Object bean, Object key);

  /**
   * Set the property of a bean. This allows a <code>BeanMap</code> to be used
   * statically for multiple beans--the bean instance tied to the map is ignored
   * and the bean passed to this method is used instead.
   *
   * @param key must be a String
   * @return the old value, if there was one, or null
   */
  abstract public Object put(Object bean, Object key, Object value);

  /**
   * Change the underlying bean this map should use.
   *
   * @param bean the new JavaBean
   * @see #getBean
   */
  public void setBean(Object bean) {
    this.bean = bean;
  }

  /**
   * Return the bean currently in use by this map.
   *
   * @return the current JavaBean
   * @see #setBean
   */
  public Object getBean() {
    return bean;
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsKey(Object key) {
    return keySet().contains(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    for (final Object key : keySet()) {
      final Object v = get(key);
      if (Objects.equals(value, v)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int size() {
    return keySet().size();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> t) {
    for (final String key : t.keySet()) {
      put(key, t.get(key));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o != this) {
      if (!(o instanceof Map other)) {
        return false;
      }
      if (size() != other.size()) {
        return false;
      }
      for (final Object key : keySet()) {
        if (!Objects.equals(get(key), other.get(key))) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int code = 0;
    for (final Object key : keySet()) {
      code += Objects.hashCode(key) ^ Objects.hashCode(get(key));
    }
    return code;
  }

  @Override
  public Set entrySet() {
    HashMap copy = new HashMap();
    for (final Object key : keySet()) {
      copy.put(key, get(key));
    }
    return Collections.unmodifiableSet(copy.entrySet());
  }

  @Override
  public Collection<Object> values() {
    Set keys = keySet();
    ArrayList<Object> values = new ArrayList<>(keys.size());
    for (Object key : keys) {
      values.add(get(key));
    }
    return Collections.unmodifiableCollection(values);
  }

}
