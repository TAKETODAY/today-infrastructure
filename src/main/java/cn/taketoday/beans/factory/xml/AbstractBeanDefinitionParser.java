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

package cn.taketoday.beans.factory.xml;

import org.w3c.dom.Element;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.ReaderContext;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionReaderUtils;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Abstract {@link BeanDefinitionParser} implementation providing
 * a number of convenience methods and a
 * {@link BeanDefinitionParser#parseInternal template method}
 * that subclasses must override to provide the actual parsing logic.
 *
 * <p>Use this {@link BeanDefinitionParser} implementation when you want
 * to parse some arbitrarily complex XML into one or more
 * {@link BeanDefinition BeanDefinitions}. If you just want to parse some
 * XML into a single {@code BeanDefinition}, you may wish to consider
 * the simpler convenience extensions of this class, namely
 * {@link AbstractSingleBeanDefinitionParser} and
 * {@link AbstractSimpleBeanDefinitionParser}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Dave Syer
 * @since 4.0
 */
public abstract class AbstractBeanDefinitionParser implements BeanDefinitionParser {

  /** Constant for the "id" attribute. */
  public static final String ID_ATTRIBUTE = "id";

  /** Constant for the "name" attribute. */
  public static final String NAME_ATTRIBUTE = "name";

  @Override
  @Nullable
  public final BeanDefinition parse(Element element, ParserContext parserContext) {
    BeanDefinition definition = parseInternal(element, parserContext);
    if (definition != null && !parserContext.isNested()) {
      try {
        String id = resolveId(element, definition, parserContext);
        if (!StringUtils.hasText(id)) {
          parserContext.getReaderContext().error(
                  "Id is required for element '" + parserContext.getDelegate().getLocalName(element)
                          + "' when used as a top-level tag", element);
        }
        String[] aliases = null;
        if (shouldParseNameAsAliases()) {
          String name = element.getAttribute(NAME_ATTRIBUTE);
          if (StringUtils.isNotEmpty(name)) {
            aliases = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
          }
        }
        definition.setBeanName(id);
        definition.setAliases(aliases);
        registerBeanDefinition(definition, parserContext.getRegistry());
        if (shouldFireEvents()) {
          BeanComponentDefinition componentDefinition = new BeanComponentDefinition(definition);
          postProcessComponentDefinition(componentDefinition);
          parserContext.registerComponent(componentDefinition);
        }
      }
      catch (BeanDefinitionStoreException ex) {
        String msg = ex.getMessage();
        parserContext.getReaderContext().error((msg != null ? msg : ex.toString()), element);
        return null;
      }
    }
    return definition;
  }

  /**
   * Resolve the ID for the supplied {@link BeanDefinition}.
   * <p>When using {@link #shouldGenerateId generation}, a name is generated automatically.
   * Otherwise, the ID is extracted from the "id" attribute, potentially with a
   * {@link #shouldGenerateIdAsFallback() fallback} to a generated id.
   *
   * @param element the element that the bean definition has been built from
   * @param definition the bean definition to be registered
   * @param parserContext the object encapsulating the current state of the parsing process;
   * provides access to a {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}
   * @return the resolved id
   * @throws BeanDefinitionStoreException if no unique name could be generated
   * for the given bean definition
   */
  protected String resolveId(Element element, BeanDefinition definition, ParserContext parserContext)
          throws BeanDefinitionStoreException {

    if (shouldGenerateId()) {
      return parserContext.getReaderContext().generateBeanName(definition);
    }
    else {
      String id = element.getAttribute(ID_ATTRIBUTE);
      if (!StringUtils.hasText(id) && shouldGenerateIdAsFallback()) {
        id = parserContext.getReaderContext().generateBeanName(definition);
      }
      return id;
    }
  }

  /**
   * Register the supplied {@link BeanDefinition bean} with the supplied
   * {@link BeanDefinitionRegistry registry}.
   * <p>Subclasses can override this method to control whether or not the supplied
   * {@link BeanDefinition bean} is actually even registered, or to
   * register even more beans.
   * <p>The default implementation registers the supplied {@link BeanDefinition bean}
   * with the supplied {@link BeanDefinitionRegistry registry} only if the {@code isNested}
   * parameter is {@code false}, because one typically does not want inner beans
   * to be registered as top level beans.
   *
   * @param definition the bean definition to be registered
   * @param registry the registry that the bean is to be registered with
   * @see BeanDefinitionReaderUtils#registerBeanDefinition(BeanDefinition, BeanDefinitionRegistry)
   */
  protected void registerBeanDefinition(BeanDefinition definition, BeanDefinitionRegistry registry) {
    BeanDefinitionReaderUtils.registerBeanDefinition(definition, registry);
  }

  /**
   * Central template method to actually parse the supplied {@link Element}
   * into one or more {@link BeanDefinition BeanDefinitions}.
   *
   * @param element the element that is to be parsed into one or more {@link BeanDefinition BeanDefinitions}
   * @param parserContext the object encapsulating the current state of the parsing process;
   * provides access to a {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}
   * @return the primary {@link BeanDefinition} resulting from the parsing of the supplied {@link Element}
   * @see #parse(Element, ParserContext)
   * @see #postProcessComponentDefinition(BeanComponentDefinition)
   */
  @Nullable
  protected abstract BeanDefinition parseInternal(Element element, ParserContext parserContext);

  /**
   * Should an ID be generated instead of read from the passed in {@link Element}?
   * <p>Disabled by default; subclasses can override this to enable ID generation.
   * Note that this flag is about <i>always</i> generating an ID; the parser
   * won't even check for an "id" attribute in this case.
   *
   * @return whether the parser should always generate an id
   */
  protected boolean shouldGenerateId() {
    return false;
  }

  /**
   * Should an ID be generated instead if the passed in {@link Element} does not
   * specify an "id" attribute explicitly?
   * <p>Disabled by default; subclasses can override this to enable ID generation
   * as fallback: The parser will first check for an "id" attribute in this case,
   * only falling back to a generated ID if no value was specified.
   *
   * @return whether the parser should generate an id if no id was specified
   */
  protected boolean shouldGenerateIdAsFallback() {
    return false;
  }

  /**
   * Determine whether the element's "name" attribute should get parsed as
   * bean definition aliases, i.e. alternative bean definition names.
   * <p>The default implementation returns {@code true}.
   *
   * @return whether the parser should evaluate the "name" attribute as aliases
   */
  protected boolean shouldParseNameAsAliases() {
    return true;
  }

  /**
   * Determine whether this parser is supposed to fire a
   * {@link BeanComponentDefinition}
   * event after parsing the bean definition.
   * <p>This implementation returns {@code true} by default; that is,
   * an event will be fired when a bean definition has been completely parsed.
   * Override this to return {@code false} in order to suppress the event.
   *
   * @return {@code true} in order to fire a component registration event
   * after parsing the bean definition; {@code false} to suppress the event
   * @see #postProcessComponentDefinition
   * @see ReaderContext#fireComponentRegistered
   */
  protected boolean shouldFireEvents() {
    return true;
  }

  /**
   * Hook method called after the primary parsing of a
   * {@link BeanComponentDefinition} but before the
   * {@link BeanComponentDefinition} has been registered with a
   * {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}.
   * <p>Derived classes can override this method to supply any custom logic that
   * is to be executed after all the parsing is finished.
   * <p>The default implementation is a no-op.
   *
   * @param componentDefinition the {@link BeanComponentDefinition} that is to be processed
   */
  protected void postProcessComponentDefinition(BeanComponentDefinition componentDefinition) {
  }

}
