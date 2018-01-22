/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */

package ch.squaredesk.nova.filesystem;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

public class Filesystem {

    public Flowable<String> readTextFile(String pathToFile) {
        return Flowable.generate(
                () -> new BufferedReader(new InputStreamReader(new FileInputStream(pathToFile))),
                (reader, emitter) -> {
                    if (reader.ready()) {
                        emitter.onNext(reader.readLine());
                    } else {
                        emitter.onComplete();
                    }
                },
                reader -> reader.close());
    }

    public Flowable<String> readTextFileFromClasspath(String resourcePath) {
        URL resourceUri = getClass().getResource(resourcePath);
        if (resourceUri == null) {
            return Flowable.error(new NoSuchFileException(resourcePath));
        } else {
            return readTextFile(getClass().getResource(resourcePath).getFile());
        }
    }

    public Single<String> readTextFileFully(String pathToFile) {
        String filePath = getWindowsPathUsableForNio(pathToFile);
        return Single.create(s -> {
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(filePath), StandardOpenOption.READ);
            long capacity = channel.size();
            // TODO: hack for simplicity. do this properly
            if (capacity > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("File is too big. Max size is " + Integer.MAX_VALUE + " bytes.");
            }


            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            channel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, final ByteBuffer attachment) {
                    s.onSuccess(new String(attachment.array()));
                }

                @Override
                public void failed(final Throwable exc, final ByteBuffer attachment) {
                    s.onError(exc);
                }
            });
        });
    }

    public Single<String> readTextFileFullyFromClasspath(String resourcePath) {
        URL resourceUri = getClass().getResource(resourcePath);
        if (resourceUri == null) {
            return Single.error(new NoSuchFileException(resourcePath));
        } else {
            return readTextFileFully(getClass().getResource(resourcePath).getFile());
        }
    }

    /**
     * Java nio throws an exception for paths like /C:/temp/test.txt. It works if for such a path the leading character is cut off. This is
     * what this method does.
     */
    private String getWindowsPathUsableForNio(String path) {
        if (path == null) {
            return null;
        }
        char[] pathAsChars = path.toCharArray();
        if (pathAsChars.length > 2 //
                && (pathAsChars[0] == File.pathSeparatorChar || pathAsChars[0] == '/') //
                && Character.isAlphabetic(pathAsChars[1]) //
                && pathAsChars[2] == ':') {
            return path.substring(1);
        } else {
            return path;
        }
    }

    public Completable writeFile(final String content, String filePath) {
        return Completable.create(s -> {
            AsynchronousFileChannel channel = AsynchronousFileChannel.open(Paths.get(filePath), StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE);
            ByteBuffer contentBuffer = ByteBuffer.wrap(content.getBytes());
            channel.write(contentBuffer, 0, null, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, final ByteBuffer attachment) {
                    s.onComplete();
                }

                @Override
                public void failed(final Throwable exc, final ByteBuffer attachment) {
                    s.onError(exc);
                }
            });
        });
    }

    public Completable writeFileSync(String content, String filePath, boolean append) throws IOException {
        return writeFileSync(content, StandardCharsets.UTF_8, filePath, append);
    }

    public Completable writeFileSync(String content, Charset encoding, String filePath, boolean append) throws IOException {
        return Completable.create(s -> {
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
                s.onComplete();
            } catch (Throwable t) {
                s.onError(t);
            } finally {
                channel.close();
            }
        });
    }

}
