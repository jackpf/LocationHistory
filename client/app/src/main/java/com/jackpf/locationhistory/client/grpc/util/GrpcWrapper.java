package com.jackpf.locationhistory.client.grpc.util;

import com.jackpf.locationhistory.client.util.Logger;

import java.io.IOException;

import io.grpc.StatusRuntimeException;

public class GrpcWrapper {
    private static final Logger log = new Logger("GrpcWrapper");

    @FunctionalInterface
    public interface WrappedGrpc<T> {
        T run() throws StatusRuntimeException;
    }

    public static <T> T executeWrapped(WrappedGrpc<T> call, String failureMessage) throws IOException {
        try {
            return call.run();
        } catch (StatusRuntimeException e) {
            log.e(failureMessage, e);
            throw new IOException(failureMessage, e);
        }
    }
}
