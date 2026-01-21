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

import java.util.Date;

import infra.mail.MailParseException;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 14:49
 */
class MimeMailMessageTests {

  @Test
  void createWithMimeMessageSetsInternalMessage() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);

    assertThat(message.getMimeMessage()).isSameAs(mimeMessage);
  }

  @Test
  void setSubjectModifiesMimeMessage() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);

    message.setSubject("Test Subject");

    assertThat(mimeMessage.getSubject()).isEqualTo("Test Subject");
  }

  @Test
  void setTextModifiesMimeMessageContent() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);

    message.setText("Test Content");

    assertThat(mimeMessage.getContent()).isEqualTo("Test Content");
  }

  @Test
  void setToModifiesRecipients() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);
    var address = "test@example.com";

    message.setTo(address);

    assertThat(mimeMessage.getRecipients(Message.RecipientType.TO))
            .hasSize(1)
            .allMatch(addr -> addr.toString().equals(address));
  }

  @Test
  void setCcModifiesCcRecipients() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);
    var address = "cc@example.com";

    message.setCc(address);

    assertThat(mimeMessage.getRecipients(Message.RecipientType.CC))
            .hasSize(1)
            .allMatch(addr -> addr.toString().equals(address));
  }

  @Test
  void setBccModifiesBccRecipients() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);
    var address = "bcc@example.com";

    message.setBcc(address);

    assertThat(mimeMessage.getRecipients(Message.RecipientType.BCC))
            .hasSize(1)
            .allMatch(addr -> addr.toString().equals(address));
  }

  @Test
  void setFromModifiesSender() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);
    var address = "from@example.com";

    message.setFrom(address);

    assertThat(mimeMessage.getFrom())
            .hasSize(1)
            .allMatch(addr -> addr.toString().equals(address));
  }

  @Test
  void setReplyToModifiesReplyAddress() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);
    var address = "reply@example.com";

    message.setReplyTo(address);

    assertThat(mimeMessage.getReplyTo())
            .hasSize(1)
            .allMatch(addr -> addr.toString().equals(address));
  }

  @Test
  void setMultipleToRecipientsModifiesRecipients() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);
    var addresses = new String[] { "test1@example.com", "test2@example.com" };

    message.setTo(addresses);

    assertThat(mimeMessage.getRecipients(Message.RecipientType.TO))
            .hasSize(2)
            .extracting(Object::toString)
            .containsExactly(addresses);
  }

  @Test
  void setMultipleCcRecipientsModifiesRecipients() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);
    var addresses = new String[] { "cc1@example.com", "cc2@example.com" };

    message.setCc(addresses);

    assertThat(mimeMessage.getRecipients(Message.RecipientType.CC))
            .hasSize(2)
            .extracting(Object::toString)
            .containsExactly(addresses);
  }

  @Test
  void setMultipleBccRecipientsModifiesRecipients() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);
    var addresses = new String[] { "bcc1@example.com", "bcc2@example.com" };

    message.setBcc(addresses);

    assertThat(mimeMessage.getRecipients(Message.RecipientType.BCC))
            .hasSize(2)
            .extracting(Object::toString)
            .containsExactly(addresses);
  }

  @Test
  void invalidEmailAddressThrowsMailParseException() {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setTo("invalid email"));
  }

  @Test
  void createWithMimeMessageHelperReturnsHelper() {
    var mimeMessage = new MimeMessage((Session) null);
    var helper = new MimeMessageHelper(mimeMessage);
    var message = new MimeMailMessage(helper);

    assertThat(message.getMimeMessageHelper()).isSameAs(helper);
  }

  @Test
  void emptySubjectSetsNullInMimeMessage() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);

    message.setSubject("");

    assertThat(mimeMessage.getSubject()).isEqualTo("");
  }

  @Test
  void emptyTextSetsEmptyContentInMimeMessage() throws Exception {
    var mimeMessage = new MimeMessage((Session) null);
    var message = new MimeMailMessage(mimeMessage);

    message.setText("");

    assertThat(mimeMessage.getContent()).isEqualTo("");
  }

  @Test
  void messagingExceptionWhenSettingFromWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setFrom(Address address) throws MessagingException {
        throw new MessagingException("Failed to set from");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setFrom("test@example.com"))
            .withCauseInstanceOf(MessagingException.class)
            .withMessageContaining("Could not parse mail");
  }

  @Test
  void messagingExceptionWhenSettingToWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {

      @Override
      public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new MessagingException("Failed to set recipients");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setTo("test@example.com"))
            .withCauseInstanceOf(MessagingException.class);
  }

  @Test
  void messagingExceptionWhenSettingCcWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new MessagingException("Failed to set CC");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setCc("cc@example.com"))
            .withCauseInstanceOf(MessagingException.class);
  }

  @Test
  void messagingExceptionWhenSettingBccWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {

      @Override
      public void setRecipient(Message.RecipientType type, Address address) throws MessagingException {
        throw new MessagingException("Failed to set BCC");
      }

    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setBcc("bcc@example.com"))
            .withCauseInstanceOf(MessagingException.class);
  }

  @Test
  void messagingExceptionWhenSettingSubjectWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setSubject(String subject) throws MessagingException {
        throw new MessagingException("Failed to set subject");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setSubject("test subject"))
            .withCauseInstanceOf(MessagingException.class);
  }

  @Test
  void messagingExceptionWhenSettingTextWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setText(String text) throws MessagingException {
        throw new MessagingException("Failed to set text");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setText("test content"))
            .withCauseInstanceOf(MessagingException.class);
  }


  @Test
  void messagingExceptionWhenSetReplyToWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setReplyTo(Address[] addresses) throws MessagingException {
        throw new MessagingException("Failed to set reply-to");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setReplyTo("replyto@example.com"))
            .withCauseInstanceOf(MessagingException.class)
            .withMessageContaining("Could not parse mail");
  }

  @Test
  void messagingExceptionWhenSetSentDateWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setSentDate(Date date) throws MessagingException {
        throw new MessagingException("Failed to set sent date");
      }
    };
    var message = new MimeMailMessage(mimeMessage);
    var date = new Date();

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setSentDate(date))
            .withCauseInstanceOf(MessagingException.class)
            .withMessageContaining("Could not parse mail");
  }

  @Test
  void messagingExceptionWhenSetMultipleToWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new MessagingException("Failed to set multiple recipients");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setTo(new String[]{"test1@example.com", "test2@example.com"}))
            .withCauseInstanceOf(MessagingException.class);
  }

  @Test
  void messagingExceptionWhenSetMultipleCcWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new MessagingException("Failed to set multiple CC");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setCc(new String[]{"cc1@example.com", "cc2@example.com"}))
            .withCauseInstanceOf(MessagingException.class);
  }

  @Test
  void messagingExceptionWhenSetMultipleBccWrapsInMailParseException() throws Exception {
    var mimeMessage = new MimeMessage((Session) null) {
      @Override
      public void setRecipients(Message.RecipientType type, Address[] addresses) throws MessagingException {
        throw new MessagingException("Failed to set multiple BCC");
      }
    };
    var message = new MimeMailMessage(mimeMessage);

    assertThatExceptionOfType(MailParseException.class)
            .isThrownBy(() -> message.setBcc(new String[]{"bcc1@example.com", "bcc2@example.com"}))
            .withCauseInstanceOf(MessagingException.class);
  }


}