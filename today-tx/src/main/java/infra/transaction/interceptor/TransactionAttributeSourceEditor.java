/*
 * Copyright 2002-present the original author or authors.
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

package infra.transaction.interceptor;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

import infra.core.io.PropertiesUtils;
import infra.util.StringUtils;

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
