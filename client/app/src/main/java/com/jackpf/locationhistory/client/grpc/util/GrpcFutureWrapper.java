package com.jackpf.locationhistory.client.grpc.util;

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

    private final Logger log = new Logger(this);

    public GrpcFutureWrapper(Consumer<T> valueCallback, Consumer<StatusRuntimeException> errorCallback) {
        this.valueCallback = valueCallback;
        this.errorCallback = errorCallback;
    }

    @Override
    public void onSuccess(T value) {
        log.d("%s response: %s", tag, value != null ? value.toString() : "null");
        valueCallback.accept(value);
    }

    @Override
    public void onFailure(Throwable t) {
        log.e(t, "%s error", tag);
        if (t instanceof StatusRuntimeException) errorCallback.accept((StatusRuntimeException) t);
        else throw new RuntimeException(t);
    }
}
