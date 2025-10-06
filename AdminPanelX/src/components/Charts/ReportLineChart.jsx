import React from 'react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

const CustomTooltip = ({ active, payload, label, dataKey, name, isCurrency }) => {
  if (active && payload && payload.length) {
    const value = payload[0].value;
    const displayValue = isCurrency 
        ? value.toLocaleString('en-US', { style: 'currency', currency: 'THB' })
        : value.toLocaleString('en-US');

    return (
      <div className="custom-tooltip">
        <p className="label">{`${label}`}</p>
        <p className="intro">{`${name} : ${displayValue}`}</p>
      </div>
    );
  }
  return null;
};

const yAxisFormatter = (value) => {
    if (value >= 1000000) return `${(value / 1000000).toFixed(1)}M`;
    if (value >= 1000) return `${(value / 1000).toFixed(0)}k`;
    return value;
};

/**
 * A generalized line/area chart for reports.
 * @param {object} props
 * @param {Array} props.data - The data array for the chart.
 * @param {string} props.xAxisKey - The key in the data object for the X-axis.
 * @param {string} props.dataKey - The key in the data object for the Y-axis value.
 * @param {string} props.name - The name to display in the legend and tooltip.
 * @param {string} props.color - The hex color for the chart's stroke and fill.
 * @param {boolean} [props.isCurrency=false] - If true, formats tooltips as THB currency.
 */
const ReportLineChart = ({ data, xAxisKey, dataKey, name, color, isCurrency = false }) => {
  const gradientId = `color-${dataKey}`;

  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart
        data={data}
        margin={{ top: 10, right: 30, left: 0, bottom: 20 }}
      >
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor={color} stopOpacity={0.8}/>
            <stop offset="95%" stopColor={color} stopOpacity={0}/>
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#4a5a76" />
        <XAxis dataKey={xAxisKey} stroke="var(--text-secondary)" tick={{ fontSize: 12 }} />
        <YAxis stroke="var(--text-secondary)" tickFormatter={yAxisFormatter} />
        <Tooltip content={<CustomTooltip name={name} isCurrency={isCurrency} />} />
        <Legend verticalAlign="bottom" />
        <Area type="monotone" dataKey={dataKey} name={name} stroke={color} fillOpacity={1} fill={`url(#${gradientId})`} />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default ReportLineChart;