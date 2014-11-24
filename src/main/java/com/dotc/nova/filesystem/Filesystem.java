package com.dotc.nova.filesystem;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

public class Filesystem {
	private final com.dotc.nova.process.Process process;

	public Filesystem(com.dotc.nova.process.Process process) {
		this.process = process;
	}

	public void readFile(String filePath, final FileReadHandler handler) {
		try {
			AsynchronousFileChannel channel = AsynchronousFileChannel
					.open(Paths.get(filePath), StandardOpenOption.READ);
			long capacity = channel.size();
			// TODO: hack for simplicity. do this properly
			if (capacity > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("File is too big. Max size is " + Integer.MAX_VALUE + " bytes.");
			}

			ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
			channel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {

				@Override
				public void completed(Integer result, final ByteBuffer attachment) {
					process.nextTick(new Runnable() {
						@Override
						public void run() {
							handler.fileRead(new String(attachment.array()));
						}
					});
				}

				@Override
				public void failed(final Throwable exc, final ByteBuffer attachment) {
					process.nextTick(new Runnable() {
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

	public void readFileFromClasspath(String resourcePath, FileReadHandler handler) {
		URL resourceUri = getClass().getResource(resourcePath);
		if (resourceUri == null) {
			handler.errorOccurred(new NoSuchFileException(resourcePath));
		} else {
			readFile(getClass().getResource(resourcePath).getFile(), handler);
		}
	}

	public String readFileSync(String filePath) throws IOException {
		FileChannel channel = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ);
		long capacity = channel.size();
		if (capacity > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("File is too big. Max size is " + Integer.MAX_VALUE + " bytes.");
		}

		ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
		channel.read(buffer, 0);
		return new String(buffer.array());
	}

	public String readFileFromClasspathSync(String resourcePath) throws IOException {
		URL resourceUri = getClass().getResource(resourcePath);
		if (resourceUri == null) {
			throw new NoSuchFileException(resourcePath);
		} else {
			return readFileSync(resourceUri.getFile());
		}
	}

	public void writeFile(final String content, String filePath, final FileWriteHandler handler) {
		try {
			AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(filePath),
					StandardOpenOption.WRITE, StandardOpenOption.CREATE);
			ByteBuffer contentBuffer = ByteBuffer.wrap(content.getBytes());
			channel.write(contentBuffer, 0, null, new CompletionHandler<Integer, ByteBuffer>() {

				@Override
				public void completed(Integer result, final ByteBuffer attachment) {
					process.nextTick(new Runnable() {
						@Override
						public void run() {
							handler.fileWritten(content);
						}
					});
				}

				@Override
				public void failed(final Throwable exc, final ByteBuffer attachment) {
					process.nextTick(new Runnable() {
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

	public void writeFileSync(String content, String filePath, boolean append) throws IOException {
		writeFileSync(content, StandardCharsets.UTF_8, filePath, append);
	}

	public void writeFileSync(String content, Charset encoding, String filePath, boolean append) throws IOException {
		Set<OpenOption> openOptions = new HashSet<>();
		openOptions.add(StandardOpenOption.WRITE);
		openOptions.add(StandardOpenOption.SYNC);
		openOptions.add(StandardOpenOption.CREATE);
		if (append) {
			openOptions.add(StandardOpenOption.APPEND);
		}
		FileChannel channel = FileChannel.open(Paths.get(filePath), openOptions);
		if (!append) {
			channel.truncate(0);
		}

		try {
			channel.write(ByteBuffer.wrap(content.getBytes(encoding)));
		} finally {
			channel.close();
		}
	}

}
