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
    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();


    public BeaconClient(
            ManagedChannel channel,
            DynamicTrustManager dynamicTrustManager,
            long timeoutMillis) {
        beaconService = BeaconServiceGrpc
                .newFutureStub(channel);
        this.channel = channel;
        this.dynamicTrustManager = dynamicTrustManager;
        this.timeoutMillis = timeoutMillis;
    }

    private BeaconServiceGrpc.BeaconServiceFutureStub createStub() {
        return beaconService
                .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public ListenableFuture<PingResponse> ping(GrpcFutureWrapper<PingResponse> callback) {
        callback.setTag("Ping");

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

    public ListenableFuture<RegisterDeviceResponse> registerDevice(String deviceId, String deviceName, String publicKey, GrpcFutureWrapper<RegisterDeviceResponse> callback) {
        callback.setTag("Register device");

        RegisterDeviceRequest request = Requests.registerDeviceRequest(deviceId, deviceName, publicKey);
        ListenableFuture<RegisterDeviceResponse> future = createStub().registerDevice(request);
        Futures.addCallback(future, callback, threadExecutor);

        return future;
    }

    public ListenableFuture<SetLocationResponse> sendLocation(String deviceId, String publicKey, BeaconRequest beaconRequest, GrpcFutureWrapper<SetLocationResponse> callback) {
        callback.setTag("Set location");

        SetLocationRequest request = Requests.setLocationRequest(
                deviceId,
                publicKey,
                beaconRequest.getLat(),
                beaconRequest.getLon(),
                (double) beaconRequest.getAccuracy(),
                beaconRequest.getTimestamp()
        );
        ListenableFuture<SetLocationResponse> future = createStub().setLocation(request);
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
