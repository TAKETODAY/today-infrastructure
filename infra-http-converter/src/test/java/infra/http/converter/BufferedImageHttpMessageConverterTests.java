/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.MediaType;
import infra.http.MockHttpInputMessage;
import infra.http.MockHttpOutputMessage;
import infra.http.StreamingHttpOutputMessage;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for BufferedImageHttpMessageConverter.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class BufferedImageHttpMessageConverterTests {

  private BufferedImageHttpMessageConverter converter;

  @BeforeEach
  public void setUp() {
    converter = new BufferedImageHttpMessageConverter();
  }

  @Test
  public void canRead() {
    assertThat(converter.canRead(BufferedImage.class, null)).as("Image not supported").isTrue();
    assertThat(converter.canRead(BufferedImage.class, new MediaType("image", "png"))).as("Image not supported").isTrue();
  }

  @Test
  public void canWrite() {
    assertThat(converter.canWrite(BufferedImage.class, null)).as("Image not supported").isTrue();
    assertThat(converter.canWrite(BufferedImage.class, new MediaType("image", "png"))).as("Image not supported").isTrue();
    assertThat(converter.canWrite(BufferedImage.class, new MediaType("*", "*"))).as("Image not supported").isTrue();
  }

  @Test
  void rejectsUnsupportedTypesAndMissingDefaults() {
    var png = new MediaType("image", "png");
    var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    assertThat(this.converter.getSupportedMediaTypes()).contains(png);
    assertThat(this.converter.canRead(String.class, png)).isFalse();
    assertThat(this.converter.canWrite(String.class, png)).isFalse();
    assertThat(this.converter.canRead(BufferedImage.class, MediaType.TEXT_PLAIN)).isFalse();
    assertThat(this.converter.canWrite(BufferedImage.class, MediaType.TEXT_PLAIN)).isFalse();
    assertThatIllegalArgumentException().isThrownBy(() -> this.converter.setDefaultContentType(MediaType.TEXT_PLAIN))
            .withMessageContaining("not supported");
    this.converter.setDefaultContentType(null);
    assertThatIllegalArgumentException().isThrownBy(() -> this.converter.write(image, null, new MockHttpOutputMessage()))
            .withMessageContaining("defaultContentType");
    assertThatExceptionOfType(HttpMessageNotWritableException.class)
            .isThrownBy(() -> this.converter.write(image, MediaType.TEXT_PLAIN, new MockHttpOutputMessage()))
            .withMessageContaining("ImageWriter");
  }

  @Test
  void requiresASupportedContentTypeWhenReading() {
    var missing = new MockHttpInputMessage(new byte[0]);
    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> this.converter.read(BufferedImage.class, missing))
            .withMessage("No Content-Type header");

    var unsupported = new MockHttpInputMessage(new byte[0]);
    unsupported.getHeaders().setContentType(MediaType.TEXT_PLAIN);
    assertThatExceptionOfType(HttpMessageNotReadableException.class)
            .isThrownBy(() -> this.converter.read(BufferedImage.class, unsupported))
            .withMessageContaining("ImageReader");
  }

  @Test
  void streamsRepeatablePngUsingATemporaryCacheDirectory(@TempDir Path directory) throws IOException {
    assertThatIllegalArgumentException().isThrownBy(() -> this.converter.setCacheDir(directory.resolve("missing").toFile()))
            .withMessageContaining("not a directory");
    this.converter.setCacheDir(directory.toFile());
    var png = new MediaType("image", "png");
    this.converter.setDefaultContentType(png);
    var image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
    image.setRGB(1, 0, 0xff102030);
    var output = mock(StreamingHttpOutputMessage.class);
    this.converter.write(image, new MediaType("image", "*"), output);
    verify(output).setContentType(png);
    var captor = ArgumentCaptor.forClass(StreamingHttpOutputMessage.Body.class);
    verify(output).setBody(captor.capture());
    var body = captor.getValue();
    assertThat(body.repeatable()).isTrue();
    var bytes = new ByteArrayOutputStream();
    body.writeTo(bytes);
    var input = new MockHttpInputMessage(bytes.toByteArray());
    input.getHeaders().setContentType(png);
    assertThat(this.converter.read(BufferedImage.class, input).getRGB(1, 0)).isEqualTo(0xff102030);
  }

  @Test
  public void read() throws IOException {
    Resource logo = new ClassPathResource("logo.jpg", BufferedImageHttpMessageConverterTests.class);
    byte[] body = FileCopyUtils.copyToByteArray(logo.getInputStream());
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
    inputMessage.getHeaders().setContentType(new MediaType("image", "png"));
    BufferedImage result = converter.read(BufferedImage.class, inputMessage);
    assertThat(result.getHeight()).as("Invalid height").isEqualTo(292);
    assertThat(result.getWidth()).as("Invalid width").isEqualTo(819);
  }

  @Test
  public void write() throws IOException {
    Resource logo = new ClassPathResource("logo.jpg", BufferedImageHttpMessageConverterTests.class);
    BufferedImage body = ImageIO.read(logo.getFile());
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    MediaType contentType = new MediaType("image", "png");
    converter.write(body, contentType, outputMessage);
    assertThat(outputMessage.getWrittenHeaders().getContentType()).as("Invalid content type").isEqualTo(contentType);
    assertThat(outputMessage.getBodyAsBytes().length > 0).as("Invalid size").isTrue();
    BufferedImage result = ImageIO.read(new ByteArrayInputStream(outputMessage.getBodyAsBytes()));
    assertThat(result.getHeight()).as("Invalid height").isEqualTo(292);
    assertThat(result.getWidth()).as("Invalid width").isEqualTo(819);
  }

  @Test
  public void writeDefaultContentType() throws IOException {
    Resource logo = new ClassPathResource("logo.jpg", BufferedImageHttpMessageConverterTests.class);
    MediaType contentType = new MediaType("image", "png");
    converter.setDefaultContentType(contentType);
    BufferedImage body = ImageIO.read(logo.getFile());
    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    converter.write(body, new MediaType("*", "*"), outputMessage);
    assertThat(outputMessage.getWrittenHeaders().getContentType()).as("Invalid content type").isEqualTo(contentType);
    assertThat(outputMessage.getBodyAsBytes().length > 0).as("Invalid size").isTrue();
    BufferedImage result = ImageIO.read(new ByteArrayInputStream(outputMessage.getBodyAsBytes()));
    assertThat(result.getHeight()).as("Invalid height").isEqualTo(292);
    assertThat(result.getWidth()).as("Invalid width").isEqualTo(819);
  }

}
