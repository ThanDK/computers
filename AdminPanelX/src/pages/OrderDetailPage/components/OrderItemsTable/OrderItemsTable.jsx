import React, { useMemo } from 'react';
import { Table, Card, Image } from 'react-bootstrap';
import './OrderItemsTable.css';

// ฟังก์ชัน helper สำหรับจัดรูปแบบตัวเลขเป็นสกุลเงิน
function formatCurrency(amount, currency) {
    const numberPart = new Intl.NumberFormat('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(amount);
    return `${currency} ${numberPart}`;
}

// Component ย่อยสำหรับ render แถวพิเศษที่แสดงรายการ part ทั้งหมดใน Custom Build
function ContainedItems({ items, currency }) {
    return (
        <tr className="build-contents-row">
            <td colSpan="4" className="p-0">
                <div className="build-contents-wrapper">
                    <h6 className="build-contents-header">CONTAINS:</h6>
                    <div className="build-contents-list">
                        {items.map((part) => (
                            <div key={part.mpn} className="build-part-item">
                                {part.imageUrl && (
                                    <Image src={part.imageUrl} className="build-part-image" alt={part.name} />
                                )}

                                <div className="part-info">
                                    <div className="part-name-line">
                                        <span className="me-2 part-quantity">{part.quantity}x</span>
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

// Component สำหรับ render แถวของสินค้าประเภท 'Custom Build'
// และจะ render Component ContainedItems ต่อท้ายถ้ามีรายการ part อยู่ข้างใน
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

// Component สำหรับ render แถวของสินค้าประเภท 'Component' ทั่วไป
function ComponentItemRow({ item, currency }) {
    return (
        <tr>
            <td className="component-image-cell">
                {item.imageUrl && (
                    <Image src={item.imageUrl} className="component-image" alt={item.name} />
                )}
            </td>
            <td>
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

// Component นี้สร้างขึ้นมาให้เป็นตารางที่ใช้ซ้ำได้
// โดยจะรับ RowComponent เข้ามาเพื่อกำหนดหน้าตาของแต่ละแถว ทำให้ใช้ได้ทั้งกับ Build และ Component
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

function OrderItemsTable({ lineItems = [], currency }) {
    // ใช้ useMemo เพื่อแยก lineItems ออกเป็น 2 กลุ่ม คือ build กับ component
    // ทำแบบนี้เพื่อป้องกันการคำนวณที่ไม่จำเป็นทุกครั้งที่ re-render
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