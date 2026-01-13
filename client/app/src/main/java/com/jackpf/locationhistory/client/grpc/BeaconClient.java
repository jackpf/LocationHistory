package com.jackpf.locationhistory.client.grpc;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jackpf.locationhistory.BeaconServiceGrpc;
import com.jackpf.locationhistory.CheckDeviceRequest;
import com.jackpf.locationhistory.CheckDeviceResponse;
import com.jackpf.locationhistory.PingRequest;
import com.jackpf.locationhistory.PingResponse;
import com.jackpf.locationhistory.RegisterDeviceRequest;
import com.jackpf.locationhistory.RegisterDeviceResponse;
import com.jackpf.locationhistory.RegisterPushHandlerRequest;
import com.jackpf.locationhistory.RegisterPushHandlerResponse;
import com.jackpf.locationhistory.SetLocationRequest;
import com.jackpf.locationhistory.SetLocationResponse;
import com.jackpf.locationhistory.client.grpc.util.GrpcFutureWrapper;
import com.jackpf.locationhistory.client.ssl.DynamicTrustManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;

public class BeaconClient implements AutoCloseable {
    private final ManagedChannel channel;
    private final DynamicTrustManager dynamicTrustManager;
    private final BeaconServiceGrpc.BeaconServiceFutureStub beaconService;
    private final long timeoutMillis;
    private final boolean waitForReady;
    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    public BeaconClient(
            ManagedChannel channel,
            DynamicTrustManager dynamicTrustManager,
            boolean waitForReady,
            long timeoutMillis) {
        beaconService = BeaconServiceGrpc
                .newFutureStub(channel);
        this.channel = channel;
        this.dynamicTrustManager = dynamicTrustManager;
        this.waitForReady = waitForReady;
        this.timeoutMillis = timeoutMillis;
    }

    private BeaconServiceGrpc.BeaconServiceFutureStub createStub() {
        BeaconServiceGrpc.BeaconServiceFutureStub stub = beaconService
                .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);

        if (waitForReady) return stub.withWaitForReady();
        else return stub;
    }

    public static boolean isPongResponse(PingResponse response) {
        return "pong".equals(response.getMessage());
    }

    public ListenableFuture<PingResponse> ping(GrpcFutureWrapper<PingResponse> callback) {
        callback.setTag("Ping");
        callback.setLoggingEnabled(false); // Disable for pings - it's very spammy

        PingRequest request = Requests.pingRequest();
        ListenableFuture<PingResponse> future = createStub().ping(request);
        Futures.addCallback(future, callback, threadExecutor);

        return future;
    }

    public ListenableFuture<CheckDeviceResponse> checkDevice(String deviceId, GrpcFutureWrapper<CheckDeviceResponse> callback) {
        callback.setTag("Check device");

        CheckDeviceRequest request = Requests.checkDeviceRequest(deviceId);
        ListenableFuture<CheckDeviceResponse> future = createStub().checkDevice(request);
        Futures.addCallback(future, callback, threadExecutor);

        return future;
    }

    public ListenableFuture<RegisterDeviceResponse> registerDevice(String deviceId, String deviceName, GrpcFutureWrapper<RegisterDeviceResponse> callback) {
        callback.setTag("Register device");

        RegisterDeviceRequest request = Requests.registerDeviceRequest(deviceId, deviceName);
        ListenableFuture<RegisterDeviceResponse> future = createStub().registerDevice(request);
        Futures.addCallback(future, callback, threadExecutor);

        return future;
    }

    public ListenableFuture<SetLocationResponse> sendLocation(String deviceId, BeaconRequest beaconRequest, GrpcFutureWrapper<SetLocationResponse> callback) {
        callback.setTag("Set location");

        SetLocationRequest request = Requests.setLocationRequest(
                deviceId,
                beaconRequest.getLat(),
                beaconRequest.getLon(),
                (double) beaconRequest.getAccuracy(),
                beaconRequest.getTimestamp()
        );
        ListenableFuture<SetLocationResponse> future = createStub().setLocation(request);
        Futures.addCallback(future, callback, threadExecutor);

        return future;
    }

    public ListenableFuture<RegisterPushHandlerResponse> registerPushHandler(
            String deviceId,
            String pushHandlerName,
            String pushHandlerUrl,
            GrpcFutureWrapper<RegisterPushHandlerResponse> callback
    ) {
        callback.setTag("Register push handler");

        RegisterPushHandlerRequest request = Requests.registerPushHandler(deviceId, pushHandlerName, pushHandlerUrl);
        ListenableFuture<RegisterPushHandlerResponse> future = createStub().registerPushHandler(request);
        Futures.addCallback(future, callback, threadExecutor);

        return future;
    }

    public ListenableFuture<RegisterPushHandlerResponse> unregisterPushHandler(
            String deviceId,
            GrpcFutureWrapper<RegisterPushHandlerResponse> callback
    ) {
        callback.setTag("Un-register push handler");

        RegisterPushHandlerRequest request = Requests.unregisterPushHandler(deviceId);
        ListenableFuture<RegisterPushHandlerResponse> future = createStub().registerPushHandler(request);
        Futures.addCallback(future, callback, threadExecutor);

        return future;
    }

    public boolean isClosed() {
        return channel.isShutdown() || channel.isTerminated();
    }

    @Override
    public void close() {
        threadExecutor.shutdown();
        channel.shutdown();
        dynamicTrustManager.close();
    }
}
