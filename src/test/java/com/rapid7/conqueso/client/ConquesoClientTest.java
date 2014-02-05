/**
 * COPYRIGHT (C) 2014, Rapid7 LLC, Boston, MA, USA.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rapid7.conqueso.client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;

public class ConquesoClientTest {
    
    @Test
    public void getPropertyValue_hit() throws IOException {
        String key = "foo";
        String value = "bar";
        
        ConquesoClient client = createClientReturningString(key, value);
        String result = client.getPropertyValue(key);
        
        assertEquals(value, result);
    }
    
    @Test(expected=ConquesoCommunicationException.class)
    public void getPropertyValue_miss() throws IOException {
        String key = "foo";
        String value = "bar";
        
        ConquesoClient client = createClientReturningString(key, value);
        client.getPropertyValue("baz");
    }
    
    @Test
    public void getRoles() throws IOException {
        String response = readFileAsString("roles-response.json");
        
        List<RoleInfo> expected = ImmutableList.of(
                new RoleInfo("analytics-service", "2014-01-22T18:22:28.000Z", "2014-02-22T18:22:28.000Z", 5),
                new RoleInfo("test-framework-server", "2014-02-03T14:54:19.000Z", "2014-03-03T14:54:19.000Z", 1));
        
        ConquesoClient client = createClientReturningString("/api/roles", response);
        ImmutableList<RoleInfo> results = client.getRoles();
        
        assertEquals(expected, results);
    }
    
    @Test
    public void getInstances() throws IOException {
        String response = readFileAsString("instances-response.json");
        
        List<InstanceInfo> expected = ImmutableList.of(
                new InstanceInfo("10.1.100.78", "analytics-service", 60000, false, 
                        "2014-02-05T17:05:39.000Z", "2014-02-05T18:46:48.000Z",
                        ImmutableMap.of("ami-id", "ami-133cb31d", "availability-zone", "us-east-1d", 
                                "local-ipv4", "10.1.100.78")),
                new InstanceInfo("10.1.100.79", "analytics-service", 50000, true, 
                        "2014-03-05T17:05:39.000Z", "2014-03-05T18:46:48.000Z",
                        ImmutableMap.of("ami-id", "ami-133cb31d", "availability-zone", "us-east-1d", 
                                "local-ipv4", "10.1.100.79"))
                );
        
        ConquesoClient client = createClientReturningString("/api/roles/analytics-service/instances", response);
        
        ImmutableList<InstanceInfo> result = client.getInstances("analytics-service");
        assertEquals(expected, result);
    }
    
    @Test
    public void parseConquesoDate() throws ParseException {
        assertEquals(createDate(2014,1,5,17,5,39), ConquesoClient.parseConquesoDate("2014-02-05T17:05:39.000Z"));
        assertEquals(createDate(2015,0,31,12,5,39), ConquesoClient.parseConquesoDate("2015-01-31T12:05:39.000Z"));
    }
    
    private Date createDate(int year, int month, int date, int hourOfDay, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, date, hourOfDay, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
    
    private String readFileAsString(String resourcePath) throws IOException {
        InputStream stream = ConquesoClientTest.class.getResourceAsStream(resourcePath);
        try {
            return CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));
        } finally {
            stream.close();
        }
    }
    
    private ConquesoClient createClientReturningString(final String relativeUrl, final String response) throws IOException {
        return createClientReturningString(ImmutableMap.of(relativeUrl, response));
    }
    
    private ConquesoClient createClientReturningString(final Map<String, String> relativeUrlToResponseMap) throws IOException {
        return new ConquesoClient(new URL("file:/tmp/foo")) {
            @Override
            String readStringFromUrl(String relativeUrl, String errorMessage) {
                String result = relativeUrlToResponseMap.get(relativeUrl);
                if (result == null) {
                    throw new ConquesoCommunicationException(errorMessage);
                }
                return result;
            }            
        };
    }

}
