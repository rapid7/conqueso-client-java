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

import java.util.Map;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

/**
 * Information about a application instance registered with a Conqueso server.
 */
public class InstanceInfo {

    private final String ipAddress;
    private final String role;
    private final long pollIntervalMillis;
    private final boolean offline;
    private final String createdAt;
    private final String updatedAt;
    private final ImmutableMap<String, String> metadata;
    
    @JsonCreator
    public InstanceInfo(
            @JsonProperty("ip") String ipAddress, 
            @JsonProperty("role") String role, 
            @JsonProperty("pollInterval") long pollIntervalMillis, 
            @JsonProperty("offline") boolean offline, 
            @JsonProperty("createdAt") String createdAt,
            @JsonProperty("updatedAt") String updatedAt, 
            @JsonProperty("metadata") Map<String, String> metadata) {
        this.ipAddress = ipAddress;
        this.role = role;
        this.pollIntervalMillis = pollIntervalMillis;
        this.offline = offline;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.metadata = ImmutableMap.copyOf(metadata);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getRole() {
        return role;
    }

    public long getPollIntervalMillis() {
        return pollIntervalMillis;
    }

    public boolean isOffline() {
        return offline;
    }

    /**
     * Retrieve create time as a String. This String
     * can be parsed using {@link ConquesoClient#parseConquesoDate(String)}.
     * @return the create time value
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Retrieve updated time as a String. This String
     * can be parsed using {@link ConquesoClient#parseConquesoDate(String)}.
     * @return the updated time value
     */
    public String getUpdatedAt() {
        return updatedAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    @Override
    public String toString() {
        return String.format("%s@%s", role, ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ipAddress, role, pollIntervalMillis, offline, createdAt, updatedAt, metadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InstanceInfo other = (InstanceInfo) obj;
        return Objects.equal(ipAddress, other.ipAddress) &&
                Objects.equal(role, other.role) &&
                Objects.equal(pollIntervalMillis, other.pollIntervalMillis) &&
                Objects.equal(offline, other.offline) &&
                Objects.equal(createdAt, other.createdAt) &&
                Objects.equal(updatedAt, other.updatedAt) &&
                Objects.equal(metadata, other.metadata);
    }    
}
