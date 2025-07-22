import React, { useState, useEffect, useMemo } from 'react';
import { Card, Button, Modal, Form, Spinner, Image } from 'react-bootstrap';
import { useAuth } from '../../../../context/AuthContext';
// --- 1. IMPORT THE NEW SERVICE FUNCTION ---
import { approveSlip, shipOrder, approveRefund, rejectRefund, fetchValidNextStatuses, updateOrderStatus, updateShippingDetails, rejectSlip, revertSlipApproval, forceRefundByAdmin } from '../../../../services/OrderService';
import { fetchAllShippingProviders } from '../../../../services/LookupService';
import { handlePromise } from '../../../../services/NotificationService';
import ConfirmationModal from '../../../../components/ConfirmationModal/ConfirmationModal';
import ReasonModal from '../../../../components/ReasonModal/ReasonModal';
// --- 2. IMPORT THE NEW ICON ---
import { BsTruck, BsPencilSquare, BsCheckCircle, BsXCircle, BsArrowRepeat, BsShieldX, BsBackspaceReverseFill, BsInfoCircleFill, BsCurrencyExchange } from 'react-icons/bs';
import './OrderActions.css';

function OrderActions({ order, onActionSuccess }) {
    const { token } = useAuth();
    
    const [showShippingModal, setShowShippingModal] = useState(false);
    const [shippingModalMode, setShippingModalMode] = useState('create');
    const [shippingInfo, setShippingInfo] = useState({ shippingProvider: '', trackingNumber: '' });
    
    const [showStatusModal, setShowStatusModal] = useState(false);
    const [confirmState, setConfirmState] = useState({ show: false, title: '', body: '', onConfirm: null, confirmVariant: 'primary', confirmText: 'Confirm' });
    const [reasonModalState, setReasonModalState] = useState({ show: false, title: '', onSubmit: null });

    const [nextStatuses, setNextStatuses] = useState([]);
    const [isFetchingStatuses, setIsFetchingStatuses] = useState(false);
    const [selectedStatus, setSelectedStatus] = useState('');
    const [shippingProviders, setShippingProviders] = useState([]);
    const [isLoadingProviders, setIsLoadingProviders] = useState(false);

    useEffect(() => {
        if (order?.id) {
            setIsFetchingStatuses(true);
            setSelectedStatus('');
            fetchValidNextStatuses(order.id, token)
                .then(setNextStatuses)
                .catch(console.error)
                .finally(() => setIsFetchingStatuses(false));
        }
    }, [order?.id, order?.orderStatus, token]);

    useEffect(() => {
        setIsLoadingProviders(true);
        fetchAllShippingProviders(token)
            .then(setShippingProviders)
            .catch(console.error)
            .finally(() => setIsLoadingProviders(false));
    }, [token]);

    const selectedProviderImage = useMemo(() => {
        if (!shippingInfo.shippingProvider || shippingProviders.length === 0) {
            return null;
        }
        const provider = shippingProviders.find(p => p.name === shippingInfo.shippingProvider);
        return provider?.imageUrl || null;
    }, [shippingInfo.shippingProvider, shippingProviders]);

    const handleAction = async (actionPromise, successMessage) => {
        setConfirmState(p => ({ ...p, show: false }));
        setReasonModalState(p => ({ ...p, show: false }));
        setShowShippingModal(false);
        setShowStatusModal(false);
        try {
            await handlePromise(actionPromise, { loading: 'Processing...', success: successMessage, error: (err) => err.message });
            onActionSuccess();
        } catch (err) {}
    };

    const confirmApproveSlip = () => setConfirmState({ show: true, title: 'Approve Payment Slip?', body: 'This will approve the payment, mark the order as PROCESSING, and deduct stock. Are you sure?', onConfirm: () => handleAction(approveSlip(order.id, token), 'Payment slip approved!'), confirmVariant: 'success', confirmText: 'Yes, Approve' });
    const confirmApproveRefund = () => setConfirmState({ show: true, title: 'Approve Refund Request?', body: 'This will refund the customer and increment stock. This action cannot be undone. Are you sure?', onConfirm: () => handleAction(approveRefund(order.id, token), 'Refund request has been approved!'), confirmVariant: 'success', confirmText: 'Yes, Approve Refund' });
    const confirmRejectRefund = () => setConfirmState({ show: true, title: 'Reject Refund Request?', body: 'This will mark the refund request as rejected. The user will be notified. Are you sure?', onConfirm: () => handleAction(rejectRefund(order.id, token), 'Refund request has been rejected.'), confirmVariant: 'danger', confirmText: 'Yes, Reject' });
    const openRejectSlipModal = () => setReasonModalState({ show: true, title: 'Reject Payment Slip', label: 'Reason for Rejection', placeholder: 'e.g., Incorrect amount, Blurry image...', onSubmit: (reason) => handleAction(rejectSlip(order.id, reason, token), 'Payment slip rejected.') });
    const openRevertApprovalModal = () => setReasonModalState({ show: true, title: 'Revert Slip Approval', label: 'Reason for Reversion', placeholder: 'e.g., Approved by mistake, Customer request...', onSubmit: (reason) => handleAction(revertSlipApproval(order.id, reason, token), 'Approval reverted and stock returned.') });

   
    const confirmForceRefund = () => setConfirmState({ 
        show: true, 
        title: 'Force Refund This Order?', 
        body: 'This will immediately process a refund for the customer and return stock. This is for admin-initiated refunds (e.g., due to product defects). Are you sure?', 
        onConfirm: () => handleAction(forceRefundByAdmin(order.id, token), 'Order has been forcibly refunded!'), 
        confirmVariant: 'danger', 
        confirmText: 'Yes, Force Refund' 
    });

    const handleOpenCreateShipModal = () => {
        const defaultProvider = shippingProviders.length > 0 ? shippingProviders[0].name : '';
        setShippingInfo({ shippingProvider: defaultProvider, trackingNumber: '' });
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

    const handleShipmentSubmit = (e) => {
        e.preventDefault();
        const actionPromise = shippingModalMode === 'create'
            ? shipOrder(order.id, shippingInfo, token)
            : updateShippingDetails(order.id, shippingInfo, token);
        const successMessage = shippingModalMode === 'create' ? 'Order marked as shipped!' : 'Shipping details updated!';
        handleAction(actionPromise, successMessage);
    };
    
    const handleStatusChangeSubmit = (e) => {
        e.preventDefault();
        if (!selectedStatus) return;
        handleAction(updateOrderStatus(order.id, selectedStatus, token), `Order status updated to ${selectedStatus}!`);
    };

    // --- 4. ADD A VARIABLE TO CHECK IF THE BUTTON SHOULD BE SHOWN ---
    const canBeForciblyRefunded = [
        'PROCESSING', 'SHIPPED', 'COMPLETED', 
        'DELIVERY_FAILED', 'RETURNED_TO_SENDER', 'REFUND_REJECTED'
    ].includes(order.orderStatus);

    return (
        <>
            <Card className="detail-card">
                <Card.Header>Actions</Card.Header>
                <Card.Body className="d-grid gap-2">
                    {order.paymentStatus === 'PENDING_APPROVAL' && (
                        <>
                            <Button variant="success" onClick={confirmApproveSlip} className="d-flex align-items-center justify-content-center gap-2"><BsCheckCircle /> Approve Payment Slip</Button>
                            <Button variant="danger" onClick={openRejectSlipModal} className="d-flex align-items-center justify-content-center gap-2"><BsShieldX /> Reject Payment Slip</Button>
                        </>
                    )}
                    {order.orderStatus === 'PROCESSING' && order.paymentDetails?.paymentMethod === 'BANK_TRANSFER' && (
                        <Button variant="outline-warning" onClick={openRevertApprovalModal} className="d-flex align-items-center justify-content-center gap-2"><BsBackspaceReverseFill /> Revert Slip Approval</Button>
                    )}
                    {(order.orderStatus === 'PROCESSING' || order.orderStatus === 'RETURNED_TO_SENDER') && (
                         <Button variant="primary" onClick={handleOpenCreateShipModal} className="d-flex align-items-center justify-content-center gap-2"><BsTruck /> Ship Order</Button>
                    )}
                    {order.orderStatus === 'SHIPPED' && (
                        <Button variant="info" onClick={handleOpenUpdateShipModal} className="d-flex align-items-center justify-content-center gap-2"><BsPencilSquare /> Update Shipping Details</Button>
                    )}
                    {order.orderStatus === 'REFUND_REQUESTED' && (
                        <>
                            <Button variant="success" onClick={confirmApproveRefund} className="d-flex align-items-center justify-content-center gap-2"><BsCheckCircle /> Approve Refund Request</Button>
                            <Button variant="danger" onClick={confirmRejectRefund} className="d-flex align-items-center justify-content-center gap-2"><BsXCircle /> Reject Refund Request</Button>
                        </>
                    )}
                    
                    {canBeForciblyRefunded && (
                        <Button variant="outline-danger" onClick={confirmForceRefund} className="d-flex align-items-center justify-content-center gap-2">
                            <BsCurrencyExchange /> Force Refund
                        </Button>
                    )}
                    
                    {/* The info box for paid orders */}
                    {['PROCESSING', 'SHIPPED'].includes(order.orderStatus) && order.orderStatus !== 'REFUND_REQUESTED' && (
                         <div className="action-info-box">
                            <BsInfoCircleFill className="info-icon" />
                            <span>
                                To cancel a paid order, it must be refunded. The customer can request this, or an admin can initiate it using 'Force Refund'.
                            </span>
                         </div>
                    )}
                    
                    <hr className="action-divider" />

                    <Button variant="outline-secondary" onClick={() => setShowStatusModal(true)} disabled={isFetchingStatuses || nextStatuses.length === 0} className="d-flex align-items-center justify-content-center gap-2">
                        {isFetchingStatuses ? <Spinner as="span" animation="border" size="sm" /> : <><BsArrowRepeat /> Change Status</>}
                    </Button>
                </Card.Body>
            </Card>

            <ConfirmationModal
                show={confirmState.show}
                onHide={() => setConfirmState({ ...confirmState, show: false })}
                onConfirm={confirmState.onConfirm}
                title={confirmState.title}
                confirmText={confirmState.confirmText}
                confirmVariant={confirmState.confirmVariant}
            >
                <p>{confirmState.body}</p>
            </ConfirmationModal>

            <ReasonModal
                show={reasonModalState.show}
                onHide={() => setReasonModalState({ ...reasonModalState, show: false })}
                onSubmit={reasonModalState.onSubmit}
                title={reasonModalState.title}
                label={reasonModalState.label}
                placeholder={reasonModalState.placeholder}
            />

            <Modal show={showShippingModal} onHide={() => setShowShippingModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>{shippingModalMode === 'create' ? 'Enter Shipping Details' : 'Update Shipping Details'}</Modal.Title>
                </Modal.Header>
                <Form onSubmit={handleShipmentSubmit}>
                    <Modal.Body>
                        {selectedProviderImage && (
                            <div className="text-center mb-3">
                                <Image src={selectedProviderImage} alt={shippingInfo.shippingProvider} fluid style={{ maxHeight: '60px' }} />
                            </div>
                        )}
                        <Form.Group className="mb-3">
                            <Form.Label>Shipping Provider</Form.Label>
                            {isLoadingProviders ? <Spinner size="sm" /> : (
                                <Form.Select
                                    required
                                    value={shippingInfo.shippingProvider}
                                    onChange={e => setShippingInfo({ ...shippingInfo, shippingProvider: e.target.value })}
                                >
                                    <option value="" disabled>-- Select a Provider --</option>
                                    {shippingProviders.map(provider => (
                                        <option key={provider.id} value={provider.name}>
                                            {provider.name}
                                        </option>
                                    ))}
                                </Form.Select>
                            )}
                        </Form.Group>
                        <Form.Group>
                            <Form.Label>Tracking Number</Form.Label>
                            <Form.Control 
                                type="text" 
                                placeholder="Enter tracking number" 
                                required 
                                value={shippingInfo.trackingNumber} 
                                onChange={e => setShippingInfo({ ...shippingInfo, trackingNumber: e.target.value })}
                            />
                        </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={() => setShowShippingModal(false)}>Cancel</Button>
                        <Button variant="primary" type="submit" disabled={!shippingInfo.shippingProvider}>Confirm</Button>
                    </Modal.Footer>
                </Form>
            </Modal>
            
            <Modal show={showStatusModal} onHide={() => setShowStatusModal(false)} centered>
                <Modal.Header closeButton><Modal.Title>Manually Change Order Status</Modal.Title></Modal.Header>
                <Form onSubmit={handleStatusChangeSubmit}>
                    <Modal.Body>
                        <p className="mb-1">Current Status: <strong>{order.orderStatus.replace(/_/g, ' ')}</strong></p>
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