import React, { useState, useEffect } from "react";

import { PieChart } from "@mui/x-charts/PieChart";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Paper from "@mui/material/Paper";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Box from "@mui/material/Box";
import "./App.css";

type Stats = {
  totalConversions: number;
  successRate: number;

  // Counters for piechart
  HL7Count: number;
  FHIRCount: number;
  REDCapCount: number;
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
  const [message, setMessage] = useState("");
  const [messageType, setMessageType] = useState("redcap-to-fhir");
  const [response, setResponse] = useState("");
  const [token, setToken] = useState<string | null>(null);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const fetchStats = async (authToken: string) => {
    try {
      const res = await fetch("http://localhost:8081/fhir/stats", {
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setStats(data);
    } catch (error) {
      console.error("Failed to fetch stats:", error);
    }
  };

  useEffect(() => {
    if (token) {
      // Fetch initial stats once
      fetchStats(token);

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
          setLogs((prev) => [data, ...prev.slice(0, 1000)]); // Keeps previous 1000 logs
          fetchStats(token);
        } catch (err) {
          console.error("Failed to parse SSE event:", err);
        }
      });

      eventSource.onerror = (err) => {
        console.error("SSE connection error:", err);
      };
      return () => eventSource.close();
    }
  }, [token]);

  const handleLogin = async () => {
    try {
      const res = await fetch("http://localhost:8081/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, password }),
      });

      if (!res.ok) {
        throw new Error("Invalid credentials");
      }

      const data = await res.json();
      setToken(data.token);
      setError(null);
    } catch (err) {
      setError("Login failed. Please check your username and password.");
      console.error(err);
    }
  };

  const handleConvert = async () => {
    let url = "";
    switch (messageType) {
      case "redcap-to-fhir":
        url = "http://localhost:8081/fhir/convert/redcap-to-fhir";
        break;
      case "hl7-to-fhir":
        url = "http://localhost:8081/fhir/convert/hl7-to-fhir";
        break;
      case "fhir-to-hl7":
        url = "http://localhost:8081/fhir/convert/fhir-to-hl7";
        break;
      default:
        return;
    }

    try {
      const res = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: message,
      });

      const data = await res.text();
      setResponse(data);
    } catch (error) {
      console.error("Conversion failed:", error);
      setResponse("Conversion failed.");
    }
  };

  // Login page appears if no token
  if (!token) {
    return (
      <div className="login-container">
        <div className="center">
          <h1>Login</h1>
          <TextField
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            margin="normal"
          />
          <br />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            margin="normal"
          />
          <br />
          <Button onClick={handleLogin} variant="contained" color="primary">
            Login
          </Button>
          {error && <p className="error">{error}</p>}
        </div>
      </div>
    );
  }

  // Placeholder while no stats
  if (!stats) return <div>Loading stats...</div>;

  return (
    <div className={"app-container"}>
      <h1 className={"dashboard-title"}>FHIR Gateway Dashboard</h1>

      <div className={"conversion-section"}>
        <h2>Manual Conversion</h2>
        <FormControl fullWidth margin="normal">
          <InputLabel>Message Type</InputLabel>
          <Select
            value={messageType}
            onChange={(e) => setMessageType(e.target.value)}
            label="Message Type"
          >
            <MenuItem value="redcap-to-fhir">REDCap to FHIR</MenuItem>
            <MenuItem value="hl7-to-fhir">HL7 to FHIR</MenuItem>
            <MenuItem value="fhir-to-hl7">FHIR to HL7</MenuItem>
          </Select>
        </FormControl>
        <TextField
          label="Enter your message here"
          multiline
          rows={10}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          variant="outlined"
          fullWidth
          margin="normal"
        />
        <Button variant="contained" color="primary" onClick={handleConvert}>
          Convert
        </Button>

        {response && (
          <Box mt={2} p={2} border={1} borderColor="grey.400" borderRadius={4}>
            <h3>Conversion Result:</h3>
            <pre style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
              {response}
            </pre>
          </Box>
        )}
      </div>
      <br />
      <div className={"stats-section"}>
        <h2>Stats</h2>
        <p className={"stats-item"}>
          Total Conversions: {stats.totalConversions}
        </p>
        <p className={"stats-item"}>
          Rate of Successful Conversions: {stats.successRate}%
        </p>
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
                { label: "REDCap", value: stats.REDCapCount, color: "#f8ea08" },
              ],
              arcLabel: "value",
            },
            {
              id: "outer-ring",
              innerRadius: 100,
              outerRadius: 130,
              data: [
                {
                  label: "Success",
                  value: stats.successRate,
                  color: "#4CAF50",
                },
                {
                  label: "Failure",
                  value: Math.round((100 - stats.successRate) * 100) / 100,
                  color: "#F44336",
                },
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
