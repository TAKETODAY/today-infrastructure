package test.jdbc.model;

/**
 * @author TODAY <br>
 * 2019-03-17 21:57
 */
public enum PostStatus {

  /** 已发布 */
  PUBLISHED(0, "已发布"),
  /** 草稿 */
  DRAFT(1, "草稿"),
  /** 回收站 */
  RECYCLE(2, "回收站");

  private final int code;
  private final String msg;

  PostStatus(int code, String msg) {
    this.code = code;
    this.msg = msg;
  }

  public static PostStatus valueOf(int code) {
    switch (code) {
      case 0:
        return PUBLISHED;
      case 1:
        return DRAFT;
      case 2:
      default:
        return RECYCLE;
    }
  }

  public int getCode() {
    return code;
  }

  public String getMsg() {
    return msg;
  }
}
