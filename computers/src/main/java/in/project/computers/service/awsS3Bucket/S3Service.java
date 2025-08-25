package in.project.computers.service.awsS3Bucket;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

    /**
     * อัปโหลดไฟล์ไปยัง S3 bucket
     * @param file ไฟล์ที่ต้องการอัปโหลด
     * @return URL ของไฟล์ที่อัปโหลด
     */
    String uploadFile(MultipartFile file);

    /**
     * ลบไฟล์ออกจาก S3 bucket
     * @param fileKey key ของไฟล์ที่ต้องการลบ
     * @return true หากลบสำเร็จ
     */
    boolean deleteFileByKey(String fileKey);

    /**
     * ดึง key ของไฟล์จาก URL
     * @param url URL เต็มของไฟล์ใน S3
     * @return key ของไฟล์
     */
    String extractKeyFromUrl(String url);
}