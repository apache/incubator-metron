package org.apache.metron.parsers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adrianwalker.multilinestring.Multiline;

public class PythonParserTest extends ScriptParserTest {

	/**
	    {
	    "customerId":117,
	   "first_name":"karthik",
	   "last_name":"narayanan",
	   "age":"38",
	   "login-time":"2016-01-28 15:29:48",
	   "ip-address":"216.21.170.221",
	   "os":"windows 10",
	   "device":"Dell Inspiron"
	   }
	   */
	  @Multiline
	  public String result=" {"+
	   "\"source\":\"userlog\","+
	    "\"customerId\":117,"+
	   "\"first_name\":\"karthik\","+
	   "\"last_name\":\"narayanan\","+
	   "\"age\":\"38\","+
	   "\"login-time\":\"2016-01-28 15:29:48\","+
	   "\"ip-address\":\"216.21.170.221\","+
	   "\"os\":\"windows 10\","+
	   "\"device\":\"Dell Inspiron\""+
	   "\"original_string\":\"2016-01-28 15:29:48|117|karthik|narayanan|38|216.21.170.221|windows 10|Dell Inspiron\""+
	   "}";
	  
	@Override
	public Map getTestData() {
		Map testData = new HashMap<String,String>();
	    String input = "2016-01-28 15:29:48|117|karthik|narayanan|38|216.21.170.221|windows 10|Dell Inspiron";
	    testData.put(input,result);
	    return testData;
	}

	@Override
	public String getScriptPath() {
		// TODO Auto-generated method stub
		return "../metron-integration-test/src/main/sample/scripts/test.py";
	}

	@Override
	public String getParseFunction() {
		// TODO Auto-generated method stub
		return "parse";
	}

	@Override
	public String getLanguage() {
		// TODO Auto-generated method stub
		return "python";
	}

	@Override
	public List<String> getTimeFields() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDateFormat() {
		// TODO Auto-generated method stub
		return "yyyy-MM-dd HH:mm:ss";
	}

	@Override
	public String getTimestampField() {
		// TODO Auto-generated method stub
		return "login_time";
	}

}
