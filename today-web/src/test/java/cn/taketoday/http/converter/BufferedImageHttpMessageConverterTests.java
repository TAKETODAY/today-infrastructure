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

package cn.taketoday.http.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

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
