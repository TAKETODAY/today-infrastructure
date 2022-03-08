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
package cn.taketoday.beans.factory.config;

import java.lang.reflect.Field;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.FactoryBeanNotInitializedException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link FactoryBean} which retrieves a static or non-static field value.
 *
 * <p>Typically used for retrieving public static final constants. Usage example:
 *
 * <pre class="code">
 * // standard definition for exposing a static field, specifying the "staticField" property
 * &lt;bean id="myField" class="cn.taketoday.beans.factory.config.FieldRetrievingFactoryBean"&gt;
 *   &lt;property name="staticField" value="java.sql.Connection.TRANSACTION_SERIALIZABLE"/&gt;
 * &lt;/bean&gt;
 *
 * // convenience version that specifies a static field pattern as bean name
 * &lt;bean id="java.sql.Connection.TRANSACTION_SERIALIZABLE"
 *       class="cn.taketoday.beans.factory.config.FieldRetrievingFactoryBean"/&gt;
 * </pre>
 *
 * <p>you can also use the following style of configuration for
 * public static fields.
 *
 * <pre class="code">&lt;util:constant static-field="java.sql.Connection.TRANSACTION_SERIALIZABLE"/&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setStaticField
 * @since 4.0 2021/11/30 14:46
 */
public class FieldRetrievingFactoryBean
        implements FactoryBean<Object>, BeanNameAware, BeanClassLoaderAware, InitializingBean {

  @Nullable
  private Class<?> targetClass;

  @Nullable
  private Object targetObject;

  @Nullable
  private String targetField;

  @Nullable
  private String staticField;

  @Nullable
  private String beanName;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  // the field we will retrieve
  @Nullable
  private Field fieldObject;

  /**
   * Set the target class on which the field is defined.
   * Only necessary when the target field is static; else,
   * a target object needs to be specified anyway.
   *
   * @see #setTargetObject
   * @see #setTargetField
   */
  public void setTargetClass(@Nullable Class<?> targetClass) {
    this.targetClass = targetClass;
  }

  /**
   * Return the target class on which the field is defined.
   */
  @Nullable
  public Class<?> getTargetClass() {
    return this.targetClass;
  }

  /**
   * Set the target object on which the field is defined.
   * Only necessary when the target field is not static;
   * else, a target class is sufficient.
   *
   * @see #setTargetClass
   * @see #setTargetField
   */
  public void setTargetObject(@Nullable Object targetObject) {
    this.targetObject = targetObject;
  }

  /**
   * Return the target object on which the field is defined.
   */
  @Nullable
  public Object getTargetObject() {
    return this.targetObject;
  }

  /**
   * Set the name of the field to be retrieved.
   * Refers to either a static field or a non-static field,
   * depending on a target object being set.
   *
   * @see #setTargetClass
   * @see #setTargetObject
   */
  public void setTargetField(@Nullable String targetField) {
    this.targetField = (targetField != null ? StringUtils.trimAllWhitespace(targetField) : null);
  }

  /**
   * Return the name of the field to be retrieved.
   */
  @Nullable
  public String getTargetField() {
    return this.targetField;
  }

  /**
   * Set a fully qualified static field name to retrieve,
   * e.g. "example.MyExampleClass.MY_EXAMPLE_FIELD".
   * Convenient alternative to specifying targetClass and targetField.
   *
   * @see #setTargetClass
   * @see #setTargetField
   */
  public void setStaticField(String staticField) {
    this.staticField = StringUtils.trimAllWhitespace(staticField);
  }

  /**
   * The bean name of this FieldRetrievingFactoryBean will be interpreted
   * as "staticField" pattern, if neither "targetClass" nor "targetObject"
   * nor "targetField" have been specified.
   * This allows for concise bean definitions with just an id/name.
   */
  @Override
  public void setBeanName(String beanName) {
    this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
  }

  @Override
  public void setBeanClassLoader(@Nullable ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public void afterPropertiesSet() throws ClassNotFoundException, NoSuchFieldException {
    if (this.targetClass != null && this.targetObject != null) {
      throw new IllegalArgumentException("Specify either targetClass or targetObject, not both");
    }

    if (this.targetClass == null && this.targetObject == null) {
      if (this.targetField != null) {
        throw new IllegalArgumentException(
                "Specify targetClass or targetObject in combination with targetField");
      }

      // If no other property specified, consider bean name as static field expression.
      if (this.staticField == null) {
        this.staticField = this.beanName;
        Assert.state(this.staticField != null, "No target field specified");
      }

      // Try to parse static field into class and field.
      int lastDotIndex = this.staticField.lastIndexOf('.');
      if (lastDotIndex == -1 || lastDotIndex == this.staticField.length()) {
        throw new IllegalArgumentException(
                "staticField must be a fully qualified class plus static field name: " +
                        "e.g. 'example.MyExampleClass.MY_EXAMPLE_FIELD'");
      }
      String className = this.staticField.substring(0, lastDotIndex);
      String fieldName = this.staticField.substring(lastDotIndex + 1);
      this.targetClass = ClassUtils.forName(className, this.beanClassLoader);
      this.targetField = fieldName;
    }

    else if (this.targetField == null) {
      // Either targetClass or targetObject specified.
      throw new IllegalArgumentException("targetField is required");
    }

    // Try to get the exact method first.
    Class<?> targetClass = (this.targetObject != null ? this.targetObject.getClass() : this.targetClass);
    this.fieldObject = targetClass.getField(this.targetField);
  }

  @Override
  @Nullable
  public Object getObject() throws IllegalAccessException {
    if (this.fieldObject == null) {
      throw new FactoryBeanNotInitializedException();
    }
    ReflectionUtils.makeAccessible(this.fieldObject);
    if (this.targetObject != null) {
      // instance field
      return this.fieldObject.get(this.targetObject);
    }
    else {
      // class field
      return this.fieldObject.get(null);
    }
  }

  @Override
  public Class<?> getObjectType() {
    return (this.fieldObject != null ? this.fieldObject.getType() : null);
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

}

