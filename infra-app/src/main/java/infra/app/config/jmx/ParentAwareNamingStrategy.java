/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.config.jmx;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import infra.beans.BeansException;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.jmx.export.metadata.JmxAttributeSource;
import infra.jmx.export.naming.MetadataNamingStrategy;
import infra.jmx.support.JmxUtils;
import infra.jmx.support.ObjectNameManager;
import infra.util.ObjectUtils;

/**
 * Extension of {@link MetadataNamingStrategy} that supports a parent
 * {@link ApplicationContext}.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/9 18:38
 */
@SuppressWarnings("NullAway")
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
