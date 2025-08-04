import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { updateUserProfile, removeProfilePicture } from '../../services/ProfileService';
import { notifySuccess, notifyError, showConfirmation } from '../../services/NotificationService';
import { Card, Button, Modal, Form, Spinner, Badge } from 'react-bootstrap';
import { BsPencilSquare, BsPersonCircle, BsTrash } from 'react-icons/bs';
import ImageCropper from '../ImageCropper/ImageCropper';
import './AdminProfileCard.css';

const AdminProfileCard = () => {
  const { user, token, updateCurrentUser } = useAuth();
  
  const [showEditModal, setShowEditModal] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const [cropModalState, setCropModalState] = useState({ show: false, src: '' });
  const [croppedImageFile, setCroppedImageFile] = useState(null);
  const [croppedPreviewUrl, setCroppedPreviewUrl] = useState('');

  const fileInputRef = useRef(null);

  useEffect(() => {
    return () => {
        if (croppedPreviewUrl) {
            URL.revokeObjectURL(croppedPreviewUrl);
        }
    };
  }, [croppedPreviewUrl]);

  if (!user) return null;

  const handleShow = () => setShowEditModal(true);
  const handleClose = () => {
    setShowEditModal(false);
    setCroppedImageFile(null);
    setCroppedPreviewUrl('');
  };
  
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
    setIsSubmitting(true);

    const formData = new FormData();
    const profileData = {
      name: event.currentTarget.name.value,
      email: event.currentTarget.email.value,
    };
    
    const password = event.currentTarget.password.value;
    if (password) {
      profileData.password = password;
    }
    
    formData.append('profileData', new Blob([JSON.stringify(profileData)], { type: 'application/json' }));

    if (croppedImageFile) {
      formData.append('file', croppedImageFile, 'profile-picture.jpg');
    }

    try {
      const updatedUser = await updateUserProfile(formData, token);
      updateCurrentUser(updatedUser);
      notifySuccess('Profile updated successfully!');
      handleClose();
    } catch (err) {
      notifyError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRemovePicture = async () => {
    if (!await showConfirmation('Are you sure?', 'This will permanently remove your profile picture.')) {
        return;
    }
    
    try {
        const updatedUser = await removeProfilePicture(token);
        updateCurrentUser(updatedUser); 
        notifySuccess('Profile picture removed.');
        
        setCroppedImageFile(null);
        setCroppedPreviewUrl('');
        
    } catch(err) {
        notifyError(err.message);
    }
  };
  
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
              <Form.Label>Profile Picture</Form.Label>
              <div className="d-flex gap-2">
                <Button as="label" htmlFor="profile-upload" variant="outline-secondary" className="flex-grow-1">
                  Upload & Crop New Image
                </Button>
                {previewSource && (
                    <Button variant="outline-danger" onClick={handleRemovePicture} title="Remove current picture">
                        <BsTrash />
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
            <Button variant="primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? <><Spinner as="span" animation="border" size="sm" /> Saving...</> : 'Save Changes'}
            </Button>
          </Modal.Footer>
        </Form>
      </Modal>

      <ImageCropper 
        show={cropModalState.show}
        imageSrc={cropModalState.src}
        onHide={() => setCropModalState({ show: false, src: '' })}
        onCropComplete={handleCropComplete}
        aspect={1}
      />
    </>
  );
};

export default AdminProfileCard;