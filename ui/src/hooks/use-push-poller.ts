import {useEffect} from "react";
import {NotificationType, type SendNotificationResponse} from "../gen/admin-service.ts";

export function usePushPoller(
    deviceId: string | undefined,
    pushEnabled: boolean | undefined,
    isVisible: boolean,
    refreshInterval: number,
    sendNotification: (deviceId: string, notificationType: NotificationType) => Promise<SendNotificationResponse | undefined>,
){
    useEffect(() => {
        const poll = () => {
            // Poll push notification updates if tab is currently active
            // We don't want to endlessly wake the device if we're buried in the users 238 open tabs
            if (!isVisible) return;
            if (!deviceId || !pushEnabled) return;

            sendNotification(deviceId, NotificationType.REQUEST_BEACON)
                .then(r => {
                    if (r && r.success) console.log(`Sent beacon notification to ${deviceId}`);
                });
        }

        poll();

        const interval = setInterval(poll, refreshInterval);
        return () => clearInterval(interval);
    }, [deviceId, pushEnabled, isVisible, refreshInterval, sendNotification]);
}