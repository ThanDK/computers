package in.project.computers.entity.computerBuild;

import in.project.computers.entity.component.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * เอกสารอธิบาย:
 * คลาสนี้เป็น Generic Helper Class ที่ใช้ภายใน ComputerBuild Entity
 * เพื่อจัดเก็บข้อมูล Component หนึ่งชิ้นพร้อมกับ "จำนวน" (quantity)
 * เช่น ใช้เก็บ RamKit 1 อัน จำนวน 2 ชิ้น
 * การใช้ Generic Type <T extends Component> ทำให้สามารถนำไปใช้กับ Component ชนิดใดก็ได้
 *
 * @param <T> ชนิดของ Component ที่จะจัดเก็บ (เช่น RamKit, Gpu, StorageDrive)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildPart<T extends Component> {
    private T component; // ข้อมูล Object ของ Component ทั้งหมด
    private int quantity;  // จำนวนของ Component ชิ้นนั้นๆ
}