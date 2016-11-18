package org.jahia.modules.youtubemanager.models;

import org.jahia.modules.youtubemanager.util.YoutubeEnums;

import java.util.List;

/**
 * Created by stefan on 2016-11-14.
 */
public class VideoDetails {

    private String title;
    private YoutubeEnums.VideoFormat format;
    private YoutubeEnums.VideoStatus status;
    private List<String> tags;
    private String description;
    private Long uuid;
    private Long size;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public YoutubeEnums.VideoFormat getFormat() {
        return format;
    }

    public void setFormat(YoutubeEnums.VideoFormat format) {
        this.format = format;
    }

    public YoutubeEnums.VideoStatus getStatus() {
        return status;
    }

    public void setStatus(YoutubeEnums.VideoStatus status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUuid() {
        return uuid;
    }

    public void setUuid(Long uuid) {
        this.uuid = uuid;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
