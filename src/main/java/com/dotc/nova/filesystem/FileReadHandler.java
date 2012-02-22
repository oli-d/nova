package com.dotc.nova.filesystem;


public interface FileReadHandler {
	public void fileRead(String fileContents);

	public void errorOccurred(Throwable error);
}
