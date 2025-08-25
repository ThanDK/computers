import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { fetchAllUsers, deleteUser, lockUser, unlockUser, createUserByAdmin, updateUserByAdmin } from '../../services/UserService';
import { notifySuccess, notifyError, handlePromise } from '../../services/NotificationService';
import PageHeader from '../../components/PageHeader/PageHeader';
import MainHeader from '../../components/MainHeader/MainHeader';
import ReusableTable from '../../components/ReusableTable/ReusableTable';
import ImageModal from '../../components/ImageModal/ImageModal';
import TableControls from '../../components/TableControls/TableControls';
import AdminProfileCard from '../../components/AdminProfileCard/AdminProfileCard';
import TruncatedText from '../../components/TruncatedText/TruncatedText';
import UserFormModal from '../../components/UserFormModal/UserFormModal';
import { Button, Form, InputGroup, Badge } from 'react-bootstrap';
import { BsSearch, BsPlusCircleFill, BsArrowCounterclockwise } from 'react-icons/bs';
import './UsersPage.css';
import '../../components/ImageModal/ImageModal.css';

const roleOptions = ["ROLE_USER", "ROLE_ADMIN"];

function UsersPage() {
  const { token, user: currentUser } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // อ่านค่า state ของตารางจาก URL search params ทำให้ URL เป็น 'source of truth'
  const tableState = useMemo(() => {
    const pageIndex = parseInt(searchParams.get('page')) || 0;
    const pageSize = parseInt(searchParams.get('pageSize')) || 10;
    const sortingParams = searchParams.get('sort');
    const sorting = sortingParams ? JSON.parse(sortingParams) : [];
    const globalFilter = searchParams.get('globalFilter') || '';
    const roleFilter = searchParams.get('roleFilter') || '';
    const columnFilters = roleFilter ? [{ id: 'role', value: roleFilter }] : [];
    return { pagination: { pageIndex, pageSize }, sorting, globalFilter, columnFilters };
  }, [searchParams]);

  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [imageModalUrl, setImageModalUrl] = useState(null);
  
  // state ของตารางจะถูก sync กับ URL
  const [pagination, setPagination] = useState(tableState.pagination);
  const [sorting, setSorting] = useState(tableState.sorting);
  const [globalFilter, setGlobalFilter] = useState(tableState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(tableState.columnFilters);

  const loadData = useCallback(async () => {
    if (!token || !currentUser) return;
    setLoading(true);
    setError('');
    try {
      const data = await fetchAllUsers(token);
      // ฟิลเตอร์ user ที่กำลัง login อยู่ออกจาก list ที่แสดงในตาราง
      setUsers(data.filter(u => u.id !== currentUser.id));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [token, currentUser]);

  useEffect(() => { loadData(); }, [loadData]);

  // effect นี้จะคอยจับการเปลี่ยนแปลงของ state ตาราง แล้วอัปเดต URL search params ตาม
  useEffect(() => {
    const newSearchParams = new URLSearchParams();
    if (pagination.pageIndex > 0) newSearchParams.set('page', pagination.pageIndex);
    if (pagination.pageSize !== 10) newSearchParams.set('pageSize', pagination.pageSize);
    if (sorting.length > 0) newSearchParams.set('sort', JSON.stringify(sorting));
    if (globalFilter) newSearchParams.set('globalFilter', globalFilter);
    const roleFilter = columnFilters.find((f) => f.id === 'role')?.value;
    if (roleFilter) newSearchParams.set('roleFilter', roleFilter);
    setSearchParams(newSearchParams, { replace: true });
  }, [pagination, sorting, globalFilter, columnFilters, setSearchParams]);

  const handleImageClick = useCallback((imageUrl) => { setImageModalUrl(imageUrl); }, []);
  const handleShowModal = (type, item = null) => setModalState({ show: true, type, currentItem: item });
  const handleCloseModal = () => setModalState({ show: false, type: 'add', currentItem: null });

  // จัดการการ submit ฟอร์ม ทั้งการสร้าง user ใหม่ และการอัปเดต
  const handleFormSubmit = async (data) => {
    setIsSubmitting(true);
    const { type, currentItem } = modalState;
    try {
      if (type === 'add') {
        await createUserByAdmin(data, token);
        notifySuccess(`User created successfully!`);
      } else {
        await updateUserByAdmin(currentItem.id, data, token);
        notifySuccess(`User updated successfully!`);
      }
      handleCloseModal();
      loadData();
    } catch (err) {
      notifyError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = useCallback(async (user) => {
      try {
          if (await deleteUser(user, token)) {
              setUsers((prev) => prev.filter((u) => u.id !== user.id));
          }
      } catch (err) {
          console.error('Deletion process failed:', err);
      }
  }, [token]);

  // ฟังก์ชันสำหรับสลับสถานะ lock/unlock user
  const handleToggleLock = useCallback(async (user) => {
      const action = user.locked ? unlockUser : lockUser;
      const actionVerb = user.locked ? 'Unlocking' : 'Locking';
      const actionPast = user.locked ? 'unlocked' : 'locked';

      // สร้าง promise แล้วส่งไปให้ handlePromise เพื่อจัดการ notification ให้อัตโนมัติ
      const promise = action(user.id, token).then(updatedUser => {
        setUsers(prev => prev.map(u => u.id === updatedUser.id ? updatedUser : u));
        return updatedUser;
      });

      handlePromise(promise, {
        loading: `${actionVerb} user...`,
        success: (updatedUser) => `User "${updatedUser.name}" has been ${actionPast}.`,
        error: (err) => `Failed to ${actionVerb.toLowerCase()} user. ${err.message}`
      });
  }, [token]);

  const handleResetFilters = () => {
    setGlobalFilter('');
    setColumnFilters([]);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
    setSorting([]);
    loadData();
  };

  const columns = useMemo(() => [
    {
      accessorKey: 'profilePictureUrl', header: 'Avatar', enableSorting: false, meta: { cellClassName: 'text-center-cell', width: '80px' },
      cell: ({ row }) => {
        const user = row.original;
        return user.profilePictureUrl ? (
          <img src={user.profilePictureUrl} alt={user.name} className="table-avatar clickable-image" onClick={() => handleImageClick(user.profilePictureUrl)} />
        ) : (
          <div className="table-avatar-placeholder">{user.name.charAt(0).toUpperCase()}</div>
        );
      },
    },
    { accessorKey: 'name', header: 'Name' },
    { accessorKey: 'email', header: 'Email', cell: info => <TruncatedText text={info.getValue()} /> },
    { accessorKey: 'role', header: 'Role', meta: { width: '150px' } },
    {
        accessorKey: 'locked', header: 'Status', meta: { cellClassName: 'text-center-cell', width: '120px' },
        cell: ({ row }) => (
            <Badge pill bg={row.original.locked ? "danger" : "success"} className="status-badge">
                {row.original.locked ? "Locked" : "Active"}
            </Badge>
        )
    },
    {
      id: 'actions', header: 'Actions', meta: { cellClassName: 'text-center-cell', width: '250px' },
      cell: ({ row }) => {
        const user = row.original;
        // เช็คว่าเป็น account ของตัวเองหรือไม่ เพื่อ disable ปุ่มบางปุ่ม
        const isSelf = currentUser?.email === user.email;

        return (
          <div className="d-flex gap-2 justify-content-center">
            <Button variant="outline-primary" size="sm" className="action-btn action-btn-edit" onClick={() => handleShowModal('edit', user)}>Edit</Button>
            <Button variant={user.locked ? "outline-success" : "outline-warning"} size="sm" className="action-btn" onClick={() => handleToggleLock(user)} disabled={isSelf} title={isSelf ? "Cannot lock your own account" : (user.locked ? "Unlock User" : "Lock User")}>
              {user.locked ? 'Unlock' : 'Lock'}
            </Button>
            <Button variant="outline-danger" size="sm" className="action-btn action-btn-delete" onClick={() => handleDelete(user)} disabled={isSelf} title={isSelf ? "Cannot delete your own account" : "Delete User"}>Delete</Button>
          </div>
        );
      },
    },
  ], [currentUser, handleImageClick, handleDelete, handleToggleLock, handleShowModal]);

  const roleFilterValue = columnFilters.find((f) => f.id === 'role')?.value || '';

  return (
    <>
      <MainHeader />
      <PageHeader title="Manage Users" subtitle="View, search, and manage user accounts" />
      
      {/* แสดง card ของ admin ที่ login อยู่ด้านบน */}
      <AdminProfileCard />

      <TableControls>
        <div className="filter-controls">
          <InputGroup className="search-bar">
            <Form.Control placeholder="Search other users..." value={globalFilter ?? ''} onChange={(e) => setGlobalFilter(e.target.value)} className="search-input" />
            <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
          </InputGroup>
          <Form.Select className="type-filter" value={roleFilterValue} onChange={(e) => {
              const value = e.target.value;
              setColumnFilters((prev) => prev.filter((f) => f.id !== 'role').concat(value ? [{ id: 'role', value }] : []));
          }}>
            <option value="">All Roles</option>
            {roleOptions.map(role => <option key={role} value={role}>{role.replace('ROLE_', '')}</option>)}
          </Form.Select>
          <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1"><BsArrowCounterclockwise /> Reset</Button>
        </div>
        <Button variant="primary" size="lg" className="d-flex align-items-center gap-2" onClick={() => handleShowModal('add')}>
          <BsPlusCircleFill /> Add New User
        </Button>
      </TableControls>

      <ReusableTable
        columns={columns}
        data={users}
        isLoading={loading}
        error={error && !users.length ? error : null}
        sorting={sorting}
        setSorting={setSorting}
        globalFilter={globalFilter}
        setGlobalFilter={setGlobalFilter}
        columnFilters={columnFilters}
        setColumnFilters={setColumnFilters}
        pagination={pagination}
        onPaginationChange={setPagination}
        keepPageOnDataUpdate={true}
      />

      <UserFormModal
        show={modalState.show}
        onHide={handleCloseModal}
        modalState={modalState}
        onSubmit={handleFormSubmit}
        isSubmitting={isSubmitting}
      />

      <ImageModal show={!!imageModalUrl} onHide={() => setImageModalUrl(null)} imageUrl={imageModalUrl} altText="User avatar" />
    </>
  );
}

export default UsersPage;