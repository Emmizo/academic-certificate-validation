package com.certsign;

import com.certsign.model.Program;
import com.certsign.model.Student;
import com.certsign.model.StudentStatus;
import com.certsign.model.User;
import com.certsign.model.UserRole;
import com.certsign.repository.ProgramRepository;
import com.certsign.repository.StudentRepository;
import com.certsign.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           ProgramRepository programRepository,
                           StudentRepository studentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedPrograms();
        seedStudents();
    }

    private void seedUsers() {
        record Seed(String username, String email, String password, UserRole role) {}

        List<Seed> seeds = List.of(
            new Seed(adminUsername,  "admin@tumbacollege.rw",     adminPassword,   UserRole.ADMIN),
            new Seed("principal",    "principal@tumbacollege.rw", "Principal@123", UserRole.PRINCIPAL),
            new Seed("secretary",    "secretary@tumbacollege.rw", "Secretary@123", UserRole.SECRETARY),
            new Seed("signer",       "signer@tumbacollege.rw",    "Signer@123",    UserRole.SIGNER),
            new Seed("verifier",     "verifier@tumbacollege.rw",  "Verifier@123",  UserRole.VERIFIER)
        );

        for (Seed s : seeds) {
            if (userRepository.findByUsername(s.username()).isEmpty()) {
                userRepository.save(User.builder()
                        .username(s.username())
                        .email(s.email())
                        .passwordHash(passwordEncoder.encode(s.password()))
                        .role(s.role())
                        .build());
                System.out.printf("[Seed] Created user: %s / %s%n", s.username(), s.password());
            }
        }
    }

    private void seedPrograms() {
        List<String> programs = List.of(
            "Bachelor of Science in Computer Science",
            "Bachelor of Business Administration",
            "Bachelor of Education",
            "Bachelor of Engineering in Civil Engineering",
            "Diploma in Information Technology",
            "Diploma in Accounting",
            "Certificate in Project Management"
        );

        for (String name : programs) {
            if (programRepository.findByNameIgnoreCase(name).isEmpty()) {
                programRepository.save(Program.builder()
                        .name(name)
                        .active(true)
                        .build());
                System.out.printf("[Seed] Created program: %s%n", name);
            }
        }
    }

    private void seedStudents() {
        record StudentSeed(String number, String name, String email, LocalDate dob) {}

        List<StudentSeed> students = List.of(
            new StudentSeed("TC2024001", "Alice Uwimana",   "alice.uwimana@example.rw",   LocalDate.of(2000, 3, 15)),
            new StudentSeed("TC2024002", "Bob Nkurunziza",  "bob.nkurunziza@example.rw",  LocalDate.of(1999, 7, 22)),
            new StudentSeed("TC2024003", "Claire Mukamana", "claire.mukamana@example.rw", LocalDate.of(2001, 1, 10)),
            new StudentSeed("TC2024004", "David Habimana",  "david.habimana@example.rw",  LocalDate.of(2000, 11, 5)),
            new StudentSeed("TC2024005", "Esther Uwineza",  "esther.uwineza@example.rw",  LocalDate.of(1998, 6, 30))
        );

        for (StudentSeed s : students) {
            if (studentRepository.findByStudentNumber(s.number()).isEmpty()) {
                studentRepository.save(Student.builder()
                        .studentNumber(s.number())
                        .fullName(s.name())
                        .email(s.email())
                        .dateOfBirth(s.dob())
                        .status(StudentStatus.ACTIVE)
                        .build());
                System.out.printf("[Seed] Created student: %s (%s)%n", s.name(), s.number());
            }
        }
    }
}
