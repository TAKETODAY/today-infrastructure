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

package cn.taketoday.annotation.config.jmx;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import cn.taketoday.beans.BeansException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.jmx.export.metadata.JmxAttributeSource;
import cn.taketoday.jmx.export.naming.MetadataNamingStrategy;
import cn.taketoday.jmx.support.JmxUtils;
import cn.taketoday.jmx.support.ObjectNameManager;
import cn.taketoday.util.ObjectUtils;

/**
 * Extension of {@link MetadataNamingStrategy} that supports a parent
 * {@link ApplicationContext}.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/9 18:38
 */
public class ParentAwareNamingStrategy extends MetadataNamingStrategy implements ApplicationContextAware {

  private ApplicationContext applicationContext;

  private boolean ensureUniqueRuntimeObjectNames;

  public ParentAwareNamingStrategy(JmxAttributeSource attributeSource) {
    super(attributeSource);
  }

  /**
   * Set if unique runtime object names should be ensured.
   *
   * @param ensureUniqueRuntimeObjectNames {@code true} if unique names should be
   * ensured.
   */
  public void setEnsureUniqueRuntimeObjectNames(boolean ensureUniqueRuntimeObjectNames) {
    this.ensureUniqueRuntimeObjectNames = ensureUniqueRuntimeObjectNames;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
    ObjectName name = super.getObjectName(managedBean, beanKey);
    if (this.ensureUniqueRuntimeObjectNames) {
      return JmxUtils.appendIdentityToObjectName(name, managedBean);
    }
    if (parentContextContainsSameBean(this.applicationContext, beanKey)) {
      return appendToObjectName(name, "context", ObjectUtils.getIdentityHexString(this.applicationContext));
    }
    return name;
  }

  private boolean parentContextContainsSameBean(ApplicationContext context, String beanKey) {
    ApplicationContext parent = context.getParent();
    if (parent == null) {
      return false;
    }
    try {
      parent.getBean(beanKey);
      return true;
    }
    catch (BeansException ex) {
      return parentContextContainsSameBean(parent, beanKey);
    }
  }

  private ObjectName appendToObjectName(ObjectName name, String key, String value)
          throws MalformedObjectNameException {
    Hashtable<String, String> keyProperties = name.getKeyPropertyList();
    keyProperties.put(key, value);
    return ObjectNameManager.getInstance(name.getDomain(), keyProperties);
  }

}
