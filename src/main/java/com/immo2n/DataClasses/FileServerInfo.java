package com.immo2n.DataClasses;

public class FileServerInfo {
    private String linkSpeed;
    private String ssid;
    private String bucketChecksum;

    public String getLinkSpeed() {
        return linkSpeed;
    }

    public void setLinkSpeed(String linkSpeed) {
        this.linkSpeed = linkSpeed;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBucketChecksum() {
        return bucketChecksum;
    }

    public void setBucketChecksum(String bucketChecksum) {
        this.bucketChecksum = bucketChecksum;
    }
}
