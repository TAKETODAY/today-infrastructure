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

package cn.taketoday.context.support.mail;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cn.taketoday.context.support.mail.SimpleMailMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @since 10.09.2003
 */
public class SimpleMailMessageTests {

  @Test
  public void testSimpleMessageCopyCtor() {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom("me@mail.org");
    message.setTo("you@mail.org");

    SimpleMailMessage messageCopy = new SimpleMailMessage(message);
    assertThat(messageCopy.getFrom()).isEqualTo("me@mail.org");
    assertThat(messageCopy.getTo()[0]).isEqualTo("you@mail.org");

    message.setReplyTo("reply@mail.org");
    message.setCc(new String[] { "he@mail.org", "she@mail.org" });
    message.setBcc(new String[] { "us@mail.org", "them@mail.org" });
    Date sentDate = new Date();
    message.setSentDate(sentDate);
    message.setSubject("my subject");
    message.setText("my text");

    assertThat(message.getFrom()).isEqualTo("me@mail.org");
    assertThat(message.getReplyTo()).isEqualTo("reply@mail.org");
    assertThat(message.getTo()[0]).isEqualTo("you@mail.org");
    List<String> ccs = Arrays.asList(message.getCc());
    assertThat(ccs.contains("he@mail.org")).isTrue();
    assertThat(ccs.contains("she@mail.org")).isTrue();
    List<String> bccs = Arrays.asList(message.getBcc());
    assertThat(bccs.contains("us@mail.org")).isTrue();
    assertThat(bccs.contains("them@mail.org")).isTrue();
    assertThat(message.getSentDate()).isEqualTo(sentDate);
    assertThat(message.getSubject()).isEqualTo("my subject");
    assertThat(message.getText()).isEqualTo("my text");

    messageCopy = new SimpleMailMessage(message);
    assertThat(messageCopy.getFrom()).isEqualTo("me@mail.org");
    assertThat(messageCopy.getReplyTo()).isEqualTo("reply@mail.org");
    assertThat(messageCopy.getTo()[0]).isEqualTo("you@mail.org");
    ccs = Arrays.asList(messageCopy.getCc());
    assertThat(ccs.contains("he@mail.org")).isTrue();
    assertThat(ccs.contains("she@mail.org")).isTrue();
    bccs = Arrays.asList(message.getBcc());
    assertThat(bccs.contains("us@mail.org")).isTrue();
    assertThat(bccs.contains("them@mail.org")).isTrue();
    assertThat(messageCopy.getSentDate()).isEqualTo(sentDate);
    assertThat(messageCopy.getSubject()).isEqualTo("my subject");
    assertThat(messageCopy.getText()).isEqualTo("my text");
  }

  @Test
  public void testDeepCopyOfStringArrayTypedFieldsOnCopyCtor() throws Exception {

    SimpleMailMessage original = new SimpleMailMessage();
    original.setTo(new String[] { "fiona@mail.org", "apple@mail.org" });
    original.setCc(new String[] { "he@mail.org", "she@mail.org" });
    original.setBcc(new String[] { "us@mail.org", "them@mail.org" });

    SimpleMailMessage copy = new SimpleMailMessage(original);

    original.getTo()[0] = "mmm@mmm.org";
    original.getCc()[0] = "mmm@mmm.org";
    original.getBcc()[0] = "mmm@mmm.org";

    assertThat(copy.getTo()[0]).isEqualTo("fiona@mail.org");
    assertThat(copy.getCc()[0]).isEqualTo("he@mail.org");
    assertThat(copy.getBcc()[0]).isEqualTo("us@mail.org");
  }

  /**
   * Tests that two equal SimpleMailMessages have equal hash codes.
   */
  @Test
  public final void testHashCode() {
    SimpleMailMessage message1 = new SimpleMailMessage();
    message1.setFrom("from@somewhere");
    message1.setReplyTo("replyTo@somewhere");
    message1.setTo("to@somewhere");
    message1.setCc("cc@somewhere");
    message1.setBcc("bcc@somewhere");
    message1.setSentDate(new Date());
    message1.setSubject("subject");
    message1.setText("text");

    // Copy the message
    SimpleMailMessage message2 = new SimpleMailMessage(message1);

    assertThat(message2).isEqualTo(message1);
    assertThat(message2.hashCode()).isEqualTo(message1.hashCode());
  }

  public final void testEqualsObject() {
    SimpleMailMessage message1;
    SimpleMailMessage message2;

    // Same object is equal
    message1 = new SimpleMailMessage();
    message2 = message1;
    assertThat(message1.equals(message2)).isTrue();

    // Null object is not equal
    message1 = new SimpleMailMessage();
    message2 = null;
    boolean condition1 = !(message1.equals(message2));
    assertThat(condition1).isTrue();

    // Different class is not equal
    boolean condition = !(message1.equals(new Object()));
    assertThat(condition).isTrue();

    // Equal values are equal
    message1 = new SimpleMailMessage();
    message2 = new SimpleMailMessage();
    assertThat(message1.equals(message2)).isTrue();

    message1 = new SimpleMailMessage();
    message1.setFrom("from@somewhere");
    message1.setReplyTo("replyTo@somewhere");
    message1.setTo("to@somewhere");
    message1.setCc("cc@somewhere");
    message1.setBcc("bcc@somewhere");
    message1.setSentDate(new Date());
    message1.setSubject("subject");
    message1.setText("text");
    message2 = new SimpleMailMessage(message1);
    assertThat(message1.equals(message2)).isTrue();
  }

  @Test
  public void testCopyCtorChokesOnNullOriginalMessage() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new SimpleMailMessage(null));
  }

  @Test
  public void testCopyToChokesOnNullTargetMessage() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new SimpleMailMessage().copyTo(null));
  }

}
