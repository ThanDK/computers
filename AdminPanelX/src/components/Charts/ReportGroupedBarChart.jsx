import React from 'react';
import { ResponsiveContainer, BarChart, CartesianGrid, XAxis, YAxis, Tooltip, Legend, Bar } from 'recharts';

// Formatter for large numbers on the Y-axis (e.g., 1,000,000 -> 1.0M)
const yAxisTickFormatter = (value) => {
  if (value >= 1000000) {
    return `${(value / 1000000).toFixed(1)}M`;
  }
  if (value >= 1000) {
    return `${(value / 1000).toFixed(0)}k`;
  }
  return value;
};

// Formatter for the tooltip to show currency and commas
const tooltipValueFormatter = (value) => {
  return `฿${value.toLocaleString('en-US')}`;
};

const ReportGroupedBarChart = ({ data, xAxisKey, dataKeys, isCurrency = true }) => {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart
        data={data}
        margin={{
          top: 20,
          right: 30,
          left: 40, // Increased left margin for wider y-axis labels
          bottom: 5,
        }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="#4a5a76" />
        <XAxis dataKey={xAxisKey} stroke="#a1a1aa" />
        <YAxis stroke="#a1a1aa" tickFormatter={yAxisTickFormatter} />
        <Tooltip
          formatter={isCurrency ? tooltipValueFormatter : (value) => value.toLocaleString()}
          cursor={{ fill: 'rgba(136, 132, 216, 0.1)' }}
          // REWORKED: Switched to opaque background and added text color overrides
          contentStyle={{
            backgroundColor: 'var(--secondary-bg)',
            borderColor: '#4a5a76',
            borderRadius: '8px'
          }}
          itemStyle={{
            color: 'var(--text-primary)',
          }}
          labelStyle={{
            color: 'var(--text-secondary)',
            fontWeight: 'bold'
          }}
        />
        <Legend />
        {dataKeys.map(keyInfo => (
          <Bar key={keyInfo.key} dataKey={keyInfo.key} fill={keyInfo.color} name={keyInfo.name} />
        ))}
      </BarChart>
    </ResponsiveContainer>
  );
};

export default ReportGroupedBarChart;