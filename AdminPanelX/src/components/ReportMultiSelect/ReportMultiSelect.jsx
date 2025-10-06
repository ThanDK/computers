import React from 'react';
import { Form, Badge, Button } from 'react-bootstrap';
import { BsX } from 'react-icons/bs';
import './ReportMultiSelect.css';

const ReportMultiSelect = ({
  options,
  selectedValues,
  onAdd,
  onRemove
}) => {
  const handleSelectChange = (e) => {
    const valueToAdd = e.target.value;
    if (valueToAdd) {
      onAdd(valueToAdd);
      // Reset the select to the placeholder
      e.target.value = ""; 
    }
  };

  // Filter out already selected options from the dropdown
  const availableOptions = options.filter(opt => !selectedValues.includes(opt.value));

  return (
    <div className="report-multi-select-wrapper">
      <Form.Select onChange={handleSelectChange} value="">
        <option value="">-- Add an option --</option>
        {availableOptions.map(opt => (
          <option key={opt.key} value={opt.value}>{opt.label}</option>
        ))}
      </Form.Select>
      
      <div className="rms-selected-tags-container">
        {selectedValues.length > 0 ? (
          selectedValues.map(value => {
            // Find the original option to display its label
            const option = options.find(opt => opt.value === value);
            return (
                <Badge pill bg="primary" key={value} className="rms-tag-badge">
                <span>{option ? option.label : value}</span>
                <Button
                    variant="link"
                    className="rms-tag-remove-btn"
                    onClick={() => onRemove(value)}
                    aria-label={`Remove ${value}`}
                >
                    <BsX size={16} />
                </Button>
                </Badge>
            );
        })
        ) : (
          <small className="rms-no-options-text">No options selected.</small>
        )}
      </div>
    </div>
  );
};

export default ReportMultiSelect;