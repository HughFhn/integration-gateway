import React, { useState, useEffect } from "react";

import { PieChart } from '@mui/x-charts/PieChart';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import "./App.css";

type Stats = {
    totalConversions: number;
    successRate: number;

    // Counters for piechart
    HL7Count: number;
    FHIRCount: number;
};

type Log = {
    timestamp: string;
    type: string;
    status: string;
    user: string;
    latency: number;
};

const App = () => {
    const [stats, setStats] = useState<Stats | null>(null);
    const [logs, setLogs] = useState<Log[]>([]);

    useEffect(() => {
        // Fetch initial stats once
        fetch("http://localhost:8081/fhir/stats", {
          headers: {
            Authorization:
              "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMDg1MTcxLCJleHAiOjE3NjAxNzE1NzF9.tNclWHkg_7jP6nUmqWYrUEgzIW2e13NGY6x9JzOUZGQ",
          },
        })
          .then((res) => res.json())
          .then((data) => setStats(data));

        // Live updates via SSE
        const eventSource = new EventSource(
          "http://localhost:8081/fhir/audit/stream"
        );
        // Log incoming messages for debugging
        eventSource.onmessage = (e) => console.log(e.data);
        window.addEventListener("beforeunload", () => eventSource.close());

        eventSource.addEventListener("audit", (event) => {
            try {
                const raw = JSON.parse(event.data);
                const data: Log = {
                    timestamp: raw.Date,
                    type: raw.Type,
                    status: raw.Status,
                    user: raw.User,
                    latency: raw.Latency,
                };
                setLogs((prev) => [data, ...prev.slice(0,1000)]); // Keeps previous 1000 logs
                fetchStats();

                } catch (err) {
                console.error("Failed to parse SSE event:", err);
                }
        });

        eventSource.onerror = (err) => {
            console.error("SSE connection error:", err);
        };
        return () => eventSource.close();
    }, []);

    // Hi this is a test
    // Function to update stats
    const fetchStats = async () => {
        try {
            const res = await fetch("http://localhost:8081/fhir/stats", {
                headers: {
                    Authorization:
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMDgyMTAzLCJleHAiOjE3NjAxNjg1MDN9.bza6cATmFoVkZhoAie0i0WsrzPACEYQpROpoDb1eIG0",
                },
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            setStats(data);
        } catch (error) {
            console.error("Failed to fetch stats:", error);
        }
    };

    // Placeholder while no stats
    if (!stats) return <div>Loading stats...</div>;

    return (
        <div className={"app-container"}>
            <h1 className={"dashboard-title"}>FHIR Gateway Dashboard</h1>

            <div className={"stats-section"}>
                <h2>Stats</h2>
                <p className={"stats-item"}>Total Conversions: {stats.totalConversions}</p>
                <p className={"stats-item"}>Rate of Successful Conversions: {stats.successRate}%</p>
            </div>

            <div className={"chart-section"}>
                <h2>Piechart Conversion Breakdown</h2>
                <PieChart
                    width={350}
                    height={350}
                    series={[
                        {
                            id: "inner-ring",
                            innerRadius: 0,
                            outerRadius: 90,
                            data: [
                                { label: "HL7", value: stats.HL7Count, color: "#A627F5" },
                                { label: "FHIR", value: stats.FHIRCount, color: "#cc5500" },
                            ],
                            arcLabel: "value",
                        },
                        {
                            id: "outer-ring",
                            innerRadius: 100,
                            outerRadius: 130,
                            data: [
                                { label: "Success", value: stats.successRate, color: "#4CAF50" },
                                { label: "Failure", value: Math.round((100 - stats.successRate) * 100)/ 100, color: "#F44336" },
                            ],
                            arcLabel: "value",
                        },
                    ]}
                />
            </div>

            <div className={"logs-section"}>
                <h2>Recent Conversions</h2>
                <TableContainer component={Paper}>
                    <Table className={"logs-table"}>
                        <TableHead>
                            <TableRow>
                                <TableCell>Timestamp</TableCell>
                                <TableCell>Type</TableCell>
                                <TableCell>Status</TableCell>
                                <TableCell>User</TableCell>
                                <TableCell align="right">Latency (ms)</TableCell>
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {logs.map((log, i) => (
                                <TableRow
                                    key={i}
                                    sx={{ "&:last-child td, &:last-child th": { border: 0 } }}
                                >
                                    <TableCell>{log.timestamp}</TableCell>
                                    <TableCell>{log.type}</TableCell>
                                    <TableCell
                                        sx={{
                                            color: log.status === "Success" ? "green" : "red",
                                            fontWeight: 500,
                                        }}
                                    >
                                        {log.status}
                                    </TableCell>
                                    <TableCell>{log.user}</TableCell>
                                    <TableCell align="right">{log.latency}</TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </TableContainer>
            </div>

        </div>
    );
};

export default App;