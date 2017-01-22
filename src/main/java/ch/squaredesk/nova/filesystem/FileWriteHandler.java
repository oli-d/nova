/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the license described in the "LICENSE" file in the root directory
 * of the distribution package.
 */

package ch.squaredesk.nova.filesystem;

public interface FileWriteHandler {
	void fileWritten(String fileContents);

	void errorOccurred(Throwable error);
}
