package org.apache.metron.parsers;

import org.adrianwalker.multilinestring.Multiline;

public class SquidParserTest extends GrokParserTest {

  @Override
  public String getRawMessage() {
    return "1461576382    161 127.0.0.1 TCP_MISS/200 103701 GET http://www.cnn.com/ - DIRECT/199.27.79.73 text/html";
  }

  /**
   * {
   *   "elapsed":161,
   *   "start_time":1461576382,
   *   "code":200,
   *   "original_string":"1461576382    161 127.0.0.1 TCP_MISS/200 103701 GET http://www.cnn.com/ - DIRECT/199.27.79.73 text/html",
   *   "method":"GET",
   *   "bytes":103701,
   *   "action":"TCP_MISS",
   *   "ip_src_addr":"127.0.0.1",
   *   "url":"http://www.cnn.com/",
   *   "timestamp":1461576382
   * }
   */
  @Multiline
  public String expectedParsedString;

  @Override
  public String getExpectedParsedString() {
    return expectedParsedString;
  }

  @Override
  public String getGrokPath() {
    return "../metron-parsers/src/main/resources/patterns/squid";
  }

  @Override
  public String getGrokPatternLabel() {
    return "SQUID_DELIMITED";
  }

  @Override
  public String[] getTimeFields() {
    return new String[0];
  }

  @Override
  public String getDateFormat() {
    return null;
  }

  @Override
  public String getTimestampField() {
    return "start_time";
  }
}
