// SDLC Phase: Implementation
// Component: StudentAdminController
// Requirements covered: FR-02, FR-03, NFR-03, NFR-04
// Description: Handles admin pages for managing students
package com.certsign.controller;

import com.certsign.dto.StudentRequest;
import com.certsign.model.Student;
import com.certsign.model.StudentStatus;
import com.certsign.repository.StudentRepository;
import java.util.Comparator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class StudentAdminController {

    private final StudentRepository studentRepository;

    /**
     * Creates the student admin controller backed by the given repository.
     */
    public StudentAdminController(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Lists all students, sorted alphabetically by full name, for use in the admin UI.
     */
    @GetMapping("/admin/students")
    public String listStudents(Model model) {
        var students = studentRepository.findAll();
        students.sort(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("students", students);
        return "admin/students";
    }

    /**
     * Shows the form for creating a new student record.
     */
    @GetMapping("/admin/students/new")
    public String newStudentForm(Model model) {
        model.addAttribute("studentRequest", new StudentRequest());
        model.addAttribute("error", null);
        model.addAttribute("formTitle", "Create Student");
        model.addAttribute("formDescription", "Register a student once, then reuse them when issuing certificates.");
        model.addAttribute("formAction", "/admin/students");
        model.addAttribute("submitLabel", "Save Student");
        return "admin/student-form";
    }

    /**
     * Handles submission of the new‑student form, validating input and preventing
     * duplicate student numbers before saving.
     */
    @PostMapping("/admin/students")
    public String createStudent(@ModelAttribute StudentRequest studentRequest, Model model) {
        String err = validate(studentRequest);
        if (err != null) {
            return buildFormModel(model, studentRequest, err, null);
        }

        boolean exists = studentRepository.findByStudentNumber(studentRequest.getStudentNumber()).isPresent();
        if (exists) {
            return buildFormModel(model, studentRequest, "A student with this Student ID already exists.", null);
        }

        Student s = Student.builder()
                .studentNumber(studentRequest.getStudentNumber())
                .fullName(studentRequest.getFullName())
                .email(studentRequest.getEmail())
                .nationalId(studentRequest.getNationalId())
                .dateOfBirth(studentRequest.getDateOfBirth())
                .status(StudentStatus.ACTIVE)
                .build();
        studentRepository.save(s);

        return "redirect:/admin/students";
    }

    /**
     * Shows the edit form for an existing student.
     */
    @GetMapping("/admin/students/{id}/edit")
    public String editStudentForm(@PathVariable("id") Long id, Model model) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        StudentRequest request = new StudentRequest();
        request.setStudentNumber(student.getStudentNumber());
        request.setFullName(student.getFullName());
        request.setEmail(student.getEmail());
        request.setNationalId(student.getNationalId());
        request.setDateOfBirth(student.getDateOfBirth());

        return buildFormModel(model, request, null, id);
    }

    /**
     * Handles submission of the edit form and updates the student record.
     */
    @PostMapping("/admin/students/{id}")
    public String updateStudent(
            @PathVariable("id") Long id,
            @ModelAttribute StudentRequest studentRequest,
            Model model
    ) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        String err = validate(studentRequest);
        if (err != null) {
            return buildFormModel(model, studentRequest, err, id);
        }

        var existingByStudentNumber = studentRepository.findByStudentNumber(studentRequest.getStudentNumber());
        if (existingByStudentNumber.isPresent() && !existingByStudentNumber.get().getId().equals(id)) {
            return buildFormModel(model, studentRequest, "A student with this Student ID already exists.", id);
        }

        student.setStudentNumber(studentRequest.getStudentNumber());
        student.setFullName(studentRequest.getFullName());
        student.setEmail(studentRequest.getEmail());
        student.setNationalId(studentRequest.getNationalId());
        student.setDateOfBirth(studentRequest.getDateOfBirth());
        studentRepository.save(student);

        return "redirect:/admin/students";
    }

    /**
     * Marks the specified student as INACTIVE so they can no longer be selected
     * for new certificate issuance.
     */
    @PostMapping("/admin/students/{id}/deactivate")
    public String deactivateStudent(@PathVariable("id") Long id) {
        studentRepository.findById(id).ifPresent(s -> {
            s.setStatus(StudentStatus.INACTIVE);
            studentRepository.save(s);
        });
        return "redirect:/admin/students";
    }

    /**
     * Marks the specified student as ACTIVE so they can be used again for issuance.
     */
    @PostMapping("/admin/students/{id}/activate")
    public String activateStudent(@PathVariable("id") Long id) {
        studentRepository.findById(id).ifPresent(s -> {
            s.setStatus(StudentStatus.ACTIVE);
            studentRepository.save(s);
        });
        return "redirect:/admin/students";
    }

    /**
     * Performs basic validation for the student creation request payload.
     */
    private String validate(StudentRequest r) {
        if (r == null) return "Invalid request";
        if (isBlank(r.getStudentNumber())) return "Student ID is required";
        if (isBlank(r.getFullName())) return "Full name is required";
        return null;
    }

    /**
     * Returns {@code true} if the supplied string is null or consists only of whitespace.
     */
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String buildFormModel(Model model, StudentRequest studentRequest, String error, Long editId) {
        boolean editMode = editId != null;
        model.addAttribute("studentRequest", studentRequest);
        model.addAttribute("error", error);
        model.addAttribute("formTitle", editMode ? "Edit Student" : "Create Student");
        model.addAttribute(
                "formDescription",
                editMode
                        ? "Update student details used during certificate issuance."
                        : "Register a student once, then reuse them when issuing certificates."
        );
        model.addAttribute("formAction", editMode ? "/admin/students/" + editId : "/admin/students");
        model.addAttribute("submitLabel", editMode ? "Update Student" : "Save Student");
        return "admin/student-form";
    }
}

