package com.jackpf.locationhistory.client.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.concurrent.atomic.AtomicReference;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@RunWith(RobolectricTestRunner.class)
public class GrpcAsyncWrapperTest {

    private AtomicReference<String> capturedValue;
    private AtomicReference<StatusRuntimeException> capturedError;
    private GrpcAsyncWrapper<String> wrapper;

    @Before
    public void setUp() {
        capturedValue = new AtomicReference<>();
        capturedError = new AtomicReference<>();
        wrapper = new GrpcAsyncWrapper<>(capturedValue::set, capturedError::set);
        wrapper.setTag("TestCall");
    }

    @Test
    public void onNext_callsValueCallback() {
        wrapper.onNext("test value");

        assertEquals("test value", capturedValue.get());
    }

    @Test
    public void onNext_withNull_callsValueCallbackWithNull() {
        wrapper.onNext(null);

        assertEquals(null, capturedValue.get());
    }

    @Test
    public void onError_withStatusRuntimeException_callsErrorCallback() {
        StatusRuntimeException error = Status.UNAVAILABLE
                .withDescription("server down")
                .asRuntimeException();

        wrapper.onError(error);

        assertSame(error, capturedError.get());
    }

    @Test
    public void onError_withOtherThrowable_wrapsInStatusRuntimeException() {
        RuntimeException error = new RuntimeException("unexpected error");

        wrapper.onError(error);

        assertNotNull(capturedError.get());
        assertEquals(Status.Code.INTERNAL, capturedError.get().getStatus().getCode());
        assertNotNull(capturedError.get().getStatus().getDescription());
        assertSame(error, capturedError.get().getStatus().getCause());
    }

    @Test
    public void onError_withCheckedException_wrapsInStatusRuntimeException() {
        Exception error = new Exception("checked exception");

        wrapper.onError(error);

        assertNotNull(capturedError.get());
        assertEquals(Status.Code.INTERNAL, capturedError.get().getStatus().getCode());
    }

    @Test
    public void onCompleted_doesNothing() {
        // Just verify it doesn't throw
        wrapper.onCompleted();
    }

    @Test
    public void setTag_updatesTag() {
        wrapper.setTag("NewTag");

        // We can't directly verify the tag, but we can verify the wrapper still works
        wrapper.onNext("test");
        assertEquals("test", capturedValue.get());
    }

    @Test
    public void multipleOnNext_callsCallbackEachTime() {
        AtomicReference<Integer> callCount = new AtomicReference<>(0);
        GrpcAsyncWrapper<String> countingWrapper = new GrpcAsyncWrapper<>(
                v -> callCount.updateAndGet(c -> c + 1),
                e -> {}
        );

        countingWrapper.onNext("first");
        countingWrapper.onNext("second");
        countingWrapper.onNext("third");

        assertEquals(Integer.valueOf(3), callCount.get());
    }
}
