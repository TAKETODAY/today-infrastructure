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

package cn.taketoday.web.testfixture.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.mock.ServletOutputStream;
import cn.taketoday.web.mock.http.Cookie;
import cn.taketoday.web.mock.http.HttpServletResponse;

/**
 * Mock implementation of the {@link HttpServletResponse} interface.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Brian Clozel
 * @author Vedran Pavic
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 3.0
 */
public class MockHttpServletResponse implements HttpServletResponse {

  private static final String CHARSET_PREFIX = "charset=";

  private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";

  private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

  //---------------------------------------------------------------------
  // ServletResponse properties
  //---------------------------------------------------------------------

  private boolean outputStreamAccessAllowed = true;

  private boolean writerAccessAllowed = true;

  private String defaultCharacterEncoding = "ISO-8859-1";

  private String characterEncoding = this.defaultCharacterEncoding;

  /**
   * {@code true} if the character encoding has been explicitly set through
   * {@link HttpServletResponse} methods or through a {@code charset} parameter
   * on the {@code Content-Type}.
   */
  private boolean characterEncodingSet = false;

  private final ByteArrayOutputStream content = new ByteArrayOutputStream(1024);

  private final ServletOutputStream outputStream = new ResponseServletOutputStream(this.content);

  @Nullable
  private PrintWriter writer;

  private long contentLength = 0;

  @Nullable
  private String contentType;

  private int bufferSize = 4096;

  private boolean committed;

  private Locale locale = Locale.getDefault();

  //---------------------------------------------------------------------
  // HttpServletResponse properties
  //---------------------------------------------------------------------

  private final List<Cookie> cookies = new ArrayList<>();

  private final Map<String, HeaderValueHolder> headers = new LinkedCaseInsensitiveMap<>();

  private int status = HttpServletResponse.SC_OK;

  @Nullable
  private String errorMessage;

  //---------------------------------------------------------------------
  // Properties for MockRequestDispatcher
  //---------------------------------------------------------------------

  @Nullable
  private String forwardedUrl;

  private final List<String> includedUrls = new ArrayList<>();

  //---------------------------------------------------------------------
  // ServletResponse interface
  //---------------------------------------------------------------------

  /**
   * Set whether {@link #getOutputStream()} access is allowed.
   * <p>Default is {@code true}.
   */
  public void setOutputStreamAccessAllowed(boolean outputStreamAccessAllowed) {
    this.outputStreamAccessAllowed = outputStreamAccessAllowed;
  }

  /**
   * Return whether {@link #getOutputStream()} access is allowed.
   */
  public boolean isOutputStreamAccessAllowed() {
    return this.outputStreamAccessAllowed;
  }

  /**
   * Set whether {@link #getWriter()} access is allowed.
   * <p>Default is {@code true}.
   */
  public void setWriterAccessAllowed(boolean writerAccessAllowed) {
    this.writerAccessAllowed = writerAccessAllowed;
  }

  /**
   * Return whether {@link #getOutputStream()} access is allowed.
   */
  public boolean isWriterAccessAllowed() {
    return this.writerAccessAllowed;
  }

  /**
   * Set the <em>default</em> character encoding for the response.
   * <p>If this method is not invoked, {@code ISO-8859-1} will be used as the
   * default character encoding.
   * <p>If the {@linkplain #getCharacterEncoding() character encoding} for the
   * response has not already been explicitly set via {@link #setCharacterEncoding(String)}
   * or {@link #setContentType(String)}, the character encoding for the response
   * will be set to the supplied default character encoding.
   *
   * @param characterEncoding the default character encoding
   * @see #setCharacterEncoding(String)
   * @see #setContentType(String)
   */
  public void setDefaultCharacterEncoding(String characterEncoding) {
    Assert.notNull(characterEncoding, "'characterEncoding' is required");
    this.defaultCharacterEncoding = characterEncoding;
    if (!this.characterEncodingSet) {
      this.characterEncoding = characterEncoding;
    }
  }

  /**
   * Determine whether the character encoding has been explicitly set through
   * {@link HttpServletResponse} methods or through a {@code charset} parameter
   * on the {@code Content-Type}.
   * <p>If {@code false}, {@link #getCharacterEncoding()} will return the
   * {@linkplain #setDefaultCharacterEncoding(String) default character encoding}.
   */
  public boolean isCharset() {
    return this.characterEncodingSet;
  }

  @Override
  public void setCharacterEncoding(String characterEncoding) {
    setExplicitCharacterEncoding(characterEncoding);
    updateContentTypePropertyAndHeader();
  }

  private void setExplicitCharacterEncoding(@Nullable String characterEncoding) {
    if (characterEncoding == null) {
      this.characterEncoding = this.defaultCharacterEncoding;
      this.characterEncodingSet = false;
      if (this.contentType != null) {
        try {
          MediaType mediaType = MediaType.parseMediaType(this.contentType);
          if (mediaType.getCharset() != null) {
            Map<String, String> parameters = new LinkedHashMap<>(mediaType.getParameters());
            parameters.remove("charset");
            mediaType = new MediaType(mediaType.getType(), mediaType.getSubtype(), parameters);
            this.contentType = mediaType.toString();
          }
        }
        catch (Exception ignored) {
          String value = this.contentType;
          int charsetIndex = value.toLowerCase().indexOf(CHARSET_PREFIX);
          if (charsetIndex != -1) {
            value = value.substring(0, charsetIndex).trim();
            if (value.endsWith(";")) {
              value = value.substring(0, value.length() - 1);
            }
            this.contentType = value;
          }
        }
      }
    }
    else {
      this.characterEncoding = characterEncoding;
      this.characterEncodingSet = true;
    }
  }

  private void updateContentTypePropertyAndHeader() {
    if (this.contentType != null) {
      String value = this.contentType;
      if (this.characterEncodingSet && !value.toLowerCase().contains(CHARSET_PREFIX)) {
        value += ';' + CHARSET_PREFIX + getCharacterEncoding();
        this.contentType = value;
      }
      doAddHeaderValue(HttpHeaders.CONTENT_TYPE, value, true);
    }
  }

  @Override
  public String getCharacterEncoding() {
    return this.characterEncoding;
  }

  @Override
  public ServletOutputStream getOutputStream() {
    Assert.state(this.outputStreamAccessAllowed, "OutputStream access not allowed");
    return this.outputStream;
  }

  @Override
  public PrintWriter getWriter() throws UnsupportedEncodingException {
    Assert.state(this.writerAccessAllowed, "Writer access not allowed");
    if (this.writer == null) {
      Writer targetWriter = new OutputStreamWriter(this.content, getCharacterEncoding());
      this.writer = new ResponsePrintWriter(targetWriter);
    }
    return this.writer;
  }

  public byte[] getContentAsByteArray() {
    return this.content.toByteArray();
  }

  /**
   * Get the content of the response body as a {@code String}, using the charset
   * specified for the response by the application, either through
   * {@link HttpServletResponse} methods or through a charset parameter on the
   * {@code Content-Type}. If no charset has been explicitly defined, the
   * {@linkplain #setDefaultCharacterEncoding(String) default character encoding}
   * will be used.
   *
   * @return the content as a {@code String}
   * @throws UnsupportedEncodingException if the character encoding is not supported
   * @see #getContentAsString(Charset)
   * @see #setCharacterEncoding(String)
   * @see #setContentType(String)
   */
  public String getContentAsString() throws UnsupportedEncodingException {
    return this.content.toString(getCharacterEncoding());
  }

  /**
   * Get the content of the response body as a {@code String}, using the provided
   * {@code fallbackCharset} if no charset has been explicitly defined and otherwise
   * using the charset specified for the response by the application, either
   * through {@link HttpServletResponse} methods or through a charset parameter on the
   * {@code Content-Type}.
   *
   * @return the content as a {@code String}
   * @throws UnsupportedEncodingException if the character encoding is not supported
   * @see #getContentAsString()
   * @see #setCharacterEncoding(String)
   * @see #setContentType(String)
   */
  public String getContentAsString(Charset fallbackCharset) throws UnsupportedEncodingException {
    if (this.characterEncodingSet) {
      return this.content.toString(getCharacterEncoding());
    }

    return this.content.toString(fallbackCharset);
  }

  @Override
  public void setContentLength(int contentLength) {
    this.contentLength = contentLength;
    doAddHeaderValue(HttpHeaders.CONTENT_LENGTH, contentLength, true);
  }

  public int getContentLength() {
    return (int) this.contentLength;
  }

  @Override
  public void setContentLengthLong(long contentLength) {
    this.contentLength = contentLength;
    doAddHeaderValue(HttpHeaders.CONTENT_LENGTH, contentLength, true);
  }

  public long getContentLengthLong() {
    return this.contentLength;
  }

  @Override
  public void setContentType(@Nullable String contentType) {
    this.contentType = contentType;
    if (contentType != null) {
      try {
        MediaType mediaType = MediaType.parseMediaType(contentType);
        if (mediaType.getCharset() != null) {
          setExplicitCharacterEncoding(mediaType.getCharset().name());
        }
      }
      catch (Exception ex) {
        // Try to get charset value anyway
        int charsetIndex = contentType.toLowerCase().indexOf(CHARSET_PREFIX);
        if (charsetIndex != -1) {
          setExplicitCharacterEncoding(contentType.substring(charsetIndex + CHARSET_PREFIX.length()));
        }
      }
      updateContentTypePropertyAndHeader();
    }
  }

  @Override
  @Nullable
  public String getContentType() {
    return this.contentType;
  }

  @Override
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  @Override
  public int getBufferSize() {
    return this.bufferSize;
  }

  @Override
  public void flushBuffer() {
    setCommitted(true);
  }

  @Override
  public void resetBuffer() {
    Assert.state(!isCommitted(), "Cannot reset buffer - response is already committed");
    this.content.reset();
  }

  private void setCommittedIfBufferSizeExceeded() {
    int bufSize = getBufferSize();
    if (bufSize > 0 && this.content.size() > bufSize) {
      setCommitted(true);
    }
  }

  public void setCommitted(boolean committed) {
    this.committed = committed;
  }

  @Override
  public boolean isCommitted() {
    return this.committed;
  }

  @Override
  public void reset() {
    resetBuffer();
    this.characterEncoding = this.defaultCharacterEncoding;
    this.characterEncodingSet = false;
    this.contentLength = 0;
    this.contentType = null;
    this.locale = Locale.getDefault();
    this.cookies.clear();
    this.headers.clear();
    this.status = HttpServletResponse.SC_OK;
    this.errorMessage = null;
  }

  @Override
  public void setLocale(@Nullable Locale locale) {
    // Although the Javadoc for cn.taketoday.web.mock.ServletResponse.setLocale(Locale) does not
    // state how a null value for the supplied Locale should be handled, both Tomcat and
    // Jetty simply ignore a null value. So we do the same here.
    if (locale == null) {
      return;
    }
    this.locale = locale;
    doAddHeaderValue(HttpHeaders.CONTENT_LANGUAGE, locale.toLanguageTag(), true);
  }

  @Override
  public Locale getLocale() {
    return this.locale;
  }

  //---------------------------------------------------------------------
  // HttpServletResponse interface
  //---------------------------------------------------------------------

  @Override
  public void addCookie(Cookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.add(cookie);
    doAddHeaderValue(HttpHeaders.SET_COOKIE, getCookieHeader(cookie), false);
  }

  @SuppressWarnings("removal")
  private String getCookieHeader(Cookie cookie) {
    StringBuilder buf = new StringBuilder();
    buf.append(cookie.getName()).append('=').append(cookie.getValue() == null ? "" : cookie.getValue());
    if (StringUtils.hasText(cookie.getPath())) {
      buf.append("; Path=").append(cookie.getPath());
    }
    if (StringUtils.hasText(cookie.getDomain())) {
      buf.append("; Domain=").append(cookie.getDomain());
    }
    int maxAge = cookie.getMaxAge();
    ZonedDateTime expires = (cookie instanceof MockCookie mockCookie ? mockCookie.getExpires() : null);
    if (maxAge >= 0) {
      buf.append("; Max-Age=").append(maxAge);
      buf.append("; Expires=");
      if (expires != null) {
        buf.append(expires.format(DateTimeFormatter.RFC_1123_DATE_TIME));
      }
      else {
        HttpHeaders headers = HttpHeaders.forWritable();
        headers.setExpires(maxAge > 0 ? System.currentTimeMillis() + 1000L * maxAge : 0);
        buf.append(headers.getFirst(HttpHeaders.EXPIRES));
      }
    }
    else if (expires != null) {
      buf.append("; Expires=");
      buf.append(expires.format(DateTimeFormatter.RFC_1123_DATE_TIME));
    }

    if (cookie.getSecure()) {
      buf.append("; Secure");
    }
    if (cookie.isHttpOnly()) {
      buf.append("; HttpOnly");
    }
    if (cookie instanceof MockCookie mockCookie) {
      if (StringUtils.hasText(mockCookie.getSameSite())) {
        buf.append("; SameSite=").append(mockCookie.getSameSite());
      }
    }
    if (StringUtils.hasText(cookie.getComment())) {
      buf.append("; Comment=").append(cookie.getComment());
    }
    return buf.toString();
  }

  public Cookie[] getCookies() {
    return this.cookies.toArray(new Cookie[0]);
  }

  @Nullable
  public Cookie getCookie(String name) {
    Assert.notNull(name, "Cookie name is required");
    for (Cookie cookie : this.cookies) {
      if (name.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  @Override
  public boolean containsHeader(String name) {
    return this.headers.containsKey(name);
  }

  /**
   * Return the names of all specified headers as a Set of Strings.
   * <p>As of Servlet 3.0, this method is also defined in {@link HttpServletResponse}.
   *
   * @return the {@code Set} of header name {@code Strings}, or an empty {@code Set} if none
   */
  @Override
  public Collection<String> getHeaderNames() {
    return this.headers.keySet();
  }

  /**
   * Return the primary value for the given header as a String, if any.
   * Will return the first value in case of multiple values.
   * <p>As of Servlet 3.0, this method is also defined in {@link HttpServletResponse}.
   * it returns a stringified value for Servlet 3.0 compatibility.
   * Consider using {@link #getHeaderValue(String)} for raw Object access.
   *
   * @param name the name of the header
   * @return the associated header value, or {@code null} if none
   */
  @Override
  @Nullable
  public String getHeader(String name) {
    HeaderValueHolder header = this.headers.get(name);
    return (header != null ? header.getStringValue() : null);
  }

  /**
   * Return all values for the given header as a List of Strings.
   * <p>As of Servlet 3.0, this method is also defined in {@link HttpServletResponse}.
   * it returns a List of stringified values for Servlet 3.0 compatibility.
   * Consider using {@link #getHeaderValues(String)} for raw Object access.
   *
   * @param name the name of the header
   * @return the associated header values, or an empty List if none
   */
  @Override
  public List<String> getHeaders(String name) {
    HeaderValueHolder header = this.headers.get(name);
    if (header != null) {
      return header.getStringValues();
    }
    else {
      return Collections.emptyList();
    }
  }

  /**
   * Return the primary value for the given header, if any.
   * <p>Will return the first value in case of multiple values.
   *
   * @param name the name of the header
   * @return the associated header value, or {@code null} if none
   */
  @Nullable
  public Object getHeaderValue(String name) {
    HeaderValueHolder header = this.headers.get(name);
    return (header != null ? header.getValue() : null);
  }

  /**
   * Return all values for the given header as a List of value objects.
   *
   * @param name the name of the header
   * @return the associated header values, or an empty List if none
   */
  public List<Object> getHeaderValues(String name) {
    HeaderValueHolder header = this.headers.get(name);
    if (header != null) {
      return header.getValues();
    }
    else {
      return Collections.emptyList();
    }
  }

  /**
   * The default implementation returns the given URL String as-is.
   * <p>Can be overridden in subclasses, appending a session id or the like.
   */
  @Override
  public String encodeURL(String url) {
    return url;
  }

  /**
   * The default implementation delegates to {@link #encodeURL},
   * returning the given URL String as-is.
   * <p>Can be overridden in subclasses, appending a session id or the like
   * in a redirect-specific fashion. For general URL encoding rules,
   * override the common {@link #encodeURL} method instead, applying
   * to redirect URLs as well as to general URLs.
   */
  @Override
  public String encodeRedirectURL(String url) {
    return encodeURL(url);
  }

  @Override
  public void sendError(int status, String errorMessage) throws IOException {
    Assert.state(!isCommitted(), "Cannot set error status - response is already committed");
    this.status = status;
    this.errorMessage = errorMessage;
    setCommitted(true);
  }

  @Override
  public void sendError(int status) throws IOException {
    Assert.state(!isCommitted(), "Cannot set error status - response is already committed");
    this.status = status;
    setCommitted(true);
  }

  @Override
  public void sendRedirect(String url) throws IOException {
    Assert.state(!isCommitted(), "Cannot send redirect - response is already committed");
    Assert.notNull(url, "Redirect URL is required");
    setHeader(HttpHeaders.LOCATION, url);
    setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    setCommitted(true);
  }

  @Nullable
  public String getRedirectedUrl() {
    return getHeader(HttpHeaders.LOCATION);
  }

  @Override
  public void setDateHeader(String name, long value) {
    setHeaderValue(name, formatDate(value));
  }

  @Override
  public void addDateHeader(String name, long value) {
    addHeaderValue(name, formatDate(value));
  }

  public long getDateHeader(String name) {
    String headerValue = getHeader(name);
    if (headerValue == null) {
      return -1;
    }
    try {
      return newDateFormat().parse(getHeader(name)).getTime();
    }
    catch (ParseException ex) {
      throw new IllegalArgumentException(
              "Value for header '" + name + "' is not a valid Date: " + headerValue);
    }
  }

  private String formatDate(long date) {
    return newDateFormat().format(new Date(date));
  }

  private DateFormat newDateFormat() {
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);
    dateFormat.setTimeZone(GMT);
    return dateFormat;
  }

  @Override
  public void setHeader(String name, @Nullable String value) {
    setHeaderValue(name, value);
  }

  @Override
  public void addHeader(String name, @Nullable String value) {
    addHeaderValue(name, value);
  }

  @Override
  public void setIntHeader(String name, int value) {
    setHeaderValue(name, value);
  }

  @Override
  public void addIntHeader(String name, int value) {
    addHeaderValue(name, value);
  }

  private void setHeaderValue(String name, @Nullable Object value) {
    if (value == null) {
      return;
    }
    boolean replaceHeader = true;
    if (setSpecialHeader(name, value, replaceHeader)) {
      return;
    }
    doAddHeaderValue(name, value, replaceHeader);
  }

  private void addHeaderValue(String name, @Nullable Object value) {
    if (value == null) {
      return;
    }
    boolean replaceHeader = false;
    if (setSpecialHeader(name, value, replaceHeader)) {
      return;
    }
    doAddHeaderValue(name, value, replaceHeader);
  }

  private boolean setSpecialHeader(String name, Object value, boolean replaceHeader) {
    if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
      setContentType(value.toString());
      return true;
    }
    else if (HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(name)) {
      setContentLength(value instanceof Number number ? number.intValue() :
                       Integer.parseInt(value.toString()));
      return true;
    }
    else if (HttpHeaders.CONTENT_LANGUAGE.equalsIgnoreCase(name)) {
      String contentLanguages = value.toString();
      HttpHeaders headers = HttpHeaders.forWritable();
      headers.add(HttpHeaders.CONTENT_LANGUAGE, contentLanguages);
      Locale language = headers.getContentLanguage();
      setLocale(language != null ? language : Locale.getDefault());
      // Since setLocale() sets the Content-Language header to the given
      // single Locale, we have to explicitly set the Content-Language header
      // to the user-provided value.
      doAddHeaderValue(HttpHeaders.CONTENT_LANGUAGE, contentLanguages, true);
      return true;
    }
    else if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(name)) {
      MockCookie cookie = MockCookie.parse(value.toString());
      if (replaceHeader) {
        setCookie(cookie);
      }
      else {
        addCookie(cookie);
      }
      return true;
    }
    else {
      return false;
    }
  }

  private void doAddHeaderValue(String name, Object value, boolean replace) {
    Assert.notNull(value, "Header value is required");
    HeaderValueHolder header = this.headers.computeIfAbsent(name, key -> new HeaderValueHolder());
    if (replace) {
      header.setValue(value);
    }
    else {
      header.addValue(value);
    }
  }

  /**
   * Set the {@code Set-Cookie} header to the supplied {@link Cookie},
   * overwriting any previous cookies.
   *
   * @param cookie the {@code Cookie} to set
   * @see #addCookie(Cookie)
   */
  private void setCookie(Cookie cookie) {
    Assert.notNull(cookie, "Cookie is required");
    this.cookies.clear();
    this.cookies.add(cookie);
    doAddHeaderValue(HttpHeaders.SET_COOKIE, getCookieHeader(cookie), true);
  }

  @Override
  public void setStatus(int status) {
    if (!this.isCommitted()) {
      this.status = status;
    }
  }

  @Override
  public int getStatus() {
    return this.status;
  }

  @Nullable
  public String getErrorMessage() {
    return this.errorMessage;
  }

  //---------------------------------------------------------------------
  // Methods for MockRequestDispatcher
  //---------------------------------------------------------------------

  public void setForwardedUrl(@Nullable String forwardedUrl) {
    this.forwardedUrl = forwardedUrl;
  }

  @Nullable
  public String getForwardedUrl() {
    return this.forwardedUrl;
  }

  public void setIncludedUrl(@Nullable String includedUrl) {
    this.includedUrls.clear();
    if (includedUrl != null) {
      this.includedUrls.add(includedUrl);
    }
  }

  @Nullable
  public String getIncludedUrl() {
    int count = this.includedUrls.size();
    Assert.state(count <= 1,
            () -> "More than 1 URL included - check getIncludedUrls instead: " + this.includedUrls);
    return (count == 1 ? this.includedUrls.get(0) : null);
  }

  public void addIncludedUrl(String includedUrl) {
    Assert.notNull(includedUrl, "Included URL is required");
    this.includedUrls.add(includedUrl);
  }

  public List<String> getIncludedUrls() {
    return this.includedUrls;
  }

  /**
   * Inner class that adapts the ServletOutputStream to mark the
   * response as committed once the buffer size is exceeded.
   */
  private class ResponseServletOutputStream extends DelegatingServletOutputStream {

    public ResponseServletOutputStream(OutputStream out) {
      super(out);
    }

    @Override
    public void write(int b) throws IOException {
      super.write(b);
      super.flush();
      setCommittedIfBufferSizeExceeded();
    }

    @Override
    public void flush() throws IOException {
      super.flush();
      setCommitted(true);
    }
  }

  /**
   * Inner class that adapts the PrintWriter to mark the
   * response as committed once the buffer size is exceeded.
   */
  private class ResponsePrintWriter extends PrintWriter {

    public ResponsePrintWriter(Writer out) {
      super(out, true);
    }

    @Override
    public void write(char[] buf, int off, int len) {
      super.write(buf, off, len);
      super.flush();
      setCommittedIfBufferSizeExceeded();
    }

    @Override
    public void write(String s, int off, int len) {
      super.write(s, off, len);
      super.flush();
      setCommittedIfBufferSizeExceeded();
    }

    @Override
    public void write(int c) {
      super.write(c);
      super.flush();
      setCommittedIfBufferSizeExceeded();
    }

    @Override
    public void flush() {
      super.flush();
      setCommitted(true);
    }

    @Override
    public void close() {
      super.flush();
      super.close();
      setCommitted(true);
    }
  }

}
