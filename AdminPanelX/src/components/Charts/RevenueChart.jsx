import React from 'react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
} from 'recharts';

// สร้างหน้าตาของ Tooltip ที่จะโชว์ตอนเอาเมาส์ไปชี้กราฟ
const CustomTooltip = ({ active, payload, label }) => {
  if (active && payload && payload.length) {
    return (
      <div className="custom-tooltip">
        <p className="label">{`${label}`}</p>
        <p className="intro">{`Revenue : ${payload[0].value.toLocaleString('en-US', { style: 'currency', currency: 'THB' })}`}</p>
      </div>
    );
  }
  return null;
};

// ฟังก์ชันสำหรับ format ตัวเลขแกน Y ให้อ่านง่ายขึ้น เช่น 1000 ให้เป็น 1k
const yAxisFormatter = (value) => {
    if (value >= 1000000) return `${(value / 1000000).toFixed(1)}M`;
    if (value >= 1000) return `${(value / 1000).toFixed(0)}k`;
    return value;
}

const RevenueChart = ({ data }) => {
  return (
    // ทำให้กราฟปรับขนาดตามพื้นที่ได้แบบ responsive
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart
        data={data}
        margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
      >
        {/* ตรงนี้คือการประกาศสี gradient ที่จะเอาไปใช้กับพื้นที่กราฟ */}
        <defs>
          <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#4ade80" stopOpacity={0.8}/>
            <stop offset="95%" stopColor="#4ade80" stopOpacity={0}/>
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="#4a5a76" />
        <XAxis dataKey="date" stroke="var(--text-secondary)" tick={{ fontSize: 12 }} />
        {/* บอกให้แกน Y ใช้ฟังก์ชัน formatter ที่ทำไว้ */}
        <YAxis stroke="var(--text-secondary)" tickFormatter={yAxisFormatter}/>
        {/* บอกให้ Tooltip ใช้หน้าตา custom ที่สร้างไว้ข้างบน */}
        <Tooltip content={<CustomTooltip />} />
        <Legend verticalAlign="bottom" />
        {/* ในส่วนของ Area ที่เป็นพื้นที่ใต้กราฟ ให้ใช้สี gradient จาก id ที่ประกาศไว้ */}
        <Area type="monotone" dataKey="revenue" stroke="#4ade80" fillOpacity={1} fill="url(#colorRevenue)" />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default RevenueChart;