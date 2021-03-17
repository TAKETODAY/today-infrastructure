package cn.taketoday.jdbc.parsing.impl;

/**
 * Created by lars on 22.09.2014.
 */
public class ForwardSlashCommentParser extends AbstractCommentParser {

  private boolean commentAlmostEnded;

  @Override
  protected void startParsing() {
    commentAlmostEnded = false;
  }

  @Override
  public boolean supports(char c, String sql, int idx) {
    return sql.length() > idx + 1 && c == '/' && sql.charAt(idx + 1) == '*';
  }

  @Override
  public boolean isEndComment(char c) {
    if (commentAlmostEnded && c == '/') {
      return true;
    }
    commentAlmostEnded = c == '*';
    return false;
  }
}
