import toast from 'react-hot-toast';
import Swal from 'sweetalert2';
import withReactContent from 'sweetalert2-react-content';

const MySwal = withReactContent(Swal);

// object สำหรับตั้งค่า theme ของ SweetAlert2 ให้เป็น dark mode
const swalDarkConfig = {
    background: 'var(--secondary-bg)',
    color: 'var(--text-primary)',
    confirmButtonColor: '#d33',
    cancelButtonColor: '#3085d6',
};

/**
 * แสดงกล่องโต้ตอบเพื่อยืนยันการกระทำ
 * @param {string} title - หัวข้อของกล่องโต้ตอบ
 * @param {string} text - ข้อความ/คำถามหลัก
 * @returns {Promise<boolean>} คืนค่า true ถ้าผู้ใช้กดยืนยัน, false ถ้าไม่
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
 * แสดง Toast notification สำหรับการกระทำที่สำเร็จ
 * @param {string} message - ข้อความที่จะแสดง
 */
export function notifySuccess(message) {
    toast.success(message);
};

/**
 * แสดง Toast notification สำหรับข้อผิดพลาด
 * @param {string} message - ข้อความที่จะแสดง
 */
export function notifyError(message) {
    toast.error(message);
};

/**
 * ครอบ Promise ด้วย Toast notification เพื่อแสดงสถานะ loading, success, และ error
 * @param {Promise<any>} promise - Promise ที่ต้องการติดตามสถานะ
 * @param {object} messages - Object ที่มีข้อความสำหรับแต่ละสถานะ (loading, success, error)
 * @returns {Promise<any>} Promise เดิมที่ส่งเข้ามา
 */
export function handlePromise(promise, messages) {
    return toast.promise(promise, messages);
};