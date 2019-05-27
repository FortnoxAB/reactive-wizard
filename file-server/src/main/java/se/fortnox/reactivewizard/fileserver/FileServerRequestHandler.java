package se.fortnox.reactivewizard.fileserver;

import javax.inject.Inject;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.util.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static rx.Observable.empty;
import static rx.Observable.merge;
import static se.fortnox.reactivewizard.fileserver.ContentTypeDetector.getContentType;

/**
 * Serves files from file system as defined by {@link FileServerConfig}
 */
public class FileServerRequestHandler implements RequestHandler<ByteBuf, ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(FileServerRequestHandler.class);
    private static final RequestLogger REQUEST_LOGGER = new RequestLogger(log);

    private final FileServerConfig config;
    private final Path defaultFile;
    private final Path rootPath;

    @Inject
    public FileServerRequestHandler(FileServerConfig config) {
        this.config = config;
        if (config.getPath() == null) {
            rootPath = null;
            defaultFile = null;
        } else {
            rootPath = Paths.get(config.getPath()).toAbsolutePath().normalize();
            defaultFile = getDefaultFile(config);
        }
    }

    private Path getDefaultFile(FileServerConfig config) {
        if (config.getIndexFile() == null) {
            return null;
        }

        Path path = Paths.get(config.getPath(), config.getIndexFile());
        if (!Files.exists(path)) {
            return null;
        }
        return path;
    }

    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        if (rootPath == null) {
            return null;
        }

        if (request.getDecodedPath().endsWith("favicon.ico")) {
            return empty();
        }

        Path file = getFile(request);
        if (file == null) {
            return null;
        }


        response.setStatus(HttpResponseStatus.OK);
        for (Map.Entry<String, String> e : config.getHeaders().entrySet()) {
            response.addHeader(e.getKey(), e.getValue());
        }

        setContentType(response, file);

        long requestStartTime = System.currentTimeMillis();
        Observable<byte[]> fileStream = FileUtil.readFile(file.toString(), 4096);
        return merge(request.discardContent(), response.writeBytesAndFlushOnEach(fileStream)
            .doAfterTerminate(() -> REQUEST_LOGGER.log(request, response, requestStartTime)));
    }

    private void setContentType(HttpServerResponse<ByteBuf> response, Path file) {
        String contentType = getContentType(file);
        if (file.toString().endsWith(".js") || contentType.startsWith("text/")) {
            contentType += "; charset=utf-8";
        }
        response.addHeader("Content-Type", contentType);
    }

    private Path getFile(HttpServerRequest<ByteBuf> request) {
        String decodedPath = request.getDecodedPath();
        String pathPrefix = config.getPathPrefix();
        if (pathPrefix != null) {
            if (!decodedPath.startsWith(pathPrefix)) {
                return null;
            }
            decodedPath = decodedPath.substring(pathPrefix.length());
        }
        if (decodedPath.startsWith("/")) {
            decodedPath = decodedPath.substring(1);
        }
        if (decodedPath.isEmpty()) {
            return defaultFile;
        }
        Path file = rootPath.resolve(Paths.get(decodedPath)).toAbsolutePath().normalize();
        if (!file.startsWith(rootPath) || !Files.exists(file) || Files.isDirectory(file)) {
            return defaultFile;
        }

        return file;
    }

}

