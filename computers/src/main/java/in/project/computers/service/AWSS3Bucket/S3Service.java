package in.project.computers.service.AWSS3Bucket;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface สำหรับบริการจัดการไฟล์ใน Amazon S3
 * กำหนดสัญญาสำหรับการอัปโหลดและลบไฟล์
 */
public interface S3Service {
    /**
     * อัปโหลดไฟล์ไปยัง S3
     * @param file ไฟล์ที่ต้องการอัปโหลด
     * @return URL ของไฟล์ที่อัปโหลดสำเร็จ
     */
    String uploadFile(MultipartFile file);

    /**
     * ลบไฟล์ออกจาก S3
     * @param filename ชื่อของไฟล์ที่ต้องการลบ
     * @return true หากการลบสำเร็จ
     */
    boolean deleteFile(String filename);
}