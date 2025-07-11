import React from 'react';
import { Form, Badge, Button } from 'react-bootstrap';
import { BsX } from 'react-icons/bs';
import './MultiSelectTag.css';

const MultiSelectTag = ({
  label,
  options,
  selectedValues,
  onAdd,
  onRemove
}) => {
  const handleSelectChange = (e) => {
    const valueToAdd = e.target.value;
    if (valueToAdd) {
      onAdd(valueToAdd);
    }
  };

  const availableOptions = options.filter(opt => !selectedValues.includes(opt.value));

  return (
    <div className="multi-select-tag-container mb-3">
      <Form.Label>{label}</Form.Label>
      <Form.Select onChange={handleSelectChange} value="">
        <option value="">-- Add an option --</option>
        {availableOptions.map(opt => (
          <option key={opt.key} value={opt.value}>{opt.label}</option>
        ))}
      </Form.Select>
      <div className="selected-tags-container mt-2">
        {selectedValues.length > 0 ? (
          selectedValues.map(value => (
            <Badge pill bg="primary" key={value} className="tag-badge">
              {value}
              <Button
                variant="link"
                className="tag-remove-btn"
                onClick={() => onRemove(value)}
              >
                <BsX />
              </Button>
            </Badge>
          ))
        ) : (
          <small className="text-muted">No options selected.</small>
        )}
      </div>
    </div>
  );
};

export default MultiSelectTag;