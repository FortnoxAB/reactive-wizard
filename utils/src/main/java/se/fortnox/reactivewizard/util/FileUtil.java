package se.fortnox.reactivewizard.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.internal.operators.BackpressureUtils;
import rx.subscriptions.BooleanSubscription;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

import static rx.internal.operators.BackpressureUtils.addCap;

/**
 * Reads and writes files fully asynchronous using Observables. Supports backpressure.
 */
public class FileUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);
    public static final int DEFAULT_CHUNK_SIZE = 4096;

    public static Observable<byte[]> readFile(String fileName) {
        return readFile(fileName, DEFAULT_CHUNK_SIZE);
    }

    public static Observable<byte[]> readFile(String fileName, int chunkSize) {
        Path path = FileSystems.getDefault().getPath(fileName);
        return readFile(path.toAbsolutePath(), chunkSize);
    }

    public static Observable<byte[]> readFile(Path path, int chunkSize) {
        return Observable.unsafeCreate(subscriber -> {
            try {
                AsynchronousFileChannel fc = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
                AsyncFileReader fileReader = new AsyncFileReader(subscriber, fc, chunkSize);
                subscriber.setProducer(fileReader::request);
            } catch (IOException e) {
                subscriber.onError(e);
            }
        });
    }

    private static class AsyncFileReader implements CompletionHandler<Integer, Long> {

        private final Subscriber<? super byte[]> subscriber;
        private final AsynchronousFileChannel fc;
        private final ByteBuffer buf;
        private final AtomicLong requestedOrPosition = new AtomicLong();

        AsyncFileReader(Subscriber<? super byte[]> subscriber, AsynchronousFileChannel fc, int chunkSize) {
            this.subscriber = subscriber;
            this.fc = fc;
            buf = ByteBuffer.allocate(chunkSize);
            subscriber.add(BooleanSubscription.create(this::close));
        }

        @Override
        public void completed(Integer bytesRead, Long expectedPos) {
            if (subscriber.isUnsubscribed()) {
                return;
            }
            if (bytesRead == -1) {
                // File has been read to it's end
                subscriber.onCompleted();
                close();
                return;
            }

            // There was data, output it on the observable
            subscriber.onNext(copyBuffer(bytesRead));

            // Decrement request, because we have emitted data
            long newPos = expectedPos + bytesRead;
            long nextReadPosition = logProducedAndGetNextReadPosition(requestedOrPosition, newPos);
            if (nextReadPosition >= 0) {
                // There is more requested, continue to read
                fc.read(buf, nextReadPosition, nextReadPosition, this);
            }
        }

        /**
         * Call this after producing to decrement the current request.
         *
         * If the request becomes zero, it stores the position to read from as a negative value in the same field. This
         * is done to avoid race conditions that occurred when reading and setting two variables.
         *
         * @param requested hold request or position
         * @param newPos is the file position to read from
         * @return the file position to read from, or -1 if no further reading should be done at this point
         */
        private long logProducedAndGetNextReadPosition(AtomicLong requested, long newPos) {
            while (true) {
                long current = requested.get();
                // If requested was MAX, we should just keep reading
                if (current == Long.MAX_VALUE) {
                    return newPos;
                }
                // If requested was 0, we have an error
                if (current == 0) {
                    return -1;
                }
                // If requested was <0, it was a position, it should not be at this point, so an error as well
                if (current < 0) {
                    return -1;
                }

                long next = current - 1;
                if (next < 0L) {
                    throw new IllegalStateException("More produced than requested: " + next);
                }

                // If requested became 0, we are out of requests, and we should set it to the next read position (negative)
                if (next == 0) {
                    if (requested.compareAndSet(current, -newPos)) {
                        return -1;
                    }
                } else {
                    // If requested became a positive value, we should just return the next position
                    if (requested.compareAndSet(current, next)) {
                        return newPos;
                    }
                }
            }
        }

        /**
         * Call this to add a request to the existing requests, and get the position to read from.
         * @param requested hold request or position
         * @param requestCount number to be added to requested
         * @return the position to read from, or -1 if no further reading should be triggered at this point
         */
        private long addRequestAndGetReadPosition(AtomicLong requested, long requestCount) {
            while (true) {
                long current = requested.get();

                if (current == 0) {
                    // There was no request, and no stored position, so we should read from 0
                    long next = addCap(current, requestCount);
                    if (requested.compareAndSet(current, next)) {
                        return 0;
                    }
                } else if (current > 0) {
                    // There was already a request, so we should not read more, there is already reading going on
                    long next = addCap(current, requestCount);
                    if (requested.compareAndSet(current, next)) {
                        return -1;
                    }
                } else {
                    // current < 0, it is a position, and request was 0
                    // set correct request and return the position
                    long next = addCap(0, requestCount);
                    if (requested.compareAndSet(current, next)) {
                        return -current;
                    }
                }
            }
        }


        private byte[] copyBuffer(Integer result) {
            buf.flip();
            byte[] outp = new byte[result];
            System.arraycopy(buf.array(), 0, outp, 0, result);
            buf.clear();
            return outp;
        }

        private void close() {
            try {
                fc.close();
            } catch (IOException e) {
                log.error("Failed closing file " + fc, e);
            }
        }

        @Override
        public void failed(Throwable throwable, Long expectedPos) {
            subscriber.onError(throwable);
        }

        public void request(long requestCount) {
            // Validate that requestCount is not 0 or invalid, so that we avoid being fooled into reading when someone requests 0
            if (!BackpressureUtils.validate(requestCount)) {
                return;
            }
            // Add the request to the accumulated requests, and check if it was 0 before
            // If it was 0, then we must trigger a new read
            final long readPosition = addRequestAndGetReadPosition(requestedOrPosition, requestCount);
            if (readPosition >= 0) {
                fc.read(buf, readPosition, readPosition, this);
            }
        }
    }

    /**
     * Write a file asynchronously
     * @param fileName of the file to write
     * @param bytes is a stream of bytes
     * @return an observable that completes once all input has been written
     */
    public static Observable<Void> write(String fileName, Observable<byte[]> bytes) {
        Path path = FileSystems.getDefault().getPath(fileName);
        return write(path.toAbsolutePath(), bytes);
    }

    /**
     * Write a file asynchronously
     * @param path of the file to write
     * @param bytes is a stream of bytes
     * @return an observable that completes once all input has been written
     */
    public static Observable<Void> write(Path path, Observable<byte[]> bytes) {
        return Observable.defer(() -> {
            try {
                AsynchronousFileChannel fc = AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                final AtomicLong position = new AtomicLong();

                return bytes
                        .concatMap(chunk ->
                                Observable.<Void>unsafeCreate(subscriber -> {
                                    try {
                                        fc.write(
                                                ByteBuffer.wrap(chunk),
                                                position.getAndAdd(chunk.length),
                                                null,
                                                new WriteCompletionHandler(subscriber));
                                    } catch (Exception e) {
                                        subscriber.onError(e);
                                    }
                                }))
                        .doOnUnsubscribe(() -> {
                            try {
                                fc.close();
                            } catch (IOException e) {
                                log.error("failed.to.close.file.channel", e);
                            }
                        });
            } catch (IOException e) {
                return Observable.error(e);
            }
        });
    }

    private static class WriteCompletionHandler implements CompletionHandler<Integer, Object> {
        private final Subscriber<? super Void> subscriber;

        public WriteCompletionHandler(Subscriber<? super Void> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            subscriber.onCompleted();
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            subscriber.onError(exc);
        }
    }

}
