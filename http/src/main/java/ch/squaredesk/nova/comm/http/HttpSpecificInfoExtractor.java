package ch.squaredesk.nova.comm.http;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpSpecificInfoExtractor {
    public static HttpSpecificInfo extractFrom(HttpRequest httpRequest) {
        Header[] headers = httpRequest.getAllHeaders();

        Map<String, String> headerMap;
        if (headers==null || headers.length==0) {
            headerMap = Collections.emptyMap();
        } else {
            headerMap = new HashMap<>(headers.length+1, 1.0f   );
            for (Header header: headers) headerMap.put(header.getName(), header.getValue());
        }
        return new HttpSpecificInfo(
                HttpRequestMethod.valueOf(httpRequest.getRequestLine().getMethod().toUpperCase()),
                headerMap);
    }
}
