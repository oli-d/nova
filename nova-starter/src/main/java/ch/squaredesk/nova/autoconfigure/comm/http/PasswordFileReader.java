/*
 * Copyright (c) 2018-2021 Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file distributed with this
 * work for additional information regarding copyright ownership. You may also obtain a copy of the license at
 *
 *      https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.autoconfigure.comm.http;

import ch.squaredesk.nova.Nova;

class PasswordFileReader {
    private PasswordFileReader() {}
    static String readPasswordFromFile (Nova nova, String passwordFile) {
        if (nova == null) {
            nova = Nova.builder().build();
        }
        return nova.filesystem.readTextFileFully(passwordFile).blockingGet();
    }
}
