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

package infra.web.view.xslt;

import org.junit.jupiter.api.Test;

import javax.xml.transform.TransformerException;

import infra.logging.Logger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class SimpleTransformErrorListenerTests {

  @Test
  void logsRecoverableErrorsButPropagatesFatalErrors() throws Exception {
    Logger logger = mock(Logger.class);
    SimpleTransformErrorListener listener = new SimpleTransformErrorListener(logger);
    TransformerException failure = new TransformerException("sample");
    listener.warning(failure);
    listener.error(failure);
    then(logger).should().warn("XSLT transformation warning", failure);
    then(logger).should().error("XSLT transformation error", failure);
    assertThatThrownBy(() -> listener.fatalError(failure)).isSameAs(failure);
  }

}
