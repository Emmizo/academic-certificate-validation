package com.certsign;

import com.certsign.model.Program;
import com.certsign.model.LicenceType;
import com.certsign.model.Student;
import com.certsign.model.StudentStatus;
import com.certsign.model.User;
import com.certsign.model.UserRole;
import com.certsign.repository.LicenceTypeRepository;
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
    private final LicenceTypeRepository licenceTypeRepository;
    private final ProgramRepository programRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           LicenceTypeRepository licenceTypeRepository,
                           ProgramRepository programRepository,
                           StudentRepository studentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.licenceTypeRepository = licenceTypeRepository;
        this.programRepository = programRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedLicenceTypes();
        seedPrograms();
        seedStudents();
    }

    private void seedUsers() {
        record Seed(String username, String fullName, String email, String password, UserRole role) {}

        List<Seed> seeds = List.of(
            new Seed(adminUsername,  "System Admin",       "admin@tumbacollege.rw",     adminPassword,   UserRole.ADMIN),
            new Seed("principal",    "Principal Officer",  "principal@tumbacollege.rw", "Principal@123", UserRole.PRINCIPAL),
            new Seed("secretary",    "Secretary Officer",  "secretary@tumbacollege.rw", "Secretary@123", UserRole.SECRETARY)
        );

        for (Seed s : seeds) {
            if (userRepository.findByUsername(s.username()).isEmpty() && !userRepository.existsByRole(s.role())) {
                userRepository.save(User.builder()
                        .username(s.username())
                        .fullName(s.fullName())
                        .email(s.email())
                        .passwordHash(passwordEncoder.encode(s.password()))
                        .role(s.role())
                        .build());
                System.out.printf("[Seed] Created user: %s / %s%n", s.username(), s.password());
            }
        }
    }

    private void seedLicenceTypes() {
        record Seed(String name, String description) {}

        List<Seed> types = List.of(
                new Seed("Bachelor", "Undergraduate degree qualification"),
                new Seed("Diploma", "Diploma-level qualification"),
                new Seed("Certificate", "Short professional certificate qualification"),
                new Seed("Training Program", "Technical training program qualification")
        );

        for (Seed s : types) {
            if (licenceTypeRepository.findByNameIgnoreCase(s.name()).isEmpty()) {
                licenceTypeRepository.save(LicenceType.builder()
                        .name(s.name())
                        .description(s.description())
                        .active(true)
                        .build());
                System.out.printf("[Seed] Created licence type: %s%n", s.name());
            }
        }
    }

    private void seedPrograms() {
        record ProgramSeed(String name, String licenceTypeName) {}

        List<ProgramSeed> programs = List.of(
            new ProgramSeed("Electricity", "Training Program"),
            new ProgramSeed("Electronic", "Training Program"),
            new ProgramSeed("IT", "Training Program"),
            new ProgramSeed("Energy", "Training Program")
        );

        for (ProgramSeed s : programs) {
            LicenceType licenceType = licenceTypeRepository.findByNameIgnoreCase(s.licenceTypeName())
                    .orElse(null);
            var existing = programRepository.findByNameIgnoreCase(s.name());
            if (existing.isPresent()) {
                Program program = existing.get();
                if (program.getLicenceType() == null && licenceType != null) {
                    program.setLicenceType(licenceType);
                    programRepository.save(program);
                }
            } else {
                programRepository.save(Program.builder()
                        .name(s.name())
                        .licenceType(licenceType)
                        .active(true)
                        .build());
                System.out.printf("[Seed] Created program: %s%n", s.name());
            }
        }
    }

    private void seedStudents() {
        record StudentSeed(String number, String name, String email, LocalDate dob, String attendedProgram, String academicYear) {}

        List<StudentSeed> students = List.of(
            new StudentSeed("TC2024001", "Alice Uwimana",   "alice.uwimana@example.rw",   LocalDate.of(2000, 3, 15), "IT", "2023-2024"),
            new StudentSeed("TC2024002", "Bob Nkurunziza",  "bob.nkurunziza@example.rw",  LocalDate.of(1999, 7, 22), "IT", "2023-2024"),
            new StudentSeed("TC2024003", "Claire Mukamana", "claire.mukamana@example.rw", LocalDate.of(2001, 1, 10), "Electricity", "2023-2024"),
            new StudentSeed("TC2024004", "David Habimana",  "david.habimana@example.rw",  LocalDate.of(2000, 11, 5), "Electronic", "2023-2024"),
            new StudentSeed("TC2024005", "Esther Uwineza",  "esther.uwineza@example.rw",  LocalDate.of(1998, 6, 30), "Energy", "2023-2024")
        );

        for (StudentSeed s : students) {
            var existingOpt = studentRepository.findByStudentNumber(s.number());
            if (existingOpt.isEmpty()) {
                studentRepository.save(Student.builder()
                        .studentNumber(s.number())
                        .fullName(s.name())
                        .email(s.email())
                        .dateOfBirth(s.dob())
                        .attendedProgram(s.attendedProgram())
                        .academicYear(s.academicYear())
                        .status(StudentStatus.ACTIVE)
                        .build());
                System.out.printf("[Seed] Created student: %s (%s)%n", s.name(), s.number());
            } else {
                Student existing = existingOpt.get();
                boolean changed = false;
                if (existing.getAttendedProgram() == null || existing.getAttendedProgram().trim().isEmpty() ||
                        existing.getAttendedProgram().contains("Computer Science") ||
                        existing.getAttendedProgram().contains("Information Technology") ||
                        existing.getAttendedProgram().contains("Business Administration") ||
                        existing.getAttendedProgram().contains("Civil Engineering") ||
                        existing.getAttendedProgram().contains("Accounting")) {
                    existing.setAttendedProgram(s.attendedProgram());
                    changed = true;
                }
                if (existing.getAcademicYear() == null || existing.getAcademicYear().trim().isEmpty() ||
                        existing.getAcademicYear().contains("/")) {
                    existing.setAcademicYear(s.academicYear());
                    changed = true;
                }
                if (changed) {
                    studentRepository.save(existing);
                    System.out.printf("[Seed] Updated existing student program/year: %s (%s)%n", existing.getFullName(), existing.getStudentNumber());
                }
            }
        }
    }
}
