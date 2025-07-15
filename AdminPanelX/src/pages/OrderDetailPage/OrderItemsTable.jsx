import React, { useMemo } from 'react';
import { Table, Card } from 'react-bootstrap';

// The single, correct function to guarantee "THB" format.
const formatCurrency = (amount, currency) => {
    const numberPart = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(amount);
    return `${currency} ${numberPart}`;
};

// The component that was crashing
function OrderItemsTable({ lineItems = [], currency }) { // <-- CRASH FIX
    // By adding "= []", if lineItems is undefined, it becomes an empty array,
    // which prevents the .filter() method from crashing.

    const { buildItems, componentItems } = useMemo(() => {
        const builds = lineItems.filter(item => item.itemType === 'BUILD');
        const components = lineItems.filter(item => item.itemType === 'COMPONENT');
        return { buildItems: builds, componentItems: components };
    }, [lineItems]);

    return (
        <>
            {/* --- Table 1: Custom Builds --- */}
            {buildItems.length > 0 && (
                <Card className="detail-card">
                    <Card.Header>Custom Builds</Card.Header>
                    <Card.Body className="p-0">
                        <Table striped hover responsive variant="dark" className="order-items-table">
                            <thead>
                                <tr>
                                    <th>Build Name</th>
                                    <th className="text-center">Qty</th>
                                    <th className="text-end">Unit Price</th>
                                    <th className="text-end">Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                                {buildItems.map((item, index) => (
                                    <React.Fragment key={`build-${index}`}>
                                        <tr>
                                            <td>
                                                <strong>{item.name}</strong>
                                                <span className="item-meta-info">Build ID: {item.buildId.slice(-8)}</span>
                                            </td>
                                            <td className="text-center">{item.quantity}</td>
                                            <td className="text-end">{formatCurrency(item.unitPrice, currency)}</td>
                                            <td className="text-end">{formatCurrency(item.unitPrice * item.quantity, currency)}</td>
                                        </tr>
                                        {item.containedItems?.length > 0 && (
                                            <tr className="build-contents-row">
                                                <td colSpan="4" className="p-0">
                                                    <div className="build-contents-wrapper">
                                                        <h6 className="build-contents-header">Contains:</h6>
                                                        <ul className="build-contents-list">
                                                            {item.containedItems.map((part, partIndex) => (
                                                                <li key={partIndex}>
                                                                    <span>{part.quantity}x {part.name}</span>
                                                                    <span className="item-meta-info">{part.mpn}</span>
                                                                </li>
                                                            ))}
                                                        </ul>
                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </React.Fragment>
                                ))}
                            </tbody>
                        </Table>
                    </Card.Body>
                </Card>
            )}

            {/* --- Table 2: Individual Components --- */}
            {componentItems.length > 0 && (
                <Card className="detail-card">
                    <Card.Header>Individual Components</Card.Header>
                     <Card.Body className="p-0">
                        <Table striped hover responsive variant="dark" className="order-items-table">
                            <thead>
                                <tr>
                                    <th>Component Name</th>
                                    <th className="text-center">Qty</th>
                                    <th className="text-end">Unit Price</th>
                                    <th className="text-end">Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                                {componentItems.map((item, index) => (
                                    <tr key={`comp-${index}`}>
                                        <td>
                                            <strong>{item.name}</strong>
                                            <span className="item-meta-info">MPN: {item.mpn}</span>
                                        </td>
                                        <td className="text-center">{item.quantity}</td>
                                        <td className="text-end">{formatCurrency(item.unitPrice, currency)}</td>
                                        <td className="text-end">{formatCurrency(item.unitPrice * item.quantity, currency)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </Table>
                    </Card.Body>
                </Card>
            )}
        </>
    );
}

export default OrderItemsTable;