import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button, ButtonGroup, Form } from 'react-bootstrap';
import { BsPlusCircle, BsXCircle } from 'react-icons/bs';

import { useAuth } from '../../context/AuthContext';
import { fetchAllOrderStatuses } from '../../services/OrderService';
import { fetchPaymentStatuses, fetchPaymentMethods } from '../../services/ReportService';
import ReportMultiSelect from '../ReportMultiSelect/ReportMultiSelect'; 
import './ReportFilters.css';

const FIELD_CONFIG = {
    orderStatus: { label: 'Order Status', type: 'ENUM' },
    paymentStatus: { label: 'Payment Status', type: 'ENUM' },
    paymentMethod: { label: 'Payment Method', type: 'ENUM' },
    totalAmount: { label: 'Total Amount', type: 'NUMERIC' },
    orderDate: { label: 'Order Date', type: 'DATE' },
};

const OPERATORS = {
    ENUM: [
        { value: 'IN', label: 'is one of' },
        { value: 'NOT_IN', label: 'is not one of' },
    ],
    NUMERIC: [
        { value: 'EQUALS', label: 'is equal to' },
        { value: 'NOT_EQUALS', label: 'is not equal to' },
        { value: 'GREATER_THAN', label: 'is greater than' },
        { value: 'LESS_THAN', label: 'is less than' },
    ],
    DATE: [
        { value: 'AFTER', label: 'is after' },
        { value: 'BEFORE', label: 'is before' },
    ],
};

const getInitialValueForType = (type) => {
    switch (type) {
        case 'ENUM': return [];
        case 'NUMERIC': return '';
        case 'DATE': return '';
        default: return '';
    }
};

const FilterRow = ({ filter, onUpdate, onRemove, lookups, isLoadingLookups }) => {
    const fieldType = FIELD_CONFIG[filter.field]?.type || 'ENUM';
    const availableOperators = OPERATORS[fieldType];

    const handleFieldChange = (newField) => {
        const newFieldType = FIELD_CONFIG[newField].type;
        const newOperator = OPERATORS[newFieldType][0].value;
        const newValue = getInitialValueForType(newFieldType);
        onUpdate(filter.id, { field: newField, operator: newOperator, value: newValue });
    };
    
    const handleOperatorChange = (newOperator) => {
        onUpdate(filter.id, { ...filter, operator: newOperator });
    };

    const handleValueChange = (newValue) => {
        onUpdate(filter.id, { ...filter, value: newValue });
    };

    const getEnumOptions = () => {
        switch (filter.field) {
            case 'orderStatus': return lookups.orderStatuses || [];
            case 'paymentStatus': return lookups.paymentStatuses || [];
            case 'paymentMethod': return lookups.paymentMethods || [];
            default: return [];
        }
    };
    
    return (
        // REWORKED: Changed align-items-center to align-items-start for robust top alignment
        <div className="filter-row-advanced d-flex align-items-start gap-3">
            <Form.Select className="field-select" value={filter.field} onChange={(e) => handleFieldChange(e.target.value)}>
                {Object.entries(FIELD_CONFIG).map(([key, { label }]) => (
                    <option key={key} value={key}>{label}</option>
                ))}
            </Form.Select>

            <Form.Select className="operator-select" value={filter.operator} onChange={(e) => handleOperatorChange(e.target.value)}>
                {availableOperators.map(op => <option key={op.value} value={op.value}>{op.label}</option>)}
            </Form.Select>

            <div className="value-input flex-grow-1">
                {fieldType === 'ENUM' && (
                    <ReportMultiSelect 
                        options={getEnumOptions().map(opt => ({ key: opt, value: opt, label: opt }))}
                        selectedValues={filter.value}
                        onAdd={(val) => handleValueChange([...filter.value, val])}
                        onRemove={(val) => handleValueChange(filter.value.filter(v => v !== val))}
                    />
                )}
                {fieldType === 'NUMERIC' && (
                    <Form.Control type="number" value={filter.value} onChange={(e) => handleValueChange(e.target.value)} placeholder="Enter a number"/>
                )}
                {fieldType === 'DATE' && (
                    <Form.Control type="date" value={filter.value} onChange={(e) => handleValueChange(e.target.value)} />
                )}
            </div>

            <button className="remove-filter-btn" onClick={onRemove} title="Remove Filter">
                <BsXCircle size={20} />
            </button>
        </div>
    );
};

const OrderFilters = ({ filters, onFiltersChange, logic, onLogicChange }) => {
    const { token } = useAuth();

    const { data: orderStatuses, isLoading: isLoadingOrder } = useQuery({ queryKey: ['orderStatuses'], queryFn: () => fetchAllOrderStatuses(token), enabled: !!token });
    const { data: paymentStatuses, isLoading: isLoadingPayment } = useQuery({ queryKey: ['paymentStatuses'], queryFn: () => fetchPaymentStatuses(token), enabled: !!token });
    const { data: paymentMethods, isLoading: isLoadingMethod } = useQuery({ queryKey: ['paymentMethods'], queryFn: () => fetchPaymentMethods(token), enabled: !!token });
    
    const isLoadingLookups = isLoadingOrder || isLoadingPayment || isLoadingMethod;

    const lookups = { orderStatuses, paymentStatuses, paymentMethods };

    const handleAddFilter = () => {
        const defaultField = 'orderStatus';
        const defaultType = FIELD_CONFIG[defaultField].type;
        const newFilter = {
            id: Date.now(),
            field: defaultField,
            operator: OPERATORS[defaultType][0].value,
            value: getInitialValueForType(defaultType),
        };
        onFiltersChange([...filters, newFilter]);
    };

    const handleUpdateFilter = (id, newProps) => {
        onFiltersChange(filters.map(f => (f.id === id ? { ...f, ...newProps } : f)));
    };

    const handleRemoveFilter = (id) => {
        onFiltersChange(filters.filter(f => f.id !== id));
    };

    return (
        <div className="order-filters-container">
            <div className="filter-controls">
                <Button variant="outline-primary" onClick={handleAddFilter} className="d-flex align-items-center gap-2">
                    <BsPlusCircle /> Add Filter
                </Button>
                <ButtonGroup>
                    <Button variant={logic === 'AND' ? 'primary' : 'outline-secondary'} onClick={() => onLogicChange('AND')}>Match All (AND)</Button>
                    <Button variant={logic === 'OR' ? 'primary' : 'outline-secondary'} onClick={() => onLogicChange('OR')}>Match Any (OR)</Button>
                </ButtonGroup>
            </div>

            {filters.length === 0 && <div className="no-filters-message">No active filters. Add one to refine your search.</div>}
            
            <div className="filters-list">
                {filters.map((filter) => (
                    <FilterRow 
                        key={filter.id} 
                        filter={filter}
                        onUpdate={handleUpdateFilter}
                        onRemove={() => handleRemoveFilter(filter.id)}
                        lookups={lookups}
                        isLoadingLookups={isLoadingLookups}
                    />
                ))}
            </div>
        </div>
    );
};

export default OrderFilters;