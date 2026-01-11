import {useEffect} from "react";
import type {StoredDevice} from "../gen/common.ts";
import {NotificationType, type SendNotificationResponse} from "../gen/admin-service.ts";

export function usePushPoller(
    storedDevice: StoredDevice | undefined,
    isVisible: boolean,
    refreshInterval: number,
    sendNotification: (deviceId: string, notificationType: NotificationType) => Promise<SendNotificationResponse | undefined>,
){
    // Poll push notification updates if tab is currently active
    // We don't want to endlessly wake the device if we're buried in the users 238 open tabs
    useEffect(() => {
        const poll = () => {
            if (!isVisible) return;
            if (!storedDevice) return;

            const device = storedDevice.device;
            if (!device || !storedDevice.pushHandler) return;

            sendNotification(device.id, NotificationType.REQUEST_BEACON)
                .then(r => {
                    if (r && r.success) console.log(`Sent beacon notification to ${device.id}`);
                });
        }

        poll();

        const interval = setInterval(poll, refreshInterval);
        return () => clearInterval(interval);
    }, [storedDevice, refreshInterval, isVisible]);
}