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

package cn.taketoday.jmx.export;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

/**
 * Extension of the {@link RequiredModelMBean} class that ensures the
 * {@link Thread#getContextClassLoader() thread context ClassLoader} is switched
 * for the managed resource's {@link ClassLoader} before any invocations occur.
 *
 * @author Rob Harrop
 * @see RequiredModelMBean
 * @since 4.0
 */
public class SpringModelMBean extends RequiredModelMBean {

  /**
   * Stores the {@link ClassLoader} to use for invocations. Defaults
   * to the current thread {@link ClassLoader}.
   */
  private ClassLoader managedResourceClassLoader = Thread.currentThread().getContextClassLoader();

  /**
   * Construct a new SpringModelMBean instance with an empty {@link ModelMBeanInfo}.
   *
   * @see RequiredModelMBean#RequiredModelMBean()
   */
  public SpringModelMBean() throws MBeanException, RuntimeOperationsException {
    super();
  }

  /**
   * Construct a new SpringModelMBean instance with the given {@link ModelMBeanInfo}.
   *
   * @see RequiredModelMBean#RequiredModelMBean(ModelMBeanInfo)
   */
  public SpringModelMBean(ModelMBeanInfo mbi) throws MBeanException, RuntimeOperationsException {
    super(mbi);
  }

  /**
   * Sets managed resource to expose and stores its {@link ClassLoader}.
   */
  @Override
  public void setManagedResource(Object managedResource, String managedResourceType)
          throws MBeanException, InstanceNotFoundException, InvalidTargetObjectTypeException {

    this.managedResourceClassLoader = managedResource.getClass().getClassLoader();
    super.setManagedResource(managedResource, managedResourceType);
  }

  /**
   * Switches the {@link Thread#getContextClassLoader() context ClassLoader} for the
   * managed resources {@link ClassLoader} before allowing the invocation to occur.
   *
   * @see javax.management.modelmbean.ModelMBean#invoke
   */
  @Override
  public Object invoke(String opName, Object[] opArgs, String[] sig)
          throws MBeanException, ReflectionException {

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
      return super.invoke(opName, opArgs, sig);
    }
    finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  /**
   * Switches the {@link Thread#getContextClassLoader() context ClassLoader} for the
   * managed resources {@link ClassLoader} before allowing the invocation to occur.
   *
   * @see javax.management.modelmbean.ModelMBean#getAttribute
   */
  @Override
  public Object getAttribute(String attrName)
          throws AttributeNotFoundException, MBeanException, ReflectionException {

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
      return super.getAttribute(attrName);
    }
    finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  /**
   * Switches the {@link Thread#getContextClassLoader() context ClassLoader} for the
   * managed resources {@link ClassLoader} before allowing the invocation to occur.
   *
   * @see javax.management.modelmbean.ModelMBean#getAttributes
   */
  @Override
  public AttributeList getAttributes(String[] attrNames) {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
      return super.getAttributes(attrNames);
    }
    finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  /**
   * Switches the {@link Thread#getContextClassLoader() context ClassLoader} for the
   * managed resources {@link ClassLoader} before allowing the invocation to occur.
   *
   * @see javax.management.modelmbean.ModelMBean#setAttribute
   */
  @Override
  public void setAttribute(Attribute attribute)
          throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {

    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
      super.setAttribute(attribute);
    }
    finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

  /**
   * Switches the {@link Thread#getContextClassLoader() context ClassLoader} for the
   * managed resources {@link ClassLoader} before allowing the invocation to occur.
   *
   * @see javax.management.modelmbean.ModelMBean#setAttributes
   */
  @Override
  public AttributeList setAttributes(AttributeList attributes) {
    ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
      return super.setAttributes(attributes);
    }
    finally {
      Thread.currentThread().setContextClassLoader(currentClassLoader);
    }
  }

}
