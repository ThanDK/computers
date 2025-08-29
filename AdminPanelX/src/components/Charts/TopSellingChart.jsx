import React from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

// ฟังก์ชันสำหรับตัดชื่อสินค้าที่ยาวเกินไปในแกน Y ไม่ให้มันล้นจอ
const formatYAxisTick = (tick) => {
  const limit = 20; 
  if (tick.length > limit) {
    return `${tick.substring(0, limit)}...`;
  }
  return tick;
};

const TopSellingChart = ({ data }) => {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart
        // กำหนดให้กราฟเป็นแนวนอน จากซ้ายไปขวา
        layout="vertical"
        data={data}
        margin={{ top: 5, right: 30, left: 30, bottom: 20 }}
      >
        <CartesianGrid strokeDasharray="3 3" stroke="#4a5a76" />
        
        <XAxis type="number" stroke="var(--text-secondary)" />
        
        <YAxis
          type="category"
          dataKey="name"
          stroke="var(--text-secondary)"
          width={100} 
          tick={{ fontSize: 14 }} 
          tickFormatter={formatYAxisTick}
          // interval={0} คือบังคับให้แสดงชื่อสินค้าทุกอัน 
          interval={0}
        />
        <Tooltip
          // ใส่สี highlight เวลาเอาเมาส์ไปชี้ที่แท่งกราฟ
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
        
        {/* 'name' ที่จะไปโชว์ใน Legend กับ Tooltip */}
        <Bar dataKey="quantitySold" name="Quantity Sold" fill="#38bdf8" />
      </BarChart>
    </ResponsiveContainer>
  );
};

export default TopSellingChart;