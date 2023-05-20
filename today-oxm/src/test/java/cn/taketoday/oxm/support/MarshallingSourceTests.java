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

package cn.taketoday.oxm.support;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.XMLReader;

import cn.taketoday.oxm.support.MarshallingSource.MarshallingXMLReader;
import cn.taketoday.oxm.xstream.XStreamMarshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/20 11:54
 */
class MarshallingSourceTests {

  @Test
  void thrownBy() {
    XStreamMarshaller marshaller = new XStreamMarshaller();
    MarshallingSource marshallingSource = new MarshallingSource(marshaller, "");

    assertThatThrownBy(() -> marshallingSource.setInputSource(null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("setInputSource is not supported");

    assertThatThrownBy(() -> marshallingSource.setXMLReader(null))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessage("setXMLReader is not supported");

    XMLReader xmlReader = marshallingSource.getXMLReader();
    assertThat(xmlReader)
            .isInstanceOf(MarshallingXMLReader.class);

    assertThatThrownBy(() -> xmlReader.getFeature("test"))
            .isInstanceOf(SAXNotRecognizedException.class)
            .hasMessage("test");

    assertThatThrownBy(() -> xmlReader.setFeature("test", false))
            .isInstanceOf(SAXNotRecognizedException.class)
            .hasMessage("test");
  }

  @Test
  void marshallingSource() {
    XStreamMarshaller marshaller = new XStreamMarshaller();
    MarshallingSource marshallingSource = new MarshallingSource(marshaller, "");

    assertThat(marshallingSource.getMarshaller())
            .isInstanceOf(XStreamMarshaller.class)
            .isSameAs(marshaller);

    assertThat(marshallingSource.getContent()).isEqualTo("");
  }

}