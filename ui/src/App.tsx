import './App.css';
import {useAdminClient} from './hooks/use-admin-client.ts';
import {Login} from "./components/Login.tsx";
import {DeviceList} from "./components/DeviceList.tsx";
import {useLogin} from "./hooks/use-login.ts";
import {MLMap} from "./components/MLMap.tsx";
import {MAP_TYPE} from "./config/config.ts";
import {OSMMap} from "./components/OSMMap.tsx";
import type {StoredLocation} from "./gen/common.ts";
import {useState} from "react";
import {usePushPoller} from "./hooks/use-push-poller.ts";
import {usePageVisibility} from "./hooks/use-page-visibility.ts";
import {Toaster} from "sonner";
import {TimeRangeSelector} from "./components/TimeRangeSelector.tsx";
import {TimeRangeOption} from "./utils/time-range-options.ts";

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
    const REFRESH_INTERVAL = 10000;

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

    const storedDeviceWithMetadata = devices.find(d => d.storedDevice?.device?.id === selectedDeviceId)
    usePushPoller(storedDeviceWithMetadata?.storedDevice, isVisible, REFRESH_INTERVAL, sendNotification);

    const [forceRecenter, setForceRecenter] = useState<boolean>(false);

    const {
        logout
    } = useLogin();

    const handleRangeChange = (x) => {
        console.log("Fetching data from", x);
        // refreshMapData(start, end);
    };

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

            <div style={{position: 'absolute', right: 200, top: 20, zIndex: 1000}}>
                <TimeRangeSelector onChange={handleRangeChange} value={TimeRangeOption.LAST_7_DAYS}/>
            </div>

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