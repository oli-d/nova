/*
 * Copyright (c) 2020 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */
package ch.squaredesk.nova.comm.http;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RequestResponseInfoHolder<T> {
    public final Request request;
    public final Response response;
    public final RequestMessageMetaData metaData;
    public final T requestObject;

    public RequestResponseInfoHolder(URL destination, Request request, Response response) {
        this.request = request;
        this.response = response;

        RequestInfo requestInfo = Optional.ofNullable(request).map(RequestResponseInfoHolder::httpSpecificInfoFrom).orElse(null);
        metaData = new RequestMessageMetaData(destination, requestInfo);
        this.requestObject = null;
    }

    private RequestResponseInfoHolder(Request request, Response response, RequestMessageMetaData metaData, T requestObject) {
        this.request = request;
        this.response = response;
        this.requestObject = requestObject;
        this.metaData = metaData;
    }

    public <U> RequestResponseInfoHolder<U> addRequestObject(U responseObject) {
        return new RequestResponseInfoHolder<>(this.request, this.response, this.metaData, responseObject);
    }

    private static RequestInfo httpSpecificInfoFrom(Request request) {
        Map<String, String> parameters = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] valueList = entry.getValue();
            String valueToPass = null;
            if (valueList != null && valueList.length > 0) {
                valueToPass = valueList[0];
            }
            parameters.put(entry.getKey(), valueToPass);
        }

        return new RequestInfo(convert(request.getMethod()), parameters);
    }

    private static HttpRequestMethod convert(Method method) {
        if (method == Method.CONNECT) {
            return HttpRequestMethod.CONNECT;
        } else if (method == Method.DELETE) {
            return HttpRequestMethod.DELETE;
        } else if (method == Method.GET) {
            return HttpRequestMethod.GET;
        } else if (method == Method.HEAD) {
            return HttpRequestMethod.HEAD;
        } else if (method == Method.OPTIONS) {
            return HttpRequestMethod.OPTIONS;
        } else if (method == Method.PATCH) {
            return HttpRequestMethod.PATCH;
        } else if (method == Method.PRI) {
            return HttpRequestMethod.PRI;
        } else if (method == Method.POST) {
            return HttpRequestMethod.POST;
        } else if (method == Method.PUT) {
            return HttpRequestMethod.PUT;
        } else if (method == Method.TRACE) {
            return HttpRequestMethod.TRACE;
        } else {
            throw new IllegalArgumentException("Unsupported HTTP method " + method);
        }
    }

}
