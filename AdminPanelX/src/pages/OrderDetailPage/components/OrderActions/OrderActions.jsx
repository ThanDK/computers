import React, { useState, useMemo } from 'react';
import { Card, Button, Modal, Form, Spinner, Image } from 'react-bootstrap';
import { useAuth } from '../../../../context/AuthContext';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    approveSlip, shipOrder, approveRefund, rejectRefund, fetchValidNextStatuses,
    updateOrderStatus, updateShippingDetails, rejectSlip, revertSlipApproval,
    forceRefundByAdmin
} from '../../../../services/OrderService';
import { fetchAllShippingProviders } from '../../../../services/LookupService';
import { notifySuccess, notifyError } from '../../../../services/NotificationService';
import ConfirmationModal from '../../../../components/ConfirmationModal/ConfirmationModal';
import ReasonModal from '../../../../components/ReasonModal/ReasonModal';
import {
    BsTruck, BsPencilSquare, BsCheckCircle, BsXCircle, BsArrowRepeat,
    BsShieldX, BsBackspaceReverseFill, BsInfoCircleFill, BsCurrencyExchange,
    BsExclamationTriangleFill
} from 'react-icons/bs';
import './OrderActions.css';

function OrderActions({ order }) {
    const { token } = useAuth();
    const queryClient = useQueryClient();

    // --- State Management ---
    const [activeModal, setActiveModal] = useState(null); // State กลางสำหรับ Modal
    const [shippingInfo, setShippingInfo] = useState({ shippingProvider: '', trackingNumber: '' });
    const [selectedStatus, setSelectedStatus] = useState('');

    const openModal = (type, props = {}) => setActiveModal({ type, props });
    const closeModal = () => setActiveModal(null);

    // --- Data Fetching ---
    const { data: nextStatuses = [], isFetching: isFetchingStatuses } = useQuery({
        queryKey: ['nextStatuses', order.id],
        queryFn: () => fetchValidNextStatuses(order.id, token),
        enabled: !!order.id && !!token,
    });

    const { data: shippingProviders = [], isLoading: isLoadingProviders } = useQuery({
        queryKey: ['shippingProviders'],
        queryFn: () => fetchAllShippingProviders(token),
        staleTime: 300000,
    });

    // --- API Mutations ---
    const useActionMutation = (mutationFn, successMessage) => useMutation({
        mutationFn,
        onSuccess: () => {
            notifySuccess(successMessage);
            queryClient.invalidateQueries({ queryKey: ['order', order.id] });
            queryClient.invalidateQueries({ queryKey: ['orders'] });
            queryClient.invalidateQueries({ queryKey: ['nextStatuses', order.id] });
        },
        onError: (err) => notifyError(err.message || 'An unexpected error occurred.'),
    });

    const approveSlipMutation = useActionMutation(() => approveSlip(order.id, token), 'Payment slip approved!');
    const rejectSlipMutation = useActionMutation((reason) => rejectSlip(order.id, reason, token), 'Payment slip rejected.');
    const revertSlipApprovalMutation = useActionMutation((reason) => revertSlipApproval(order.id, reason, token), 'Approval reverted and stock returned.');
    const shipOrderMutation = useActionMutation(() => shipOrder(order.id, shippingInfo, token), 'Order marked as shipped!');
    const updateShippingMutation = useActionMutation(() => updateShippingDetails(order.id, shippingInfo, token), 'Shipping details updated!');
    const approveRefundMutation = useActionMutation(() => approveRefund(order.id, token), 'Refund request has been approved!');
    const rejectRefundMutation = useActionMutation(() => rejectRefund(order.id, token), 'Refund request has been rejected.');
    const forceRefundMutation = useActionMutation(() => forceRefundByAdmin(order.id, token), 'Order has been forcibly refunded!');
    
    const updateStatusMutation = useMutation({
        mutationFn: (newStatus) => updateOrderStatus(order.id, newStatus, token),
        onSuccess: (data, newStatus) => {
            notifySuccess(`Order status updated to ${newStatus.replace(/_/g, ' ')}!`);
            queryClient.invalidateQueries({ queryKey: ['order', order.id] });
            queryClient.invalidateQueries({ queryKey: ['orders'] });
            queryClient.invalidateQueries({ queryKey: ['nextStatuses', order.id] });
            setSelectedStatus('');
        },
        onError: (err) => notifyError(err.message || 'An unexpected error occurred.'),
    });

    // --- Handlers ---
    const handleShipmentSubmit = (e) => {
        e.preventDefault();
        closeModal();
        activeModal.props.mode === 'create' ? shipOrderMutation.mutate() : updateShippingMutation.mutate();
    };

    const handleStatusChangeSubmit = (e) => {
        e.preventDefault();
        closeModal();
        if (selectedStatus) updateStatusMutation.mutate(selectedStatus);
    };

    // --- Memoized Logic ---
    const isAnyActionPending = [
        approveSlipMutation, rejectSlipMutation, revertSlipApprovalMutation,
        shipOrderMutation, updateShippingMutation, approveRefundMutation,
        rejectRefundMutation, forceRefundMutation, updateStatusMutation
    ].some(m => m.isPending);
    
    // สร้างรายการ Action (ปุ่ม) ที่จะแสดงผลตามเงื่อนไข (อ่านง่ายขึ้น)
    const availableActions = useMemo(() => {
        const { orderStatus, paymentStatus, paymentDetails } = order;
        const actions = [];

        if (paymentStatus === 'PENDING_APPROVAL') {
            actions.push({
                key: 'approve-slip', label: 'Approve Payment Slip', Icon: BsCheckCircle, variant: 'success',
                onClick: () => openModal('confirm', { title: 'Approve Payment Slip?', body: 'This will approve the payment, mark as PROCESSING, and deduct stock. Are you sure?', onConfirm: approveSlipMutation.mutate, confirmVariant: 'success', confirmText: 'Yes, Approve' })
            });
            actions.push({
                key: 'reject-slip', label: 'Reject Payment Slip', Icon: BsShieldX, variant: 'danger',
                onClick: () => openModal('reason', { title: 'Reject Payment Slip', label: 'Reason for Rejection', placeholder: 'e.g., Incorrect amount...', onSubmit: rejectSlipMutation.mutate })
            });
        }
        if (orderStatus === 'PROCESSING' && paymentDetails?.paymentMethod === 'BANK_TRANSFER') {
            actions.push({
                key: 'revert-approval', label: 'Revert Slip Approval', Icon: BsBackspaceReverseFill, variant: 'outline-warning',
                onClick: () => openModal('reason', { title: 'Revert Slip Approval', label: 'Reason for Reversion', placeholder: 'e.g., Approved by mistake...', onSubmit: revertSlipApprovalMutation.mutate })
            });
        }
        if (orderStatus === 'PROCESSING' || orderStatus === 'RETURNED_TO_SENDER') {
            actions.push({
                key: 'ship-order', label: 'Ship Order', Icon: BsTruck, variant: 'primary',
                onClick: () => {
                    const defaultProvider = shippingProviders[0]?.name || '';
                    setShippingInfo({ shippingProvider: defaultProvider, trackingNumber: '' });
                    openModal('shipping', { mode: 'create' });
                }
            });
        }
        if (orderStatus === 'SHIPPED') {
            actions.push({
                key: 'update-shipping', label: 'Update Shipping Details', Icon: BsPencilSquare, variant: 'info',
                onClick: () => {
                    setShippingInfo({ shippingProvider: order.shippingDetails?.shippingProvider || '', trackingNumber: order.shippingDetails?.trackingNumber || '' });
                    openModal('shipping', { mode: 'edit' });
                }
            });
        }
        if (orderStatus === 'REFUND_REQUESTED') {
            actions.push({
                key: 'approve-refund', label: 'Approve Refund Request', Icon: BsCheckCircle, variant: 'success',
                onClick: () => openModal('confirm', { title: 'Approve Refund Request?', body: 'This will refund the customer and increment stock. This action cannot be undone. Are you sure?', onConfirm: approveRefundMutation.mutate, confirmVariant: 'success', confirmText: 'Yes, Approve Refund' })
            });
            actions.push({
                key: 'reject-refund', label: 'Reject Refund Request', Icon: BsXCircle, variant: 'danger',
                onClick: () => openModal('confirm', { title: 'Reject Refund Request?', body: 'This will mark the refund request as rejected. The user will be notified. Are you sure?', onConfirm: rejectRefundMutation.mutate, confirmVariant: 'danger', confirmText: 'Yes, Reject' })
            });
        }
        if (['PROCESSING', 'SHIPPED', 'COMPLETED', 'DELIVERY_FAILED', 'RETURNED_TO_SENDER', 'REFUND_REJECTED'].includes(orderStatus)) {
            actions.push({
                key: 'force-refund', label: 'Force Refund', Icon: BsCurrencyExchange, variant: 'outline-danger',
                onClick: () => openModal('confirm', { title: 'Force Refund This Order?', body: 'This will immediately process a refund and return stock. For admin-initiated refunds. Are you sure?', onConfirm: forceRefundMutation.mutate, confirmVariant: 'danger', confirmText: 'Yes, Force Refund' })
            });
        }
        return actions;
    }, [order, shippingProviders, approveSlipMutation, rejectSlipMutation, revertSlipApprovalMutation, approveRefundMutation, rejectRefundMutation, forceRefundMutation]);

    // Logic สำหรับแสดงกล่องข้อความเตือน/ข้อมูล
    const getInfoBox = () => {
        if (order.orderStatus === 'REJECTED_SLIP') return <div className="action-warning-box"><BsExclamationTriangleFill className="warning-icon" /><span>The payment slip was rejected. The customer has been notified.</span></div>;
        if (order.orderStatus === 'REFUND_REJECTED') return <div className="action-warning-box"><BsShieldX className="warning-icon" /><span>Refund request rejected. You may force a refund if needed.</span></div>;
        if (['PROCESSING', 'SHIPPED'].includes(order.orderStatus) && order.orderStatus !== 'REFUND_REQUESTED') return <div className="action-info-box"><BsInfoCircleFill className="info-icon" /><span>To cancel a paid order, it must be refunded via 'Force Refund'.</span></div>;
        return null;
    };

    return (
        <>
            <Card className="detail-card">
                <Card.Header>Actions</Card.Header>
                <Card.Body className="d-grid gap-2">
                    {getInfoBox()}
                    {availableActions.map(({ key, label, Icon, variant, onClick }) => (
                        <Button
                            key={key}
                            variant={variant}
                            onClick={onClick}
                            disabled={isAnyActionPending}
                            className="d-flex align-items-center justify-content-center gap-2"
                        >
                            <Icon /> {label}
                        </Button>
                    ))}
                    <hr className="action-divider" />
                    <Button
                        variant="outline-secondary"
                        onClick={() => openModal('status')}
                        disabled={isAnyActionPending || isFetchingStatuses || nextStatuses.length === 0}
                        className="d-flex align-items-center justify-content-center gap-2"
                    >
                        {isFetchingStatuses ? <Spinner as="span" animation="border" size="sm" /> : <><BsArrowRepeat /> Change Status</>}
                    </Button>
                </Card.Body>
            </Card>

            <ConfirmationModal show={activeModal?.type === 'confirm'} onHide={closeModal} {...activeModal?.props} onConfirm={() => { activeModal.props.onConfirm(); closeModal(); }} />
            <ReasonModal show={activeModal?.type === 'reason'} onHide={closeModal} {...activeModal?.props} onSubmit={(reason) => { activeModal.props.onSubmit(reason); closeModal(); }} />

            <Modal show={activeModal?.type === 'shipping'} onHide={closeModal} centered>
                <Modal.Header closeButton><Modal.Title>{activeModal?.props.mode === 'create' ? 'Enter Shipping Details' : 'Update Shipping Details'}</Modal.Title></Modal.Header>
                <Form onSubmit={handleShipmentSubmit}>
                    <Modal.Body>
                        <Form.Group className="mb-3">
                            <Form.Label>Shipping Provider</Form.Label>
                            {isLoadingProviders ? <Spinner size="sm" /> : (
                                <Form.Select required value={shippingInfo.shippingProvider} onChange={e => setShippingInfo({ ...shippingInfo, shippingProvider: e.target.value })}>
                                    <option value="" disabled>-- Select a Provider --</option>
                                    {shippingProviders.map(p => <option key={p.id} value={p.name}>{p.name}</option>)}
                                </Form.Select>
                            )}
                        </Form.Group>
                        <Form.Group>
                            <Form.Label>Tracking Number</Form.Label>
                            <Form.Control type="text" placeholder="Enter tracking number" required value={shippingInfo.trackingNumber} onChange={e => setShippingInfo({ ...shippingInfo, trackingNumber: e.target.value })} />
                        </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={closeModal}>Cancel</Button>
                        <Button variant="primary" type="submit" disabled={!shippingInfo.shippingProvider || shipOrderMutation.isPending || updateShippingMutation.isPending}>Confirm</Button>
                    </Modal.Footer>
                </Form>
            </Modal>

            <Modal show={activeModal?.type === 'status'} onHide={closeModal} centered>
                <Modal.Header closeButton><Modal.Title>Manually Change Order Status</Modal.Title></Modal.Header>
                <Form onSubmit={handleStatusChangeSubmit}>
                    <Modal.Body>
                        <p className="mb-1">Current Status: <strong>{order.orderStatus.replace(/_/g, ' ')}</strong></p>
                        <Form.Group className="mt-3">
                            <Form.Label>Select New Status</Form.Label>
                            <Form.Select required value={selectedStatus} onChange={e => setSelectedStatus(e.target.value)}>
                                <option value="" disabled>-- Choose a valid next status --</option>
                                {nextStatuses.map(status => <option key={status} value={status}>{status.replace(/_/g, ' ')}</option>)}
                            </Form.Select>
                        </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={closeModal}>Cancel</Button>
                        <Button variant="primary" type="submit" disabled={!selectedStatus || updateStatusMutation.isPending}>Confirm Change</Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        </>
    );
}
export default OrderActions;