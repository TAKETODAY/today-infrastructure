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

package cn.taketoday.framework.test.mock.mockito;

import java.util.List;

import cn.taketoday.test.context.ContextConfigurationAttributes;
import cn.taketoday.test.context.ContextCustomizer;
import cn.taketoday.test.context.ContextCustomizerFactory;
import cn.taketoday.test.context.TestContextAnnotationUtils;

/**
 * A {@link ContextCustomizerFactory} to add Mockito support.
 *
 * @author Phillip Webb
 */
class MockitoContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {
    // We gather the explicit mock definitions here since they form part of the
    // MergedContextConfiguration key. Different mocks need to have a different key.
    DefinitionsParser parser = new DefinitionsParser();
    parseDefinitions(testClass, parser);
    return new MockitoContextCustomizer(parser.getDefinitions());
  }

  private void parseDefinitions(Class<?> testClass, DefinitionsParser parser) {
    parser.parse(testClass);
    if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
      parseDefinitions(testClass.getEnclosingClass(), parser);
    }
  }

}
