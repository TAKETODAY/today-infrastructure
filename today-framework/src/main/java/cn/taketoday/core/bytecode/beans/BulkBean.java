/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.beans;

import java.security.ProtectionDomain;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.core.AbstractClassGenerator;
import cn.taketoday.core.bytecode.core.CglibReflectUtils;
import cn.taketoday.core.bytecode.core.KeyFactory;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author Juozas Baliuka
 */
@SuppressWarnings("all")
abstract public class BulkBean {
  private static final BulkBeanKey KEY_FACTORY = (BulkBeanKey) KeyFactory.create(BulkBeanKey.class);

  interface BulkBeanKey {
    public Object newInstance(String target, String[] getters, String[] setters, String[] types);
  }

  protected Class target;
  protected String[] getters, setters;
  protected Class[] types;

  protected BulkBean() { }

  abstract public void getPropertyValues(Object bean, Object[] values);

  abstract public void setPropertyValues(Object bean, Object[] values);

  public Object[] getPropertyValues(Object bean) {
    Object[] values = new Object[getters.length];
    getPropertyValues(bean, values);
    return values;
  }

  public Class[] getPropertyTypes() {
    return (Class[]) types.clone();
  }

  public String[] getGetters() {
    return (String[]) getters.clone();
  }

  public String[] getSetters() {
    return (String[]) setters.clone();
  }

  public static BulkBean create(Class target, String[] getters, String[] setters, Class[] types) {
    Generator gen = new Generator();
    gen.setTarget(target);
    gen.setGetters(getters);
    gen.setSetters(setters);
    gen.setTypes(types);
    return gen.create();
  }

  public static class Generator extends AbstractClassGenerator {

    private Class target;
    private String[] getters;
    private String[] setters;
    private Class[] types;

    public Generator() {
      super(BulkBean.class);
    }

    public void setTarget(Class target) {
      this.target = target;
      setNeighbor(target);
    }

    public void setGetters(String[] getters) {
      this.getters = getters;
    }

    public void setSetters(String[] setters) {
      this.setters = setters;
    }

    public void setTypes(Class[] types) {
      this.types = types;
    }

    protected ClassLoader getDefaultClassLoader() {
      return target.getClassLoader();
    }

    protected ProtectionDomain getProtectionDomain() {
      return ReflectionUtils.getProtectionDomain(target);
    }

    public BulkBean create() {

      setNamePrefix(target.getName());
      String targetClassName = target.getName();
      String[] typeClassNames = CglibReflectUtils.getNames(types);
      Object key = KEY_FACTORY.newInstance(targetClassName, getters, setters, typeClassNames);
      return (BulkBean) super.create(key);
    }

    public void generateClass(ClassVisitor v) throws Exception {
      new BulkBeanEmitter(v, getClassName(), target, getters, setters, types);
    }

    protected Object firstInstance(Class type) {
      BulkBean instance = (BulkBean) ReflectionUtils.newInstance(type);
      instance.target = target;

      int length = getters.length;
      instance.getters = new String[length];
      System.arraycopy(getters, 0, instance.getters, 0, length);

      instance.setters = new String[length];
      System.arraycopy(setters, 0, instance.setters, 0, length);

      instance.types = new Class[types.length];
      System.arraycopy(types, 0, instance.types, 0, types.length);

      return instance;
    }

    protected Object nextInstance(Object instance) {
      return instance;
    }
  }
}
