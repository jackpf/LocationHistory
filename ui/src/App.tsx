import './App.css';
import {useAdminClient} from './hooks/use-admin-client.ts';
import {Login} from "./components/Login.tsx";
import {DeviceList} from "./components/DeviceList.tsx";
import {useLogin} from "./hooks/use-login.ts";
import {MLMap} from "./components/MLMap.tsx";

const Dashboard = () => {
    const REFRESH_INTERVAL = 10000;

    const {
        setSelectedDeviceId,
        approveDevice,
        deleteDevice,
        devices,
        selectedDeviceId,
        history,
        lastUpdated,
        error
    } = useAdminClient(REFRESH_INTERVAL);

    const {
        logout
    } = useLogin();

    return (
        <div className="app-container">
            {error && <div className="error-text">{error}</div>}

            {/* Sidebar */}
            <DeviceList devices={devices} selectedDeviceId={selectedDeviceId}
                        setSelectedDeviceId={setSelectedDeviceId}
                        approveDevice={approveDevice}
                        deleteDevice={deleteDevice}
                        logout={logout}/>

            {/* Map Area */}
            {/*<OSMMap*/}
            {/*    history={history}*/}
            {/*    lastUpdated={lastUpdated}*/}
            {/*    selectedDeviceId={selectedDeviceId}*/}
            {/*/>*/}
            <MLMap
                history={history}
                lastUpdated={lastUpdated}
                selectedDeviceId={selectedDeviceId}
            />
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