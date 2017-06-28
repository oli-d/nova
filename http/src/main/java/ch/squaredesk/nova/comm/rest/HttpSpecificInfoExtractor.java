package ch.squaredesk.nova.comm.rest;

public class HttpSpecificInfoExtractor {
    public static HttpSpecificInfo extractFrom(Object httpRequest) {
//        Header[] headers = httpRequest.getAllHeaders();
//
//        Map<String, String> headerMap;
//        if (headers==null || headers.length==0) {
//            headerMap = Collections.emptyMap();
//        } else {
//            headerMap = new HashMap<>(headers.length+1, 1.0f   );
//            for (Header header: headers) headerMap.put(header.getName(), header.getValue());
//        }
//        return new HttpSpecificInfo(
//                HttpRequestMethod.valueOf(httpRequest.getRequestLine().getMethod().toUpperCase()),
//                headerMap);
        return null;
    }
}
