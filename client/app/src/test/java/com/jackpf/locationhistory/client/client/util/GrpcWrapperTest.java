package com.jackpf.locationhistory.client.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@RunWith(RobolectricTestRunner.class)
public class GrpcWrapperTest {

    @Test
    public void executeWrapped_successfulCall_returnsResult() throws IOException {
        String expected = "success";

        String result = GrpcWrapper.executeWrapped(
                () -> expected,
                "failed",
                e -> fail("Should not call failure callback")
        );

        assertEquals(expected, result);
    }

    @Test
    public void executeWrapped_statusRuntimeException_wrapsInIOException() {
        StatusRuntimeException grpcError = Status.UNAVAILABLE
                .withDescription("server down")
                .asRuntimeException();

        try {
            GrpcWrapper.executeWrapped(
                    () -> {
                        throw grpcError;
                    },
                    "Connection failed",
                    e -> {
                    }
            );
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Connection failed", e.getMessage());
            assertSame(grpcError, e.getCause());
        }
    }

    @Test
    public void executeWrapped_statusRuntimeException_callsFailureCallback() {
        StatusRuntimeException grpcError = Status.UNAVAILABLE
                .withDescription("server down")
                .asRuntimeException();
        AtomicReference<StatusRuntimeException> captured = new AtomicReference<>();

        try {
            GrpcWrapper.executeWrapped(
                    () -> {
                        throw grpcError;
                    },
                    "Connection failed",
                    captured::set
            );
            fail("Expected IOException");
        } catch (IOException e) {
            assertSame(grpcError, captured.get());
        }
    }

    @Test
    public void executeWrapped_executionException_wrapsInIOException() {
        RuntimeException cause = new RuntimeException("inner error");
        ExecutionException execError = new ExecutionException("exec failed", cause);

        try {
            GrpcWrapper.executeWrapped(
                    () -> {
                        throw execError;
                    },
                    "Operation failed",
                    e -> {
                    }
            );
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Operation failed", e.getMessage());
            assertSame(execError, e.getCause());
        }
    }

    @Test
    public void executeWrapped_executionExceptionWithGrpcCause_callsFailureCallback() {
        StatusRuntimeException grpcError = Status.INTERNAL
                .withDescription("internal error")
                .asRuntimeException();
        ExecutionException execError = new ExecutionException("exec failed", grpcError);
        AtomicReference<StatusRuntimeException> captured = new AtomicReference<>();

        try {
            GrpcWrapper.executeWrapped(
                    () -> {
                        throw execError;
                    },
                    "Operation failed",
                    captured::set
            );
            fail("Expected IOException");
        } catch (IOException e) {
            assertSame(grpcError, captured.get());
        }
    }

    @Test
    public void executeWrapped_interruptedException_wrapsInIOException() {
        InterruptedException interruptedError = new InterruptedException("interrupted");

        try {
            GrpcWrapper.executeWrapped(
                    () -> {
                        throw interruptedError;
                    },
                    "Operation interrupted",
                    e -> {
                    }
            );
            fail("Expected IOException");
        } catch (IOException e) {
            assertEquals("Operation interrupted", e.getMessage());
            assertSame(interruptedError, e.getCause());
        }
    }

    @Test
    public void executeWrapped_nullReturn_returnsNull() throws IOException {
        String result = GrpcWrapper.executeWrapped(
                () -> null,
                "failed",
                e -> fail("Should not call failure callback")
        );

        assertEquals(null, result);
    }
}
