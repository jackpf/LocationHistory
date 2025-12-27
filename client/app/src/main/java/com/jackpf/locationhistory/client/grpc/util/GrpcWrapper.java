package com.jackpf.locationhistory.client.grpc.util;

import com.jackpf.locationhistory.client.util.Log;

import java.io.IOException;

import io.grpc.StatusRuntimeException;

public class GrpcWrapper {
    @FunctionalInterface
    public interface WrappedGrpc<T> {
        T run() throws StatusRuntimeException;
    }

    public static <T> T executeWrapped(WrappedGrpc<T> call, String failureMessage) throws IOException {
        try {
            return call.run();
        } catch (StatusRuntimeException e) {
            Log.e(failureMessage, e);
            throw new IOException(failureMessage, e);
        }
    }
}
