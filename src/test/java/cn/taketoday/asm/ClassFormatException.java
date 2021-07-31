package cn.taketoday.asm;

/**
 * A {@link RuntimeException} thrown by {@link ClassFile} when a class file is malformed.
 *
 * @author Eric Bruneton
 */
public class ClassFormatException extends RuntimeException {

  private static final long serialVersionUID = -6426141818319882225L;

  /**
   * Constructs a new ClassFormatException instance.
   *
   * @param message the detailed message of this exception.
   */
  public ClassFormatException(final String message) {
    super(message);
  }

  /**
   * Constructs a new ClassFormatException instance.
   *
   * @param message the detailed message of this exception.
   * @param cause the cause of this exception.
   */
  public ClassFormatException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
