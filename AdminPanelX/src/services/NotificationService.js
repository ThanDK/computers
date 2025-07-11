import toast from 'react-hot-toast';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';


const MySwal = withReactContent(Swal);


const swalDarkConfig = {
    background: 'var(--secondary-bg)',
    color: 'var(--text-primary)',
    confirmButtonColor: '#d33',
    cancelButtonColor: '#3085d6',
};


export const showConfirmation = async (title, text) => {
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



export const notifySuccess = (message) => {
    toast.success(message);
};


export const notifyError = (message) => {
    toast.error(message);
};


export const handlePromise = (promise, messages) => {
    toast.promise(promise, messages);
};
