import {useState} from 'react';
import './App.css';
import {useAdminClient} from './hooks/use-admin-client.ts';
import {Login} from "./components/Login.tsx";
import {MainMap} from "./components/MainMap.tsx";
import {DeviceList} from "./components/DeviceList.tsx";

const Dashboard = () => {
    const {
        devices,
        selectedDeviceId,
        setSelectedDeviceId,
        history,
        lastUpdated,
        error
    } = useAdminClient();

    return (
        <div className="app-container">
            {error && <div className="error-text">{error}</div>}

            {/* Sidebar */}
            <DeviceList devices={devices} selectedDeviceId={selectedDeviceId}
                        setSelectedDeviceId={setSelectedDeviceId}/>

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
    /**
     * Currently using session storage to store the admin password
     * This is less secure than in memory, but has the user convenience
     * of not being destroyed on every page refresh
     */
    const [token, setToken] = useState<string | null>(() => {
        return sessionStorage.getItem('auth_token');
    });

    const handleLogin = (password: string) => {
        sessionStorage.setItem('auth_token', password);
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