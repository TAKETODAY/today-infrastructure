/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StreamUtils;

/**
 * An update event used to provide log updates.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class LogUpdateEvent extends UpdateEvent {

  private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

  private static final Pattern TRAILING_NEW_LINE_PATTERN = Pattern.compile("\\n$");

  private final StreamType streamType;

  private final byte[] payload;

  private final String string;

  LogUpdateEvent(StreamType streamType, byte[] payload) {
    this.streamType = streamType;
    this.payload = payload;
    String string = new String(payload, StandardCharsets.UTF_8);
    string = ANSI_PATTERN.matcher(string).replaceAll("");
    string = TRAILING_NEW_LINE_PATTERN.matcher(string).replaceAll("");
    this.string = string;
  }

  public void print() {
    switch (this.streamType) {
      case STD_OUT -> System.out.println(this);
      case STD_ERR -> System.err.println(this);
    }
  }

  public StreamType getStreamType() {
    return this.streamType;
  }

  public byte[] getPayload() {
    return this.payload;
  }

  @Override
  public String toString() {
    return this.string;
  }

  static void readAll(InputStream inputStream, Consumer<LogUpdateEvent> consumer) throws IOException {
    try {
      LogUpdateEvent event;
      while ((event = LogUpdateEvent.read(inputStream)) != null) {
        consumer.accept(event);
      }
    }
    catch (IllegalStateException ex) {
      byte[] message = ex.getMessage().getBytes(StandardCharsets.UTF_8);
      consumer.accept(new LogUpdateEvent(StreamType.STD_ERR, message));
      StreamUtils.drain(inputStream);
    }
    finally {
      inputStream.close();
    }
  }

  private static LogUpdateEvent read(InputStream inputStream) throws IOException {
    byte[] header = read(inputStream, 8);
    if (header == null) {
      return null;
    }
    StreamType streamType = StreamType.forId(header[0]);
    long size = 0;
    for (int i = 0; i < 4; i++) {
      size = (size << 8) + (header[i + 4] & 0xff);
    }
    byte[] payload = read(inputStream, size);
    return new LogUpdateEvent(streamType, payload);
  }

  private static byte[] read(InputStream inputStream, long size) throws IOException {
    byte[] data = new byte[(int) size];
    int offset = 0;
    do {
      int amountRead = inputStream.read(data, offset, data.length - offset);
      if (amountRead == -1) {
        return null;
      }
      offset += amountRead;
    }
    while (offset < data.length);
    return data;
  }

  /**
   * Stream types supported by the event.
   */
  public enum StreamType {

    /**
     * Input from {@code stdin}.
     */
    STD_IN,

    /**
     * Output to {@code stdout}.
     */
    STD_OUT,

    /**
     * Output to {@code stderr}.
     */
    STD_ERR;

    static StreamType forId(byte id) {
      int upperBound = values().length;
      Assert.state(id > 0 && id < upperBound,
              () -> "Stream type is out of bounds. Must be >= 0 and < " + upperBound + ", but was " + id);
      return values()[id];
    }

  }

}
