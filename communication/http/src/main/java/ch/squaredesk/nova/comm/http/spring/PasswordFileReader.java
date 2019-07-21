package ch.squaredesk.nova.comm.http.spring;

import ch.squaredesk.nova.Nova;

class PasswordFileReader {
    static String readPasswordFromFile (Nova nova, String passwordFile) {
        if (nova == null) {
            nova = Nova.builder().build();
        }
        return nova.filesystem.readTextFileFully(passwordFile).blockingGet();
    }
}
