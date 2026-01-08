export const sendNtfyPushNotification = async (ntfyUrl: string) => {
    const response = await fetch(ntfyUrl, {
        method: 'POST',
        body: 'TRIGGER_BEACON',
        headers: { 'Priority': 'high' }
    });

    if (!response.ok) throw new Error('Failed to send ping: ' + response);
    return true;
};