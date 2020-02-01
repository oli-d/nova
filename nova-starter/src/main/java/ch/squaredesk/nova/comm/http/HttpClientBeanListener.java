package ch.squaredesk.nova.comm.http.spring;

import com.ning.http.client.AsyncHttpClient;

public interface HttpClientBeanListener {
    void httpClientAvailableInContext(AsyncHttpClient httpClient);
}
