/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory.xml;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static org.assertj.core.api.Assertions.assertThat;

class XmlBeanDefinitionStoreExceptionTests {

  @Test
  void exposesLineNumberAndResourceForParseErrors() {
    var cause = new SAXParseException("malformed element", null, null, 42, 3);
    var exception = new XmlBeanDefinitionStoreException("beans.xml", "Parse failed", cause);

    assertThat(exception.getLineNumber()).isEqualTo(42);
    assertThat(exception.getResourceDescription()).isEqualTo("beans.xml");
    assertThat(exception).hasCause(cause);
  }

  @Test
  void returnsMinusOneForSaxErrorsWithoutLocation() {
    var exception = new XmlBeanDefinitionStoreException("beans.xml", "Read failed", new SAXException("read error"));

    assertThat(exception.getLineNumber()).isEqualTo(-1);
  }

}
