import React from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

const formatYAxisTick = (tick) => {
  const limit = 25; 
  if (tick.length > limit) {
    return `${tick.substring(0, limit)}...`;
  }
  return tick;
};

/**
 * A generalized horizontal bar chart for reports.
 * @param {object} props
 * @param {Array} props.data - The data array for the chart.
 * @param {string} props.yAxisKey - The key in the data object for the Y-axis category labels.
 * @param {string} props.dataKey - The key in the data object for the X-axis bar value.
 * @param {string} props.name - The name to display in the legend and tooltip.
 * @param {string} props.color - The hex color for the chart bars.
 */
const ReportBarChart = ({ data, yAxisKey, dataKey, name, color }) => {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart
        layout="vertical"
        data={data}
        margin={{ top: 5, right: 30, left: 30, bottom: 20 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="#4a5a76" />
        
        <XAxis type="number" stroke="var(--text-secondary)" />
        
        <YAxis
          type="category"
          dataKey={yAxisKey}
          stroke="var(--text-secondary)"
          width={150} 
          tick={{ fontSize: 14 }} 
          tickFormatter={formatYAxisTick}
          interval={0}
        />
        <Tooltip
          cursor={{ fill: 'rgba(74, 90, 118, 0.5)' }}
          contentStyle={{
            backgroundColor: 'var(--secondary-bg)',
            border: '1px solid #4a5a76',
            color: 'var(--text-primary)'
          }}
        />
        
        <Legend 
            verticalAlign="bottom" 
            height={36} 
            wrapperStyle={{ color: 'var(--text-primary)' }} 
        />
        
        <Bar dataKey={dataKey} name={name} fill={color} />
      </BarChart>
    </ResponsiveContainer>
  );
};

export default ReportBarChart;