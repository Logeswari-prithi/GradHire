# 🎓 GradHire - College Placement & Student Monitoring Portal

## ✅ Quick Start (3 Steps)

### Step 1: Setup MySQL
```sql
CREATE DATABASE gradhire_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Step 2: Edit `src/main/resources/application.properties`
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 3: Open in IntelliJ IDEA
1. **File → Open** → select the `gradhire` folder
2. IntelliJ detects Maven → click **"Load Maven Project"** when prompted
3. Wait for dependencies to download (first time only ~2-3 min)
4. Open `src/main/java/com/gradhire/GradHireApplication.java`
5. Click ▶ **Run**
6. Open browser: **http://localhost:8080**

---

## 🔑 Login Credentials

| Role    | Email                  | Password  | First Login |
|---------|------------------------|-----------|-------------|
| Admin   | admin@gradhire.edu     | Admin@123 | Direct in   |
| Staff   | Create via Admin panel | Admin@123 | Must change |
| Student | Create via Admin panel | Admin@123 | Must change |

---

## 🚀 Features by Role

### Admin
- Analytics dashboard with Chart.js charts
- Create Staff & Student accounts
- Enable/disable user accounts
- Reset passwords
- Bulk upload students from Excel (.xlsx)
- View and manage notifications
- View error reports from users

### Staff
- Search and view students
- Add and manage placements
- Update placement round statuses
- Bulk Excel upload

### Student
- View & edit own profile (CGPA, skills, education)
- View placement history with round details
- Download PDF report (iText)
- Report issues to admin

---

## 📁 Project Structure

```
src/main/java/com/gradhire/
├── GradHireApplication.java       ← Entry point
├── config/JpaConfig.java          ← JPA config
├── controller/                    ← REST endpoints
├── dto/                           ← Request/response objects
├── entity/                        ← JPA entities (pure jakarta.persistence)
├── exception/                     ← Global error handling
├── initializer/DataInitializer.java ← Auto-creates admin + batches
├── repository/                    ← Spring Data JPA repos
├── security/                      ← JWT + Spring Security
└── service/                       ← Business logic

src/main/resources/
├── application.properties
└── static/
    ├── index.html                  ← Login page
    ├── css/main.css
    ├── js/app.js, admin.js
    └── pages/
        ├── admin-dashboard.html
        ├── staff-dashboard.html
        ├── student-dashboard.html
        └── change-password.html
```

---

## 🗄️ Database Tables (Auto-created)

| Table           | Purpose                          |
|-----------------|----------------------------------|
| users           | All accounts (admin/staff/student)|
| batches         | Year-wise batches (2025/2026/2027)|
| students        | Student profiles                 |
| staff           | Staff profiles                   |
| placements      | Company applications             |
| round_statuses  | APTITUDE/TECHNICAL/HR/FINAL rounds|
| notifications   | System notifications             |
| error_reports   | User-submitted error reports     |

---

## 📊 Excel Upload Format

| Column A | Column B  | Column C | Column D   | Column E   | Column F |
|----------|-----------|----------|------------|------------|----------|
| RegNo    | FullName  | Email    | Department | BatchYear  | Phone    |

---

## 🌐 Key API Endpoints

```
POST   /api/auth/login
POST   /api/auth/change-password
GET    /api/admin/analytics
POST   /api/admin/users
GET    /api/student
GET    /api/student/search?q=...
POST   /api/placements
GET    /api/placements/student/{id}
GET    /api/reports/student/{id}/pdf
POST   /api/staff/upload-students
```

---

## 🚢 Build for Production

```bash
mvn clean package -DskipTests
java -jar target/gradhire-1.0.0.jar
```
