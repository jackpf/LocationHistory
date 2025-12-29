import {useState} from 'react';
import './App.css';
import {useAdminClient} from './hooks/use-admin-client.ts';
import {Login} from "./components/Login.tsx";
import {MainMap} from "./components/MainMap.tsx";
import {DeviceList} from "./components/DeviceList.tsx";

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

    return (
        <div className="app-container">
            {error && <div className="error-text">{error}</div>}

            {/* Sidebar */}
            <DeviceList devices={devices} selectedDeviceId={selectedDeviceId}
                        setSelectedDeviceId={setSelectedDeviceId}
                        approveDevice={approveDevice}/>

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
    const [token, setToken] = useState<string | null>(() => {
        return localStorage.getItem('auth_token');
    });

    const handleLogin = (password: string) => {
        localStorage.setItem('auth_token', password);
        setToken(password);
    };

    // const handleLogout = () => {
    //     localStorage.removeItem('auth_token');
    //     setToken(null);
    // };

    if (!token) {
        return <Login onLogin={handleLogin}/>;
    }

    return <Dashboard/>;
}

export default App;