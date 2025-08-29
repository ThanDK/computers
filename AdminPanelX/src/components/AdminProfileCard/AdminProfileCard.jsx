import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useMutation, useQueryClient } from '@tanstack/react-query'; // แก้ไข - เพิ่ม tanstack query hooks
import { updateUserProfile, removeProfilePicture } from '../../services/ProfileService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';
import { Card, Button, Modal, Form, Spinner, Badge } from 'react-bootstrap';
import { BsPencilSquare, BsPersonCircle, BsTrash } from 'react-icons/bs';
import ImageCropper from '../ImageCropper/ImageCropper';
import './AdminProfileCard.css';

const AdminProfileCard = () => {
  const { user, token, updateCurrentUser } = useAuth();
  const queryClient = useQueryClient(); // แก้ไข - สร้าง instance ของ query client
  
  const [showEditModal, setShowEditModal] = useState(false);

  // State ชุดนี้สำหรับจัดการขั้นตอนการครอปรูปทั้งหมด
  const [cropModalState, setCropModalState] = useState({ show: false, src: '' });
  const [croppedImageFile, setCroppedImageFile] = useState(null);
  const [croppedPreviewUrl, setCroppedPreviewUrl] = useState('');

  const fileInputRef = useRef(null);

  // จัดการ cleanup URL ชั่วคราวของรูปกัน memory leak
  useEffect(() => {
    return () => {
        if (croppedPreviewUrl) {
            URL.revokeObjectURL(croppedPreviewUrl);
        }
    };
  }, [croppedPreviewUrl]);
  
  //สร้าง mutation สำหรับอัปเดตโปรไฟล์
  const updateProfileMutation = useMutation({
    mutationFn: (formData) => updateUserProfile(formData, token),
    onSuccess: (updatedUser) => {
        updateCurrentUser(updatedUser);
        notifySuccess('Profile updated successfully!');
        handleClose();
    },
    onError: (err) => {
        notifyError(err.message);
    }
  });
  
  // สร้าง mutation สำหรับลบรูปโปรไฟล์
  const removePictureMutation = useMutation({
    mutationFn: () => removeProfilePicture(token),
    onSuccess: (updatedUser) => {
        updateCurrentUser(updatedUser);
        notifySuccess('Profile picture removed.');
        setCroppedImageFile(null);
        setCroppedPreviewUrl('');
    },
    onError: (err) => {
        notifyError(err.message);
    }
  });


  if (!user) return null;

  const handleShow = () => setShowEditModal(true);
  const handleClose = () => {
    setShowEditModal(false);
    setCroppedImageFile(null);
    setCroppedPreviewUrl('');
  };
  
  // เมื่อ user เลือกไฟล์, อ่านไฟล์แล้วเปิดหน้าต่างครอปรูป
  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) return; 

    const reader = new FileReader();
    reader.onloadend = () => {
        setCropModalState({ show: true, src: reader.result?.toString() || '' });
    };
    reader.readAsDataURL(file);
    
    if(fileInputRef.current) fileInputRef.current.value = "";
  };
  
  // รับข้อมูลรูปที่ครอปเสร็จแล้วจาก component ImageCropper
  const handleCropComplete = (croppedFile) => {
    if (croppedFile) {
      setCroppedImageFile(croppedFile);
      if (croppedPreviewUrl) {
        URL.revokeObjectURL(croppedPreviewUrl);
      }
      setCroppedPreviewUrl(URL.createObjectURL(croppedFile));
    }
    setCropModalState({ show: false, src: '' });
  };

  const handleFormSubmit = async (event) => {
    event.preventDefault();
    // setIsSubmitting(true); // แก้ไข - ลบออก

    const form = event.currentTarget;
    const password = form.password.value;
    const confirmPassword = form.confirmPassword.value;

    if (password !== confirmPassword) {
      notifyError("Passwords do not match.");
      return;
    }

    // ตอน submit, ต้องใช้ FormData เพราะเราส่งไฟล์ไปด้วย
    // ส่วนข้อมูล text จะถูกแปลงเป็น JSON blob แล้วส่งไปพร้อมกัน
    const formData = new FormData();
    const profileData = {
      name: form.name.value,
      email: form.email.value,
    };
    
    if (password) {
      profileData.password = password;
    }
    
    formData.append('profileData', new Blob([JSON.stringify(profileData)], { type: 'application/json' }));

    if (croppedImageFile) {
      formData.append('file', croppedImageFile, 'profile-picture.jpg');
    }


    updateProfileMutation.mutate(formData);
  };

  const handleRemovePicture = async () => {
    if (!await showConfirmation('Are you sure?', 'This will permanently remove your profile picture.')) {
        return;
    }

    removePictureMutation.mutate();
  };
  
  // ถ้ามีรูป preview ใหม่ให้ใช้รูปนั้น, ถ้าไม่มีก็ใช้รูปโปรไฟล์ปัจจุบัน
  const previewSource = croppedPreviewUrl || user.profilePictureUrl;

  return (
    <>
      <Card className="admin-profile-card mb-4">
        <Card.Body>
          <div className="d-flex align-items-center">
            {user.profilePictureUrl ? (
              <img src={user.profilePictureUrl} alt={user.name} className="profile-avatar" />
            ) : (
              <BsPersonCircle className="profile-avatar-placeholder" />
            )}
            <div className="profile-info">
              <Card.Title as="h5" className="mb-0">{user.name}</Card.Title>
              <Card.Text className="text-muted mb-0">{user.email}</Card.Text>
              <Badge pill bg="primary" className="mt-2">{user.role.replace('ROLE_', '')}</Badge>
            </div>
            <Button variant="outline-primary" className="ms-auto" onClick={handleShow}>
              <BsPencilSquare className="me-1" /> Edit Profile
            </Button>
          </div>
        </Card.Body>
      </Card>

      <Modal show={showEditModal} onHide={handleClose} centered backdrop="static">
        <Modal.Header closeButton>
          <Modal.Title>Edit Your Profile</Modal.Title>
        </Modal.Header>
        <Form onSubmit={handleFormSubmit}>
          <Modal.Body>
            <Form.Group className="mb-3">
              <Form.Label>Full Name</Form.Label>
              <Form.Control type="text" name="name" defaultValue={user.name} required />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Email Address</Form.Label>
              <Form.Control type="email" name="email" defaultValue={user.email} required />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>New Password</Form.Label>
              <Form.Control type="password" name="password" placeholder="Leave blank to keep current password" />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Confirm New Password</Form.Label>
              <Form.Control type="password" name="confirmPassword" placeholder="Confirm your new password" />
            </Form.Group>
            <Form.Group className="mb-3">
              <Form.Label>Profile Picture</Form.Label>
              <div className="d-flex gap-2">
                <Button as="label" htmlFor="profile-upload" variant="outline-secondary" className="flex-grow-1">
                  Upload & Crop New Image
                </Button>
                {previewSource && (
                    <Button variant="outline-danger" onClick={handleRemovePicture} disabled={removePictureMutation.isPending} title="Remove current picture">
                        {removePictureMutation.isPending ? <Spinner size="sm" /> : <BsTrash />}
                    </Button>
                )}
              </div>
              <Form.Control id="profile-upload" type="file" ref={fileInputRef} accept="image/*" onChange={handleFileChange} className="d-none" />
            </Form.Group>
            
            {previewSource && (
                <div className="mt-3 text-center">
                    <p className="mb-2 preview-label">
                        {croppedPreviewUrl ? 'New Profile Picture Preview:' : 'Current Profile Picture:'}
                    </p>
                    <img src={previewSource} alt="Profile Preview" className="cropped-preview-image" />
                </div>
            )}
          </Modal.Body>
          <Modal.Footer>
            <Button variant="secondary" onClick={handleClose}>Cancel</Button>
            <Button variant="primary" type="submit" disabled={updateProfileMutation.isPending}>
              {updateProfileMutation.isPending ? <><Spinner as="span" animation="border" size="sm" /> Saving...</> : 'Save Changes'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ImageCropper 
        show={cropModalState.show}
        imageSrc={cropModalState.src}
        onHide={() => setCropModalState({ show: false, src: '' })}
        onCropComplete={handleCropComplete}
        aspect={1} // กำหนด aspect ratio เป็น 1:1 (สี่เหลี่ยมจัตุรัส)
      />
    </>
  );
};

export default AdminProfileCard;