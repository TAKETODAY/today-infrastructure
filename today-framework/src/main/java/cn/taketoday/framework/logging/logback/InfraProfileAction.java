/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.logging.logback;

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.action.BaseModelAction;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;

/**
 * Logback {@link BaseModelAction} for {@code <infra-property>} tags. Allows a section of
 * a Logback configuration to only be enabled when a specific profile is active.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InfraProfileModel
 * @see InfraProfileModelHandler
 * @since 4.0
 */
class InfraProfileAction extends BaseModelAction {

  @Override
  protected Model buildCurrentModel(
          SaxEventInterpretationContext context, String name, Attributes attributes) {
    InfraProfileModel model = new InfraProfileModel();
    model.setName(attributes.getValue(NAME_ATTRIBUTE));
    return model;
  }

}
