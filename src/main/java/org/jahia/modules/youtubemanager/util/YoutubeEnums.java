package org.jahia.modules.youtubemanager.util;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by stefan on 2016-11-14.
 */
public class YoutubeEnums {

    public enum VideoStatus {
        PUBLIC("public"),
        PRIVATE("private");

        private final String displayName;

        VideoStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }

    public enum VideoFormat {

        ALL("video/*"),
        MP4("video/mp4");

        private final String displayName;

        VideoFormat(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }

    public enum AuthorizationScopeTypes {
        UPLOAD("https://www.googleapis.com/auth/youtube.upload"),
        YOUTUBE("https://www.googleapis.com/auth/youtube"),
        PARTNER("https://www.googleapis.com/auth/youtubepartner"),
        FORCE_SSL("https://www.googleapis.com/auth/youtube.force-ssl"),
        READ_ONLY("https://www.googleapis.com/auth/youtube.readonly");

        private final String displayName;

        AuthorizationScopeTypes(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public static String build(String scope) {
            for (AuthorizationScopeTypes authorizationScopeType : AuthorizationScopeTypes.values()) {
                if (authorizationScopeType.name().equals(scope)) {
                    return authorizationScopeType.displayName;
                }
            }
            return null;
        }
    }

    public static <T> List<String> getAvailableTypes(Class<T> enumClass) {
        List availableTypes = new LinkedList();
        for (T enumType : enumClass.getEnumConstants()) {
            availableTypes.add(enumType.toString());
        }
        return availableTypes;
    }
}
