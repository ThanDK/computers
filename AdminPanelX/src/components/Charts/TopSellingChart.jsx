// src/components/charts/TopSellingChart.jsx
import React from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

const TopSellingChart = ({ data }) => {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart
        layout="vertical"
        data={data}
        margin={{ top: 5, right: 30, left: 20, bottom: 20 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="#4a5a76" />
        <XAxis type="number" stroke="var(--text-secondary)" />
        <YAxis
          type="category"
          dataKey="name"
          stroke="var(--text-secondary)"
          width={150}
          tick={{ fontSize: 12 }}
          interval={0}
        />
        <Tooltip cursor={{ fill: 'rgba(74, 90, 118, 0.5)' }}/>
        <Legend verticalAlign="bottom" height={36}/>
        <Bar dataKey="quantitySold" name="Quantity Sold" fill="#38bdf8" />
      </BarChart>
    </ResponsiveContainer>
  );
};

export default TopSellingChart;