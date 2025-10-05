import React from 'react';
import { Card } from 'react-bootstrap';
import styles from './ProductSpecifications.module.css';

const arrayFormatter = (arr) => arr && arr.length > 0 ? arr.join(', ') : null;

const SPEC_CONFIG = {
    cpu: [
        { key: 'socket', label: 'Socket' },
        { key: 'wattage', label: 'TDP', unit: ' W' },
    ],
    motherboard: [
        { key: 'socket', label: 'Socket' },
        { key: 'form_factor', label: 'Form Factor' },
        { key: 'ram_type', label: 'Memory Type' },
        { key: 'max_ram_gb', label: 'Max Memory', unit: ' GB' },
        { key: 'ram_slot_count', label: 'Memory Slots' },
        { key: 'pcie_x16_slot_count', label: 'PCIe x16 Slots' },
        { key: 'm2_slot_count', label: 'M.2 Slots' },
        { key: 'sata_port_count', label: 'SATA Ports' },
        { key: 'wattage', label: 'Estimated Wattage', unit: ' W' },
    ],
    ram: [
        { key: 'ram_type', label: 'Type' },
        { key: 'ram_size_gb', label: 'Capacity', unit: ' GB' },
        { key: 'moduleCount', label: 'Modules' },
        { key: 'wattage', 'label': 'Estimated Wattage', unit: ' W' },
    ],
    gpu: [
        { key: 'length_mm', label: 'Length', unit: ' mm' },
        { key: 'wattage', label: 'Recommended PSU', unit: ' W' },
    ],
    storage: [
        { key: 'storage_interface', label: 'Interface' },
        { key: 'capacity_gb', label: 'Capacity', unit: ' GB' },
        { key: 'form_factor', label: 'Form Factor' },
    ],
    psu: [
        { key: 'wattage', label: 'Wattage', unit: ' W' },
        { key: 'form_factor', label: 'Form Factor' },
    ],
    case: [
        { key: 'motherboard_form_factor_support', label: 'Motherboard Support', formatter: arrayFormatter },
        { key: 'psu_form_factor_support', label: 'PSU Support', formatter: arrayFormatter },
        { key: 'max_gpu_length_mm', label: 'Max GPU Length', unit: ' mm' },
        { key: 'max_cooler_height_mm', label: 'Max Cooler Height', unit: ' mm' },
        { key: 'supportedRadiatorSizesMm', label: 'Radiator Support (mm)', formatter: arrayFormatter },
        { key: 'bays_2_5_inch', label: '2.5" Bays' },
        { key: 'bays_3_5_inch', label: '3.5" Bays' },
    ],
    cooler: [
        { key: 'socket_support', label: 'Socket Support', formatter: arrayFormatter },
        { key: 'radiatorSize_mm', label: 'Radiator Size', unit: ' mm' },
        { key: 'height_mm', label: 'Height (Air)', unit: ' mm' },
        { key: 'wattage', label: 'Estimated Wattage', unit: ' W' },
    ],
};

const SpecItem = ({ label, value, unit = '' }) => {
    if (value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
        return null;
    }
    
    const displayValue = Array.isArray(value) ? value.join(', ') : String(value);

    return (
        <div className={styles.specRow}>
            <strong className={styles.specLabel}>{label}</strong>
            <span className={styles.specValue}>{displayValue}{unit}</span>
        </div>
    );
};

const ProductSpecifications = ({ product }) => {
    if (!product || !product.type) {
        return null;
    }

    const specConfigForType = SPEC_CONFIG[product.type.toLowerCase()] || [];

    const hasVisibleSpecs = specConfigForType.some(spec => {
        const value = product[spec.key];
        if (typeof value === 'number' && value === 0) {
            if (['height_mm', 'radiatorSize_mm', 'length_mm'].includes(spec.key)) {
                return false;
            }
        }
        return !(value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0));
    });

    if (!hasVisibleSpecs) {
        return null;
    }

    return (
        <Card className={styles.specCard}>
            <Card.Header as="h5">Specifications</Card.Header>
            <Card.Body className={styles.specBody}>
                <SpecItem label="Brand" value={product.brandName} />
                <SpecItem label="MPN" value={product.mpn} />

                {specConfigForType.map(spec => {
                    const value = product[spec.key];
                    const formattedValue = spec.formatter ? spec.formatter(value) : value;
                    
                    return (
                        <SpecItem 
                            key={spec.key} 
                            label={spec.label} 
                            value={formattedValue} 
                            unit={spec.unit} 
                        />
                    );
                })}
            </Card.Body>
        </Card>
    );
};

export default ProductSpecifications;