// src/components/OrderDetails/OrderItemsTable/OrderItemsTable.js

import React, { useMemo } from 'react';
import { Table, Card, Image } from 'react-bootstrap';
import './OrderItemsTable.css';

// This utility function is well-written and remains unchanged.
function formatCurrency(amount, currency) {
    const numberPart = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(amount);
    return `${currency} ${numberPart}`;
}

// REFACTORED: Simplified component structure and improved React key usage.
function ContainedItems({ items, currency }) {
    return (
        <tr className="build-contents-row">
            <td colSpan="4" className="p-0">
                <div className="build-contents-wrapper">
                    <h6 className="build-contents-header">CONTAINS:</h6>
                    {/* SIMPLIFIED: Replaced <ul> and <li> with a simpler div structure. */}
                    <div className="build-contents-list">
                        {items.map((part) => (
                            // IMPROVED: Using a more stable key and removed the `<li>` wrapper.
                            <div key={part.mpn} className="build-part-item">
                                {part.imageUrl && (
                                    <Image src={part.imageUrl} className="build-part-image" alt={part.name} />
                                )}

                                <div className="part-info">
                                    <div className="part-name-line">
                                        <span className="me-2 part-quantity">{part.quantity}x</span>
                                        {/* CLEANED: Using a CSS class for the status dot. */}
                                        <span className="status-dot me-2"></span>
                                        <span className="part-name">{part.name}</span>
                                    </div>
                                    <span className="item-meta-info">MPN: {part.mpn}</span>
                                </div>

                                <span className="part-price">
                                    {formatCurrency(part.priceAtTimeOfOrder, currency)}
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
            </td>
        </tr>
    );
}


// --- The rest of the file with minor cleanups ---
// NO LOGIC CHANGE: This component was already well-structured.
function BuildItemRow({ item, currency }) {
    return (
        <>
            <tr>
                <td>
                    <strong>{item.name}</strong>
                    <span className="item-meta-info">Build ID: {item.buildId?.slice(-8)}</span>
                </td>
                <td className="text-center">{item.quantity}</td>
                <td className="text-end">{formatCurrency(item.unitPrice, currency)}</td>
                <td className="text-end">{formatCurrency(item.unitPrice * item.quantity, currency)}</td>
            </tr>
            {item.containedItems?.length > 0 && <ContainedItems items={item.containedItems} currency={currency} />}
        </>
    );
}

// REFACTORED: Removed inline styles for better separation of concerns.
function ComponentItemRow({ item, currency }) {
    return (
        <tr>
            <td className="component-image-cell">
                {item.imageUrl && (
                    <Image src={item.imageUrl} className="component-image" alt={item.name} />
                )}
            </td>
            <td>
                {/* CLEANED: Replaced inline style with a dedicated CSS class. */}
                <div className="item-name-wrapper">
                    <span className="status-dot me-2"></span>
                    <strong>{item.name}</strong>
                </div>
                <span className="item-meta-info">MPN: {item.mpn}</span>
            </td>
            <td className="text-center">{item.quantity}</td>
            <td className="text-end">{formatCurrency(item.unitPrice, currency)}</td>
            <td className="text-end">{formatCurrency(item.unitPrice * item.quantity, currency)}</td>
        </tr>
    );
}

// NO LOGIC CHANGE: This reusable component is excellent as-is.
function ItemCategoryTable({ title, headerName, items, currency, RowComponent, showImageColumn }) {
    if (!items || items.length === 0) {
        return null;
    }
    return (
        <Card className="detail-card mb-4">
            <Card.Header>{title}</Card.Header>
            <Card.Body className="p-0">
                <Table striped hover responsive variant="dark" className="order-items-table m-0">
                    <thead>
                        <tr>
                            {showImageColumn && <th className="image-header-cell">Image</th>}
                            <th>{headerName}</th>
                            <th className="text-center">Qty</th>
                            <th className="text-end">Unit Price</th>
                            <th className="text-end">Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>
                        {items.map((item, index) => (
                            <RowComponent
                                key={item.buildId || item.mpn || index}
                                item={item}
                                currency={currency}
                            />
                        ))}
                    </tbody>
                </Table>
            </Card.Body>
        </Card>
    );
}

// NO LOGIC CHANGE: `useMemo` is used correctly here.
function OrderItemsTable({ lineItems = [], currency }) {
    const { buildItems, componentItems } = useMemo(() => {
        return lineItems.reduce((acc, item) => {
            if (item.itemType === 'BUILD') {
                acc.buildItems.push(item);
            } else if (item.itemType === 'COMPONENT') {
                acc.componentItems.push(item);
            }
            return acc;
        }, { buildItems: [], componentItems: [] });
    }, [lineItems]);

    return (
        <>
            <ItemCategoryTable
                title="Custom Builds"
                headerName="Build Name"
                items={buildItems}
                currency={currency}
                RowComponent={BuildItemRow}
                showImageColumn={false}
            />
            <ItemCategoryTable
                title="Individual Components"
                headerName="Component Name"
                items={componentItems}
                currency={currency}
                RowComponent={ComponentItemRow}
                showImageColumn={true}
            />
        </>
    );
}

export default OrderItemsTable;