# 🛠 Detailed Development Workflow (ขั้นตอนการพัฒนาอย่างละเอียด)

This document outlines the systematic process used to build the **FireCash** application, moving from environment setup to final deployment.
(เอกสารนี้ระบุขั้นตอนการทำงานอย่างเป็นระบบในการสร้างแอปพลิเคชัน FireCash ตั้งแต่การเตรียมสภาพแวดล้อมไปจนถึงการนำไปใช้งานจริง)

---

## 1. Environment & Project Setup (การเตรียมสภาพแวดล้อมและเริ่มโปรเจกต์)
*Focus: Setting the foundation (เน้นการวางรากฐาน)*

* **Project Initialization (การเริ่มต้นโปรเจกต์):** Create the Android project using Kotlin and Jetpack Compose with a structured Gradle setup.
  *(สร้างโปรเจกต์ Android โดยใช้ Kotlin และ Jetpack Compose พร้อมการตั้งค่า Gradle ที่เป็นระบบ)*
* **Dependency Management (การจัดการไลบรารี):** Configure essential libraries such as OpenCV (for Image Processing), ML Kit (for OCR), and Retrofit (for API communication).
  *(กำหนดค่าไลบรารีที่จำเป็น เช่น OpenCV สำหรับการประมวลผลภาพ, ML Kit สำหรับการอ่านข้อความ และ Retrofit สำหรับการสื่อสารกับ API)*
* **Configuration (การตั้งค่าระบบ):** Optimize `gradle.properties` to improve build speed and prevent connection errors.
  *(ปรับแต่ง gradle.properties เพื่อเพิ่มความเร็วในการ Build และป้องกันข้อผิดพลาดในการเชื่อมต่อ)*

---

含ま## 2. Core Engine Development (การพัฒนาเครื่องยนต์หลัก)
*Focus: Building the intelligence (เน้นการสร้างความฉลาดให้กับระบบ)*

* **Computer Vision Pipeline (ลำดับการประมวลผลภาพ):** 
    * Implement **CameraX** for image capture. *(ใช้ CameraX สำหรับการถ่ายภาพ)*
    * Use **OpenCV** for **Perspective Warp** (adjusting tilted images) and **Edge Detection**. *(ใช้ OpenCV สำหรับการปรับระนาบภาพและตรวจจับขอบ)*
* **Data Extraction (การดึงข้อมูล):** 
    * Use **ML Kit** for **Barcode Scanning** (QR Code) and **OCR** (Text Recognition). *(ใช้ ML Kit สำหรับการสแกนบาร์โค้ดและอ่านข้อความ)*
    * Implement **Regex-based Parsing** to extract amounts, dates, and merchant names. *(ใช้การเขียนโปรแกรมแบบ Regex เพื่อดึงข้อมูลยอดเงิน วันที่ และชื่อร้านค้า)*
* **Verification Logic (ตรรกะการตรวจสอบ):** 
    * Develop a **Multi-provider Architecture** to verify transactions via third-party APIs. *(พัฒนาโครงสร้างแบบหลายผู้ให้บริการเพื่อตรวจสอบธุรกรรมผ่าน API ภายนอก)*
    * Implement **Fraud Detection** by cross-referencing OCR data with QR payload. *(สร้างระบบตรวจจับการทุจริตโดยการเปรียบเทียบข้อมูลจาก OCR และข้อมูลใน QR Code)*

---

## 3. UI & State Management (การสร้างหน้าจอและการจัดการสถานะ)
*Focus: User experience and data flow (เน้นประสบการณ์ผู้ใช้และการไหลของข้อมูล)*

* **Single-State Architecture (สถาปัตยกรรมสถานะเดี่ยว):** Use a central state holder in `MainApp.kt` to manage all app data, reducing complexity.
  *(ใช้ตัวจัดการสถานะกลางใน MainApp.kt เพื่อควบคุมข้อมูลทั้งหมดของแอป ช่วยลดความซับซ้อน)*
* **Responsive UI (หน้าจอที่ตอบสนองไว):** Build interactive components like **Charts** using **Jetpack Compose Canvas** with support for **Gestures** (swiping/tapping).
  *(สร้างส่วนประกอบที่โต้ตอบได้ เช่น กราฟ โดยใช้ Compose Canvas ที่รองรับการสัมผัสและการปัดหน้าจอ)*
* **Seamless Navigation (การเปลี่ยนหน้าจอที่ลื่นไหล):** Implement navigation based on state changes for high performance.
  *(ใช้การเปลี่ยนหน้าจอตามการเปลี่ยนสถานะของข้อมูลเพื่อให้แอปทำงานได้รวดเร็ว)*

---

## 4. Background Services (การทำงานเบื้องหลัง)
*Focus: Automation (เน้นการทำงานอัตโนมัติ)*

* **Notification Listening (การดักจับการแจ้งเตือน):** Develop a service to automatically capture bank transaction notifications.
  *(พัฒนาบริการสำหรับดักจับการแจ้งเตือนธุรกรรมจากธนาคารโดยอัตโนมัติ)*
* **Persistent Background Task (การทำงานเบื้องหลังที่ต่อเนื่อง):** Use a **Foreground Service** to ensure the app remains active and continues monitoring.
  *(ใช้ Foreground Service เพื่อให้มั่นใจว่าแอปยังคงทำงานและเฝ้าติดตามข้อมูลอยู่ตลอดเวลา)*

---

## 5. Quality Assurance & Deployment (การควบคุมคุณภาพและการนำไปใช้งาน)
*Focus: Stability and reliability (เน้นความเสถียรและความน่าเชื่อถือ)*

* **Rigorous Testing (การทดสอบอย่างเข้มงวด):** 
    * **Unit Testing:** Validate parsing logic and edge cases. *(ทดสอบความถูกต้องของตรรกะการดึงข้อมูลและกรณีที่ผิดปกติ)*
    * **Manual Verification:** Test on physical devices to ensure UI stability and gesture responsiveness. *(ทดสอบบนเครื่องจริงเพื่อยืนยันความเสถียรของหน้าจอและการตอบสนองต่อการสัมผัส)*
* **CI/CD Pipeline (กระบวนการทำงานอัตโนมัติ):** Use GitHub Actions to automate the build process and generate release versions.
  *(ใช้ GitHub Actions เพื่อทำให้การ Build และการออกเวอร์ชันใช้งานเป็นไปอย่างอัตโนมัติ)*

---
**[End of Document]**