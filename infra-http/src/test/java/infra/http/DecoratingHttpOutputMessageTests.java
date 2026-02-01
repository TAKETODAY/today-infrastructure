package infra.http;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/2/1 10:08
 */
class DecoratingHttpOutputMessageTests {

  @Test
  void shouldDelegateSupportsZeroCopy() {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    when(mockDelegate.supportsZeroCopy()).thenReturn(true);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);

    assertTrue(message.supportsZeroCopy());
    verify(mockDelegate).supportsZeroCopy();
  }

  @Test
  void shouldDelegateSetHeaders() {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    HttpHeaders headers = Mockito.mock(HttpHeaders.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.setHeaders(headers);

    verify(mockDelegate).setHeaders(headers);
  }

  @Test
  void shouldDelegateSetHeader() {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.setHeader("Content-Type", "application/json");

    verify(mockDelegate).setHeader("Content-Type", "application/json");
  }

  @Test
  void shouldDelegateSetContentType() {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    MediaType mediaType = Mockito.mock(MediaType.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.setContentType(mediaType);

    verify(mockDelegate).setContentType(mediaType);
  }

  @Test
  void shouldDelegateSetContentLength() {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.setContentLength(1024L);

    verify(mockDelegate).setContentLength(1024L);
  }

  @Test
  void shouldDelegateSendFileWithPath() throws IOException {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    Path path = Mockito.mock(Path.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.sendFile(path, 0L, 100L);

    verify(mockDelegate).sendFile(path, 0L, 100L);
  }

  @Test
  void shouldDelegateSendFileWithFileAndRange() throws IOException {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    File file = Mockito.mock(File.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.sendFile(file, 0L, 100L);

    verify(mockDelegate).sendFile(file, 0L, 100L);
  }

  @Test
  void shouldDelegateSendFileWithFileOnly() throws IOException {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    File file = Mockito.mock(File.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.sendFile(file);

    verify(mockDelegate).sendFile(file);
  }

  @Test
  void shouldDelegateRemoveHeader() {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    when(mockDelegate.removeHeader("Content-Type")).thenReturn(true);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    boolean result = message.removeHeader("Content-Type");

    assertTrue(result);
    verify(mockDelegate).removeHeader("Content-Type");
  }

  @Test
  void shouldDelegateGetBody() throws IOException {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    OutputStream outputStream = Mockito.mock(OutputStream.class);
    when(mockDelegate.getBody()).thenReturn(outputStream);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    OutputStream result = message.getBody();

    assertSame(outputStream, result);
    verify(mockDelegate).getBody();
  }

  @Test
  void shouldDelegateAddHeaders() {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);
    HttpHeaders headers = Mockito.mock(HttpHeaders.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.addHeaders(headers);

    verify(mockDelegate).addHeaders(headers);
  }

  @Test
  void shouldDelegateAddHeader() {
    HttpOutputMessage mockDelegate = Mockito.mock(HttpOutputMessage.class);

    DecoratingHttpOutputMessage message = new DecoratingHttpOutputMessage(mockDelegate);
    message.addHeader("Authorization", "Bearer token");

    verify(mockDelegate).addHeader("Authorization", "Bearer token");
  }

}