package com.jackpf.locationhistory.client.grpc.util;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.jackpf.locationhistory.client.util.Logger;

import java.util.function.Consumer;

import io.grpc.StatusRuntimeException;
import lombok.Setter;

public class GrpcFutureWrapper<T> implements FutureCallback<T> {
    private final Consumer<T> valueCallback;
    private final Consumer<StatusRuntimeException> errorCallback;
    @Setter
    private String tag;
    @Setter
    private boolean loggingEnabled;

    private final Logger log = new Logger(this);

    public GrpcFutureWrapper(Consumer<T> valueCallback, Consumer<StatusRuntimeException> errorCallback) {
        this.valueCallback = valueCallback;
        this.errorCallback = errorCallback;
    }

    public static <T> GrpcFutureWrapper<T> empty() {
        return new GrpcFutureWrapper<>((v) -> {
        }, (e) -> {
        });
    }

    @Override
    public void onSuccess(T value) {
        if (loggingEnabled) {
            log.d("%s response: %s", tag, value != null ? value.toString() : "null");
        }
        valueCallback.accept(value);
    }

    @Override
    public void onFailure(@NonNull Throwable t) {
        if (loggingEnabled) {
            log.e(t, "%s error", tag);
        }
        if (t instanceof StatusRuntimeException) {
            errorCallback.accept((StatusRuntimeException) t);
        } else {
            errorCallback.accept(io.grpc.Status.INTERNAL
                    .withDescription("Unexpected GRPC error: " + t.getMessage())
                    .withCause(t)
                    .asRuntimeException());
        }
    }
}
