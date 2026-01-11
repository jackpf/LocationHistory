import './App.css';
import {useAdminClient} from './hooks/use-admin-client.ts';
import {Login} from "./components/Login.tsx";
import {DeviceList} from "./components/DeviceList.tsx";
import {useLogin} from "./hooks/use-login.ts";
import {MLMap} from "./components/MLMap.tsx";
import {MAP_TYPE} from "./config/config.ts";
import {OSMMap} from "./components/OSMMap.tsx";
import type {StoredLocation} from "./gen/common.ts";
import {useMemo, useState} from "react";
import {usePushPoller} from "./hooks/use-push-poller.ts";
import {usePageVisibility} from "./hooks/use-page-visibility.ts";
import {Toaster} from "sonner";

const DisplayMap = ({history, selectedDeviceId, forceRecenter, setForceRecenter}: {
    history: StoredLocation[],
    selectedDeviceId: string | null,
    forceRecenter: boolean,
    setForceRecenter: (forceRecenter: boolean) => void,
}) => {
    switch (MAP_TYPE) {
        case "maptiler":
            return (
                <MLMap
                    history={history}
                    selectedDeviceId={selectedDeviceId}
                    forceRecenter={forceRecenter}
                    setForceRecenter={setForceRecenter}
                />
            )
        case "openstreetmaps":
            return (
                <OSMMap
                    history={history}
                    selectedDeviceId={selectedDeviceId}
                    forceRecenter={forceRecenter}
                    setForceRecenter={setForceRecenter}
                />
            )
        default:
            alert("Invalid map type " + MAP_TYPE + ", must be one of: [maptiler, openstreetmaps]");
            return null;
    }
}

const Dashboard = () => {
    const REFRESH_INTERVAL = 10_000;
    const PUSH_NOTIFICATION_INTERVAL = 60_000;

    const isVisible = usePageVisibility();

    const {
        setSelectedDeviceId,
        approveDevice,
        deleteDevice,
        sendNotification,
        devices,
        selectedDeviceId,
        history,
        error
    } = useAdminClient(REFRESH_INTERVAL);

    const storedDevice = useMemo(() => devices.find(d => d.storedDevice?.device?.id === selectedDeviceId)?.storedDevice, [devices, selectedDeviceId]);

    usePushPoller(
        storedDevice?.device?.id,
        !!storedDevice?.pushHandler,
        isVisible,
        PUSH_NOTIFICATION_INTERVAL,
        sendNotification
    );

    const [forceRecenter, setForceRecenter] = useState<boolean>(false);

    const {
        logout
    } = useLogin();

    return (
        <div className="app-container">
            {error && <div className="error-text">{error}</div>}

            <Toaster
                position="top-center"
                toastOptions={{
                    style: {
                        background: 'rgba(0, 0, 0, 0.8)',
                        color: '#fff',
                        borderRadius: '25px',
                        border: 'none',
                        fontSize: '14px',
                        padding: '10px 20px',
                        textAlign: 'center',
                        minWidth: 'fit-content'
                    },
                }}
            />

            <DeviceList devices={devices}
                        selectedDeviceId={selectedDeviceId}
                        setSelectedDeviceId={setSelectedDeviceId}
                        setForceRecenter={setForceRecenter}
                        approveDevice={approveDevice}
                        deleteDevice={deleteDevice}
                        sendNotification={sendNotification}
                        logout={logout}/>

            <DisplayMap history={history}
                        selectedDeviceId={selectedDeviceId}
                        forceRecenter={forceRecenter}
                        setForceRecenter={setForceRecenter}/>
        </div>
    );
};

function App() {
    const {
        login,
        token,
        setError,
        error
    } = useLogin();

    if (!token) {
        return <Login onLogin={login} setError={setError} error={error}/>;
    }

    return <Dashboard/>;
}

export default App;