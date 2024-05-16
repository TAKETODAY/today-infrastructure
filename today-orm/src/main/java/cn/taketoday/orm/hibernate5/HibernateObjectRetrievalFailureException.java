/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.orm.hibernate5;

import org.hibernate.HibernateException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.ObjectRetrievalFailureException;
import cn.taketoday.util.ReflectionUtils;

/**
 * Hibernate-specific subclass of ObjectRetrievalFailureException.
 * Converts Hibernate's UnresolvableObjectException and WrongClassException.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionFactoryUtils#convertHibernateAccessException
 * @since 4.0
 */
public class HibernateObjectRetrievalFailureException extends ObjectRetrievalFailureException {

  public HibernateObjectRetrievalFailureException(UnresolvableObjectException ex) {
    super(ex.getEntityName(), getIdentifier(ex), ex.getMessage(), ex);
  }

  public HibernateObjectRetrievalFailureException(WrongClassException ex) {
    super(ex.getEntityName(), getIdentifier(ex), ex.getMessage(), ex);
  }

  @Nullable
  static Object getIdentifier(HibernateException hibEx) {
    try {
      // getIdentifier declares Serializable return value on 5.x but Object on 6.x
      // -> not binary compatible, let's invoke it reflectively for the time being
      return ReflectionUtils.invokeMethod(hibEx.getClass().getMethod("getIdentifier"), hibEx);
    }
    catch (NoSuchMethodException ex) {
      return null;
    }
  }

}
