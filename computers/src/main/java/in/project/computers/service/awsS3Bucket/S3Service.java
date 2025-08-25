package in.project.computers.service.awsS3Bucket;

import org.springframework.web.multipart.MultipartFile;

/**
 * Interface สำหรับบริการจัดการไฟล์ใน Amazon S3
 * <p>
 * กำหนดสัญญา (contract) สำหรับการดำเนินการต่างๆ ที่เกี่ยวกับไฟล์ เช่น
 * การอัปโหลด, การลบ, และการจัดการข้อมูลที่เกี่ยวข้องกับไฟล์ใน S3
 */
public interface S3Service {

    /**
     * อัปโหลดไฟล์ไปยัง S3 bucket
     * <p>
     * เมธอดนี้จะรับไฟล์, สร้างชื่อที่ไม่ซ้ำกัน, และอัปโหลดไปยัง S3
     * @param file ไฟล์ (MultipartFile) ที่ต้องการอัปโหลด
     * @return URL สาธารณะของไฟล์ที่อัปโหลดสำเร็จ
     */
    String uploadFile(MultipartFile file);

    /**
     * ลบไฟล์ออกจาก S3 โดยใช้ key ของไฟล์
     * <p>
     * Key คือชื่อไฟล์และ path ที่ไม่ซ้ำกันภายใน bucket
     * @param fileKey key ของไฟล์ที่ต้องการลบ (ไม่ใช่ URL เต็ม)
     * @return true หากการลบสำเร็จ, false หากล้มเหลว
     */
    boolean deleteFileByKey(String fileKey);

    /**
     * สกัด key ของไฟล์ออกจาก URL เต็มของ S3
     * <p>
     * เนื่องจาก API ของ S3 มักต้องการ key ในการดำเนินการ (เช่น การลบ)
     * เมธอดนี้จึงช่วยแปลง URL ที่เก็บในฐานข้อมูลกลับไปเป็น key
     * @param url URL เต็มของไฟล์ใน S3
     * @return key ของไฟล์ที่สกัดได้
     */
    String extractKeyFromUrl(String url);
}