package com.dotc.nova.filesystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;
import java.util.concurrent.Future;

import com.dotc.nova.ProcessingLoop;

public class Filesystem {
	private final ProcessingLoop eventDispatcher;

	public Filesystem(ProcessingLoop eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
	}

	public void readFile(String filePath, final FileReadHandler handler) {
		try {
			AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(filePath));
			long capacity = channel.size();
			// TODO: hack for simplicity. do this properly
			if (capacity > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("File is too big. Max size is " + Integer.MAX_VALUE + " bytes.");
			}

			ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
			channel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {

				@Override
				public void completed(Integer result, final ByteBuffer attachment) {
					eventDispatcher.dispatch(new Runnable() {

						@Override
						public void run() {
							handler.fileRead(new String(attachment.array()));
						}
					});
				}

				@Override
				public void failed(final Throwable exc, final ByteBuffer attachment) {
					eventDispatcher.dispatch(new Runnable() {

						@Override
						public void run() {
							handler.errorOccurred(exc);
						}
					});
				}
			});
		} catch (Exception e) {
			handler.errorOccurred(e);
		}
	}

	public String readFileSync(String filePath) throws IOException {
		AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(filePath));
		long capacity = channel.size();
		if (capacity > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("File is too big. Max size is " + Integer.MAX_VALUE + " bytes.");
		}

		ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
		Future<Integer> future = channel.read(buffer, 0);
		try {
			future.get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new String(buffer.array());
	}

}
