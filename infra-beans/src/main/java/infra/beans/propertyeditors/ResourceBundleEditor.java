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

package infra.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.Locale;
import java.util.ResourceBundle;

import infra.beans.factory.config.CustomEditorConfigurer;
import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * {@link java.beans.PropertyEditor} implementation for standard JDK
 * {@link ResourceBundle ResourceBundles}.
 *
 * <p>Only supports conversion <i>from</i> a String, but not <i>to</i> a String.
 *
 * Find below some examples of using this class in a (properly configured)
 * Framework container using XML-based metadata:
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        the 'messages' property is of type java.util.ResourceBundle.
 *        the 'DialogMessages.properties' file exists at the root of the CLASSPATH
 *    --&gt;
 *    &lt;property name="messages" value="DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        the 'DialogMessages.properties' file exists in the 'com/messages' package
 *    --&gt;
 *    &lt;property name="messages" value="com/messages/DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>A 'properly configured' Framework {@link infra.context.ApplicationContext container}
 * might contain a {@link CustomEditorConfigurer}
 * definition such that the conversion can be effected transparently:
 *
 * <pre class="code"> &lt;bean class="infra.beans.support.CustomEditorConfigurer"&gt;
 *    &lt;property name="customEditors"&gt;
 *        &lt;map&gt;
 *            &lt;entry key="java.util.ResourceBundle"&gt;
 *                &lt;bean class="infra.beans.propertyeditors.ResourceBundleEditor"/&gt;
 *            &lt;/entry&gt;
 *        &lt;/map&gt;
 *    &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>Please note that this {@link java.beans.PropertyEditor} is <b>not</b>
 * registered by default with any of the Framework infrastructure.
 *
 * <p>Thanks to David Leal Valmana for the suggestion and initial prototype.
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ResourceBundleEditor extends PropertyEditorSupport {

  /**
   * The separator used to distinguish between the base name and the locale
   * (if any) when {@link #setAsText(String) converting from a String}.
   */
  public static final String BASE_NAME_SEPARATOR = "_";

  @Override
  public void setAsText(String text) throws IllegalArgumentException {
    Assert.hasText(text, "'text' must not be empty");
    String name = text.trim();

    int separator = name.indexOf(BASE_NAME_SEPARATOR);
    if (separator == -1) {
      setValue(ResourceBundle.getBundle(name));
    }
    else {
      // The name potentially contains locale information
      String baseName = name.substring(0, separator);
      if (StringUtils.isBlank(baseName)) {
        throw new IllegalArgumentException("Invalid ResourceBundle name: '" + text + "'");
      }
      String localeString = name.substring(separator + 1);
      Locale locale = StringUtils.parseLocaleString(localeString);
      setValue(locale != null ? ResourceBundle.getBundle(baseName, locale) : ResourceBundle.getBundle(baseName));
    }
  }

}
