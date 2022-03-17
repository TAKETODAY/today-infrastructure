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

package cn.taketoday.core.testfixture.xml;

import org.assertj.core.api.AssertProvider;
import org.xmlunit.assertj.XmlAssert;

import java.io.StringWriter;

/**
 * {@link AssertProvider} to allow XML content assertions. Ultimately delegates
 * to {@link XmlAssert}.
 *
 * @author Phillip Webb
 */
public class XmlContent implements AssertProvider<XmlContentAssert> {

  private final Object source;

  private XmlContent(Object source) {
    this.source = source;
  }

  @Override
  public XmlContentAssert assertThat() {
    return new XmlContentAssert(this.source);
  }

  public static XmlContent from(Object source) {
    return of(source);
  }

  public static XmlContent of(Object source) {
    if (source instanceof StringWriter) {
      return of(source.toString());
    }
    return new XmlContent(source);
  }

}
