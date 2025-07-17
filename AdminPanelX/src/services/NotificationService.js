// src/services/NotificationService.js
import toast from 'react-hot-toast';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';

const MySwal = withReactContent(Swal);

// Configuration for dark-themed SweetAlert2 modals
const swalDarkConfig = {
    background: 'var(--secondary-bg)',
    color: 'var(--text-primary)',
    confirmButtonColor: '#d33',
    cancelButtonColor: '#3085d6',
};

/**
 * Displays a confirmation dialog using SweetAlert2.
 * @param {string} title - The title of the dialog.
 * @param {string} text - The main text/question of the dialog.
 * @returns {Promise<boolean>} - True if the user confirmed, false otherwise.
 */
export async function showConfirmation(title, text) {
    const result = await MySwal.fire({
        title,
        text,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Yes, proceed!',
        ...swalDarkConfig
    });
    return result.isConfirmed;
};

/**
 * Displays a success notification toast.
 * @param {string} message - The message to display.
 */
export function notifySuccess(message) {
    toast.success(message);
};

/**
 * Displays an error notification toast.
 * @param {string} message - The message to display.
 */
export function notifyError(message) {
    toast.error(message);
};

/**
 * Wraps a promise with toast notifications for loading, success, and error states.
 * @param {Promise<any>} promise - The promise to track.
 * @param {object} messages - The messages for different states (loading, success, error).
 * @returns {Promise<any>} - The original promise.
 */
export function handlePromise(promise, messages) {
    return toast.promise(promise, messages);
};