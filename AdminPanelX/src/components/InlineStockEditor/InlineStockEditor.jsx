import React, { useState, useEffect, useRef } from 'react';
import { InputGroup, Form, Button } from 'react-bootstrap';
import { BsCheck, BsX } from 'react-icons/bs';

function InlineStockEditor({ initialValue, onSave, onCancel }) {
  const [value, setValue] = useState(initialValue);
  const wrapperRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    const handleClickAway = (e) => {
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
    return () => {
      document.removeEventListener('mousedown', handleClickAway);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [onCancel]);

  const handleSave = () => {
    onSave(value);
  };

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