/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 *
 */

package ch.squaredesk.nova.comm.http;

import ch.squaredesk.nova.comm.bla.Adapta;

import java.util.ArrayList;
import java.util.List;

public class HttpAdapta extends Adapta {
    @Override
    public <T> List<HttpInvocation<T>> invocations (Class<T> type) {
        ArrayList<HttpInvocation<T>> retVal = new ArrayList<>();

        T request = null;
        HttpInvocation<T> i = new HttpInvocation<>(request);
        retVal.add(i);
        i.complete(12, "");
        i.complete("Hallo", "Welt");

        return retVal;
    }

}
