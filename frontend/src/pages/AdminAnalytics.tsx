import React, { useEffect, useState } from 'react';
import * as api from '../api/apiClient';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';

const AdminAnalytics: React.FC = () => {
    const [data, setData] = useState<any | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const load = async () => {
            setLoading(true);
            try {
                const res = await api.getAdminAnalyticsOverview();
                setData(res);
            } catch (err) {
                console.error('Failed to load analytics', err);
            } finally { setLoading(false); }
        };
        load();
    }, []);

    if (loading) return <div>Loading analytics...</div>;
    if (!data) return <div>No analytics available.</div>;

    const topRooms = (data.topRooms || []).map((r: any) => ({ name: r.name, value: (r.count || 0) }));
    const util = (data.utilization || []).map((r: any) => ({ name: r.name, value: Math.round((r.utilization || 0) * 10000) / 100.0 }));

    const colors = ['#0d6efd', '#6c757d', '#198754', '#dc3545', '#fd7e14'];

    return (
        <div className="container py-4">
            <h2 className="mb-4">Admin Analytics</h2>

            <div className="row g-4">
                <div className="col-12 col-lg-6">
                    <div className="card shadow-sm">
                        <div className="card-body">
                            <h5 className="card-title">Top Booked Rooms</h5>
                            <p className="text-muted small">Booking counts over all time</p>
                            <div style={{ width: '100%', height: 260 }}>
                                <ResponsiveContainer>
                                    <BarChart data={topRooms} layout="vertical">
                                        <XAxis type="number" />
                                        <YAxis dataKey="name" type="category" width={150} />
                                        <Tooltip />
                                        <Bar dataKey="value" fill="#0d6efd">
                                            {topRooms.map((entry: any, index: number) => (
                                                <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
                                            ))}
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-12 col-lg-6">
                    <div className="card shadow-sm">
                        <div className="card-body">
                            <h5 className="card-title">Room Utilization (last 30 days)</h5>
                            <p className="text-muted small">Estimated percent of working hours booked</p>
                            <div style={{ width: '100%', height: 260 }}>
                                <ResponsiveContainer>
                                    <BarChart data={util} layout="vertical">
                                        <XAxis type="number" domain={[0, 100]} unit="%" />
                                        <YAxis dataKey="name" type="category" width={150} />
                                        <Tooltip formatter={(v:number) => v + '%'} />
                                        <Bar dataKey="value">
                                            {util.map((entry: any, index: number) => (
                                                <Cell key={`cellu-${index}`} fill={colors[index % colors.length]} />
                                            ))}
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="row mt-4">
                <div className="col-12">
                    <div className="card shadow-sm">
                        <div className="card-body">
                            <h5 className="card-title">Recommendations</h5>
                            <p className="text-muted small">AI-driven suggestions for resource planning</p>
                            <ul className="list-group list-group-flush">
                                {(data.recommendations || []).map((r: any, i: number) => (
                                    <li key={i} className="list-group-item">
                                        <strong>{r.name}</strong>
                                        <div className="text-muted small">{r.reason}</div>
                                    </li>
                                ))}
                            </ul>
                            <div className="mt-3">
                                <h6>AI Insights</h6>
                                {data.aiInsightsError && <div className="text-danger small">AI insights error: {data.aiInsightsError}</div>}
                                {data.aiInsights && (
                                    <div className="row">
                                        <div className="col-md-6">
                                            <div className="card mb-2">
                                                <div className="card-body">
                                                    <h6 className="card-title">Insights</h6>
                                                    <ul className="list-group list-group-flush">
                                                        {(data.aiInsights.insights || []).map((s: string, i: number) => (
                                                            <li key={i} className="list-group-item small">{s}</li>
                                                        ))}
                                                    </ul>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="col-md-6">
                                            <div className="card mb-2">
                                                <div className="card-body">
                                                    <h6 className="card-title">AI Recommendations</h6>
                                                    <ul className="list-group list-group-flush">
                                                        {(data.aiInsights.recommendations || []).map((r: string, i: number) => (
                                                            <li key={i} className="list-group-item small">{r}</li>
                                                        ))}
                                                    </ul>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                )}
                                {!data.aiInsights && data.aiInsightsRaw && <pre className="small bg-light p-2" style={{whiteSpace: 'pre-wrap'}}>{data.aiInsightsRaw}</pre>}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminAnalytics;
