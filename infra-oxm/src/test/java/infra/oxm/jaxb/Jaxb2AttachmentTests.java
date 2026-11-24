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

package infra.oxm.jaxb;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import infra.oxm.UnmarshallingFailureException;
import infra.oxm.mime.MimeContainer;
import jakarta.activation.DataHandler;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import jakarta.xml.bind.attachment.AttachmentUnmarshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class Jaxb2AttachmentTests {

  @Test
  void byteArrayAttachmentsPreserveSliceAndMimeType() throws Exception {
    MimeContainer container = mock();
    AttachmentMarshaller attachmentMarshaller = attachmentMarshaller(container);
    given(container.convertToXopPackage()).willReturn(true);
    assertThat(attachmentMarshaller.isXOPPackage()).isTrue();
    String cid = attachmentMarshaller.addMtomAttachment(new byte[] { 1, 2, 3, 4 }, 1, 2,
            "application/octet-stream", "https://example.org/types", "payload");
    ArgumentCaptor<String> id = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<DataHandler> handler = ArgumentCaptor.forClass(DataHandler.class);
    verify(container).addAttachment(id.capture(), handler.capture());
    assertThat(cid).startsWith("cid:").endsWith("%40example.org");
    assertThat(id.getValue()).startsWith("<").endsWith("@example.org>");
    assertThat(handler.getValue().getContentType()).isEqualTo("application/octet-stream");
    assertThat(handler.getValue().getName()).isEqualTo("ByteArrayDataSource");
    try (var stream = handler.getValue().getInputStream()) {
      assertThat(stream.readAllBytes()).containsExactly(2, 3);
    }
    assertThatThrownBy(() -> handler.getValue().getDataSource().getOutputStream())
            .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void invalidNamespaceFallsBackToDataHandlerName() {
    MimeContainer container = mock();
    AttachmentMarshaller attachmentMarshaller = attachmentMarshaller(container);
    DataHandler handler = mock();
    given(handler.getName()).willReturn("payload");
    assertThat(attachmentMarshaller.addMtomAttachment(handler, "http://[invalid", "payload"))
            .startsWith("cid:").endsWith("%40payload");
  }

  @Test
  void readsEncodedContentIdsAndRawAttachmentIds() throws Exception {
    MimeContainer container = mock();
    AttachmentUnmarshaller attachmentUnmarshaller = attachmentUnmarshaller(container);
    DataHandler handler = mock();
    given(container.getAttachment("<part@example.org>")).willReturn(handler);
    given(container.getAttachment("raw-id")).willReturn(handler);
    given(container.isXopPackage()).willReturn(true);
    given(handler.getInputStream()).willReturn(new ByteArrayInputStream(new byte[] { 4, 2 }));
    assertThat(attachmentUnmarshaller.isXOPPackage()).isTrue();
    assertThat(attachmentUnmarshaller.getAttachmentAsDataHandler("raw-id")).isSameAs(handler);
    assertThat(attachmentUnmarshaller.getAttachmentAsByteArray("cid:part%40example.org"))
            .containsExactly(4, 2);
    assertThatThrownBy(() -> attachmentUnmarshaller.getAttachmentAsDataHandler("cid:missing"))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("No attachment found for <missing>");
  }

  @Test
  void attachmentReadFailureIsTranslated() throws Exception {
    MimeContainer container = mock();
    AttachmentUnmarshaller attachmentUnmarshaller = attachmentUnmarshaller(container);
    DataHandler handler = mock();
    IOException failure = new IOException("unreadable attachment");
    given(container.getAttachment("raw-id")).willReturn(handler);
    given(handler.getInputStream()).willThrow(failure);
    assertThatThrownBy(() -> attachmentUnmarshaller.getAttachmentAsByteArray("raw-id"))
            .isInstanceOf(UnmarshallingFailureException.class).hasCause(failure);
  }

  private static AttachmentMarshaller attachmentMarshaller(MimeContainer container) {
    Marshaller delegate = mock();
    var marshaller = new Jaxb2Marshaller() {
      @Override
      public Marshaller createMarshaller() {
        return delegate;
      }
    };
    marshaller.setMtomEnabled(true);
    marshaller.marshal("graph", new StreamResult(new StringWriter()), container);
    ArgumentCaptor<AttachmentMarshaller> captor = ArgumentCaptor.forClass(AttachmentMarshaller.class);
    verify(delegate).setAttachmentMarshaller(captor.capture());
    return captor.getValue();
  }

  private static AttachmentUnmarshaller attachmentUnmarshaller(MimeContainer container) {
    Unmarshaller delegate = mock();
    var marshaller = new Jaxb2Marshaller() {
      @Override
      public Unmarshaller createUnmarshaller() {
        return delegate;
      }
    };
    marshaller.setMtomEnabled(true);
    marshaller.unmarshal(new DOMSource(), container);
    ArgumentCaptor<AttachmentUnmarshaller> captor = ArgumentCaptor.forClass(AttachmentUnmarshaller.class);
    verify(delegate).setAttachmentUnmarshaller(captor.capture());
    return captor.getValue();
  }
}
