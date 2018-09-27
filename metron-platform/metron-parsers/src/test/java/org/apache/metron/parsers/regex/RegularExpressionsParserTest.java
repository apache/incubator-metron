/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsers.regex;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.apache.metron.parsers.regex.RegularExpressionsParser;

public class RegularExpressionsParserTest {
    private RegularExpressionsParser regularExpressionsParser;
    private JSONObject parserConfig;

    @Test
    public void convertCamelCaseToUnderScoreTest() {
        Assert.assertEquals("dst_process_name", regularExpressionsParser.convert("dstProcessName"));
        Assert.assertEquals("process", regularExpressionsParser.convert("process"));
        Assert.assertEquals("dst_process_id", regularExpressionsParser.convert("dstProcessId"));

    }

    @Before
    public void setUp() throws Exception {
        parserConfig =
                getJsonConfig(Paths.get("src/test/resources/config/RegularExpressionsParserConfig.json").toString());
        regularExpressionsParser = new RegularExpressionsParser();
        regularExpressionsParser.configure(parserConfig);
    }

    @Test
    public void testSSHDParse1() throws Exception {
        final String message =
                "<38>Jun 20 15:01:17 deviceName sshd[11672]: Accepted publickey for prod from 22.22.22.22 port 55555 ssh2";
        final JSONObject parsed = parse(message);
        // Expected
        final Map<String, Object> expectedJson = new HashMap<>();
        expectedJson.put("dst_process_name", "sshd");
        expectedJson.put("dst_process_id", "11672");
        expectedJson.put("dst_user_id", "prod");
        expectedJson.put("ip_src_addr", "22.22.22.22");
        expectedJson.put("ip_src_port", "55555");
        expectedJson.put("app_protocol", "ssh2");
        assertTrue(validate(expectedJson, parsed));

    }

    private JSONObject parse(String message) throws Exception {
        regularExpressionsParser.configure(parserConfig);
        final List<JSONObject> result = regularExpressionsParser.parse(message.getBytes());
        if (result.size() > 0) {
            return result.get(0);
        }
        throw new Exception("Could not parse : " + message);
    }

    private boolean validate(Map<String, Object> expectedJson, Map<String, Object> parsed) {
        boolean success = true;
        for (final Map.Entry<String, Object> entry : expectedJson.entrySet()) {
            if (parsed.get(entry.getKey()) != null && parsed.get(entry.getKey()) instanceof String
                    && ((String) parsed.get(entry.getKey())).equalsIgnoreCase((String) entry.getValue())) {

            } else if (parsed.get(entry.getKey()) != null && parsed.get(entry.getKey()) instanceof Long
                    && ((Long) parsed.get(entry.getKey())).longValue() == ((Long) entry.getValue())
                    .longValue()) {

            } else if (parsed.get(entry.getKey()) != null) {
                System.out.println("Key : " + entry.getKey() + " : Expected [" + entry.getValue()
                        + "] Actual [" + parsed.get(entry.getKey()) + "]");
                success = false;
                break;
            } else {
                System.out.println("Key not found : " + entry.getKey());
                success = false;
                break;
            }
        }
        if (!success) {
            System.out.println("original_string = " + parsed.get("original_string"));
            System.out.println("Parsed = " + ((JSONObject) parsed).toJSONString());
            return false;
        }
        return true;
    }

    public static JSONObject getJsonConfig(String filePath) {
        try {
            final String content = new String(Files.readAllBytes(Paths.get(filePath)));
            return (JSONObject) new JSONParser().parse(content);
        } catch (final Exception e) {
              e.printStackTrace();
        }
        return null;
    }
}
