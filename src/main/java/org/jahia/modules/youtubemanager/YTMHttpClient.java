package org.jahia.modules.youtubemanager;

import com.github.scribejava.core.model.HttpClient;
import com.github.scribejava.core.model.OAuthAsyncRequestCallback;
import com.github.scribejava.core.model.OAuthRequestAsync;
import com.github.scribejava.core.model.Verb;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by stefan on 2016-11-14.
 */
public class YTMHttpClient extends DefaultHttpClient implements HttpClient {

    public YTMHttpClient() {
        super();
    }
    @Override
    public void close() throws IOException {

    }

    @Override
    public <T> Future<T> executeAsync(String userAgent, Map<String, String> headers, Verb httpVerb, String completeUrl, String bodyContents, OAuthAsyncRequestCallback<T> callback, OAuthRequestAsync.ResponseConverter<T> converter) {
        return null;
    }
}
