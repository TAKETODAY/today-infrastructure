/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jmx.export.naming;

import org.jspecify.annotations.Nullable;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import infra.jmx.support.ObjectNameManager;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;

/**
 * An implementation of the {@code ObjectNamingStrategy} interface that
 * creates a name based on the identity of a given instance.
 *
 * <p>The resulting {@code ObjectName} will be in the form
 * <i>package</i>:class=<i>class name</i>,hashCode=<i>identity hash (in hex)</i>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class IdentityNamingStrategy implements ObjectNamingStrategy {

  /**
   * The type key.
   */
  public static final String TYPE_KEY = "type";

  /**
   * The hash code key.
   */
  public static final String HASH_CODE_KEY = "hashCode";

  /**
   * Returns an instance of {@code ObjectName} based on the identity
   * of the managed resource.
   */
  @Override
  public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
    String domain = ClassUtils.getPackageName(managedBean.getClass());
    Hashtable<String, String> keys = new Hashtable<>();
    keys.put(TYPE_KEY, ClassUtils.getShortName(managedBean.getClass()));
    keys.put(HASH_CODE_KEY, ObjectUtils.getIdentityHexString(managedBean));
    return ObjectNameManager.getInstance(domain, keys);
  }

}
