package com.jackpf.locationhistory.client.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicReference;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@RunWith(RobolectricTestRunner.class)
public class GrpcFutureWrapperTest {

    private AtomicReference<String> capturedValue;
    private AtomicReference<StatusRuntimeException> capturedError;
    private GrpcFutureWrapper<String> wrapper;

    @Before
    public void setUp() {
        capturedValue = new AtomicReference<>();
        capturedError = new AtomicReference<>();
        wrapper = new GrpcFutureWrapper<>(capturedValue::set, capturedError::set);
        wrapper.setTag("TestCall");
        wrapper.setLoggingEnabled(true);
    }

    @Test
    public void onSuccess_callsValueCallback() {
        wrapper.onSuccess("test value");

        assertEquals("test value", capturedValue.get());
    }

    @Test
    public void onSuccess_withNull_callsValueCallbackWithNull() {
        wrapper.onSuccess(null);

        assertEquals(null, capturedValue.get());
    }

    @Test
    public void onFailure_withStatusRuntimeException_callsErrorCallback() {
        StatusRuntimeException error = Status.UNAVAILABLE
                .withDescription("server down")
                .asRuntimeException();

        wrapper.onFailure(error);

        assertSame(error, capturedError.get());
    }

    @Test
    public void onFailure_withOtherThrowable_wrapsInStatusRuntimeException() {
        RuntimeException error = new RuntimeException("unexpected error");

        wrapper.onFailure(error);

        assertNotNull(capturedError.get());
        assertEquals(Status.Code.INTERNAL, capturedError.get().getStatus().getCode());
        assertNotNull(capturedError.get().getStatus().getDescription());
        assertSame(error, capturedError.get().getStatus().getCause());
    }

    @Test
    public void onFailure_withCheckedException_wrapsInStatusRuntimeException() {
        Exception error = new Exception("checked exception");

        wrapper.onFailure(error);

        assertNotNull(capturedError.get());
        assertEquals(Status.Code.INTERNAL, capturedError.get().getStatus().getCode());
    }

    @Test
    public void empty_createsWrapperThatIgnoresCallbacks() {
        GrpcFutureWrapper<String> emptyWrapper = GrpcFutureWrapper.empty();

        // Should not throw
        emptyWrapper.onSuccess("value");
        emptyWrapper.onFailure(new RuntimeException("error"));
    }

    @Test
    public void setLoggingEnabled_false_stillCallsCallbacks() {
        wrapper.setLoggingEnabled(false);

        wrapper.onSuccess("test value");

        assertEquals("test value", capturedValue.get());
    }

    @Test
    public void setTag_updatesTag() {
        wrapper.setTag("NewTag");

        // We can't directly verify the tag, but we can verify the wrapper still works
        wrapper.onSuccess("test");
        assertEquals("test", capturedValue.get());
    }

    @Test
    public void multipleOnSuccess_callsCallbackEachTime() {
        AtomicReference<Integer> callCount = new AtomicReference<>(0);
        GrpcFutureWrapper<String> countingWrapper = new GrpcFutureWrapper<>(
                v -> callCount.updateAndGet(c -> c + 1),
                e -> {}
        );

        countingWrapper.onSuccess("first");
        countingWrapper.onSuccess("second");
        countingWrapper.onSuccess("third");

        assertEquals(Integer.valueOf(3), callCount.get());
    }

    @Test
    public void onFailure_loggingDisabled_stillCallsCallback() {
        wrapper.setLoggingEnabled(false);
        StatusRuntimeException error = Status.INTERNAL.asRuntimeException();

        wrapper.onFailure(error);

        assertSame(error, capturedError.get());
    }
}
