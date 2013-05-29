package com.dotc.nova.filesystem;

public interface FileWriteHandler {
	public void fileWritten(String fileContents);

	public void errorOccurred(Throwable error);
}
