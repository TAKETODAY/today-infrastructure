package cn.taketoday.jdbc.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: lars Date: 6/14/13 Time: 12:02 AM
 */
public class IOUtils {
  private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

  public static byte[] toByteArray(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
    int length;
    while ((length = input.read(buffer)) != -1) {
      output.write(buffer, 0, length);
    }
    return output.toByteArray();
  }

}
