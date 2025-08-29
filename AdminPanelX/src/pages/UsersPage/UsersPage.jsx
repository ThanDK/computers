import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { 
    fetchAllUsers, 
    deleteUser, 
    lockUser, 
    unlockUser, 
    createUserByAdmin, 
    updateUserByAdmin 
} from '../../services/UserService';
import { 
    notifySuccess, 
    notifyError, 
    showConfirmation, 
    handlePromise 
} from '../../services/NotificationService';

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
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  // อ่าน state ของตารางจาก URL, ทำให้แชร์ลิงก์พร้อม filter ได้
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

  // ดึงข้อมูล user ทั้งหมด, แต่กรอง user ที่ login อยู่ออก
  const { data: users = [], isLoading, isFetching, error, refetch } = useQuery({
    queryKey: ['users'],
    queryFn: () => fetchAllUsers(token),
    enabled: !!token && !!currentUser,
    select: (data) => data.filter(u => u.id !== currentUser.id),
  });

  // custom hook สำหรับสร้าง mutation (create/update) ให้สั้นลง
  const useUserFormMutation = (mutationFn, { successMessage }) => {
    return useMutation({
        mutationFn,
        onSuccess: () => {
            notifySuccess(successMessage);
            queryClient.invalidateQueries({ queryKey: ['users'] });
            handleCloseModal();
        },
        onError: (err) => notifyError(err.message),
    });
  };

  const createUserMutation = useUserFormMutation(
    (data) => createUserByAdmin(data, token),
    { successMessage: `User created successfully!` }
  );

  const updateUserMutation = useUserFormMutation(
    (vars) => updateUserByAdmin(vars.id, vars.data, token),
    { successMessage: `User updated successfully!` }
  );

  // mutation สำหรับลบ user
  const deleteUserMutation = useMutation({
    mutationFn: (user) => deleteUser(user, token),
    onSuccess: (result, user) => {
        notifySuccess(`User "${user.name}" deleted successfully.`);
        queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (err) => {
        if (err.message !== 'Deletion cancelled by user.') {
            notifyError(err.message);
        }
    },
  });

  // mutation สำหรับ lock/unlock user
  const toggleLockMutation = useMutation({
    mutationFn: (user) => (user.locked ? unlockUser(user.id, token) : lockUser(user.id, token)),
    onSuccess: (updatedUser) => {
        // อัปเดต cache ทันที ไม่ต้อง refetch
        queryClient.setQueryData(['users'], (oldData) =>
            oldData.map((u) => u.id === updatedUser.id ? updatedUser : u)
        );
        return updatedUser;
    }
  });

  const [modalState, setModalState] = useState({ show: false, type: 'add', currentItem: null });
  const [imageModalUrl, setImageModalUrl] = useState(null);
  
  // state ของตาราง, sync กับ URL ด้านบน
  const [pagination, setPagination] = useState(tableState.pagination);
  const [sorting, setSorting] = useState(tableState.sorting);
  const [globalFilter, setGlobalFilter] = useState(tableState.globalFilter);
  const [columnFilters, setColumnFilters] = useState(tableState.columnFilters);
  
  // เมื่อ state ตารางเปลี่ยน, ให้อัปเดต URL ตาม
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

  // จัดการ submit form ทั้ง add และ edit
  const handleFormSubmit = async (data) => {
    const { type, currentItem } = modalState;
    if (type === 'add') {
      createUserMutation.mutate(data);
    } else {
      updateUserMutation.mutate({ id: currentItem.id, data });
    }
  };

  const handleDelete = useCallback(async (user) => {
    const confirmed = await showConfirmation(
      'Are you sure?', 
      `You are about to delete user "${user.name}" (${user.email}). This is permanent.`
    );
    if (confirmed) {
        deleteUserMutation.mutate(user);
    }
  }, [deleteUserMutation]);

  // สลับสถานะ lock/unlock ของ user
  const handleToggleLock = useCallback(async (user) => {
      const actionVerb = user.locked ? 'Unlocking' : 'Locking';
      const actionPast = user.locked ? 'unlocked' : 'locked';

      // ใช้ handlePromise จัดการ notification loading/success/error ให้อัตโนมัติ
      const promise = toggleLockMutation.mutateAsync(user);

      handlePromise(promise, {
        loading: `${actionVerb} user...`,
        success: (updatedUser) => `User "${updatedUser.name}" has been ${actionPast}.`,
        error: (err) => `Failed to ${actionVerb.toLowerCase()} user. ${err.message}`
      });
  }, [toggleLockMutation]);

  const handleResetFilters = () => {
    setGlobalFilter('');
    setColumnFilters([]);
    setPagination((p) => ({ ...p, pageIndex: 0 }));
    setSorting([]);
    refetch();
  };

  // config คอลัมน์สำหรับตาราง
  const columns = useMemo(() => [
    {
      accessorKey: 'profilePictureUrl', 
      header: 'Avatar', 
      enableSorting: false, 
      meta: { cellClassName: 'text-center-cell', width: '80px' },
      cell: ({ row }) => {
        const user = row.original;
        return user.profilePictureUrl ? (
          <img 
            src={user.profilePictureUrl} 
            alt={user.name} 
            className="table-avatar clickable-image" 
            onClick={() => handleImageClick(user.profilePictureUrl)} 
          />
        ) : (
          <div className="table-avatar-placeholder">{user.name.charAt(0).toUpperCase()}</div>
        );
      },
    },
    { 
      accessorKey: 'name', 
      header: 'Name' 
    },
    { 
      accessorKey: 'email', 
      header: 'Email', 
      cell: info => <TruncatedText text={info.getValue()} /> 
    },
    { 
      accessorKey: 'role', 
      header: 'Role', 
      meta: { width: '150px' } 
    },
    {
        accessorKey: 'locked', 
        header: 'Status', 
        meta: { cellClassName: 'text-center-cell', width: '120px' },
        cell: ({ row }) => (
            <Badge pill bg={row.original.locked ? "danger" : "success"} className="status-badge">
                {row.original.locked ? "Locked" : "Active"}
            </Badge>
        )
    },
    {
      id: 'actions', 
      header: 'Actions', 
      meta: { cellClassName: 'text-center-cell', width: '250px' },
      cell: ({ row }) => {
        const user = row.original;
        const isSelf = currentUser?.email === user.email; // เช็คว่าเป็น user ตัวเองหรือไม่

        return (
          <div className="d-flex gap-2 justify-content-center">
            <Button variant="outline-primary" size="sm" className="action-btn action-btn-edit" onClick={() => handleShowModal('edit', user)}>
              Edit
            </Button>
            <Button 
              variant={user.locked ? "outline-success" : "outline-warning"} 
              size="sm" 
              className="action-btn" 
              onClick={() => handleToggleLock(user)} 
              disabled={isSelf} 
              title={isSelf ? "Cannot lock your own account" : (user.locked ? "Unlock User" : "Lock User")}
            >
              {user.locked ? 'Unlock' : 'Lock'}
            </Button>
            <Button 
              variant="outline-danger" 
              size="sm" 
              className="action-btn action-btn-delete" 
              onClick={() => handleDelete(user)} 
              disabled={isSelf} 
              title={isSelf ? "Cannot delete your own account" : "Delete User"}
            >
              Delete
            </Button>
          </div>
        );
      },
    },
  ], [currentUser, handleImageClick, handleDelete, handleToggleLock]);

  const roleFilterValue = columnFilters.find((f) => f.id === 'role')?.value || '';
  const isSubmitting = createUserMutation.isPending || updateUserMutation.isPending;

  return (
    <>
      <MainHeader />
      <PageHeader title="Manage Users" subtitle="View, search, and manage user accounts" />
      
      <AdminProfileCard />

      <TableControls>
        <div className="filter-controls">
          <InputGroup className="search-bar">
            <Form.Control 
              placeholder="Search other users..." 
              value={globalFilter ?? ''} 
              onChange={(e) => setGlobalFilter(e.target.value)} 
              className="search-input" 
            />
            <InputGroup.Text className="search-input-group-text"><BsSearch /></InputGroup.Text>
          </InputGroup>

          <Form.Select 
            className="type-filter" 
            value={roleFilterValue} 
            onChange={(e) => {
              const value = e.target.value;
              setColumnFilters((prev) => prev.filter((f) => f.id !== 'role').concat(value ? [{ id: 'role', value }] : []));
            }}
          >
            <option value="">All Roles</option>
            {roleOptions.map(role => <option key={role} value={role}>{role.replace('ROLE_', '')}</option>)}
          </Form.Select>

          <Button variant="outline-secondary" onClick={handleResetFilters} className="d-flex align-items-center gap-1">
            <BsArrowCounterclockwise /> Reset
          </Button>
        </div>

        <Button variant="primary" size="lg" className="d-flex align-items-center gap-2" onClick={() => handleShowModal('add')}>
          <BsPlusCircleFill /> Add New User
        </Button>
      </TableControls>

      <ReusableTable
        columns={columns}
        data={users}
        isLoading={isLoading || isFetching}
        error={error && !users.length ? error.message : null}
        pagination={pagination}
        onPaginationChange={setPagination}
        sorting={sorting}
        setSorting={setSorting}
        globalFilter={globalFilter}
        setGlobalFilter={setGlobalFilter}
        columnFilters={columnFilters}
        setColumnFilters={setColumnFilters}
        keepPageOnDataUpdate={true}
      />

      <UserFormModal
        show={modalState.show}
        onHide={handleCloseModal}
        modalState={modalState}
        onSubmit={handleFormSubmit}
        isSubmitting={isSubmitting}
      />

      <ImageModal 
        show={!!imageModalUrl} 
        onHide={() => setImageModalUrl(null)} 
        imageUrl={imageModalUrl} 
        altText="User avatar" 
      />
    </>
  );
}

export default UsersPage;