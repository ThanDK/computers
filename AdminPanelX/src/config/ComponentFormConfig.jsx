// src/config/componentFormConfig.js

import React from 'react';
import { Form, Row, Col } from 'react-bootstrap';
import MultiSelectTag from '../components/MultiSelectTag/MultiSelectTag';

// --- Shared Helper Functions ---

export const renderField = (name, label, { type = "text", md = 6, value, onChange }) => (
    <Form.Group as={Col} md={md} className="mb-3">
        <Form.Label>{label}</Form.Label>
        <Form.Control type={type} name={name} value={value || ''} onChange={onChange} />
    </Form.Group>
);

export const renderSelect = (name, label, { options, keyField, valueField, md = 6, value, onChange }) => (
    <Form.Group as={Col} md={md} className="mb-3">
        <Form.Label>{label}</Form.Label>
        <Form.Select name={name} value={value || ''} onChange={onChange}>
            <option value="">-- Select --</option>
            {options.map(opt => (
                <option key={opt[keyField]} value={opt[valueField]}>{opt[valueField]}</option>
            ))}
        </Form.Select>
    </Form.Group>
);

const mapToTagOptions = (items, keyField, valueField) =>
    items.map(i => ({ key: i[keyField], value: i[valueField], label: i[valueField] }));

// --- Shared Form Configuration ---

export const componentTypes = [
    { label: "CPU", value: "cpu" },
    { label: "Motherboard", value: "motherboard" },
    { label: "RAM Kit", value: "ram" },
    { label: "GPU", value: "gpu" },
    { label: "PSU", value: "psu" },
    { label: "Case", value: "case" },
    { label: "Cooler", value: "cooler" },
    { label: "Storage Drive", value: "storage" }
];

export const COMPONENT_CONFIG = {
    cpu: {
        initialState: { wattage: "", socket: "" },
        render: ({ formData, lookups, handleChange }) => (
            <Row>
                {renderField("wattage", "Wattage", { type: "number", value: formData.wattage, onChange: handleChange })}
                {renderSelect("socket", "Socket", { options: lookups.sockets, keyField: 'id', valueField: 'name', value: formData.socket, onChange: handleChange })}
            </Row>
        )
    },
    motherboard: {
        initialState: { socket: "", ram_type: "", form_factor: "", max_ram_gb: "", pcie_x16_slot_count: "", ram_slot_count: "", sata_port_count: "", m2_slot_count: "", wattage: "" },
        render: ({ formData, lookups, handleChange }) => (
            <>
                <Row>
                    {renderSelect("socket", "Socket", { options: lookups.sockets, keyField: 'id', valueField: 'name', md: 4, value: formData.socket, onChange: handleChange })}
                    {renderSelect("ram_type", "RAM Type", { options: lookups.ramTypes, keyField: 'id', valueField: 'name', md: 4, value: formData.ram_type, onChange: handleChange })}
                    {renderSelect("form_factor", "Form Factor", { options: lookups.formFactors.MOTHERBOARD, keyField: 'id', valueField: 'name', md: 4, value: formData.form_factor, onChange: handleChange })}
                </Row>
                <Row>
                    {renderField("wattage", "Wattage", { type: "number", md: 3, value: formData.wattage, onChange: handleChange })}
                    {renderField("max_ram_gb", "Max RAM (GB)", { type: "number", md: 3, value: formData.max_ram_gb, onChange: handleChange })}
                    {renderField("ram_slot_count", "RAM Slots", { type: "number", md: 3, value: formData.ram_slot_count, onChange: handleChange })}
                    {renderField("pcie_x16_slot_count", "PCIe x16 Slots", { type: "number", md: 3, value: formData.pcie_x16_slot_count, onChange: handleChange })}
                </Row>
                <Row>
                    {renderField("sata_port_count", "SATA Ports", { type: "number", value: formData.sata_port_count, onChange: handleChange })}
                    {renderField("m2_slot_count", "M.2 Slots", { type: "number", value: formData.m2_slot_count, onChange: handleChange })}
                </Row>
            </>
        )
    },
    ram: {
        initialState: { ram_type: "", ram_size_gb: "", moduleCount: "", wattage: "" },
        render: ({ formData, lookups, handleChange }) => (
            <Row>
                {renderSelect("ram_type", "RAM Type", { options: lookups.ramTypes, keyField: 'id', valueField: 'name', md: 4, value: formData.ram_type, onChange: handleChange })}
                {renderField("ram_size_gb", "Total Size (GB)", { type: "number", md: 4, value: formData.ram_size_gb, onChange: handleChange })}
                {renderField("moduleCount", "Module Count (Sticks)", { type: "number", md: 4, value: formData.moduleCount, onChange: handleChange })}
                {renderField("wattage", "Wattage", { type: "number", md: 12, value: formData.wattage, onChange: handleChange })}
            </Row>
        )
    },
    gpu: {
        initialState: { wattage: "", length_mm: "" },
        render: ({ formData, handleChange }) => (
            <Row>
                {renderField("wattage", "Wattage (TDP)", { type: "number", value: formData.wattage, onChange: handleChange })}
                {renderField("length_mm", "Length (mm)", { type: "number", value: formData.length_mm, onChange: handleChange })}
            </Row>
        )
    },
    psu: {
        initialState: { wattage: "", form_factor: "" },
        render: ({ formData, lookups, handleChange }) => (
            <Row>
                {renderField("wattage", "Wattage", { type: "number", value: formData.wattage, onChange: handleChange })}
                {renderSelect("form_factor", "Form Factor", { options: lookups.formFactors.PSU, keyField: 'id', valueField: 'name', value: formData.form_factor, onChange: handleChange })}
            </Row>
        )
    },
    storage: {
        initialState: { storage_interface: "", capacity_gb: "", form_factor: "" },
        render: ({ formData, lookups, handleChange }) => (
            <Row>
                {renderSelect("storage_interface", "Interface", { options: lookups.storageInterfaces, keyField: 'id', valueField: 'name', md: 4, value: formData.storage_interface, onChange: handleChange })}
                {renderSelect("form_factor", "Form Factor", { options: lookups.formFactors.STORAGE, keyField: 'id', valueField: 'name', md: 4, value: formData.form_factor, onChange: handleChange })}
                {renderField("capacity_gb", "Capacity (GB)", { type: "number", md: 4, value: formData.capacity_gb, onChange: handleChange })}
            </Row>
        )
    },
    cooler: {
        initialState: { socket_support: [], height_mm: "", wattage: "", radiatorSize_mm: "0" },
        render: ({ formData, lookups, handleChange, handleTagAdd, handleTagRemove }) => (
             <>
                <Row className="mb-3">
                    <Col>
                        <MultiSelectTag 
                            label="Supported Sockets" 
                            options={mapToTagOptions(lookups.sockets, 'id', 'name')} 
                            selectedValues={formData.socket_support || []} 
                            onAdd={(v) => handleTagAdd('socket_support', v)} 
                            onRemove={(v) => handleTagRemove('socket_support', v)}
                        />
                    </Col>
                </Row>
                <Row>
                    {renderField("height_mm", "Height (mm) for Air Coolers", { type: "number", md: 4, value: formData.height_mm, onChange: handleChange })}
                    {renderField("radiatorSize_mm", "Radiator Size (mm) for AIOs (0 for Air)", { type: "number", md: 4, value: formData.radiatorSize_mm, onChange: handleChange })}
                    {renderField("wattage", "Wattage (TDP)", { type: "number", md: 4, value: formData.wattage, onChange: handleChange })}
                </Row>
            </>
        )
    },
    case: {
        initialState: { motherboard_form_factor_support: [], psu_form_factor_support: [], max_gpu_length_mm: "", max_cooler_height_mm: "", bays_2_5_inch: "", bays_3_5_inch: "", supportedRadiatorSizesMm: [] },
        render: ({ formData, lookups, handleChange, handleTagAdd, handleTagRemove }) => {
            const radiatorOptions = lookups.radiatorSizes.map(s => ({ key: s, value: s, label: `${s}mm` }));
            return (
                <>
                    <Row className="mb-3">
                        <Col md={6}>
                            <MultiSelectTag 
                                label="Supported MB Form Factors" 
                                options={mapToTagOptions(lookups.formFactors.MOTHERBOARD, 'id', 'name')} 
                                selectedValues={formData.motherboard_form_factor_support || []} 
                                onAdd={(v) => handleTagAdd('motherboard_form_factor_support', v)} 
                                onRemove={(v) => handleTagRemove('motherboard_form_factor_support', v)} 
                            />
                        </Col>
                        <Col md={6}>
                            <MultiSelectTag 
                                label="Supported PSU Form Factors" 
                                options={mapToTagOptions(lookups.formFactors.PSU, 'id', 'name')} 
                                selectedValues={formData.psu_form_factor_support || []} 
                                onAdd={(v) => handleTagAdd('psu_form_factor_support', v)} 
                                onRemove={(v) => handleTagRemove('psu_form_factor_support', v)}
                            />
                        </Col>
                    </Row>
                    <Row>
                        {renderField("max_gpu_length_mm", "Max GPU Length (mm)", { type: "number", value: formData.max_gpu_length_mm, onChange: handleChange })}
                        {renderField("max_cooler_height_mm", "Max Cooler Height (mm)", { type: "number", value: formData.max_cooler_height_mm, onChange: handleChange })}
                    </Row>
                    <Row>
                        {renderField("bays_2_5_inch", "2.5 Inch Bays", { type: "number", value: formData.bays_2_5_inch, onChange: handleChange })}
                        {renderField("bays_3_5_inch", "3.5 Inch Bays", { type: "number", value: formData.bays_3_5_inch, onChange: handleChange })}
                    </Row>
                    <Row>
                        <Col>
                            <MultiSelectTag 
                                label="Supported Radiator Sizes (mm)" 
                                options={radiatorOptions} 
                                selectedValues={(formData.supportedRadiatorSizesMm || []).map(s => `${s}`)} 
                                onAdd={(v) => handleTagAdd('supportedRadiatorSizesMm', parseInt(v))} 
                                onRemove={(v) => handleTagRemove('supportedRadiatorSizesMm', parseInt(v))}
                            />
                        </Col>
                    </Row>
                </>
            );
        }
    }
};