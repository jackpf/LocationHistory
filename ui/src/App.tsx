import './App.css';
import {useAdminClient} from './hooks/use-admin-client.ts';
import {Login} from "./components/Login.tsx";
import {MainMap} from "./components/MainMap.tsx";
import {DeviceList} from "./components/DeviceList.tsx";
import {useLogin} from "./hooks/use-login.ts";

const Dashboard = () => {
    const REFRESH_INTERVAL = 10000;

    const {
        setSelectedDeviceId,
        approveDevice,
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
                        logout={logout}/>

            {/* Map Area */}
            <MainMap
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