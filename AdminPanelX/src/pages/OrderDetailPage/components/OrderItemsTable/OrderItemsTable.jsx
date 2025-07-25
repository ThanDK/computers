import React, { useMemo } from 'react';
import { Table, Card } from 'react-bootstrap';
import './OrderItemsTable.css';

// ... all JSX and logic from the original file ...
const greenDotStyle = {
    display: 'inline-block',
    width: '10px',
    height: '10px',
    backgroundColor: '#387145ff',
    borderRadius: '50%',
};

function formatCurrency(amount, currency) {
    const numberPart = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(amount);
    return `${currency} ${numberPart}`; 
}

function ContainedItems({ items, currency }) {
    return (
        <tr className="build-contents-row">
            <td colSpan="4" className="p-0">
                <div className="build-contents-wrapper">
                    <h6 className="build-contents-header">Contains:</h6>
                    <ul className="build-contents-list">
                        {items.map((part, index) => (
                            <li key={index}>
                                <div className="build-part-item">
                                    <div className="part-info">
                                        <span className="me-2 part-quantity">{part.quantity}x</span>
                                        <span style={greenDotStyle} className="me-2"></span>
                                        <span className="part-name">{part.name}</span>
                                    </div>
                                    <span className="part-price">
                                        {formatCurrency(part.priceAtTimeOfOrder, currency)}
                                    </span>
                                </div>
                                <span className="item-meta-info">MPN: {part.mpn}</span>
                            </li>
                        ))}
                    </ul>
                </div>
            </td>
        </tr>
    );
}

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

function ComponentItemRow({ item, currency }) {
    return (
        <tr>
            <td>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    <span style={greenDotStyle} className="me-2"></span>
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

function ItemCategoryTable({ title, headerName, items, currency, RowComponent }) {
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
            />
            <ItemCategoryTable
                title="Individual Components"
                headerName="Component Name"
                items={componentItems}
                currency={currency}
                RowComponent={ComponentItemRow}
            />
        </>
    );
}

export default OrderItemsTable;