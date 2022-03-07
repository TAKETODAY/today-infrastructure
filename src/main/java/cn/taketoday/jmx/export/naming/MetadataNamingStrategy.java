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

package cn.taketoday.jmx.export.naming;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.jmx.export.annotation.AnnotationJmxAttributeSource;
import cn.taketoday.jmx.export.metadata.JmxAttributeSource;
import cn.taketoday.jmx.export.metadata.ManagedResource;
import cn.taketoday.jmx.support.ObjectNameManager;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * An implementation of the {@link ObjectNamingStrategy} interface
 * that reads the {@code ObjectName} from the source-level metadata.
 * Falls back to the bean key (bean name) if no {@code ObjectName}
 * can be found in source-level metadata.
 *
 * <p>Uses the {@link JmxAttributeSource} strategy interface, so that
 * metadata can be read using any supported implementation. Out of the box,
 * {@link AnnotationJmxAttributeSource}
 * introspects a well-defined set of Java 5 annotations that come with Spring.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see ObjectNamingStrategy
 * @see AnnotationJmxAttributeSource
 * @since 4.0
 */
public class MetadataNamingStrategy implements ObjectNamingStrategy, InitializingBean {

  /**
   * The {@code JmxAttributeSource} implementation to use for reading metadata.
   */
  @Nullable
  private JmxAttributeSource attributeSource;

  @Nullable
  private String defaultDomain;

  /**
   * Create a new {@code MetadataNamingStrategy} which needs to be
   * configured through the {@link #setAttributeSource} method.
   */
  public MetadataNamingStrategy() { }

  /**
   * Create a new {@code MetadataNamingStrategy} for the given
   * {@code JmxAttributeSource}.
   *
   * @param attributeSource the JmxAttributeSource to use
   */
  public MetadataNamingStrategy(JmxAttributeSource attributeSource) {
    Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
    this.attributeSource = attributeSource;
  }

  /**
   * Set the implementation of the {@code JmxAttributeSource} interface to use
   * when reading the source-level metadata.
   */
  public void setAttributeSource(JmxAttributeSource attributeSource) {
    Assert.notNull(attributeSource, "JmxAttributeSource must not be null");
    this.attributeSource = attributeSource;
  }

  /**
   * Specify the default domain to be used for generating ObjectNames
   * when no source-level metadata has been specified.
   * <p>The default is to use the domain specified in the bean name
   * (if the bean name follows the JMX ObjectName syntax); else,
   * the package name of the managed bean class.
   */
  public void setDefaultDomain(String defaultDomain) {
    this.defaultDomain = defaultDomain;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.attributeSource == null) {
      throw new IllegalArgumentException("Property 'attributeSource' is required");
    }
  }

  /**
   * Reads the {@code ObjectName} from the source-level metadata associated
   * with the managed resource's {@code Class}.
   */
  @Override
  public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
    Assert.state(attributeSource != null, "No JmxAttributeSource set");
    Class<?> managedClass = AopUtils.getTargetClass(managedBean);
    ManagedResource mr = attributeSource.getManagedResource(managedClass);

    // Check that an object name has been specified.
    if (mr != null && StringUtils.hasText(mr.getObjectName())) {
      return ObjectNameManager.getInstance(mr.getObjectName());
    }
    else {
      Assert.state(beanKey != null, "No ManagedResource attribute and no bean key specified");
      try {
        return ObjectNameManager.getInstance(beanKey);
      }
      catch (MalformedObjectNameException ex) {
        String domain = this.defaultDomain;
        if (domain == null) {
          domain = ClassUtils.getPackageName(managedClass);
        }
        Hashtable<String, String> properties = new Hashtable<>();
        properties.put("type", ClassUtils.getShortName(managedClass));
        properties.put("name", beanKey);
        return ObjectNameManager.getInstance(domain, properties);
      }
    }
  }

}
