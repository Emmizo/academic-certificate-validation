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
            model.addAttribute("studentRequest", studentRequest);
            model.addAttribute("error", err);
            return "admin/student-form";
        }

        boolean exists = studentRepository.findByStudentNumber(studentRequest.getStudentNumber()).isPresent();
        if (exists) {
            model.addAttribute("studentRequest", studentRequest);
            model.addAttribute("error", "A student with this Student ID already exists.");
            return "admin/student-form";
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
}

