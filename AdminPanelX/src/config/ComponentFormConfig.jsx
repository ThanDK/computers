import React from 'react';
import { Form, Row, Col } from 'react-bootstrap';
import MultiSelectTag from '../components/MultiSelectTag/MultiSelectTag';

export const renderField = (name, label, { type = "text", md = 6, value, onChange, required = true }) => (
    <Form.Group as={Col} md={md} className="mb-3">
        <Form.Label>{label}</Form.Label>
        <Form.Control type={type} name={name} value={value || ''} onChange={onChange} required={required} />
    </Form.Group>
);

export const renderSelect = (name, label, { options, md = 6, value, onChange, required = true }) => (
    <Form.Group as={Col} md={md} className="mb-3">
        <Form.Label>{label}</Form.Label>
        <Form.Select name={name} value={value || ''} onChange={onChange} required={required}>
            <option value="">-- Select --</option>
            {(options || []).map(opt => (
                <option key={opt.id} value={opt.name}>{opt.name}</option>
            ))}
        </Form.Select>
    </Form.Group>
);

export const renderBrandSelect = ({ formData, lookups, onChange }) => (
    <Form.Group as={Col} md={6} className="mb-3">
        <Form.Label>Brand</Form.Label>
        <Form.Select
            name="brandId"
            value={formData.brandId || ''}
            onChange={onChange}
            disabled={!lookups?.brands}
            required
        >
            <option value="">-- Select a Brand --</option>
            {(lookups?.brands || []).map(brand => (
                <option key={brand.id} value={brand.id}>
                    {brand.name}
                </option>
            ))}
        </Form.Select>
    </Form.Group>
);

const mapToTagOptions = (items = [], keyField = 'id', valueField = 'name') =>
    items.map(i => ({ key: i[keyField], value: i[valueField], label: i[valueField] }));

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
        fields: [
            { name: 'wattage', label: 'Wattage', type: 'number', required: true },
            { name: 'socket', label: 'Socket', type: 'select', required: true }
        ],
        render: ({ formData, lookups, handleChange }) => (
            <Row>
                {renderField("wattage", "Wattage", { type: "number", value: formData.wattage, onChange: handleChange })}
                {renderSelect("socket", "Socket", { options: lookups.sockets, value: formData.socket, onChange: handleChange })}
            </Row>
        )
    },
    motherboard: {
        initialState: { socket: "", ram_type: "", form_factor: "", max_ram_gb: "", pcie_x16_slot_count: "", ram_slot_count: "", sata_port_count: "", m2_slot_count: "", wattage: "" },
        fields: [
            { name: 'socket', label: 'Socket', type: 'select', required: true },
            { name: 'ram_type', label: 'RAM Type', type: 'select', required: true },
            { name: 'form_factor', label: 'Form Factor', type: 'select', required: true },
            { name: 'wattage', label: 'Wattage', type: 'number', required: true },
            { name: 'max_ram_gb', label: 'Max RAM (GB)', type: 'number', required: true },
            { name: 'ram_slot_count', label: 'RAM Slots', type: 'number', required: true },
            { name: 'pcie_x16_slot_count', label: 'PCIe x16 Slots', type: 'number', required: true },
            { name: 'sata_port_count', label: 'SATA Ports', type: 'number', required: true },
            { name: 'm2_slot_count', label: 'M.2 Slots', type: 'number', required: true }
        ],
        render: ({ formData, lookups, handleChange }) => (
            <>
                <Row>
                    {renderSelect("socket", "Socket", { options: lookups.sockets, md: 4, value: formData.socket, onChange: handleChange })}
                    {renderSelect("ram_type", "RAM Type", { options: lookups.ramTypes, md: 4, value: formData.ram_type, onChange: handleChange })}
                    {renderSelect("form_factor", "Form Factor", { options: lookups.formFactors.MOTHERBOARD, md: 4, value: formData.form_factor, onChange: handleChange })}
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
        fields: [
            { name: 'ram_type', label: 'RAM Type', type: 'select', required: true },
            { name: 'ram_size_gb', label: 'Total Size (GB)', type: 'number', required: true },
            { name: 'moduleCount', label: 'Module Count (Sticks)', type: 'number', required: true },
            { name: 'wattage', label: 'Wattage', type: 'number', required: true }
        ],
        render: ({ formData, lookups, handleChange }) => (
            <Row>
                {renderSelect("ram_type", "RAM Type", { options: lookups.ramTypes, md: 4, value: formData.ram_type, onChange: handleChange })}
                {renderField("ram_size_gb", "Total Size (GB)", { type: "number", md: 4, value: formData.ram_size_gb, onChange: handleChange })}
                {renderField("moduleCount", "Module Count (Sticks)", { type: "number", md: 4, value: formData.moduleCount, onChange: handleChange })}
                {renderField("wattage", "Wattage", { type: "number", md: 12, value: formData.wattage, onChange: handleChange })}
            </Row>
        )
    },
    gpu: {
        initialState: { wattage: "", length_mm: "" },
        fields: [
            { name: 'wattage', label: 'Wattage (TDP)', type: 'number', required: true },
            { name: 'length_mm', label: 'Length (mm)', type: 'number', required: true }
        ],
        render: ({ formData, handleChange }) => (
            <Row>
                {renderField("wattage", "Wattage (TDP)", { type: "number", value: formData.wattage, onChange: handleChange })}
                {renderField("length_mm", "Length (mm)", { type: "number", value: formData.length_mm, onChange: handleChange })}
            </Row>
        )
    },
    psu: {
        initialState: { wattage: "", form_factor: "" },
        fields: [
            { name: 'wattage', label: 'Wattage', type: 'number', required: true },
            { name: 'form_factor', label: 'Form Factor', type: 'select', required: true }
        ],
        render: ({ formData, lookups, handleChange }) => (
            <Row>
                {renderField("wattage", "Wattage", { type: "number", value: formData.wattage, onChange: handleChange })}
                {renderSelect("form_factor", "Form Factor", { options: lookups.formFactors.PSU, value: formData.form_factor, onChange: handleChange })}
            </Row>
        )
    },
    storage: {
        initialState: { storage_interface: "", capacity_gb: "", form_factor: "" },
        fields: [
            { name: 'storage_interface', label: 'Interface', type: 'select', required: true },
            { name: 'form_factor', label: 'Form Factor', type: 'select', required: true },
            { name: 'capacity_gb', label: 'Capacity (GB)', type: 'number', required: true }
        ],
        render: ({ formData, lookups, handleChange }) => (
            <Row>
                {renderSelect("storage_interface", "Interface", { options: lookups.storageInterfaces, md: 4, value: formData.storage_interface, onChange: handleChange })}
                {renderSelect("form_factor", "Form Factor", { options: lookups.formFactors.STORAGE, md: 4, value: formData.form_factor, onChange: handleChange })}
                {renderField("capacity_gb", "Capacity (GB)", { type: "number", md: 4, value: formData.capacity_gb, onChange: handleChange })}
            </Row>
        )
    },
    cooler: {
        initialState: { socket_support: [], height_mm: "", wattage: "", radiatorSize_mm: "" },
        fields: [
            { name: 'socket_support', label: 'Supported Sockets', type: 'tags', required: true },
            { name: 'wattage', label: 'Wattage (TDP)', type: 'number', required: true },
            { name: 'height_mm', label: 'Height (mm)', type: 'number', required: false },
            { name: 'radiatorSize_mm', label: 'Radiator Size (mm)', type: 'number', required: false }
        ],
        render: ({ formData, lookups, handleChange, handleTagAdd, handleTagRemove }) => (
             <>
                <Row className="mb-3">
                    <Col>
                        <MultiSelectTag 
                            label="Supported Sockets" 
                            options={mapToTagOptions(lookups.sockets)} 
                            selectedValues={formData.socket_support || []} 
                            onAdd={(v) => handleTagAdd('socket_support', v)} 
                            onRemove={(v) => handleTagRemove('socket_support', v)}
                        />
                    </Col>
                </Row>
                <Row>
                    {renderField("height_mm", "Height (mm) for Air Coolers (0 for AIO)", { type: "number", md: 4, value: formData.height_mm, onChange: handleChange, required: false })}
                    {renderField("radiatorSize_mm", "Radiator Size (mm) for AIOs (0 for Air)", { type: "number", md: 4, value: formData.radiatorSize_mm, onChange: handleChange, required: false })}
                    {renderField("wattage", "Wattage (TDP)", { type: "number", md: 4, value: formData.wattage, onChange: handleChange })}
                </Row>
            </>
        )
    },
    case: {
        initialState: { motherboard_form_factor_support: [], psu_form_factor_support: [], max_gpu_length_mm: "", max_cooler_height_mm: "", bays_2_5_inch: "", bays_3_5_inch: "", supportedRadiatorSizesMm: [] },
        fields: [
            { name: 'motherboard_form_factor_support', label: 'Supported MB Form Factors', type: 'tags', required: true },
            { name: 'psu_form_factor_support', label: 'Supported PSU Form Factors', type: 'tags', required: true },
            { name: 'max_gpu_length_mm', label: 'Max GPU Length (mm)', type: 'number', required: true },
            { name: 'max_cooler_height_mm', label: 'Max Cooler Height (mm)', type: 'number', required: true },
            { name: 'bays_2_5_inch', label: '2.5 Inch Bays', type: 'number', required: true },
            { name: 'bays_3_5_inch', label: '3.5 Inch Bays', type: 'number', required: true },
            { name: 'supportedRadiatorSizesMm', label: 'Supported Radiator Sizes (mm)', type: 'tags', required: false }
        ],
        render: ({ formData, lookups, handleChange, handleTagAdd, handleTagRemove }) => {
            const radiatorOptions = (lookups.radiatorSizes || []).map(s => ({ key: s, value: String(s), label: `${s}mm` }));
            return (
                <>
                    <Row className="mb-3">
                        <Col md={6}>
                            <MultiSelectTag 
                                label="Supported MB Form Factors" 
                                options={mapToTagOptions(lookups.formFactors.MOTHERBOARD)} 
                                selectedValues={formData.motherboard_form_factor_support || []} 
                                onAdd={(v) => handleTagAdd('motherboard_form_factor_support', v)} 
                                onRemove={(v) => handleTagRemove('motherboard_form_factor_support', v)} 
                            />
                        </Col>
                        <Col md={6}>
                            <MultiSelectTag 
                                label="Supported PSU Form Factors" 
                                options={mapToTagOptions(lookups.formFactors.PSU)} 
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
                                selectedValues={(formData.supportedRadiatorSizesMm || []).map(s => String(s))} 
                                onAdd={(v) => handleTagAdd('supportedRadiatorSizesMm', parseInt(v, 10))} 
                                onRemove={(v) => handleTagRemove('supportedRadiatorSizesMm', parseInt(v, 10))}
                            />
                        </Col>
                    </Row>
                </>
            );
        }
    }
};