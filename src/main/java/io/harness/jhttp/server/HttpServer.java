package io.harness.jhttp.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.harness.jhttp.api.RequestProcessor;
import io.harness.jhttp.processor.DirectoryIndex;
import io.harness.jhttp.processor.ResourceNotFound;
import io.harness.jhttp.processor.StaticFile;
import org.apache.commons.io.IOUtils;

import io.harness.jhttp.processor.DirectoryListing;

/**
 * This class creates a server socket, registers all processors and for each connection creates a
 * {@link ConnectionHandler} running in the new pooled thread.
 * 
 *
 */
public class HttpServer {

    private final ExecutorService executor;

    private final List<RequestProcessor> processors;

    private final PathResolver fileResolver;

    private final int port;

    private volatile ServerSocket serverSocket;

    public HttpServer(Path serverRoot, int port, int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.fileResolver = new PathResolver(serverRoot);
        this.port = port;

        processors = new ArrayList<>();
        processors.add(new DirectoryIndex());
        processors.add(new DirectoryListing());
        processors.add(new StaticFile());
        processors.add(new ResourceNotFound());
    }

    /**
     * Start listening.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            do {
                final Socket clientSocket = serverSocket.accept();
                final ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, processors,
                        fileResolver);
                executor.submit(connectionHandler);
            } while (!serverSocket.isClosed());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Stop listening.
     */
    public void stop() {
        IOUtils.closeQuietly(serverSocket);
    }

    public int getServerPort() {
        if (serverSocket == null) {
            return 0;
        } else {
            return serverSocket.getLocalPort();
        }
    }
}
