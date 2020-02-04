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

package ch.squaredesk.nova.events.consumers;

class ParamHelper {
        private ParamHelper () {}

        static Object elementAtIndex(int idx, Object... data) {
            return data == null || data.length <= idx ?
                    null :
                    data[idx];
        }
}
