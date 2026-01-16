import React, {useState} from "react";

interface LoginProps {
    onLogin: (password: string) => void;
    setError: (error: string) => void;
    error: string | null;
}

export const Login: React.FC<LoginProps> = ({onLogin, setError, error}) => {
    const [password, setPassword] = useState("");

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!password.trim()) {
            setError("Password cannot be empty");
            return;
        }
        // Pass the password up to the parent
        onLogin(password);
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <h2 style={styles.title}>Location History</h2>
                <p style={styles.subtitle}>
                    Please enter admin password to continue <br/>
                    <i>(this is the password configured for your location-history-server container)</i>
                </p>

                <form onSubmit={handleSubmit} style={styles.form}>
                    <input
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) => {
                            setPassword(e.target.value);
                            setError("");
                        }}
                        style={styles.input}
                        autoFocus
                    />
                    {error && <p style={styles.error}>{error}</p>}
                    <button type="submit" style={styles.button}>
                        Enter
                    </button>
                </form>
            </div>
        </div>
    );
};

// Simple inline styles (works instantly without Tailwind/Bootstrap)
const styles: { [key: string]: React.CSSProperties } = {
    container: {
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        height: "100vh",
        backgroundColor: "#f3f4f6",
        fontFamily: "system-ui, sans-serif",
    },
    card: {
        background: "white",
        padding: "2rem",
        borderRadius: "12px",
        boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
        width: "100%",
        maxWidth: "400px",
        textAlign: "center",
    },
    title: {margin: "0 0 0.5rem 0", color: "#1f2937"},
    subtitle: {margin: "0 0 1.5rem 0", color: "#6b7280", fontSize: "0.9rem"},
    form: {display: "flex", flexDirection: "column", gap: "1rem"},
    input: {
        padding: "0.75rem",
        borderRadius: "6px",
        border: "1px solid #d1d5db",
        fontSize: "1rem",
    },
    button: {
        padding: "0.75rem",
        borderRadius: "6px",
        border: "none",
        backgroundColor: "#2563eb",
        color: "white",
        fontSize: "1rem",
        fontWeight: "bold",
        cursor: "pointer",
        transition: "background 0.2s",
    },
    error: {color: "#dc2626", fontSize: "0.875rem", margin: 0},
};