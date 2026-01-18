package com.jackpf.locationhistory.client.location;

import java.util.function.Consumer;

public interface LocationProvider {
    void provide(String source, int timeout, Consumer<LocationData> consumer);
}
