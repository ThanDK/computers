import React, { useState, useEffect, useRef } from 'react';
import { InputGroup, Form, Button } from 'react-bootstrap';
import { BsCheck, BsX } from 'react-icons/bs';

// Component สำหรับการแก้ไขค่าในบรรทัด (inline)
function InlineStockEditor({ initialValue, onSave, onCancel }) {
  const [value, setValue] = useState(initialValue);
  const wrapperRef = useRef(null);
  const inputRef = useRef(null);

  // ใช้ effect นี้เพื่อดักจับการคลิกนอก component หรือการกดปุ่ม Esc เพื่อยกเลิก
  useEffect(() => {
    const handleClickAway = (e) => {
      // เช็คว่าจุดที่คลิกไม่ได้อยู่ข้างใน div ที่ครอบ component นี้อยู่
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        onCancel();
      }
    };
    const handleEscape = (e) => {
      if (e.key === 'Escape') {
        onCancel();
      }
    };

    document.addEventListener('mousedown', handleClickAway);
    document.addEventListener('keydown', handleEscape);

    // ตอน component หายไป ต้องลบ event listener ออกด้วยเพื่อกัน memory leak
    return () => {
      document.removeEventListener('mousedown', handleClickAway);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [onCancel]);

  const handleSave = () => {
    onSave(value);
  };

  // ทำให้กด Enter เพื่อเซฟได้
  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleSave();
    }
  };

  return (
    <div ref={wrapperRef}>
      <InputGroup size="sm" style={{ width: '120px' }}>
        <Form.Control
          ref={inputRef}
          type="number"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={handleKeyDown}
          // autoFocus จะทำให้ cursor ไปอยู่ที่ input นี้ทันทีที่ component แสดงผล
          autoFocus
        />
        <Button variant="outline-success" onClick={handleSave}>
          <BsCheck />
        </Button>
        <Button variant="outline-light" onClick={onCancel}>
          <BsX />
        </Button>
      </InputGroup>
    </div>
  );
}

export default InlineStockEditor;