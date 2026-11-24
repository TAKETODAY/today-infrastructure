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

package infra.mail.javamail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import infra.core.io.ByteArrayResource;
import infra.core.io.InputStreamResource;
import infra.core.io.Resource;
import jakarta.mail.Address;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class MimeMessageHelperTests {

  private final Session session = Session.getInstance(new Properties());

  private final MimeMessage message = new MimeMessage(this.session);

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = "UTF-8")
  void appendsRecipientsAndPreservesPersonalNames(String encoding) throws Exception {
    MimeMessageHelper helper = new MimeMessageHelper(this.message, encoding);
    helper.setValidateAddresses(true);
    helper.setFrom("sender@example.org", "Émetteur");
    helper.setReplyTo("reply@example.org", "Réponse");
    helper.setTo("first@example.org");
    helper.setCc("copy@example.org");
    helper.setBcc("hidden@example.org");
    helper.addTo("second@example.org");
    helper.addCc("copy2@example.org");
    helper.addBcc("hidden2@example.org");
    helper.addTo("third@example.org", "Réception");
    helper.addCc("copy3@example.org", "Copié");
    helper.addBcc("hidden3@example.org", "Caché");
    helper.setSubject("Résumé");
    helper.setPriority(2);
    helper.setText("Body");

    MimeMessage received = roundTrip();
    assertThat(received.getSubject()).isEqualTo("Résumé");
    assertThat(received.getHeader("X-Priority", null)).isEqualTo("2");
    assertThat((InternetAddress) received.getFrom()[0]).satisfies(address -> {
      assertThat(address.getAddress()).isEqualTo("sender@example.org");
      assertThat(address.getPersonal()).isEqualTo("Émetteur");
    });
    assertThat((InternetAddress) received.getReplyTo()[0]).satisfies(address -> {
      assertThat(address.getAddress()).isEqualTo("reply@example.org");
      assertThat(address.getPersonal()).isEqualTo("Réponse");
    });
    assertRecipients(received, RecipientType.TO, "first@example.org", "second@example.org", "third@example.org", "Réception");
    assertRecipients(received, RecipientType.CC, "copy@example.org", "copy2@example.org", "copy3@example.org", "Copié");
    assertRecipients(received, RecipientType.BCC, "hidden@example.org", "hidden2@example.org", "hidden3@example.org", "Caché");
  }

  @Test
  void rejectsInvalidAddressWithoutAddingItToRecipients() throws Exception {
    MimeMessageHelper helper = new MimeMessageHelper(this.message);
    helper.setTo("local-mailbox");
    helper.setValidateAddresses(true);

    assertThatExceptionOfType(AddressException.class).isThrownBy(() -> helper.addTo("other-mailbox"));
    assertThat(this.message.getRecipients(RecipientType.TO)).containsExactly(new InternetAddress("local-mailbox"));
  }

  @Test
  void reportsUnsupportedEncodingForEmbeddedPersonalName() {
    MimeMessageHelper helper = new MimeMessageHelper(this.message, "not-a-charset");

    assertThatExceptionOfType(MessagingException.class)
            .isThrownBy(() -> helper.setTo("Émetteur <sender@example.org>"))
            .withMessageContaining("Failed to parse embedded personal name")
            .withCauseInstanceOf(UnsupportedEncodingException.class);
  }

  @ParameterizedTest
  @CsvSource({ "1, mixed", "2, related" })
  void usesOneMultipartForBodyInlineAndAttachment(int mode, String subtype) throws Exception {
    MimeMessageHelper helper = new MimeMessageHelper(this.message, mode);
    helper.setText("<b>body</b>", true);
    helper.addInline("logo", "logo.png", bytes("image"));
    helper.addAttachment("report.txt", bytes("report"));

    assertThat(helper.getMimeMultipart()).isSameAs(helper.getRootMimeMultipart());
    MimeMessage received = roundTrip();
    assertThat(received.isMimeType("multipart/" + subtype)).isTrue();
    MimeMultipart parts = (MimeMultipart) received.getContent();
    assertThat(parts.getCount()).isEqualTo(3);
    assertThat(parts.getBodyPart(0).isMimeType("text/html")).isTrue();
    assertThat(parts.getBodyPart(0).getContent()).isEqualTo("<b>body</b>");
    assertFile((MimeBodyPart) parts.getBodyPart(1), Part.INLINE, "logo.png", "image/png", "image");
    assertFile((MimeBodyPart) parts.getBodyPart(2), Part.ATTACHMENT, "report.txt", "text/plain", "report");
  }

  @Test
  void keepsInlinePartsWithAlternativeBodyAndFilesAtTheRoot(@TempDir Path directory) throws Exception {
    Path logo = Files.writeString(directory.resolve("logo.png"), "image");
    Path report = Files.writeString(directory.resolve("report.txt"), "report");
    MimeMessageHelper helper = new MimeMessageHelper(this.message, true, "UTF-8");
    helper.setText("Draft");
    helper.setEncodeFilenames(true);
    helper.addInline("guide", "café.txt", bytes("guide"));
    helper.setText("Résumé", "<b>Résumé</b><img src='cid:logo'>");
    helper.addInline("logo", logo.toFile());
    helper.addAttachment("résumé.txt", report.toFile());

    MimeMessage received = roundTrip();
    assertThat(received.isMimeType("multipart/mixed")).isTrue();
    MimeMultipart root = (MimeMultipart) received.getContent();
    assertThat(root.getCount()).isEqualTo(2);
    assertThat(root.getBodyPart(0).isMimeType("multipart/related")).isTrue();
    MimeMultipart related = (MimeMultipart) root.getBodyPart(0).getContent();
    assertThat(related.getCount()).isEqualTo(3);
    assertThat(related.getBodyPart(0).isMimeType("multipart/alternative")).isTrue();
    MimeMultipart alternatives = (MimeMultipart) related.getBodyPart(0).getContent();
    assertThat(alternatives.getCount()).isEqualTo(2);
    assertThat(alternatives.getBodyPart(0).isMimeType("text/plain")).isTrue();
    assertThat(alternatives.getBodyPart(0).getContent()).isEqualTo("Résumé");
    assertThat(alternatives.getBodyPart(1).isMimeType("text/html")).isTrue();
    assertThat(alternatives.getBodyPart(1).getContent()).isEqualTo("<b>Résumé</b><img src='cid:logo'>");
    assertFile((MimeBodyPart) related.getBodyPart(1), Part.INLINE, "café.txt", "text/plain", "guide");
    MimeBodyPart inline = (MimeBodyPart) related.getBodyPart(2);
    assertThat(inline.getContentID()).isEqualTo("<logo>");
    assertFile(inline, Part.INLINE, null, "image/png", "image");
    assertFile((MimeBodyPart) root.getBodyPart(1), Part.ATTACHMENT, "résumé.txt", "text/plain", "report");
  }

  @Test
  void infersContentTypeFromNamedInlineResource() throws Exception {
    Resource logo = new ByteArrayResource("image".getBytes(StandardCharsets.UTF_8)) {
      @Override
      public String getName() {
        return "logo.png";
      }
    };
    MimeMessageHelper helper = new MimeMessageHelper(this.message, true);
    helper.setText("body");
    helper.addInline("logo", logo);

    MimeBodyPart inline = (MimeBodyPart) helper.getMimeMultipart().getBodyPart(1);
    assertThat(inline.getContentID()).isEqualTo("<logo>");
    assertThat(inline.getDataHandler().getContentType()).isEqualTo("image/png");
  }

  @Test
  void requiresRepeatableStreamsForInlinePartsAndAttachments() throws Exception {
    MimeMessageHelper helper = new MimeMessageHelper(this.message, true);
    try (var input = new ByteArrayInputStream(new byte[] { 1 })) {
      Resource openResource = new InputStreamResource(input);
      assertThatIllegalArgumentException().isThrownBy(() -> helper.addInline("logo", openResource))
              .withMessageContaining("fresh stream");
      assertThatIllegalArgumentException().isThrownBy(() -> helper.addAttachment("report.txt", openResource))
              .withMessageContaining("fresh stream");
    }
    helper.addAttachment("report.txt", bytes("report"));
    var dataSource = helper.getRootMimeMultipart().getBodyPart(1).getDataHandler().getDataSource();
    assertThat(dataSource.getName()).isEqualTo("report.txt");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(dataSource::getOutputStream)
            .withMessageContaining("Read-only");
    try (var first = dataSource.getInputStream(); var second = dataSource.getInputStream()) {
      assertThat(first).isNotSameAs(second);
      assertThat(first.readAllBytes()).isEqualTo(second.readAllBytes());
    }
  }

  @Test
  void rejectsMultipartContentWhenMultipartModeIsDisabled() throws Exception {
    MimeMessageHelper helper = new MimeMessageHelper(this.message, false);
    helper.setText("body");

    assertThat(helper.isMultipart()).isFalse();
    assertThatIllegalStateException().isThrownBy(() -> helper.setText("plain", "<b>html</b>"))
            .withMessageContaining("Not in multipart mode");
    assertThatIllegalStateException().isThrownBy(() -> helper.addAttachment("report.txt", bytes("report")))
            .withMessageContaining("Not in multipart mode");
    assertThat(roundTrip().getContent()).isEqualTo("body");
    assertThatIllegalArgumentException().isThrownBy(() -> new MimeMessageHelper(this.message, -1))
            .withMessageContaining("multipart modes");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void rejectsMissingRecipientAddress(String address) {
    MimeMessageHelper helper = new MimeMessageHelper(this.message);
    if (address == null) {
      assertThatIllegalArgumentException().isThrownBy(() -> helper.addTo(address)).withMessageContaining("To address");
    }
    else {
      assertThatExceptionOfType(AddressException.class).isThrownBy(() -> helper.addTo(address));
    }
  }

  private MimeMessage roundTrip() throws Exception {
    this.message.saveChanges();
    var output = new ByteArrayOutputStream();
    this.message.writeTo(output);
    return new MimeMessage(this.session, new ByteArrayInputStream(output.toByteArray()));
  }

  private static void assertRecipients(MimeMessage message, RecipientType type,
          String first, String second, String third, String personal) throws MessagingException {
    Address[] recipients = message.getRecipients(type);
    assertThat(Arrays.stream(recipients).map(InternetAddress.class::cast))
            .extracting(InternetAddress::getAddress).containsExactly(first, second, third);
    assertThat(((InternetAddress) recipients[2]).getPersonal()).isEqualTo(personal);
  }

  private static void assertFile(MimeBodyPart part, String disposition, String filename, String type, String text) throws Exception {
    assertThat(part.getDisposition()).isEqualTo(disposition);
    String actualFilename = part.getFileName();
    assertThat(actualFilename != null ? MimeUtility.decodeText(actualFilename) : null).isEqualTo(filename);
    assertThat(part.isMimeType(type)).isTrue();
    try (var input = part.getInputStream()) {
      assertThat(input.readAllBytes()).isEqualTo(text.getBytes(StandardCharsets.UTF_8));
    }
  }

  private static ByteArrayResource bytes(String text) {
    return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
  }
}
