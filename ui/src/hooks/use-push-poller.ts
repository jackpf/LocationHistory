import {useEffect} from "react";
import {NTFY_HANDLER_NAME, sendNtfyPushNotification} from "../utils/ntfy-client.ts";
import type {StoredDevice} from "../gen/common.ts";

export function usePushPoller(
    storedDevice: StoredDevice | undefined,
    isVisible: boolean,
    refreshInterval: number
){
    // Poll push notification updates if tab is currently active
    // We don't want to endlessly wake the device if we're buried in the users 238 open tabs
    useEffect(() => {
        const poll = () => {
            if (!isVisible) {
                console.log("Page not visible, skipping notification");
                return;
            }

            if (!storedDevice) {
                console.log("No selected device, skipping notification");
                return;
            }

            if (storedDevice && storedDevice.device && storedDevice.pushHandler) {
                const pushHandlerName = storedDevice.pushHandler.name
                const pushHandlerUrl = storedDevice.pushHandler.url

                if (pushHandlerName === NTFY_HANDLER_NAME) {
                    console.log(`Sending notification to ${storedDevice.device.id} on ${pushHandlerUrl}`);
                    sendNtfyPushNotification(pushHandlerUrl)
                        .then(r => {
                            if (r) console.log("Push notification sent")
                            else console.error("Push notification failed")
                        });
                } else {
                    console.error(`Invalid push handler ${pushHandlerName}`)
                }
            }
        }

        poll();

        const interval = setInterval(poll, refreshInterval);
        return () => clearInterval(interval);
    }, [storedDevice, refreshInterval, isVisible]);
}