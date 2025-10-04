import toast from 'react-hot-toast';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';

const MySwal = withReactContent(Swal);

// Minimal config to align button colors with the app's theme.
const swalButtonConfig = {
    confirmButtonColor: '#d33',
    cancelButtonColor: '#6e7881', // A standard bootstrap secondary/grey color
};

export const showConfirmation = async (title, text) => {
    const result = await MySwal.fire({
        title,
        text,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Yes, proceed!',
        ...swalButtonConfig // Re-introduce button styling
    });
    return result.isConfirmed;
};

export const notifySuccess = (message) => {
    toast.success(message);
};

export const notifyError = (message) => {
    toast.error(message);
};

export const handlePromise = (promise, messages) => {
    return toast.promise(promise, messages);
};