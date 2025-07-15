import React, { useState, useEffect } from 'react';
import { Card, Button, Modal, Form, Spinner } from 'react-bootstrap';
import { useAuth } from '../../context/AuthContext';
import {
    approveSlip,
    shipOrder,
    approveRefund,
    rejectRefund,
    fetchValidNextStatuses,
    updateOrderStatus,
    updateShippingDetails
} from '../../services/OrderService';
import { handlePromise } from '../../services/NotificationService';
import {
    BsTruck,
    BsPencilSquare,
    BsCheckCircle,
    BsXCircle,
    BsArrowRepeat
} from 'react-icons/bs';

function OrderActions({ order, onActionSuccess }) {
    const { token } = useAuth();

    const [showShippingModal, setShowShippingModal] = useState(false);
    const [shippingModalMode, setShippingModalMode] = useState('create');
    const [shippingInfo, setShippingInfo] = useState({ shippingProvider: '', trackingNumber: '' });

    const [showStatusModal, setShowStatusModal] = useState(false);
    const [nextStatuses, setNextStatuses] = useState([]);
    const [isFetchingStatuses, setIsFetchingStatuses] = useState(false);
    const [selectedStatus, setSelectedStatus] = useState('');

    // ***************************************************************
    // ********************* THE ONLY FIX IS HERE **********************
    // ***************************************************************
    useEffect(() => {
        // This check prevents errors on initial load when `order` might be null.
        if (order && order.id) {
            setIsFetchingStatuses(true);
            // Clear out the old selection whenever the status changes
            setSelectedStatus(''); 
            fetchValidNextStatuses(order.id, token)
                .then(setNextStatuses)
                .catch(console.error)
                .finally(() => setIsFetchingStatuses(false));
        }
    // By explicitly depending on `order.orderStatus` and `order.id`, we guarantee
    // this effect re-runs every time the status changes, fixing the stale state bug.
    }, [order?.orderStatus, order?.id, token]);
    // ***************************************************************
    // ********************** END OF THE FIX *************************
    // ***************************************************************

    const handleAction = async (actionPromise, successMessage) => {
        try {
            await handlePromise(actionPromise, {
                loading: 'Processing...',
                success: successMessage,
                error: (err) => err.message || 'An error occurred.'
            });
            onActionSuccess();
        } catch (err) {
            // Error is handled by react-hot-toast's handlePromise
        }
    };

    const handleOpenCreateShipModal = () => {
        setShippingInfo({ shippingProvider: '', trackingNumber: '' });
        setShippingModalMode('create');
        setShowShippingModal(true);
    };

    const handleOpenUpdateShipModal = () => {
        setShippingInfo({
            shippingProvider: order.shippingDetails?.shippingProvider || '',
            trackingNumber: order.shippingDetails?.trackingNumber || ''
        });
        setShippingModalMode('edit');
        setShowShippingModal(true);
    };

    const handleApproveSlip = () => handleAction(approveSlip(order.id, token), 'Payment slip approved!');
    const handleApproveRefund = () => handleAction(approveRefund(order.id, token), 'Refund has been approved!');
    const handleRejectRefund = () => handleAction(rejectRefund(order.id, token), 'Refund has been rejected.');

    const handleShipmentSubmit = (e) => {
        e.preventDefault();
        if (shippingModalMode === 'create') {
            handleAction(shipOrder(order.id, shippingInfo, token), 'Order marked as shipped!');
        } else {
            handleAction(updateShippingDetails(order.id, shippingInfo, token), 'Shipping details updated!');
        }
        setShowShippingModal(false);
    };

    const handleStatusChangeSubmit = (e) => {
        e.preventDefault();
        if (!selectedStatus) return;
        handleAction(updateOrderStatus(order.id, selectedStatus, token), `Order status updated to ${selectedStatus}!`);
        setShowStatusModal(false);
    };

    return (
        <>
            <Card className="detail-card">
                <Card.Header>Actions</Card.Header>
                <Card.Body className="d-grid gap-2">
                    {order.paymentStatus === 'PENDING_APPROVAL' && (
                        <Button variant="success" onClick={handleApproveSlip} className="d-flex align-items-center justify-content-center gap-2">
                            <BsCheckCircle /> Approve Payment Slip
                        </Button>
                    )}

                    {(order.orderStatus === 'PROCESSING' || order.orderStatus === 'RETURNED_TO_SENDER') && (
                         <Button variant="primary" onClick={handleOpenCreateShipModal} className="d-flex align-items-center justify-content-center gap-2">
                            <BsTruck /> Ship Order
                        </Button>
                    )}
                    
                    {order.orderStatus === 'SHIPPED' && (
                        <Button variant="info" onClick={handleOpenUpdateShipModal} className="d-flex align-items-center justify-content-center gap-2">
                            <BsPencilSquare /> Update Shipping
                        </Button>
                    )}

                    {order.orderStatus === 'REFUND_REQUESTED' && (
                        <>
                            <Button variant="success" onClick={handleApproveRefund} className="d-flex align-items-center justify-content-center gap-2">
                                <BsCheckCircle /> Approve Refund
                            </Button>
                            <Button variant="danger" onClick={handleRejectRefund} className="d-flex align-items-center justify-content-center gap-2">
                                <BsXCircle /> Reject Refund
                            </Button>
                        </>
                    )}

                    <Button
                        variant="outline-secondary"
                        onClick={() => setShowStatusModal(true)}
                        disabled={isFetchingStatuses || nextStatuses.length === 0}
                        className="d-flex align-items-center justify-content-center gap-2"
                    >
                        {isFetchingStatuses ? <Spinner as="span" animation="border" size="sm" /> : <><BsArrowRepeat /> Change Status</>}
                    </Button>
                </Card.Body>
            </Card>

            <Modal show={showShippingModal} onHide={() => setShowShippingModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>
                        {shippingModalMode === 'create' ? 'Enter Shipping Details' : 'Update Shipping Details'}
                    </Modal.Title>
                </Modal.Header>
                <Form onSubmit={handleShipmentSubmit}>
                    <Modal.Body>
                        <Form.Group className="mb-3">
                            <Form.Label>Shipping Provider</Form.Label>
                            <Form.Control type="text" placeholder="e.g., Kerry Express" required value={shippingInfo.shippingProvider} onChange={e => setShippingInfo({...shippingInfo, shippingProvider: e.target.value})}/>
                        </Form.Group>
                        <Form.Group>
                            <Form.Label>Tracking Number</Form.Label>
                            <Form.Control type="text" placeholder="Enter tracking number" required value={shippingInfo.trackingNumber} onChange={e => setShippingInfo({...shippingInfo, trackingNumber: e.target.value})}/>
                        </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={() => setShowShippingModal(false)}>Cancel</Button>
                        <Button variant="primary" type="submit">Confirm</Button>
                    </Modal.Footer>
                </Form>
            </Modal>

            <Modal show={showStatusModal} onHide={() => setShowStatusModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>Manually Change Order Status</Modal.Title>
                </Modal.Header>
                <Form onSubmit={handleStatusChangeSubmit}>
                    <Modal.Body>
                        <p className="mb-1">Current Status:</p>
                        <p><strong>{order.orderStatus.replace(/_/g, ' ')}</strong></p>
                        <Form.Group className="mt-3">
                            <Form.Label>Select New Status</Form.Label>
                            <Form.Select required value={selectedStatus} onChange={e => setSelectedStatus(e.target.value)}>
                                <option value="" disabled>-- Choose a valid next status --</option>
                                {nextStatuses.map(status => (<option key={status} value={status}>{status.replace(/_/g, ' ')}</option>))}
                            </Form.Select>
                        </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={() => setShowStatusModal(false)}>Cancel</Button>
                        <Button variant="primary" type="submit" disabled={!selectedStatus}>Confirm Change</Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        </>
    );
}

export default OrderActions;