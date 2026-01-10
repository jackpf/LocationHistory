export const sendUnifiedPushNotification = async (pushUrl: string, message: string) => {
    const response = await fetch(pushUrl, {
        method: 'POST',
        body: message,
        headers: { 'Priority': 'high' }
    });

    if (!response.ok) throw new Error('Failed to send message: ' + response);
    return true;
};

export const UNIFIED_PUSH_HANDLER = "UnifiedPush";
export const TRIGGER_BEACON_MESSAGE = "TRIGGER_BEACON";
export const TRIGGER_ALARM_MESSAGE = "TRIGGER_ALARM";