import React from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

// A helper function to truncate long text
const formatYAxisTick = (tick) => {
  const limit = 20; // Character limit
  if (tick.length > limit) {
    return `${tick.substring(0, limit)}...`;
  }
  return tick;
};

const TopSellingChart = ({ data }) => {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart
        layout="vertical"
        data={data}
        // FIX: Adjusted margins to give chart more space
        margin={{ top: 5, right: 30, left: 30, bottom: 20 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="#4a5a76" />
        <XAxis type="number" stroke="var(--text-secondary)" />
        <YAxis
          type="category"
          dataKey="name"
          stroke="var(--text-secondary)"
          // FIX: Reduced width to give bar more space
          width={100} 
          // FIX: Increased font size and added a formatter for long names
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
        <Legend verticalAlign="bottom" height={36}/>
        <Bar dataKey="quantitySold" name="Quantity Sold" fill="#38bdf8" />
      </BarChart>
    </ResponsiveContainer>
  );
};

export default TopSellingChart;