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

package cn.taketoday.beans.factory.parsing;

import cn.taketoday.beans.BeanMetadataElement;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanReference;

/**
 * Interface that describes the logical view of a set of {@link BeanDefinition BeanDefinitions}
 * and {@link BeanReference BeanReferences} as presented in some configuration context.
 *
 * <p>With the introduction of {@link cn.taketoday.beans.factory.xml.NamespaceHandler pluggable custom XML tags},
 * it is now possible for a single logical configuration entity, in this case an XML tag, to
 * create multiple {@link BeanDefinition BeanDefinitions} and {@link BeanReference RuntimeBeanReferences}
 * in order to provide more succinct configuration and greater convenience to end users. As such, it can
 * no longer be assumed that each configuration entity (e.g. XML tag) maps to one {@link BeanDefinition}.
 * For tool vendors and other users who wish to present visualization or support for configuring Framework
 * applications it is important that there is some mechanism in place to tie the {@link BeanDefinition BeanDefinitions}
 * in the {@link cn.taketoday.beans.factory.BeanFactory} back to the configuration data in a way
 * that has concrete meaning to the end user. As such, {@link cn.taketoday.beans.factory.xml.NamespaceHandler}
 * implementations are able to publish events in the form of a {@code ComponentDefinition} for each
 * logical entity being configured. Third parties can then {@link ReaderEventListener subscribe to these events},
 * allowing for a user-centric view of the bean metadata.
 *
 * <p>Each {@code ComponentDefinition} has a {@link #getSource source object} which is configuration-specific.
 * In the case of XML-based configuration this is typically the {@link org.w3c.dom.Node} which contains the user
 * supplied configuration information. In addition to this, each {@link BeanDefinition} enclosed in a
 * {@code ComponentDefinition} has its own {@link BeanDefinition#getSource() source object} which may point
 * to a different, more specific, set of configuration data. Beyond this, individual pieces of bean metadata such
 * as the {@link cn.taketoday.beans.PropertyValue PropertyValues} may also have a source object giving an
 * even greater level of detail. Source object extraction is handled through the
 * {@link SourceExtractor} which can be customized as required.
 *
 * <p>Whilst direct access to important {@link BeanReference BeanReferences} is provided through
 * {@link #getBeanReferences}, tools may wish to inspect all {@link BeanDefinition BeanDefinitions} to gather
 * the full set of {@link BeanReference BeanReferences}. Implementations are required to provide
 * all {@link BeanReference BeanReferences} that are required to validate the configuration of the
 * overall logical entity as well as those required to provide full user visualisation of the configuration.
 * It is expected that certain {@link BeanReference BeanReferences} will not be important to
 * validation or to the user view of the configuration and as such these may be omitted. A tool may wish to
 * display any additional {@link BeanReference BeanReferences} sourced through the supplied
 * {@link BeanDefinition BeanDefinitions} but this is not considered to be a typical case.
 *
 * <p>Tools can determine the important of contained {@link BeanDefinition BeanDefinitions} by checking the
 * {@link BeanDefinition#getRole role identifier}. The role is essentially a hint to the tool as to how
 * important the configuration provider believes a {@link BeanDefinition} is to the end user. It is expected
 * that tools will <strong>not</strong> display all {@link BeanDefinition BeanDefinitions} for a given
 * {@code ComponentDefinition} choosing instead to filter based on the role. Tools may choose to make
 * this filtering user configurable. Particular notice should be given to the
 * {@link BeanDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE role identifier}. {@link BeanDefinition BeanDefinitions}
 * classified with this role are completely unimportant to the end user and are required only for
 * internal implementation reasons.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see AbstractComponentDefinition
 * @see CompositeComponentDefinition
 * @see BeanComponentDefinition
 * @see ReaderEventListener#componentRegistered(ComponentDefinition)
 * @since 4.0
 */
public interface ComponentDefinition extends BeanMetadataElement {

  /**
   * Get the user-visible name of this {@code ComponentDefinition}.
   * <p>This should link back directly to the corresponding configuration data
   * for this component in a given context.
   */
  String getName();

  /**
   * Return a friendly description of the described component.
   * <p>Implementations are encouraged to return the same value from
   * {@code toString()}.
   */
  String getDescription();

  /**
   * Return the {@link BeanDefinition BeanDefinitions} that were registered
   * to form this {@code ComponentDefinition}.
   * <p>It should be noted that a {@code ComponentDefinition} may well be related with
   * other {@link BeanDefinition BeanDefinitions} via {@link BeanReference references},
   * however these are <strong>not</strong> included as they may be not available immediately.
   * Important {@link BeanReference BeanReferences} are available from {@link #getBeanReferences()}.
   *
   * @return the array of BeanDefinitions, or an empty array if none
   */
  BeanDefinition[] getBeanDefinitions();

  /**
   * Return the {@link BeanDefinition BeanDefinitions} that represent all relevant
   * inner beans within this component.
   * <p>Other inner beans may exist within the associated {@link BeanDefinition BeanDefinitions},
   * however these are not considered to be needed for validation or for user visualization.
   *
   * @return the array of BeanDefinitions, or an empty array if none
   */
  BeanDefinition[] getInnerBeanDefinitions();

  /**
   * Return the set of {@link BeanReference BeanReferences} that are considered
   * to be important to this {@code ComponentDefinition}.
   * <p>Other {@link BeanReference BeanReferences} may exist within the associated
   * {@link BeanDefinition BeanDefinitions}, however these are not considered
   * to be needed for validation or for user visualization.
   *
   * @return the array of BeanReferences, or an empty array if none
   */
  BeanReference[] getBeanReferences();

}
