/*
 * Copyright (c) Squaredesk GmbH and Oliver Dotzauer.
 *
 * This program is distributed under the squaredesk open source license. See the LICENSE file
 * distributed with this work for additional information regarding copyright ownership. You may also
 * obtain a copy of the license at
 *
 *   https://squaredesk.ch/license/oss/LICENSE
 */
package ch.squaredesk.nova.comm.websockets;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class WebSocket {
    private static final Logger logger = LoggerFactory.getLogger(WebSocket.class);

    private final ConcurrentHashMap<String, Object> userProperties = new ConcurrentHashMap<>(1);
    private final CopyOnWriteArrayList<Consumer<WebSocket>> closeHandlers = new CopyOnWriteArrayList<>();

    private final SendAction sendAction;
    private final Consumer<CloseReason> closeAction;
    private final Supplier<Boolean> isOpenSupplier;
    private final Disposable closeEventSubscription;

    public WebSocket(SendAction sendAction, Consumer<CloseReason> closeAction, Supplier<Boolean> isOpenSupplier,
                     Single<Long> webSocketClosedSingle) {
        this.sendAction = Objects.requireNonNull(sendAction, "sendAction must not be null");
        this.closeAction = Objects.requireNonNull(closeAction, "closeAction must not be null");
        this.isOpenSupplier = Objects.requireNonNull(isOpenSupplier, "isOpenSupplier must not be null");

        this.closeEventSubscription = Objects.requireNonNull(webSocketClosedSingle, "webSocketClosedSingle must not be null")
                .subscribe(timestamp -> closeHandlers.forEach(handler -> {
                    try {
                        handler.accept(WebSocket.this);
                    } catch (Exception e) {
                        logger.error("An error occurred, trying to notify handler about close event", e);
                    }
                }));
        }

    public final <T> void send(T message) throws Exception {
        sendAction.accept(message);
    }

    public void close() {
        close(CloseReason.NORMAL_CLOSURE);
    }

    public void close(CloseReason closeReason) {
        if (!closeReason.mightBeUsedByEndpoint) {
            throw new IllegalArgumentException("CloseReason " + closeReason + " cannot be used by endpoints");
        }
        closeEventSubscription.dispose();
        if (closeAction != null) {
            closeAction.accept(closeReason);
        }
    }

    public void clearUserProperties() {
        userProperties.clear();
    }

    public void setUserProperty(String propertyId, Object value) {
        if (value == null) {
            userProperties.remove(propertyId);
        } else {
            userProperties.put(propertyId, value);
        }
    }

    public String getUserProperty(String propertyId) {
        return getUserProperty(propertyId, String.class);
    }

    public <PropertyType> PropertyType getUserProperty(String propertyId, Class<PropertyType> propertyType) {
        return (PropertyType)userProperties.get(propertyId);
    }

    public boolean isOpen() {
        return isOpenSupplier.get();
    }

    public void onClose(Consumer<WebSocket> handler) {
        if (handler != null) {
            closeHandlers.add(handler);
        }
    }

}
