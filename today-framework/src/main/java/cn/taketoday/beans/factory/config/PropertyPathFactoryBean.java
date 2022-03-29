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

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.PropertyAccessorFactory;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * {@link FactoryBean} that evaluates a property path on a given target object.
 *
 * <p>The target object can be specified directly or via a bean name.
 *
 * <p>Usage examples:
 *
 * <pre class="code">&lt;!-- target bean to be referenced by name --&gt;
 * &lt;bean id="tb" class="cn.taketoday.beans.factory.TestBean" singleton="false"&gt;
 *   &lt;property name="age" value="10"/&gt;
 *   &lt;property name="spouse"&gt;
 *     &lt;bean class="cn.taketoday.beans.factory.TestBean"&gt;
 *       &lt;property name="age" value="11"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 12, which is the value of property 'age' of the inner bean --&gt;
 * &lt;bean id="propertyPath1" class="cn.taketoday.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetObject"&gt;
 *     &lt;bean class="cn.taketoday.beans.factory.TestBean"&gt;
 *       &lt;property name="age" value="12"/&gt;
 *     &lt;/bean&gt;
 *   &lt;/property&gt;
 *   &lt;property name="propertyPath" value="age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 11, which is the value of property 'spouse.age' of bean 'tb' --&gt;
 * &lt;bean id="propertyPath2" class="cn.taketoday.beans.factory.config.PropertyPathFactoryBean"&gt;
 *   &lt;property name="targetBeanName" value="tb"/&gt;
 *   &lt;property name="propertyPath" value="spouse.age"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;!-- will result in 10, which is the value of property 'age' of bean 'tb' --&gt;
 * &lt;bean id="tb.age" class="cn.taketoday.beans.factory.config.PropertyPathFactoryBean"/&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setTargetObject
 * @see #setTargetBeanName
 * @see #setPropertyPath
 * @since 4.0 2021/11/30 15:08
 */
public class PropertyPathFactoryBean implements FactoryBean<Object>, BeanNameAware, BeanFactoryAware {
  private static final Logger logger = LoggerFactory.getLogger(PropertyPathFactoryBean.class);

  @Nullable
  private BeanWrapper targetBeanWrapper;

  @Nullable
  private String targetBeanName;

  @Nullable
  private String propertyPath;

  @Nullable
  private Class<?> resultType;

  @Nullable
  private String beanName;

  @Nullable
  private BeanFactory beanFactory;

  /**
   * Specify a target object to apply the property path to.
   * Alternatively, specify a target bean name.
   *
   * @param targetObject a target object, for example a bean reference
   * or an inner bean
   * @see #setTargetBeanName
   */
  public void setTargetObject(Object targetObject) {
    this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(targetObject);
  }

  /**
   * Specify the name of a target bean to apply the property path to.
   * Alternatively, specify a target object directly.
   *
   * @param targetBeanName the bean name to be looked up in the
   * containing bean factory (e.g. "testBean")
   * @see #setTargetObject
   */
  public void setTargetBeanName(@Nullable String targetBeanName) {
    this.targetBeanName = StringUtils.trimAllWhitespace(targetBeanName);
  }

  /**
   * Specify the property path to apply to the target.
   *
   * @param propertyPath the property path, potentially nested
   * (e.g. "age" or "spouse.age")
   */
  public void setPropertyPath(@Nullable String propertyPath) {
    this.propertyPath = StringUtils.trimAllWhitespace(propertyPath);
  }

  /**
   * Specify the type of the result from evaluating the property path.
   * <p>Note: This is not necessary for directly specified target objects
   * or singleton target beans, where the type can be determined through
   * introspection. Just specify this in case of a prototype target,
   * provided that you need matching by type (for example, for autowiring).
   *
   * @param resultType the result type, for example "java.lang.Integer"
   */
  public void setResultType(@Nullable Class<?> resultType) {
    this.resultType = resultType;
  }

  /**
   * The bean name of this PropertyPathFactoryBean will be interpreted
   * as "beanName.property" pattern, if neither "targetObject" nor
   * "targetBeanName" nor "propertyPath" have been specified.
   * This allows for concise bean definitions with just an id/name.
   */
  @Override
  public void setBeanName(String beanName) {
    this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;

    if (targetBeanWrapper != null && targetBeanName != null) {
      throw new IllegalArgumentException("Specify either 'targetObject' or 'targetBeanName', not both");
    }

    if (targetBeanWrapper == null && targetBeanName == null) {
      if (propertyPath != null) {
        throw new IllegalArgumentException(
                "Specify 'targetObject' or 'targetBeanName' in combination with 'propertyPath'");
      }

      // No other properties specified: check bean name.
      int dotIndex = beanName != null ? beanName.indexOf('.') : -1;
      if (dotIndex == -1) {
        throw new IllegalArgumentException(
                "Neither 'targetObject' nor 'targetBeanName' specified, and PropertyPathFactoryBean " +
                        "bean name '" + beanName + "' does not follow 'beanName.property' syntax");
      }
      this.targetBeanName = beanName.substring(0, dotIndex);
      this.propertyPath = beanName.substring(dotIndex + 1);
    }
    else if (propertyPath == null) {
      // either targetObject or targetBeanName specified
      throw new IllegalArgumentException("'propertyPath' is required");
    }

    if (targetBeanWrapper == null && beanFactory.isSingleton(targetBeanName)) {
      // Eagerly fetch singleton target bean, and determine result type.
      Object bean = beanFactory.getBean(targetBeanName);
      this.targetBeanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(bean);
      this.resultType = targetBeanWrapper.getPropertyType(propertyPath);
    }
  }

  @Override
  @Nullable
  public Object getObject() throws BeansException {
    BeanWrapper target = this.targetBeanWrapper;
    if (target != null) {
      if (logger.isWarnEnabled() && targetBeanName != null
              && beanFactory instanceof ConfigurableBeanFactory configurable
              && configurable.isCurrentlyInCreation(targetBeanName)) {
        logger.warn("Target bean '{}' is still in creation due to a circular " +
                "reference - obtained value for property '{}' may be outdated!", targetBeanName, propertyPath);
      }
    }
    else {
      // Fetch prototype target bean...
      Assert.state(beanFactory != null, "No BeanFactory available");
      Assert.state(targetBeanName != null, "No target bean name specified");
      Object bean = beanFactory.getBean(targetBeanName);
      target = PropertyAccessorFactory.forBeanPropertyAccess(bean);
    }
    Assert.state(propertyPath != null, "No property path specified");
    return target.getPropertyValue(propertyPath);
  }

  @Override
  public Class<?> getObjectType() {
    return this.resultType;
  }

  /**
   * While this FactoryBean will often be used for singleton targets,
   * the invoked getters for the property path might return a new object
   * for each call, so we have to assume that we're not returning the
   * same object for each {@link #getObject()} call.
   */
  @Override
  public boolean isSingleton() {
    return false;
  }

}
