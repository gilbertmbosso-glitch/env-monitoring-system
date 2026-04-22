import React, { useEffect, useState } from "react";
import axios from "axios";
import {
    Area,
    AreaChart,
    CartesianGrid,
    Line,
    LineChart,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";
import "./App.css";

const BASE_URL = process.env.REACT_APP_API_BASE_URL || "http://localhost:8082/api/data";
const CITY_OPTIONS = [
    { value: "beijing", label: "Beijing" },
    { value: "amsterdam", label: "Amsterdam" },
    { value: "newyork", label: "New York" },
];

function App() {
    const [city, setCity] = useState("beijing");
    const [latest, setLatest] = useState(null);
    const [history, setHistory] = useState([]);
    const [forecast, setForecast] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        let mounted = true;

        const fetchDashboard = async () => {
            try {
                const [latestRes, historyRes, forecastRes] = await Promise.all([
                    axios.get(`${BASE_URL}/latest?city=${city}`),
                    axios.get(`${BASE_URL}/history?city=${city}`),
                    axios.get(`${BASE_URL}/forecast?city=${city}&horizon=6`),
                ]);

                if (!mounted) {
                    return;
                }

                setLatest(latestRes.data);
                setHistory(historyRes.data);
                setForecast(forecastRes.data);
                setError("");
            } catch (fetchError) {
                if (mounted) {
                    setError("Unable to load monitoring data.");
                }
                console.error("Dashboard load failed", fetchError);
            }
        };

        fetchDashboard();
        const interval = setInterval(fetchDashboard, 8000);

        return () => {
            mounted = false;
            clearInterval(interval);
        };
    }, [city]);

    const historyChartData = history.slice(-12).map((item) => ({
        time: formatTime(item.timestamp),
        pollution: Number(item.pollution.toFixed(2)),
        temperature: Number(item.temperature.toFixed(2)),
    }));

    const forecastChartData = (forecast?.forecast ?? []).map((item) => ({
        time: formatTime(item.timestamp),
        forecast: Number(item.value.toFixed(2)),
    }));

    const selectedCity = CITY_OPTIONS.find((option) => option.value === city)?.label ?? city;
    const warningLevel = forecast?.warningLevel?.toLowerCase() ?? "low";

    return (
        <div className="dashboard-shell">
            <div className="dashboard-backdrop" />
            <main className="dashboard">
                <section className="hero">
                    <div>
                        <p className="eyebrow">IoT Environmental Intelligence</p>
                        <h1>{selectedCity} Monitoring and Pollution Forecasting</h1>
                        <p className="hero-copy">
                            Real-time collection, historical storage, AI-based trend forecasting,
                            and early-warning support in one dashboard.
                        </p>
                    </div>

                    <div className="hero-controls">
                        <label htmlFor="city">City</label>
                        <select id="city" value={city} onChange={(event) => setCity(event.target.value)}>
                            {CITY_OPTIONS.map((option) => (
                                <option key={option.value} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </div>
                </section>

                {error ? <div className="error-banner">{error}</div> : null}

                <section className="summary-grid">
                    <MetricCard label="Pollution" value={latest?.pollution} unit="AQI" accent="warm" />
                    <MetricCard label="Temperature" value={latest?.temperature} unit="°C" accent="cool" />
                    <MetricCard label="Humidity" value={latest?.humidity} unit="%" accent="neutral" />
                    <MetricCard
                        label="Forecast Peak"
                        value={forecast?.peakForecast}
                        unit="AQI"
                        accent={warningLevel}
                    />
                </section>

                <section className={`warning-panel warning-${warningLevel}`}>
                    <div>
                        <p className="warning-label">Early Warning</p>
                        <h2>{forecast?.warningLevel ?? "LOW"} Risk</h2>
                    </div>
                    <p>{forecast?.warningMessage ?? "Loading warning assessment..."}</p>
                </section>

                <section className="charts-grid">
                    <article className="panel">
                        <div className="panel-heading">
                            <div>
                                <p className="panel-kicker">Stored history</p>
                                <h3>Pollution trend</h3>
                            </div>
                            <span>{historyChartData.length} recent points</span>
                        </div>
                        <div className="chart-wrap">
                            <ResponsiveContainer width="100%" height="100%">
                                <AreaChart data={historyChartData}>
                                    <defs>
                                        <linearGradient id="pollutionFill" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="#ff7a59" stopOpacity={0.75} />
                                            <stop offset="95%" stopColor="#ff7a59" stopOpacity={0.05} />
                                        </linearGradient>
                                    </defs>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
                                    <XAxis dataKey="time" stroke="#8ea7a3" />
                                    <YAxis stroke="#8ea7a3" />
                                    <Tooltip />
                                    <Area
                                        type="monotone"
                                        dataKey="pollution"
                                        stroke="#ff7a59"
                                        fill="url(#pollutionFill)"
                                        strokeWidth={3}
                                    />
                                </AreaChart>
                            </ResponsiveContainer>
                        </div>
                    </article>

                    <article className="panel">
                        <div className="panel-heading">
                            <div>
                                <p className="panel-kicker">AI horizon</p>
                                <h3>Next 6-hour forecast</h3>
                            </div>
                            <span>{forecast?.averageForecast?.toFixed(2) ?? "--"} avg AQI</span>
                        </div>
                        <div className="chart-wrap">
                            <ResponsiveContainer width="100%" height="100%">
                                <LineChart data={forecastChartData}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
                                    <XAxis dataKey="time" stroke="#8ea7a3" />
                                    <YAxis stroke="#8ea7a3" />
                                    <Tooltip />
                                    <Line
                                        type="monotone"
                                        dataKey="forecast"
                                        stroke="#7ce0b8"
                                        strokeWidth={3}
                                        dot={{ r: 4, strokeWidth: 0, fill: "#d9fff1" }}
                                    />
                                </LineChart>
                            </ResponsiveContainer>
                        </div>
                    </article>
                </section>

                <section className="footer-strip">
                    <div className="mini-stat">
                        <span>Last captured</span>
                        <strong>{latest?.timestamp ? formatDateTime(latest.timestamp) : "Loading..."}</strong>
                    </div>
                    <div className="mini-stat">
                        <span>Forecast source points</span>
                        <strong>{forecast?.sourceSeries?.length ?? 0}</strong>
                    </div>
                    <div className="mini-stat">
                        <span>Predicted next value</span>
                        <strong>
                            {forecast?.forecast?.[0]?.value !== undefined
                                ? `${forecast.forecast[0].value.toFixed(2)} AQI`
                                : "Loading..."}
                        </strong>
                    </div>
                </section>
            </main>
        </div>
    );
}

function MetricCard({ label, value, unit, accent }) {
    return (
        <article className={`metric-card accent-${accent}`}>
            <p>{label}</p>
            <h2>{value !== null && value !== undefined ? value.toFixed(2) : "--"}</h2>
            <span>{unit}</span>
        </article>
    );
}

function formatTime(timestamp) {
    if (!timestamp) {
        return "--";
    }
    return new Date(timestamp).toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
    });
}

function formatDateTime(timestamp) {
    return new Date(timestamp).toLocaleString();
}

export default App;
