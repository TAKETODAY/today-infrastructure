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

package infra.scripting.config;

import infra.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@code NamespaceHandler} that supports the wiring of
 * objects backed by dynamic languages such as Groovy, JRuby and
 * BeanShell. The following is an example (from the reference
 * documentation) that details the wiring of a Groovy backed bean:
 *
 * <pre class="code">
 * &lt;lang:groovy id="messenger"
 *     refresh-check-delay="5000"
 *     script-source="classpath:Messenger.groovy"&gt;
 * &lt;lang:property name="message" value="I Can Do The Frug"/&gt;
 * &lt;/lang:groovy&gt;
 * </pre>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:47
 */
public class LangNamespaceHandler extends NamespaceHandlerSupport {

  @Override
  public void init() {
    registerScriptBeanDefinitionParser("bsh", "infra.scripting.bsh.BshScriptFactory");
    registerScriptBeanDefinitionParser("groovy", "infra.scripting.groovy.GroovyScriptFactory");
    registerScriptBeanDefinitionParser("std", "infra.scripting.support.StandardScriptFactory");
    registerBeanDefinitionParser("defaults", new ScriptingDefaultsParser());
  }

  private void registerScriptBeanDefinitionParser(String key, String scriptFactoryClassName) {
    registerBeanDefinitionParser(key, new ScriptBeanDefinitionParser(scriptFactoryClassName));
  }

}
