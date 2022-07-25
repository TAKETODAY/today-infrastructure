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

package cn.taketoday.transaction.interceptor;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.util.StringUtils;

/**
 * Property editor that converts a String into a {@link TransactionAttributeSource}.
 * The transaction attribute string must be parseable by the
 * {@link TransactionAttributeEditor} in this package.
 *
 * <p>Strings are in property syntax, with the form:<br>
 * {@code FQCN.methodName=&lt;transaction attribute string&gt;}
 *
 * <p>For example:<br>
 * {@code com.mycompany.mycode.MyClass.myMethod=PROPAGATION_MANDATORY,ISOLATION_DEFAULT}
 *
 * <p><b>NOTE:</b> The specified class must be the one where the methods are
 * defined; in case of implementing an interface, the interface class name.
 *
 * <p>Note: Will register all overloaded methods for a given name.
 * Does not support explicit registration of certain overloaded methods.
 * Supports "xxx*" mappings, e.g. "notify*" for "notify" and "notifyAll".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionAttributeEditor
 * @since 4.0 2022/3/7 15:33
 */
public class TransactionAttributeSourceEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    MethodMapTransactionAttributeSource source = new MethodMapTransactionAttributeSource();
    if (StringUtils.isNotEmpty(text)) {
      // Use properties editor to tokenize the hold string.
      Properties props = PropertiesUtils.parse(text);

      // Now we have properties, process each one individually.
      for (String name : props.stringPropertyNames()) {
        String value = props.getProperty(name);
        // Convert value to a transaction attribute.

        TransactionAttribute attr = TransactionAttribute.parse(value);
        // Register name and attribute.
        source.addTransactionalMethod(name, attr);
      }
    }
    setValue(source);
  }

}
