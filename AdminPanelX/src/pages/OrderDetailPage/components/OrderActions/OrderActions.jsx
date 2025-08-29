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

    // State สำหรับจัดการ modal ต่างๆ
    const [showShippingModal, setShowShippingModal] = useState(false);
    const [shippingModalMode, setShippingModalMode] = useState('create');
    const [shippingInfo, setShippingInfo] = useState({ shippingProvider: '', trackingNumber: '' });
    const [showStatusModal, setShowStatusModal] = useState(false);
    const [selectedStatus, setSelectedStatus] = useState('');

    const [confirmState, setConfirmState] = useState({
        show: false,
        title: '',
        body: '',
        onConfirm: null,
        confirmVariant: 'primary',
        confirmText: 'Confirm'
    });

    const [reasonModalState, setReasonModalState] = useState({
        show: false,
        title: '',
        onSubmit: null
    });

    // ดึงสถานะถัดไปที่เป็นไปได้ของออเดอร์
    const { data: nextStatuses = [], isFetching: isFetchingStatuses } = useQuery({
        queryKey: ['nextStatuses', order.id],
        queryFn: () => fetchValidNextStatuses(order.id, token),
        enabled: !!order.id && !!token,
    });

    // ดึงข้อมูลบริษัทขนส่งทั้งหมด (cache 5 นาที)
    const { data: shippingProviders = [], isLoading: isLoadingProviders } = useQuery({
        queryKey: ['shippingProviders'],
        queryFn: () => fetchAllShippingProviders(token),
        enabled: !!token,
        staleTime: 300000,
    });

    // หา URL รูปของบริษัทขนส่งที่เลือกในฟอร์ม
    const selectedProviderImage = useMemo(() => {
        if (!shippingInfo.shippingProvider || shippingProviders.length === 0) {
            return null;
        }
        const provider = shippingProviders.find(p => p.name === shippingInfo.shippingProvider);
        return provider?.imageUrl || null;
    }, [shippingInfo.shippingProvider, shippingProviders]);

    // Helper hook สำหรับสร้าง mutation เพื่อลดการเขียนโค้ดซ้ำ
    // จะจัดการ onSuccess (แจ้งเตือน, refresh data) และ onError ให้เอง
    const useActionMutation = (mutationFn, successMessage) => {
        return useMutation({
            mutationFn,
            onSuccess: () => {
                notifySuccess(successMessage);
                queryClient.invalidateQueries({ queryKey: ['order', order.id] });
                queryClient.invalidateQueries({ queryKey: ['orders'] });
            },
            onError: (err) => {
                notifyError(err.message || 'An unexpected error occurred.');
            }
        });
    };

    const approveSlipMutation = useActionMutation(() => approveSlip(order.id, token), 'Payment slip approved!');
    const rejectSlipMutation = useActionMutation((reason) => rejectSlip(order.id, reason, token), 'Payment slip rejected.');
    const revertSlipApprovalMutation = useActionMutation((reason) => revertSlipApproval(order.id, reason, token), 'Approval reverted and stock returned.');
    const shipOrderMutation = useActionMutation(() => shipOrder(order.id, shippingInfo, token), 'Order marked as shipped!');
    const updateShippingMutation = useActionMutation(() => updateShippingDetails(order.id, shippingInfo, token), 'Shipping details updated!');
    const approveRefundMutation = useActionMutation(() => approveRefund(order.id, token), 'Refund request has been approved!');
    const rejectRefundMutation = useActionMutation(() => rejectRefund(order.id, token), 'Refund request has been rejected.');
    const forceRefundMutation = useActionMutation(() => forceRefundByAdmin(order.id, token), 'Order has been forcibly refunded!');
    const updateStatusMutation = useActionMutation(() => updateOrderStatus(order.id, selectedStatus, token), `Order status updated to ${selectedStatus}!`);

    // กลุ่มฟังก์ชันสำหรับเปิด modal แบบต่างๆ โดยเซ็ต state ของ modal ให้พร้อมใช้งาน
    const confirmApproveSlip = () => setConfirmState({
        show: true,
        title: 'Approve Payment Slip?',
        body: 'This will approve the payment, mark as PROCESSING, and deduct stock. Are you sure?',
        onConfirm: () => approveSlipMutation.mutate(),
        confirmVariant: 'success',
        confirmText: 'Yes, Approve'
    });

    const confirmApproveRefund = () => setConfirmState({
        show: true,
        title: 'Approve Refund Request?',
        body: 'This will refund the customer and increment stock. This action cannot be undone. Are you sure?',
        onConfirm: () => approveRefundMutation.mutate(),
        confirmVariant: 'success',
        confirmText: 'Yes, Approve Refund'
    });

    const confirmRejectRefund = () => setConfirmState({
        show: true,
        title: 'Reject Refund Request?',
        body: 'This will mark the refund request as rejected. The user will be notified. Are you sure?',
        onConfirm: () => rejectRefundMutation.mutate(),
        confirmVariant: 'danger',
        confirmText: 'Yes, Reject'
    });
    
    const confirmForceRefund = () => setConfirmState({
        show: true,
        title: 'Force Refund This Order?',
        body: 'This will immediately process a refund and return stock. For admin-initiated refunds. Are you sure?',
        onConfirm: () => forceRefundMutation.mutate(),
        confirmVariant: 'danger',
        confirmText: 'Yes, Force Refund'
    });
    
    const openRejectSlipModal = () => setReasonModalState({
        show: true,
        title: 'Reject Payment Slip',
        label: 'Reason for Rejection',
        placeholder: 'e.g., Incorrect amount...',
        onSubmit: (reason) => rejectSlipMutation.mutate(reason)
    });

    const openRevertApprovalModal = () => setReasonModalState({
        show: true,
        title: 'Revert Slip Approval',
        label: 'Reason for Reversion',
        placeholder: 'e.g., Approved by mistake...',
        onSubmit: (reason) => revertSlipApprovalMutation.mutate(reason)
    });
    
    // เปิด modal สร้างการจัดส่ง พร้อมเซ็ตค่า default
    const handleOpenCreateShipModal = () => {
        const defaultProvider = shippingProviders.length > 0 ? shippingProviders[0].name : '';
        setShippingInfo({ shippingProvider: defaultProvider, trackingNumber: '' });
        setShippingModalMode('create');
        setShowShippingModal(true);
    };

    // เปิด modal แก้ไขการจัดส่ง พร้อมดึงข้อมูลเก่ามาแสดง
    const handleOpenUpdateShipModal = () => {
        setShippingInfo({
            shippingProvider: order.shippingDetails?.shippingProvider || '',
            trackingNumber: order.shippingDetails?.trackingNumber || ''
        });
        setShippingModalMode('edit');
        setShowShippingModal(true);
    };

    // จัดการ submit ฟอร์มจัดส่ง (เช็คโหมด create/edit)
    const handleShipmentSubmit = (e) => {
        e.preventDefault();
        setShowShippingModal(false);
        if (shippingModalMode === 'create') {
            shipOrderMutation.mutate();
        } else {
            updateShippingMutation.mutate();
        }
    };
    
    const handleStatusChangeSubmit = (e) => {
        e.preventDefault();
        setShowShippingModal(false);
        if (!selectedStatus) return;
        updateStatusMutation.mutate();
    };

    // เช็คว่า order status ปัจจุบันสามารถกด 'Force Refund' ได้หรือไม่
    const canBeForciblyRefunded = [
        'PROCESSING', 'SHIPPED', 'COMPLETED',
        'DELIVERY_FAILED', 'RETURNED_TO_SENDER', 'REFUND_REJECTED'
    ].includes(order.orderStatus);

    // Flag รวมสำหรับ disable ปุ่มทั้งหมด ถ้ามี action ใดๆ กำลังทำงานอยู่
    const isAnyActionPending =
        approveSlipMutation.isPending ||
        rejectSlipMutation.isPending ||
        revertSlipApprovalMutation.isPending ||
        shipOrderMutation.isPending ||
        updateShippingMutation.isPending ||
        approveRefundMutation.isPending ||
        rejectRefundMutation.isPending ||
        forceRefundMutation.isPending ||
        updateStatusMutation.isPending;

    return (
        <>
            <Card className="detail-card">
                <Card.Header>Actions</Card.Header>
                <Card.Body className="d-grid gap-2">

                    {order.paymentStatus === 'PENDING_APPROVAL' && (
                        <>
                            <Button
                                variant="success"
                                onClick={confirmApproveSlip}
                                disabled={isAnyActionPending}
                                className="d-flex align-items-center justify-content-center gap-2"
                            >
                                <BsCheckCircle /> Approve Payment Slip
                            </Button>
                            <Button
                                variant="danger"
                                onClick={openRejectSlipModal}
                                disabled={isAnyActionPending}
                                className="d-flex align-items-center justify-content-center gap-2"
                            >
                                <BsShieldX /> Reject Payment Slip
                            </Button>
                        </>
                    )}

                    {order.orderStatus === 'REJECTED_SLIP' && (
                        <div className="action-warning-box">
                            <BsExclamationTriangleFill className="warning-icon" />
                            <span>The payment slip was rejected. The customer has been notified.</span>
                        </div>
                    )}

                    {order.orderStatus === 'PROCESSING' && order.paymentDetails?.paymentMethod === 'BANK_TRANSFER' && (
                        <Button
                            variant="outline-warning"
                            onClick={openRevertApprovalModal}
                            disabled={isAnyActionPending}
                            className="d-flex align-items-center justify-content-center gap-2"
                        >
                            <BsBackspaceReverseFill /> Revert Slip Approval
                        </Button>
                    )}

                    {(order.orderStatus === 'PROCESSING' || order.orderStatus === 'RETURNED_TO_SENDER') && (
                        <Button
                            variant="primary"
                            onClick={handleOpenCreateShipModal}
                            disabled={isAnyActionPending}
                            className="d-flex align-items-center justify-content-center gap-2"
                        >
                            <BsTruck /> Ship Order
                        </Button>
                    )}

                    {order.orderStatus === 'SHIPPED' && (
                        <Button
                            variant="info"
                            onClick={handleOpenUpdateShipModal}
                            disabled={isAnyActionPending}
                            className="d-flex align-items-center justify-content-center gap-2"
                        >
                            <BsPencilSquare /> Update Shipping Details
                        </Button>
                    )}

                    {order.orderStatus === 'REFUND_REQUESTED' && (
                        <>
                            <Button
                                variant="success"
                                onClick={confirmApproveRefund}
                                disabled={isAnyActionPending}
                                className="d-flex align-items-center justify-content-center gap-2"
                            >
                                <BsCheckCircle /> Approve Refund Request
                            </Button>
                            <Button
                                variant="danger"
                                onClick={confirmRejectRefund}
                                disabled={isAnyActionPending}
                                className="d-flex align-items-center justify-content-center gap-2"
                            >
                                <BsXCircle /> Reject Refund Request
                            </Button>
                        </>
                    )}

                    {order.orderStatus === 'REFUND_REJECTED' && (
                         <div className="action-warning-box">
                            <BsShieldX className="warning-icon" />
                            <span>Refund request rejected. You may force a refund if needed.</span>
                         </div>
                    )}
                    
                    {canBeForciblyRefunded && (
                        <Button
                            variant="outline-danger"
                            onClick={confirmForceRefund}
                            disabled={isAnyActionPending}
                            className="d-flex align-items-center justify-content-center gap-2"
                        >
                            <BsCurrencyExchange /> Force Refund
                        </Button>
                    )}

                    {['PROCESSING', 'SHIPPED'].includes(order.orderStatus) && order.orderStatus !== 'REFUND_REQUESTED' && (
                        <div className="action-info-box">
                            <BsInfoCircleFill className="info-icon" />
                            <span>To cancel a paid order, it must be refunded via 'Force Refund'.</span>
                        </div>
                    )}

                    <hr className="action-divider" />

                    <Button
                        variant="outline-secondary"
                        onClick={() => setShowStatusModal(true)}
                        disabled={isFetchingStatuses || nextStatuses.length === 0}
                        className="d-flex align-items-center justify-content-center gap-2"
                    >
                        {isFetchingStatuses
                            ? <Spinner as="span" animation="border" size="sm" />
                            : <><BsArrowRepeat /> Change Status</>
                        }
                    </Button>
                </Card.Body>
            </Card>

            <ConfirmationModal
                show={confirmState.show}
                onHide={() => setConfirmState({ ...confirmState, show: false })}
                onConfirm={() => {
                    confirmState.onConfirm?.();
                    setConfirmState({ ...confirmState, show: false });
                }}
                title={confirmState.title}
                confirmText={confirmState.confirmText}
                confirmVariant={confirmState.confirmVariant}
            >
                <p>{confirmState.body}</p>
            </ConfirmationModal>

            <ReasonModal
                show={reasonModalState.show}
                onHide={() => setReasonModalState({ ...reasonModalState, show: false })}
                onSubmit={(reason) => {
                    reasonModalState.onSubmit?.(reason);
                    setReasonModalState({ ...reasonModalState, show: false });
                }}
                title={reasonModalState.title}
                label={reasonModalState.label}
                placeholder={reasonModalState.placeholder}
            />

            <Modal show={showShippingModal} onHide={() => setShowShippingModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>
                        {shippingModalMode === 'create' ? 'Enter Shipping Details' : 'Update Shipping Details'}
                    </Modal.Title>
                </Modal.Header>

                <Form onSubmit={handleShipmentSubmit}>
                    <Modal.Body>
                        {selectedProviderImage && (
                            <div className="text-center mb-3">
                                <Image
                                    src={selectedProviderImage}
                                    alt={shippingInfo.shippingProvider}
                                    fluid
                                    style={{ maxHeight: '60px' }}
                                />
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
                                        <option key={provider.id} value={provider.name}>{provider.name}</option>
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
                        <Button
                            variant="primary"
                            type="submit"
                            disabled={!shippingInfo.shippingProvider || shipOrderMutation.isPending || updateShippingMutation.isPending}
                        >
                            Confirm
                        </Button>
                    </Modal.Footer>
                </Form>
            </Modal>

            <Modal show={showStatusModal} onHide={() => setShowStatusModal(false)} centered>
                <Modal.Header closeButton>
                    <Modal.Title>Manually Change Order Status</Modal.Title>
                </Modal.Header>
                <Form onSubmit={handleStatusChangeSubmit}>
                    <Modal.Body>
                        <p className="mb-1">
                            Current Status: <strong>{order.orderStatus.replace(/_/g, ' ')}</strong>
                        </p>
                        <Form.Group className="mt-3">
                            <Form.Label>Select New Status</Form.Label>
                            <Form.Select
                                required
                                value={selectedStatus}
                                onChange={e => setSelectedStatus(e.target.value)}
                            >
                                <option value="" disabled>-- Choose a valid next status --</option>
                                {nextStatuses.map(status => (
                                    <option key={status} value={status}>{status.replace(/_/g, ' ')}</option>
                                ))}
                            </Form.Select>
                        </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={() => setShowStatusModal(false)}>Cancel</Button>
                        <Button
                            variant="primary"
                            type="submit"
                            disabled={!selectedStatus || updateStatusMutation.isPending}
                        >
                            Confirm Change
                        </Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        </>
    );
}
export default OrderActions;