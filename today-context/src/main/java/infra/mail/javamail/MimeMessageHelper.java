/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.mail.javamail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import infra.core.io.InputStreamSource;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mail.MailMessage;
import infra.mail.SimpleMailMessage;
import infra.util.MimeType;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.activation.FileTypeMap;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimePart;
import jakarta.mail.internet.MimeUtility;

/**
 * Helper class for populating a {@link MimeMessage}.
 *
 * <p>Mirrors the simple setters of {@link SimpleMailMessage},
 * directly applying the values to the underlying MimeMessage. Allows for defining
 * a character encoding for the entire message, automatically applied by all methods
 * of this helper class.
 *
 * <p>Offers support for HTML text content, inline elements such as images, and typical
 * mail attachments. Also supports personal names that accompany mail addresses. Note that
 * advanced settings can still be applied directly to the underlying MimeMessage object!
 *
 * <p>Typically used in {@link MimeMessagePreparator} implementations or
 * {@link JavaMailSender} client code: simply instantiating it as a MimeMessage wrapper,
 * invoking setters on the wrapper, using the underlying MimeMessage for mail sending.
 * Also used internally by {@link JavaMailSenderImpl}.
 *
 * <p>Sample code for an HTML mail with an inline image and a PDF attachment:
 *
 * <pre class="code">
 * mailSender.send(new MimeMessagePreparator() {
 *   public void prepare(MimeMessage mimeMessage) throws MessagingException {
 *     MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
 *     message.setFrom("me@mail.com");
 *     message.setTo("you@mail.com");
 *     message.setSubject("my subject");
 *     message.setText("my text &lt;img src='cid:myLogo'&gt;", true);
 *     message.addInline("myLogo", new ClassPathResource("img/mylogo.gif"));
 *     message.addAttachment("myDocument.pdf", new ClassPathResource("doc/myDocument.pdf"));
 *   }
 * });</pre>
 *
 * Consider using {@link MimeMailMessage} (which implements the common
 * {@link MailMessage} interface, just like
 * {@link SimpleMailMessage}) on top of this helper,
 * in order to let message population code interact with a simple message
 * or a MIME message through a common interface.
 *
 * <p><b>Warning regarding multipart mails:</b> Simple MIME messages that
 * just contain HTML text but no inline elements or attachments will work on
 * more or less any email client that is capable of HTML rendering. However,
 * inline elements and attachments are still a major compatibility issue
 * between email clients: It's virtually impossible to get inline elements
 * and attachments working across Microsoft Outlook, Lotus Notes and Mac Mail.
 * Consider choosing a specific multipart mode for your needs: The javadoc
 * on the MULTIPART_MODE constants contains more detailed information.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setText(String, boolean)
 * @see #setText(String, String)
 * @see #addInline(String, Resource)
 * @see #addAttachment(String, InputStreamSource)
 * @see #MULTIPART_MODE_MIXED_RELATED
 * @see #MULTIPART_MODE_RELATED
 * @see #getMimeMessage()
 * @see JavaMailSender
 * @since 4.0
 */
public class MimeMessageHelper {

  /**
   * Constant indicating a non-multipart message.
   */
  public static final int MULTIPART_MODE_NO = 0;

  /**
   * Constant indicating a multipart message with a single root multipart
   * element of type "mixed". Texts, inline elements and attachements
   * will all get added to that root element.
   * <p>This was default behavior. It is known to work properly
   * on Outlook. However, other mail clients tend to misinterpret inline
   * elements as attachments and/or show attachments inline as well.
   */
  public static final int MULTIPART_MODE_MIXED = 1;

  /**
   * Constant indicating a multipart message with a single root multipart
   * element of type "related". Texts, inline elements and attachements
   * will all get added to that root element.
   * <p>This was the default behavior.
   * This is the "Microsoft multipart mode", as natively sent by Outlook.
   * It is known to work properly on Outlook, Outlook Express, Yahoo Mail, and
   * to a large degree also on Mac Mail (with an additional attachment listed
   * for an inline element, despite the inline element also shown inline).
   * Does not work properly on Lotus Notes (attachments won't be shown there).
   */
  public static final int MULTIPART_MODE_RELATED = 2;

  /**
   * Constant indicating a multipart message with a root multipart element
   * "mixed" plus a nested multipart element of type "related". Texts and
   * inline elements will get added to the nested "related" element,
   * while attachments will get added to the "mixed" root element.
   * <p>This is the default since. This is arguably the most correct
   * MIME structure, according to the MIME spec: It is known to work properly
   * on Outlook, Outlook Express, Yahoo Mail, and Lotus Notes. Does not work
   * properly on Mac Mail. If you target Mac Mail or experience issues with
   * specific mails on Outlook, consider using MULTIPART_MODE_RELATED instead.
   */
  public static final int MULTIPART_MODE_MIXED_RELATED = 3;

  private static final String MULTIPART_SUBTYPE_MIXED = "mixed";

  private static final String MULTIPART_SUBTYPE_RELATED = "related";

  private static final String MULTIPART_SUBTYPE_ALTERNATIVE = "alternative";

  private static final String CONTENT_TYPE_ALTERNATIVE = "text/alternative";

  private static final String CONTENT_TYPE_HTML = "text/html";

  private static final String CONTENT_TYPE_CHARSET_SUFFIX = ";charset=";

  private static final String HEADER_PRIORITY = "X-Priority";

  private final MimeMessage mimeMessage;

  @Nullable
  private MimeMultipart rootMimeMultipart;

  @Nullable
  private MimeMultipart mimeMultipart;

  @Nullable
  private final String encoding;

  private FileTypeMap fileTypeMap;

  private boolean encodeFilenames = false;

  private boolean validateAddresses = false;

  /**
   * Create a new MimeMessageHelper for the given MimeMessage,
   * assuming a simple text message (no multipart content,
   * i.e. no alternative texts and no inline elements or attachments).
   * <p>The character encoding for the message will be taken from
   * the passed-in MimeMessage object, if carried there. Else,
   * JavaMail's default encoding will be used.
   *
   * @param mimeMessage the mime message to work on
   * @see #MimeMessageHelper(jakarta.mail.internet.MimeMessage, boolean)
   * @see #getDefaultEncoding(jakarta.mail.internet.MimeMessage)
   * @see JavaMailSenderImpl#setDefaultEncoding
   */
  public MimeMessageHelper(MimeMessage mimeMessage) {
    this(mimeMessage, null);
  }

  /**
   * Create a new MimeMessageHelper for the given MimeMessage,
   * assuming a simple text message (no multipart content,
   * i.e. no alternative texts and no inline elements or attachments).
   *
   * @param mimeMessage the mime message to work on
   * @param encoding the character encoding to use for the message
   * @see #MimeMessageHelper(jakarta.mail.internet.MimeMessage, boolean)
   */
  public MimeMessageHelper(MimeMessage mimeMessage, @Nullable String encoding) {
    this.mimeMessage = mimeMessage;
    this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
    this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
  }

  /**
   * Create a new MimeMessageHelper for the given MimeMessage,
   * in multipart mode (supporting alternative texts, inline
   * elements and attachments) if requested.
   * <p>Consider using the MimeMessageHelper constructor that
   * takes a multipartMode argument to choose a specific multipart
   * mode other than MULTIPART_MODE_MIXED_RELATED.
   * <p>The character encoding for the message will be taken from
   * the passed-in MimeMessage object, if carried there. Else,
   * JavaMail's default encoding will be used.
   *
   * @param mimeMessage the mime message to work on
   * @param multipart whether to create a multipart message that
   * supports alternative texts, inline elements and attachments
   * (corresponds to MULTIPART_MODE_MIXED_RELATED)
   * @throws MessagingException if multipart creation failed
   * @see #MimeMessageHelper(jakarta.mail.internet.MimeMessage, int)
   * @see #getDefaultEncoding(jakarta.mail.internet.MimeMessage)
   * @see JavaMailSenderImpl#setDefaultEncoding
   */
  public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart) throws MessagingException {
    this(mimeMessage, multipart, null);
  }

  /**
   * Create a new MimeMessageHelper for the given MimeMessage,
   * in multipart mode (supporting alternative texts, inline
   * elements and attachments) if requested.
   * <p>Consider using the MimeMessageHelper constructor that
   * takes a multipartMode argument to choose a specific multipart
   * mode other than MULTIPART_MODE_MIXED_RELATED.
   *
   * @param mimeMessage the mime message to work on
   * @param multipart whether to create a multipart message that
   * supports alternative texts, inline elements and attachments
   * (corresponds to MULTIPART_MODE_MIXED_RELATED)
   * @param encoding the character encoding to use for the message
   * @throws MessagingException if multipart creation failed
   * @see #MimeMessageHelper(jakarta.mail.internet.MimeMessage, int, String)
   */
  public MimeMessageHelper(MimeMessage mimeMessage, boolean multipart, @Nullable String encoding)
          throws MessagingException {

    this(mimeMessage, (multipart ? MULTIPART_MODE_MIXED_RELATED : MULTIPART_MODE_NO), encoding);
  }

  /**
   * Create a new MimeMessageHelper for the given MimeMessage,
   * in multipart mode (supporting alternative texts, inline
   * elements and attachments) if requested.
   * <p>The character encoding for the message will be taken from
   * the passed-in MimeMessage object, if carried there. Else,
   * JavaMail's default encoding will be used.
   *
   * @param mimeMessage the mime message to work on
   * @param multipartMode which kind of multipart message to create
   * (MIXED, RELATED, MIXED_RELATED, or NO)
   * @throws MessagingException if multipart creation failed
   * @see #MULTIPART_MODE_NO
   * @see #MULTIPART_MODE_MIXED
   * @see #MULTIPART_MODE_RELATED
   * @see #MULTIPART_MODE_MIXED_RELATED
   * @see #getDefaultEncoding(jakarta.mail.internet.MimeMessage)
   * @see JavaMailSenderImpl#setDefaultEncoding
   */
  public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
    this(mimeMessage, multipartMode, null);
  }

  /**
   * Create a new MimeMessageHelper for the given MimeMessage,
   * in multipart mode (supporting alternative texts, inline
   * elements and attachments) if requested.
   *
   * @param mimeMessage the mime message to work on
   * @param multipartMode which kind of multipart message to create
   * (MIXED, RELATED, MIXED_RELATED, or NO)
   * @param encoding the character encoding to use for the message
   * @throws MessagingException if multipart creation failed
   * @see #MULTIPART_MODE_NO
   * @see #MULTIPART_MODE_MIXED
   * @see #MULTIPART_MODE_RELATED
   * @see #MULTIPART_MODE_MIXED_RELATED
   */
  public MimeMessageHelper(MimeMessage mimeMessage, int multipartMode, @Nullable String encoding)
          throws MessagingException {

    this.mimeMessage = mimeMessage;
    createMimeMultiparts(mimeMessage, multipartMode);
    this.encoding = (encoding != null ? encoding : getDefaultEncoding(mimeMessage));
    this.fileTypeMap = getDefaultFileTypeMap(mimeMessage);
  }

  /**
   * Return the underlying MimeMessage object.
   */
  public final MimeMessage getMimeMessage() {
    return this.mimeMessage;
  }

  /**
   * Determine the MimeMultipart objects to use, which will be used
   * to store attachments on the one hand and text(s) and inline elements
   * on the other hand.
   * <p>Texts and inline elements can either be stored in the root element
   * itself (MULTIPART_MODE_MIXED, MULTIPART_MODE_RELATED) or in a nested element
   * rather than the root element directly (MULTIPART_MODE_MIXED_RELATED).
   * <p>By default, the root MimeMultipart element will be of type "mixed"
   * (MULTIPART_MODE_MIXED) or "related" (MULTIPART_MODE_RELATED).
   * The main multipart element will either be added as nested element of
   * type "related" (MULTIPART_MODE_MIXED_RELATED) or be identical to the root
   * element itself (MULTIPART_MODE_MIXED, MULTIPART_MODE_RELATED).
   *
   * @param mimeMessage the MimeMessage object to add the root MimeMultipart
   * object to
   * @param multipartMode the multipart mode, as passed into the constructor
   * (MIXED, RELATED, MIXED_RELATED, or NO)
   * @throws MessagingException if multipart creation failed
   * @see #setMimeMultiparts
   * @see #MULTIPART_MODE_NO
   * @see #MULTIPART_MODE_MIXED
   * @see #MULTIPART_MODE_RELATED
   * @see #MULTIPART_MODE_MIXED_RELATED
   */
  protected void createMimeMultiparts(MimeMessage mimeMessage, int multipartMode) throws MessagingException {
    switch (multipartMode) {
      case MULTIPART_MODE_NO -> setMimeMultiparts(null, null);
      case MULTIPART_MODE_MIXED -> {
        MimeMultipart mixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
        mimeMessage.setContent(mixedMultipart);
        setMimeMultiparts(mixedMultipart, mixedMultipart);
      }
      case MULTIPART_MODE_RELATED -> {
        MimeMultipart relatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
        mimeMessage.setContent(relatedMultipart);
        setMimeMultiparts(relatedMultipart, relatedMultipart);
      }
      case MULTIPART_MODE_MIXED_RELATED -> {
        MimeMultipart rootMixedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_MIXED);
        mimeMessage.setContent(rootMixedMultipart);
        MimeMultipart nestedRelatedMultipart = new MimeMultipart(MULTIPART_SUBTYPE_RELATED);
        MimeBodyPart relatedBodyPart = new MimeBodyPart();
        relatedBodyPart.setContent(nestedRelatedMultipart);
        rootMixedMultipart.addBodyPart(relatedBodyPart);
        setMimeMultiparts(rootMixedMultipart, nestedRelatedMultipart);
      }
      default -> throw new IllegalArgumentException("Only multipart modes MIXED_RELATED, RELATED and NO supported");
    }
  }

  /**
   * Set the given MimeMultipart objects for use by this MimeMessageHelper.
   *
   * @param root the root MimeMultipart object, which attachments will be added to;
   * or {@code null} to indicate no multipart at all
   * @param main the main MimeMultipart object, which text(s) and inline elements
   * will be added to (can be the same as the root multipart object, or an element
   * nested underneath the root multipart element)
   */
  protected final void setMimeMultiparts(@Nullable MimeMultipart root, @Nullable MimeMultipart main) {
    this.rootMimeMultipart = root;
    this.mimeMultipart = main;
  }

  /**
   * Return whether this helper is in multipart mode,
   * i.e. whether it holds a multipart message.
   *
   * @see #MimeMessageHelper(MimeMessage, boolean)
   */
  public final boolean isMultipart() {
    return (this.rootMimeMultipart != null);
  }

  /**
   * Return the root MIME "multipart/mixed" object, if any.
   * Can be used to manually add attachments.
   * <p>This will be the direct content of the MimeMessage,
   * in case of a multipart mail.
   *
   * @throws IllegalStateException if this helper is not in multipart mode
   * @see #isMultipart
   * @see #getMimeMessage
   * @see jakarta.mail.internet.MimeMultipart#addBodyPart
   */
  public final MimeMultipart getRootMimeMultipart() throws IllegalStateException {
    if (this.rootMimeMultipart == null) {
      throw new IllegalStateException("Not in multipart mode - " +
              "create an appropriate MimeMessageHelper via a constructor that takes a 'multipart' flag " +
              "if you need to set alternative texts or add inline elements or attachments.");
    }
    return this.rootMimeMultipart;
  }

  /**
   * Return the underlying MIME "multipart/related" object, if any.
   * Can be used to manually add body parts, inline elements, etc.
   * <p>This will be nested within the root MimeMultipart,
   * in case of a multipart mail.
   *
   * @throws IllegalStateException if this helper is not in multipart mode
   * @see #isMultipart
   * @see #getRootMimeMultipart
   * @see jakarta.mail.internet.MimeMultipart#addBodyPart
   */
  public final MimeMultipart getMimeMultipart() throws IllegalStateException {
    if (this.mimeMultipart == null) {
      throw new IllegalStateException("Not in multipart mode - " +
              "create an appropriate MimeMessageHelper via a constructor that takes a 'multipart' flag " +
              "if you need to set alternative texts or add inline elements or attachments.");
    }
    return this.mimeMultipart;
  }

  /**
   * Determine the default encoding for the given MimeMessage.
   *
   * @param mimeMessage the passed-in MimeMessage
   * @return the default encoding associated with the MimeMessage,
   * or {@code null} if none found
   */
  @Nullable
  protected String getDefaultEncoding(MimeMessage mimeMessage) {
    if (mimeMessage instanceof SmartMimeMessage smartMimeMessage) {
      return smartMimeMessage.getDefaultEncoding();
    }
    return null;
  }

  /**
   * Return the specific character encoding used for this message, if any.
   */
  @Nullable
  public String getEncoding() {
    return this.encoding;
  }

  /**
   * Determine the default Java Activation FileTypeMap for the given MimeMessage.
   *
   * @param mimeMessage the passed-in MimeMessage
   * @return the default FileTypeMap associated with the MimeMessage,
   * or a default ConfigurableMimeFileTypeMap if none found for the message
   * @see ConfigurableMimeFileTypeMap
   */
  protected FileTypeMap getDefaultFileTypeMap(MimeMessage mimeMessage) {
    if (mimeMessage instanceof SmartMimeMessage smartMimeMessage) {
      FileTypeMap fileTypeMap = smartMimeMessage.getDefaultFileTypeMap();
      if (fileTypeMap != null) {
        return fileTypeMap;
      }
    }
    ConfigurableMimeFileTypeMap fileTypeMap = new ConfigurableMimeFileTypeMap();
    fileTypeMap.afterPropertiesSet();
    return fileTypeMap;
  }

  /**
   * Set the Java Activation Framework {@code FileTypeMap} to use
   * for determining the content type of inline content and attachments
   * that get added to the message.
   * <p>The default is the {@code FileTypeMap} that the underlying
   * MimeMessage carries, if any, or the Activation Framework's default
   * {@code FileTypeMap} instance else.
   *
   * @see #addInline
   * @see #addAttachment
   * @see #getDefaultFileTypeMap(jakarta.mail.internet.MimeMessage)
   * @see JavaMailSenderImpl#setDefaultFileTypeMap
   * @see jakarta.activation.FileTypeMap#getDefaultFileTypeMap
   * @see ConfigurableMimeFileTypeMap
   */
  public void setFileTypeMap(@Nullable FileTypeMap fileTypeMap) {
    this.fileTypeMap = (fileTypeMap != null ? fileTypeMap : getDefaultFileTypeMap(getMimeMessage()));
  }

  /**
   * Return the {@code FileTypeMap} used by this MimeMessageHelper.
   *
   * @see #setFileTypeMap
   */
  public FileTypeMap getFileTypeMap() {
    return this.fileTypeMap;
  }

  /**
   * Set whether to encode attachment filenames passed to this helper's
   * {@code #addAttachment} methods.
   * <p>The default is {@code false} for standard MIME behavior; turn this to
   * {@code true} for compatibility with older email clients. On a related note,
   * check out JavaMail's {@code mail.mime.encodefilename} system property.
   * <p><b>NOTE:</b> The default changed to {@code false} in 5.3, in favor of
   * JavaMail's standard {@code mail.mime.encodefilename} system property.
   *
   * @see #addAttachment(String, DataSource)
   * @see MimeBodyPart#setFileName(String)
   * @since 5.2.9
   */
  public void setEncodeFilenames(boolean encodeFilenames) {
    this.encodeFilenames = encodeFilenames;
  }

  /**
   * Return whether to encode attachment filenames passed to this helper's
   * {@code #addAttachment} methods.
   *
   * @see #setEncodeFilenames
   * @since 5.2.9
   */
  public boolean isEncodeFilenames() {
    return this.encodeFilenames;
  }

  /**
   * Set whether to validate all addresses which get passed to this helper.
   * <p>The default is {@code false}.
   *
   * @see #validateAddress
   */
  public void setValidateAddresses(boolean validateAddresses) {
    this.validateAddresses = validateAddresses;
  }

  /**
   * Return whether this helper will validate all addresses passed to it.
   *
   * @see #setValidateAddresses
   */
  public boolean isValidateAddresses() {
    return this.validateAddresses;
  }

  /**
   * Validate the given mail address.
   * Called by all of MimeMessageHelper's address setters and adders.
   * <p>The default implementation invokes {@link InternetAddress#validate()},
   * provided that address validation is activated for the helper instance.
   *
   * @param address the address to validate
   * @throws AddressException if validation failed
   * @see #isValidateAddresses()
   * @see jakarta.mail.internet.InternetAddress#validate()
   */
  protected void validateAddress(InternetAddress address) throws AddressException {
    if (isValidateAddresses()) {
      address.validate();
    }
  }

  /**
   * Validate all given mail addresses.
   * <p>The default implementation simply delegates to {@link #validateAddress}
   * for each address.
   *
   * @param addresses the addresses to validate
   * @throws AddressException if validation failed
   * @see #validateAddress(InternetAddress)
   */
  protected void validateAddresses(InternetAddress[] addresses) throws AddressException {
    for (InternetAddress address : addresses) {
      validateAddress(address);
    }
  }

  public void setFrom(InternetAddress from) throws MessagingException {
    Assert.notNull(from, "From address is required");
    validateAddress(from);
    this.mimeMessage.setFrom(from);
  }

  public void setFrom(String from) throws MessagingException {
    Assert.notNull(from, "From address is required");
    setFrom(parseAddress(from));
  }

  public void setFrom(String from, String personal) throws MessagingException, UnsupportedEncodingException {
    Assert.notNull(from, "From address is required");
    setFrom(getEncoding() != null ?
            new InternetAddress(from, personal, getEncoding()) : new InternetAddress(from, personal));
  }

  public void setReplyTo(InternetAddress replyTo) throws MessagingException {
    Assert.notNull(replyTo, "Reply-to address is required");
    validateAddress(replyTo);
    this.mimeMessage.setReplyTo(new InternetAddress[] { replyTo });
  }

  public void setReplyTo(String replyTo) throws MessagingException {
    Assert.notNull(replyTo, "Reply-to address is required");
    setReplyTo(parseAddress(replyTo));
  }

  public void setReplyTo(String replyTo, String personal) throws MessagingException, UnsupportedEncodingException {
    Assert.notNull(replyTo, "Reply-to address is required");
    InternetAddress replyToAddress = (getEncoding() != null) ?
            new InternetAddress(replyTo, personal, getEncoding()) : new InternetAddress(replyTo, personal);
    setReplyTo(replyToAddress);
  }

  public void setTo(InternetAddress to) throws MessagingException {
    Assert.notNull(to, "To address is required");
    validateAddress(to);
    this.mimeMessage.setRecipient(Message.RecipientType.TO, to);
  }

  public void setTo(InternetAddress[] to) throws MessagingException {
    Assert.notNull(to, "To address array is required");
    validateAddresses(to);
    this.mimeMessage.setRecipients(Message.RecipientType.TO, to);
  }

  public void setTo(String to) throws MessagingException {
    Assert.notNull(to, "To address is required");
    setTo(parseAddress(to));
  }

  public void setTo(String[] to) throws MessagingException {
    Assert.notNull(to, "To address array is required");
    InternetAddress[] addresses = new InternetAddress[to.length];
    for (int i = 0; i < to.length; i++) {
      addresses[i] = parseAddress(to[i]);
    }
    setTo(addresses);
  }

  public void addTo(InternetAddress to) throws MessagingException {
    Assert.notNull(to, "To address is required");
    validateAddress(to);
    this.mimeMessage.addRecipient(Message.RecipientType.TO, to);
  }

  public void addTo(String to) throws MessagingException {
    Assert.notNull(to, "To address is required");
    addTo(parseAddress(to));
  }

  public void addTo(String to, String personal) throws MessagingException, UnsupportedEncodingException {
    Assert.notNull(to, "To address is required");
    addTo(getEncoding() != null ?
            new InternetAddress(to, personal, getEncoding()) :
            new InternetAddress(to, personal));
  }

  public void setCc(InternetAddress cc) throws MessagingException {
    Assert.notNull(cc, "Cc address is required");
    validateAddress(cc);
    this.mimeMessage.setRecipient(Message.RecipientType.CC, cc);
  }

  public void setCc(InternetAddress[] cc) throws MessagingException {
    Assert.notNull(cc, "Cc address array is required");
    validateAddresses(cc);
    this.mimeMessage.setRecipients(Message.RecipientType.CC, cc);
  }

  public void setCc(String cc) throws MessagingException {
    Assert.notNull(cc, "Cc address is required");
    setCc(parseAddress(cc));
  }

  public void setCc(String[] cc) throws MessagingException {
    Assert.notNull(cc, "Cc address array is required");
    InternetAddress[] addresses = new InternetAddress[cc.length];
    for (int i = 0; i < cc.length; i++) {
      addresses[i] = parseAddress(cc[i]);
    }
    setCc(addresses);
  }

  public void addCc(InternetAddress cc) throws MessagingException {
    Assert.notNull(cc, "Cc address is required");
    validateAddress(cc);
    this.mimeMessage.addRecipient(Message.RecipientType.CC, cc);
  }

  public void addCc(String cc) throws MessagingException {
    Assert.notNull(cc, "Cc address is required");
    addCc(parseAddress(cc));
  }

  public void addCc(String cc, String personal) throws MessagingException, UnsupportedEncodingException {
    Assert.notNull(cc, "Cc address is required");
    addCc(getEncoding() != null ?
            new InternetAddress(cc, personal, getEncoding()) :
            new InternetAddress(cc, personal));
  }

  public void setBcc(InternetAddress bcc) throws MessagingException {
    Assert.notNull(bcc, "Bcc address is required");
    validateAddress(bcc);
    this.mimeMessage.setRecipient(Message.RecipientType.BCC, bcc);
  }

  public void setBcc(InternetAddress[] bcc) throws MessagingException {
    Assert.notNull(bcc, "Bcc address array is required");
    validateAddresses(bcc);
    this.mimeMessage.setRecipients(Message.RecipientType.BCC, bcc);
  }

  public void setBcc(String bcc) throws MessagingException {
    Assert.notNull(bcc, "Bcc address is required");
    setBcc(parseAddress(bcc));
  }

  public void setBcc(String[] bcc) throws MessagingException {
    Assert.notNull(bcc, "Bcc address array is required");
    InternetAddress[] addresses = new InternetAddress[bcc.length];
    for (int i = 0; i < bcc.length; i++) {
      addresses[i] = parseAddress(bcc[i]);
    }
    setBcc(addresses);
  }

  public void addBcc(InternetAddress bcc) throws MessagingException {
    Assert.notNull(bcc, "Bcc address is required");
    validateAddress(bcc);
    this.mimeMessage.addRecipient(Message.RecipientType.BCC, bcc);
  }

  public void addBcc(String bcc) throws MessagingException {
    Assert.notNull(bcc, "Bcc address is required");
    addBcc(parseAddress(bcc));
  }

  public void addBcc(String bcc, String personal) throws MessagingException, UnsupportedEncodingException {
    Assert.notNull(bcc, "Bcc address is required");
    addBcc(getEncoding() != null ?
            new InternetAddress(bcc, personal, getEncoding()) :
            new InternetAddress(bcc, personal));
  }

  private InternetAddress parseAddress(String address) throws MessagingException {
    InternetAddress[] parsed = InternetAddress.parse(address);
    if (parsed.length != 1) {
      throw new AddressException("Illegal address", address);
    }
    InternetAddress raw = parsed[0];
    try {
      return (getEncoding() != null ?
              new InternetAddress(raw.getAddress(), raw.getPersonal(), getEncoding()) : raw);
    }
    catch (UnsupportedEncodingException ex) {
      throw new MessagingException("Failed to parse embedded personal name to correct encoding", ex);
    }
  }

  /**
   * Set the priority ("X-Priority" header) of the message.
   *
   * @param priority the priority value;
   * typically between 1 (highest) and 5 (lowest)
   * @throws MessagingException in case of errors
   */
  public void setPriority(int priority) throws MessagingException {
    this.mimeMessage.setHeader(HEADER_PRIORITY, Integer.toString(priority));
  }

  /**
   * Set the sent-date of the message.
   *
   * @param sentDate the date to set (never {@code null})
   * @throws MessagingException in case of errors
   */
  public void setSentDate(Date sentDate) throws MessagingException {
    Assert.notNull(sentDate, "Sent date is required");
    this.mimeMessage.setSentDate(sentDate);
  }

  /**
   * Set the subject of the message, using the correct encoding.
   *
   * @param subject the subject text
   * @throws MessagingException in case of errors
   */
  public void setSubject(String subject) throws MessagingException {
    Assert.notNull(subject, "Subject is required");
    if (getEncoding() != null) {
      this.mimeMessage.setSubject(subject, getEncoding());
    }
    else {
      this.mimeMessage.setSubject(subject);
    }
  }

  /**
   * Set the given text directly as content in non-multipart mode
   * or as default body part in multipart mode.
   * Always applies the default content type "text/plain".
   * <p><b>NOTE:</b> Invoke {@link #addInline} <i>after</i> {@code setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param text the text for the message
   * @throws MessagingException in case of errors
   */
  public void setText(String text) throws MessagingException {
    setText(text, false);
  }

  /**
   * Set the given text directly as content in non-multipart mode
   * or as default body part in multipart mode.
   * The "html" flag determines the content type to apply.
   * <p><b>NOTE:</b> Invoke {@link #addInline} <i>after</i> {@code setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param text the text for the message
   * @param html whether to apply content type "text/html" for an
   * HTML mail, using default content type ("text/plain") else
   * @throws MessagingException in case of errors
   */
  public void setText(String text, boolean html) throws MessagingException {
    Assert.notNull(text, "Text is required");
    MimePart partToUse;
    if (isMultipart()) {
      partToUse = getMainPart();
    }
    else {
      partToUse = this.mimeMessage;
    }
    if (html) {
      setHtmlTextToMimePart(partToUse, text);
    }
    else {
      setPlainTextToMimePart(partToUse, text);
    }
  }

  /**
   * Set the given plain text and HTML text as alternatives, offering
   * both options to the email client. Requires multipart mode.
   * <p><b>NOTE:</b> Invoke {@link #addInline} <i>after</i> {@code setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param plainText the plain text for the message
   * @param htmlText the HTML text for the message
   * @throws MessagingException in case of errors
   */
  public void setText(String plainText, String htmlText) throws MessagingException {
    Assert.notNull(plainText, "Plain text is required");
    Assert.notNull(htmlText, "HTML text is required");

    MimeMultipart messageBody = new MimeMultipart(MULTIPART_SUBTYPE_ALTERNATIVE);
    getMainPart().setContent(messageBody, CONTENT_TYPE_ALTERNATIVE);

    // Create the plain text part of the message.
    MimeBodyPart plainTextPart = new MimeBodyPart();
    setPlainTextToMimePart(plainTextPart, plainText);
    messageBody.addBodyPart(plainTextPart);

    // Create the HTML text part of the message.
    MimeBodyPart htmlTextPart = new MimeBodyPart();
    setHtmlTextToMimePart(htmlTextPart, htmlText);
    messageBody.addBodyPart(htmlTextPart);
  }

  private MimeBodyPart getMainPart() throws MessagingException {
    MimeMultipart mimeMultipart = getMimeMultipart();
    MimeBodyPart bodyPart = null;
    for (int i = 0; i < mimeMultipart.getCount(); i++) {
      BodyPart bp = mimeMultipart.getBodyPart(i);
      if (bp.getFileName() == null) {
        bodyPart = (MimeBodyPart) bp;
      }
    }
    if (bodyPart == null) {
      MimeBodyPart mimeBodyPart = new MimeBodyPart();
      mimeMultipart.addBodyPart(mimeBodyPart);
      bodyPart = mimeBodyPart;
    }
    return bodyPart;
  }

  private void setPlainTextToMimePart(MimePart mimePart, String text) throws MessagingException {
    if (getEncoding() != null) {
      mimePart.setText(text, getEncoding());
    }
    else {
      mimePart.setText(text);
    }
  }

  private void setHtmlTextToMimePart(MimePart mimePart, String text) throws MessagingException {
    if (getEncoding() != null) {
      mimePart.setContent(text, CONTENT_TYPE_HTML + CONTENT_TYPE_CHARSET_SUFFIX + getEncoding());
    }
    else {
      mimePart.setContent(text, CONTENT_TYPE_HTML);
    }
  }

  /**
   * Add an inline element to the MimeMessage, taking the content from a
   * {@code jakarta.activation.DataSource}.
   * <p>Note that the InputStream returned by the DataSource implementation
   * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
   * {@code getInputStream()} multiple times.
   * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@link #setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param contentId the content ID to use. Will end up as "Content-ID" header
   * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
   * Can be referenced in HTML source via src="cid:myId" expressions.
   * @param dataSource the {@code jakarta.activation.DataSource} to take
   * the content from, determining the InputStream and the content type
   * @throws MessagingException in case of errors
   * @see #addInline(String, java.io.File)
   * @see #addInline(String, Resource)
   */
  public void addInline(String contentId, DataSource dataSource) throws MessagingException {
    addInline(contentId, null, dataSource);
  }

  /**
   * Add an inline element to the MimeMessage, taking the content from a
   * {@code jakarta.activation.DataSource} and assigning the provided
   * {@code inlineFileName} to the element.
   * <p>Note that the InputStream returned by the DataSource implementation
   * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
   * {@code getInputStream()} multiple times.
   * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@link #setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param contentId the content ID to use. Will end up as "Content-ID" header
   * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
   * Can be referenced in HTML source via src="cid:myId" expressions.
   * @param inlineFilename the fileName to use for the inline element's part
   * @param dataSource the {@code jakarta.activation.DataSource} to take
   * the content from, determining the InputStream and the content type
   * @throws MessagingException in case of errors
   * @see #addInline(String, java.io.File)
   * @see #addInline(String, Resource)
   * @since 5.0
   */
  public void addInline(String contentId, @Nullable String inlineFilename, DataSource dataSource)
          throws MessagingException {

    Assert.notNull(contentId, "Content ID is required");
    Assert.notNull(dataSource, "DataSource is required");
    MimeBodyPart mimeBodyPart = new MimeBodyPart();
    mimeBodyPart.setDisposition(Part.INLINE);
    mimeBodyPart.setContentID("<" + contentId + ">");
    mimeBodyPart.setDataHandler(new DataHandler(dataSource));
    if (inlineFilename != null) {
      try {
        mimeBodyPart.setFileName(isEncodeFilenames() ?
                MimeUtility.encodeText(inlineFilename) : inlineFilename);
      }
      catch (UnsupportedEncodingException ex) {
        throw new MessagingException("Failed to encode inline filename", ex);
      }
    }
    getMimeMultipart().addBodyPart(mimeBodyPart);
  }

  /**
   * Add an inline element to the MimeMessage, taking the content from a
   * {@code java.io.File}.
   * <p>The content type will be determined by the name of the given
   * content file. Do not use this for temporary files with arbitrary
   * filenames (possibly ending in ".tmp" or the like)!
   * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@link #setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param contentId the content ID to use. Will end up as "Content-ID" header
   * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
   * Can be referenced in HTML source via src="cid:myId" expressions.
   * @param file the File resource to take the content from
   * @throws MessagingException in case of errors
   * @see #setText
   * @see #addInline(String, Resource)
   * @see #addInline(String, jakarta.activation.DataSource)
   */
  public void addInline(String contentId, File file) throws MessagingException {
    Assert.notNull(file, "File is required");
    FileDataSource dataSource = new FileDataSource(file);
    dataSource.setFileTypeMap(getFileTypeMap());
    addInline(contentId, dataSource);
  }

  /**
   * Add an inline element to the MimeMessage, taking the content from a
   * {@code infra.core.io.Resource}.
   * <p>The content type will be determined by the name of the given
   * content file. Do not use this for temporary files with arbitrary
   * filenames (possibly ending in ".tmp" or the like)!
   * <p>Note that the InputStream returned by the Resource implementation
   * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
   * {@code getInputStream()} multiple times.
   * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@link #setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param contentId the content ID to use. Will end up as "Content-ID" header
   * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
   * Can be referenced in HTML source via src="cid:myId" expressions.
   * @param resource the resource to take the content from
   * @throws MessagingException in case of errors
   * @see #setText
   * @see #addInline(String, java.io.File)
   * @see #addInline(String, jakarta.activation.DataSource)
   */
  public void addInline(String contentId, Resource resource) throws MessagingException {
    Assert.notNull(resource, "Resource is required");
    String contentType;
    String filename = resource.getName();
    if (filename == null) {
      contentType = MimeType.APPLICATION_OCTET_STREAM_VALUE;
    }
    else {
      contentType = getFileTypeMap().getContentType(filename);
    }
    addInline(contentId, resource, contentType);
  }

  /**
   * Add an inline element to the MimeMessage, taking the content from an
   * {@code infra.core.InputStreamResource}, and
   * specifying the content type explicitly.
   * <p>You can determine the content type for any given filename via a Java
   * Activation Framework's FileTypeMap, for example the one held by this helper.
   * <p>Note that the InputStream returned by the InputStreamSource implementation
   * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
   * {@code getInputStream()} multiple times.
   * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@code setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param contentId the content ID to use. Will end up as "Content-ID" header
   * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
   * Can be referenced in HTML source via src="cid:myId" expressions.
   * @param inputStreamSource the resource to take the content from
   * @param contentType the content type to use for the element
   * @throws MessagingException in case of errors
   * @see #setText
   * @see #getFileTypeMap
   * @see #addInline(String, Resource)
   * @see #addInline(String, jakarta.activation.DataSource)
   */
  public void addInline(String contentId, InputStreamSource inputStreamSource, String contentType)
          throws MessagingException {

    addInline(contentId, "inline", inputStreamSource, contentType);
  }

  /**
   * Add an inline element to the MimeMessage, taking the content from an
   * {@code infra.core.InputStreamResource}, and
   * specifying the inline fileName explicitly.
   * <p>The content type will be determined by the name of the given
   * content file. Do not use this for temporary files with arbitrary
   * filenames (possibly ending in ".tmp" or the like)!
   * <p>Note that the InputStream returned by the InputStreamSource implementation
   * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
   * {@code getInputStream()} multiple times.
   * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@code setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param contentId the content ID to use. Will end up as "Content-ID" header
   * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
   * Can be referenced in HTML source via src="cid:myId" expressions.
   * @param inlineFilename the file name to use for the inline element
   * @param inputStreamSource the resource to take the content from
   * @throws MessagingException in case of errors
   * @see #setText(String)
   * @see #getFileTypeMap
   * @see #addInline(String, Resource)
   * @see #addInline(String, String, jakarta.activation.DataSource)
   * @since 5.0
   */
  public void addInline(String contentId, String inlineFilename, InputStreamSource inputStreamSource)
          throws MessagingException {

    String contentType = getFileTypeMap().getContentType(inlineFilename);
    addInline(contentId, inlineFilename, inputStreamSource, contentType);
  }

  /**
   * Add an inline element to the MimeMessage, taking the content from an
   * {@code infra.core.InputStreamResource}, and
   * specifying the inline fileName and content type explicitly.
   * <p>You can determine the content type for any given filename via a Java
   * Activation Framework's FileTypeMap, for example the one held by this helper.
   * <p>Note that the InputStream returned by the InputStreamSource implementation
   * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
   * {@code getInputStream()} multiple times.
   * <p><b>NOTE:</b> Invoke {@code addInline} <i>after</i> {@code setText};
   * else, mail readers might not be able to resolve inline references correctly.
   *
   * @param contentId the content ID to use. Will end up as "Content-ID" header
   * in the body part, surrounded by angle brackets: e.g. "myId" &rarr; "&lt;myId&gt;".
   * Can be referenced in HTML source via src="cid:myId" expressions.
   * @param inlineFilename the fileName to use for the inline element's part
   * @param inputStreamSource the resource to take the content from
   * @param contentType the content type to use for the element
   * @throws MessagingException in case of errors
   * @see #setText
   * @see #getFileTypeMap
   * @see #addInline(String, Resource)
   * @see #addInline(String, String, jakarta.activation.DataSource)
   * @since 5.0
   */
  public void addInline(String contentId, String inlineFilename, InputStreamSource inputStreamSource, String contentType)
          throws MessagingException {

    Assert.notNull(inputStreamSource, "InputStreamSource is required");
    if (inputStreamSource instanceof Resource resource && resource.isOpen()) {
      throw new IllegalArgumentException(
              "Passed-in Resource contains an open stream: invalid argument. " +
                      "JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
    }
    DataSource dataSource = createDataSource(inputStreamSource, contentType, inlineFilename);
    addInline(contentId, inlineFilename, dataSource);
  }

  /**
   * Add an attachment to the MimeMessage, taking the content from a
   * {@code jakarta.activation.DataSource}.
   * <p>Note that the InputStream returned by the DataSource implementation
   * needs to be a <i>fresh one on each call</i>, as JavaMail will invoke
   * {@code getInputStream()} multiple times.
   *
   * @param attachmentFilename the name of the attachment as it will
   * appear in the mail (the content type will be determined by this)
   * @param dataSource the {@code jakarta.activation.DataSource} to take
   * the content from, determining the InputStream and the content type
   * @throws MessagingException in case of errors
   * @see #addAttachment(String, InputStreamSource)
   * @see #addAttachment(String, java.io.File)
   */
  public void addAttachment(String attachmentFilename, DataSource dataSource) throws MessagingException {
    Assert.notNull(attachmentFilename, "Attachment filename is required");
    Assert.notNull(dataSource, "DataSource is required");
    try {
      MimeBodyPart mimeBodyPart = new MimeBodyPart();
      mimeBodyPart.setDisposition(Part.ATTACHMENT);
      mimeBodyPart.setFileName(isEncodeFilenames() ?
              MimeUtility.encodeText(attachmentFilename) : attachmentFilename);
      mimeBodyPart.setDataHandler(new DataHandler(dataSource));
      getRootMimeMultipart().addBodyPart(mimeBodyPart);
    }
    catch (UnsupportedEncodingException ex) {
      throw new MessagingException("Failed to encode attachment filename", ex);
    }
  }

  /**
   * Add an attachment to the MimeMessage, taking the content from a
   * {@code java.io.File}.
   * <p>The content type will be determined by the name of the given
   * content file. Do not use this for temporary files with arbitrary
   * filenames (possibly ending in ".tmp" or the like)!
   *
   * @param attachmentFilename the name of the attachment as it will
   * appear in the mail
   * @param file the File resource to take the content from
   * @throws MessagingException in case of errors
   * @see #addAttachment(String, InputStreamSource)
   * @see #addAttachment(String, jakarta.activation.DataSource)
   */
  public void addAttachment(String attachmentFilename, File file) throws MessagingException {
    Assert.notNull(file, "File is required");
    FileDataSource dataSource = new FileDataSource(file);
    dataSource.setFileTypeMap(getFileTypeMap());
    addAttachment(attachmentFilename, dataSource);
  }

  /**
   * Add an attachment to the MimeMessage, taking the content from an
   * {@code infra.core.io.InputStreamResource}.
   * <p>The content type will be determined by the given filename for
   * the attachment. Thus, any content source will be fine, including
   * temporary files with arbitrary filenames.
   * <p>Note that the InputStream returned by the InputStreamSource
   * implementation needs to be a <i>fresh one on each call</i>, as
   * JavaMail will invoke {@code getInputStream()} multiple times.
   *
   * @param attachmentFilename the name of the attachment as it will
   * appear in the mail
   * @param inputStreamSource the resource to take the content from
   * (all of Infra Resource implementations can be passed in here)
   * @throws MessagingException in case of errors
   * @see #addAttachment(String, java.io.File)
   * @see #addAttachment(String, jakarta.activation.DataSource)
   * @see Resource
   */
  public void addAttachment(String attachmentFilename, InputStreamSource inputStreamSource)
          throws MessagingException {

    String contentType = getFileTypeMap().getContentType(attachmentFilename);
    addAttachment(attachmentFilename, inputStreamSource, contentType);
  }

  /**
   * Add an attachment to the MimeMessage, taking the content from an
   * {@code infra.core.io.InputStreamResource}.
   * <p>Note that the InputStream returned by the InputStreamSource
   * implementation needs to be a <i>fresh one on each call</i>, as
   * JavaMail will invoke {@code getInputStream()} multiple times.
   *
   * @param attachmentFilename the name of the attachment as it will
   * appear in the mail
   * @param inputStreamSource the resource to take the content from
   * (all of Infra Resource implementations can be passed in here)
   * @param contentType the content type to use for the element
   * @throws MessagingException in case of errors
   * @see #addAttachment(String, java.io.File)
   * @see #addAttachment(String, jakarta.activation.DataSource)
   * @see Resource
   */
  public void addAttachment(String attachmentFilename, InputStreamSource inputStreamSource, String contentType)
          throws MessagingException {

    Assert.notNull(inputStreamSource, "InputStreamSource is required");
    if (inputStreamSource instanceof Resource resource && resource.isOpen()) {
      throw new IllegalArgumentException(
              "Passed-in Resource contains an open stream: invalid argument. " +
                      "JavaMail requires an InputStreamSource that creates a fresh stream for every call.");
    }
    DataSource dataSource = createDataSource(inputStreamSource, contentType, attachmentFilename);
    addAttachment(attachmentFilename, dataSource);
  }

  /**
   * Create an Activation Framework DataSource for the given InputStreamSource.
   *
   * @param inputStreamSource the InputStreamSource (typically Infra Resource)
   * @param contentType the content type
   * @param name the name of the DataSource
   * @return the Activation Framework DataSource
   */
  protected DataSource createDataSource(
          final InputStreamSource inputStreamSource, final String contentType, final String name) {

    return new DataSource() {
      @Override
      public InputStream getInputStream() throws IOException {
        return inputStreamSource.getInputStream();
      }

      @Override
      public OutputStream getOutputStream() {
        throw new UnsupportedOperationException("Read-only jakarta.activation.DataSource");
      }

      @Override
      public String getContentType() {
        return contentType;
      }

      @Override
      public String getName() {
        return name;
      }
    };
  }

}
