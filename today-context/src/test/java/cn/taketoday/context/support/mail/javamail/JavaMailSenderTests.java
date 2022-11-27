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

package cn.taketoday.context.support.mail.javamail;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import cn.taketoday.context.support.mail.MailParseException;
import cn.taketoday.context.support.mail.MailSendException;
import cn.taketoday.context.support.mail.SimpleMailMessage;
import cn.taketoday.util.ObjectUtils;
import jakarta.activation.FileTypeMap;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 09.10.2004
 */
public class JavaMailSenderTests {

  @Test
  public void javaMailSenderWithSimpleMessage() throws MessagingException, IOException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setPort(30);
    sender.setUsername("username");
    sender.setPassword("password");

    SimpleMailMessage simpleMessage = new SimpleMailMessage();
    simpleMessage.setFrom("me@mail.org");
    simpleMessage.setReplyTo("reply@mail.org");
    simpleMessage.setTo("you@mail.org");
    simpleMessage.setCc("he@mail.org", "she@mail.org");
    simpleMessage.setBcc("us@mail.org", "them@mail.org");
    Date sentDate = new GregorianCalendar(2004, 1, 1).getTime();
    simpleMessage.setSentDate(sentDate);
    simpleMessage.setSubject("my subject");
    simpleMessage.setText("my text");
    sender.send(simpleMessage);

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedPort()).isEqualTo(30);
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();

    assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
    MimeMessage sentMessage = sender.transport.getSentMessage(0);
    List<Address> froms = Arrays.asList(sentMessage.getFrom());
    assertThat(froms.size()).isEqualTo(1);
    assertThat(((InternetAddress) froms.get(0)).getAddress()).isEqualTo("me@mail.org");
    List<Address> replyTos = Arrays.asList(sentMessage.getReplyTo());
    assertThat(((InternetAddress) replyTos.get(0)).getAddress()).isEqualTo("reply@mail.org");
    List<Address> tos = Arrays.asList(sentMessage.getRecipients(Message.RecipientType.TO));
    assertThat(tos.size()).isEqualTo(1);
    assertThat(((InternetAddress) tos.get(0)).getAddress()).isEqualTo("you@mail.org");
    List<Address> ccs = Arrays.asList(sentMessage.getRecipients(Message.RecipientType.CC));
    assertThat(ccs.size()).isEqualTo(2);
    assertThat(((InternetAddress) ccs.get(0)).getAddress()).isEqualTo("he@mail.org");
    assertThat(((InternetAddress) ccs.get(1)).getAddress()).isEqualTo("she@mail.org");
    List<Address> bccs = Arrays.asList(sentMessage.getRecipients(Message.RecipientType.BCC));
    assertThat(bccs.size()).isEqualTo(2);
    assertThat(((InternetAddress) bccs.get(0)).getAddress()).isEqualTo("us@mail.org");
    assertThat(((InternetAddress) bccs.get(1)).getAddress()).isEqualTo("them@mail.org");
    assertThat(sentMessage.getSentDate().getTime()).isEqualTo(sentDate.getTime());
    assertThat(sentMessage.getSubject()).isEqualTo("my subject");
    assertThat(sentMessage.getContent()).isEqualTo("my text");
  }

  @Test
  public void javaMailSenderWithSimpleMessages() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    SimpleMailMessage simpleMessage1 = new SimpleMailMessage();
    simpleMessage1.setTo("he@mail.org");
    SimpleMailMessage simpleMessage2 = new SimpleMailMessage();
    simpleMessage2.setTo("she@mail.org");
    sender.send(simpleMessage1, simpleMessage2);

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();

    assertThat(sender.transport.getSentMessages().size()).isEqualTo(2);
    MimeMessage sentMessage1 = sender.transport.getSentMessage(0);
    List<Address> tos1 = Arrays.asList(sentMessage1.getRecipients(Message.RecipientType.TO));
    assertThat(tos1.size()).isEqualTo(1);
    assertThat(((InternetAddress) tos1.get(0)).getAddress()).isEqualTo("he@mail.org");
    MimeMessage sentMessage2 = sender.transport.getSentMessage(1);
    List<Address> tos2 = Arrays.asList(sentMessage2.getRecipients(Message.RecipientType.TO));
    assertThat(tos2.size()).isEqualTo(1);
    assertThat(((InternetAddress) tos2.get(0)).getAddress()).isEqualTo("she@mail.org");
  }

  @Test
  public void javaMailSenderWithMimeMessage() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    MimeMessage mimeMessage = sender.createMimeMessage();
    mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress("you@mail.org"));
    sender.send(mimeMessage);

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(mimeMessage);
  }

  @Test
  public void javaMailSenderWithMimeMessages() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    MimeMessage mimeMessage1 = sender.createMimeMessage();
    mimeMessage1.setRecipient(Message.RecipientType.TO, new InternetAddress("he@mail.org"));
    MimeMessage mimeMessage2 = sender.createMimeMessage();
    mimeMessage2.setRecipient(Message.RecipientType.TO, new InternetAddress("she@mail.org"));
    sender.send(mimeMessage1, mimeMessage2);

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(2);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(mimeMessage1);
    assertThat(sender.transport.getSentMessage(1)).isEqualTo(mimeMessage2);
  }

  @Test
  public void javaMailSenderWithMimeMessagePreparator() {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    final List<Message> messages = new ArrayList<>();

    MimeMessagePreparator preparator = mimeMessage -> {
      mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress("you@mail.org"));
      messages.add(mimeMessage);
    };
    sender.send(preparator);

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(messages.get(0));
  }

  @Test
  public void javaMailSenderWithMimeMessagePreparators() {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    final List<Message> messages = new ArrayList<>();

    MimeMessagePreparator preparator1 = mimeMessage -> {
      mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress("he@mail.org"));
      messages.add(mimeMessage);
    };
    MimeMessagePreparator preparator2 = mimeMessage -> {
      mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress("she@mail.org"));
      messages.add(mimeMessage);
    };
    sender.send(preparator1, preparator2);

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(2);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(messages.get(0));
    assertThat(sender.transport.getSentMessage(1)).isEqualTo(messages.get(1));
  }

  @Test
  public void javaMailSenderWithMimeMessageHelper() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    MimeMessageHelper message = new MimeMessageHelper(sender.createMimeMessage());
    assertThat(message.getEncoding()).isNull();
    boolean condition = message.getFileTypeMap() instanceof ConfigurableMimeFileTypeMap;
    assertThat(condition).isTrue();

    message.setTo("you@mail.org");
    sender.send(message.getMimeMessage());

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(message.getMimeMessage());
  }

  @Test
  public void javaMailSenderWithMimeMessageHelperAndSpecificEncoding() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    MimeMessageHelper message = new MimeMessageHelper(sender.createMimeMessage(), "UTF-8");
    assertThat(message.getEncoding()).isEqualTo("UTF-8");
    FileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
    message.setFileTypeMap(fileTypeMap);
    assertThat(message.getFileTypeMap()).isEqualTo(fileTypeMap);

    message.setTo("you@mail.org");
    sender.send(message.getMimeMessage());

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(message.getMimeMessage());
  }

  @Test
  public void javaMailSenderWithMimeMessageHelperAndDefaultEncoding() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");
    sender.setDefaultEncoding("UTF-8");

    FileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
    sender.setDefaultFileTypeMap(fileTypeMap);
    MimeMessageHelper message = new MimeMessageHelper(sender.createMimeMessage());
    assertThat(message.getEncoding()).isEqualTo("UTF-8");
    assertThat(message.getFileTypeMap()).isEqualTo(fileTypeMap);

    message.setTo("you@mail.org");
    sender.send(message.getMimeMessage());

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(message.getMimeMessage());
  }

  @Test
  public void javaMailSenderWithParseExceptionOnSimpleMessage() {
    MockJavaMailSender sender = new MockJavaMailSender();
    SimpleMailMessage simpleMessage = new SimpleMailMessage();
    simpleMessage.setFrom("");
    try {
      sender.send(simpleMessage);
    }
    catch (MailParseException ex) {
      // expected
      boolean condition = ex.getCause() instanceof AddressException;
      assertThat(condition).isTrue();
    }
  }

  @Test
  public void javaMailSenderWithParseExceptionOnMimeMessagePreparator() {
    MockJavaMailSender sender = new MockJavaMailSender();
    MimeMessagePreparator preparator = mimeMessage -> mimeMessage.setFrom(new InternetAddress(""));
    try {
      sender.send(preparator);
    }
    catch (MailParseException ex) {
      // expected
      boolean condition = ex.getCause() instanceof AddressException;
      assertThat(condition).isTrue();
    }
  }

  @Test
  public void javaMailSenderWithCustomSession() throws MessagingException {
    final Session session = Session.getInstance(new Properties());
    MockJavaMailSender sender = new MockJavaMailSender() {
      @Override
      protected Transport getTransport(Session sess) throws NoSuchProviderException {
        assertThat(sess).isEqualTo(session);
        return super.getTransport(sess);
      }
    };
    sender.setSession(session);
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    MimeMessage mimeMessage = sender.createMimeMessage();
    mimeMessage.setSubject("custom");
    mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress("you@mail.org"));
    mimeMessage.setSentDate(new GregorianCalendar(2005, 3, 1).getTime());
    sender.send(mimeMessage);

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(mimeMessage);
  }

  @Test
  public void javaMailProperties() throws MessagingException {
    Properties props = new Properties();
    props.setProperty("bogusKey", "bogusValue");
    MockJavaMailSender sender = new MockJavaMailSender() {
      @Override
      protected Transport getTransport(Session sess) throws NoSuchProviderException {
        assertThat(sess.getProperty("bogusKey")).isEqualTo("bogusValue");
        return super.getTransport(sess);
      }
    };
    sender.setJavaMailProperties(props);
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    MimeMessage mimeMessage = sender.createMimeMessage();
    mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress("you@mail.org"));
    sender.send(mimeMessage);

    assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
    assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
    assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
    assertThat(sender.transport.isCloseCalled()).isTrue();
    assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
    assertThat(sender.transport.getSentMessage(0)).isEqualTo(mimeMessage);
  }

  @Test
  public void failedMailServerConnect() {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost(null);
    sender.setUsername("username");
    sender.setPassword("password");
    SimpleMailMessage simpleMessage1 = new SimpleMailMessage();
    assertThatExceptionOfType(MailSendException.class).isThrownBy(() ->
                    sender.send(simpleMessage1))
            .satisfies(ex -> assertThat(ex.getFailedMessages()).containsExactly(Assertions.entry(simpleMessage1, (Exception) ex.getCause())));
  }

  @Test
  public void failedMailServerClose() {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("");
    sender.setUsername("username");
    sender.setPassword("password");
    SimpleMailMessage simpleMessage1 = new SimpleMailMessage();
    assertThatExceptionOfType(MailSendException.class).isThrownBy(() ->
                    sender.send(simpleMessage1))
            .satisfies(ex -> assertThat(ex.getFailedMessages()).isEmpty());
  }

  @Test
  public void failedSimpleMessage() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    SimpleMailMessage simpleMessage1 = new SimpleMailMessage();
    simpleMessage1.setTo("he@mail.org");
    simpleMessage1.setSubject("fail");
    SimpleMailMessage simpleMessage2 = new SimpleMailMessage();
    simpleMessage2.setTo("she@mail.org");

    try {
      sender.send(simpleMessage1, simpleMessage2);
    }
    catch (MailSendException ex) {
      assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
      assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
      assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
      assertThat(sender.transport.isCloseCalled()).isTrue();
      assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
      assertThat(sender.transport.getSentMessage(0).getAllRecipients()[0]).isEqualTo(new InternetAddress("she@mail.org"));
      assertThat(ex.getFailedMessages().size()).isEqualTo(1);
      assertThat(ex.getFailedMessages().keySet().iterator().next()).isEqualTo(simpleMessage1);
      Object subEx = ex.getFailedMessages().values().iterator().next();
      boolean condition = subEx instanceof MessagingException;
      assertThat(condition).isTrue();
      assertThat(((MessagingException) subEx).getMessage()).isEqualTo("failed");
    }
  }

  @Test
  public void failedMimeMessage() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.setUsername("username");
    sender.setPassword("password");

    MimeMessage mimeMessage1 = sender.createMimeMessage();
    mimeMessage1.setRecipient(Message.RecipientType.TO, new InternetAddress("he@mail.org"));
    mimeMessage1.setSubject("fail");
    MimeMessage mimeMessage2 = sender.createMimeMessage();
    mimeMessage2.setRecipient(Message.RecipientType.TO, new InternetAddress("she@mail.org"));

    try {
      sender.send(mimeMessage1, mimeMessage2);
    }
    catch (MailSendException ex) {
      assertThat(sender.transport.getConnectedHost()).isEqualTo("host");
      assertThat(sender.transport.getConnectedUsername()).isEqualTo("username");
      assertThat(sender.transport.getConnectedPassword()).isEqualTo("password");
      assertThat(sender.transport.isCloseCalled()).isTrue();
      assertThat(sender.transport.getSentMessages().size()).isEqualTo(1);
      assertThat(sender.transport.getSentMessage(0)).isEqualTo(mimeMessage2);
      assertThat(ex.getFailedMessages().size()).isEqualTo(1);
      assertThat(ex.getFailedMessages().keySet().iterator().next()).isEqualTo(mimeMessage1);
      Object subEx = ex.getFailedMessages().values().iterator().next();
      boolean condition = subEx instanceof MessagingException;
      assertThat(condition).isTrue();
      assertThat(((MessagingException) subEx).getMessage()).isEqualTo("failed");
    }
  }

  @Test
  public void testConnection() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost("host");
    sender.testConnection();
  }

  @Test
  public void testConnectionWithFailure() throws MessagingException {
    MockJavaMailSender sender = new MockJavaMailSender();
    sender.setHost(null);
    assertThatExceptionOfType(MessagingException.class).isThrownBy(
            sender::testConnection);
  }

  private static class MockJavaMailSender extends JavaMailSenderImpl {

    private MockTransport transport;

    @Override
    protected Transport getTransport(Session session) throws NoSuchProviderException {
      this.transport = new MockTransport(session, null);
      return transport;
    }
  }

  private static class MockTransport extends Transport {

    private String connectedHost = null;
    private int connectedPort = -2;
    private String connectedUsername = null;
    private String connectedPassword = null;
    private boolean closeCalled = false;
    private List<Message> sentMessages = new ArrayList<>();

    private MockTransport(Session session, URLName urlName) {
      super(session, urlName);
    }

    public String getConnectedHost() {
      return connectedHost;
    }

    public int getConnectedPort() {
      return connectedPort;
    }

    public String getConnectedUsername() {
      return connectedUsername;
    }

    public String getConnectedPassword() {
      return connectedPassword;
    }

    public boolean isCloseCalled() {
      return closeCalled;
    }

    public List<Message> getSentMessages() {
      return sentMessages;
    }

    public MimeMessage getSentMessage(int index) {
      return (MimeMessage) this.sentMessages.get(index);
    }

    @Override
    public void connect(String host, int port, String username, String password) throws MessagingException {
      if (host == null) {
        throw new MessagingException("no host");
      }
      this.connectedHost = host;
      this.connectedPort = port;
      this.connectedUsername = username;
      this.connectedPassword = password;
      setConnected(true);
    }

    @Override
    public synchronized void close() throws MessagingException {
      if ("".equals(connectedHost)) {
        throw new MessagingException("close failure");
      }
      this.closeCalled = true;
    }

    @Override
    public void sendMessage(Message message, Address[] addresses) throws MessagingException {
      if ("fail".equals(message.getSubject())) {
        throw new MessagingException("failed");
      }
      if (addresses == null || (message.getAllRecipients() == null ? addresses.length > 0 :
                                !ObjectUtils.nullSafeEquals(addresses, message.getAllRecipients()))) {
        throw new MessagingException("addresses not correct");
      }
      if (message.getSentDate() == null) {
        throw new MessagingException("No sentDate specified");
      }
      if (message.getSubject() != null && message.getSubject().contains("custom")) {
        assertThat(message.getSentDate()).isEqualTo(new GregorianCalendar(2005, 3, 1).getTime());
      }
      this.sentMessages.add(message);
    }
  }

}
