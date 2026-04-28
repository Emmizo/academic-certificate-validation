package com.certsign.controller;

import com.certsign.dto.ProgramRequest;
import com.certsign.model.Program;
import com.certsign.repository.ProgramRepository;
import java.util.Comparator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProgramAdminController {

    private final ProgramRepository programRepository;

    public ProgramAdminController(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    @GetMapping("/admin/programs")
    public String listPrograms(Model model) {
        var programs = programRepository.findAll();
        programs.sort(Comparator.comparing(Program::getName, String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("programs", programs);
        model.addAttribute("programRequest", new ProgramRequest());
        model.addAttribute("error", null);
        return "admin/programs";
    }

    @PostMapping("/admin/programs")
    public String createProgram(@ModelAttribute ProgramRequest programRequest, Model model) {
        if (programRequest == null || isBlank(programRequest.getName())) {
            return renderWithError(model, "Program name is required.");
        }
        String normalizedName = programRequest.getName().trim();
        if (programRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            return renderWithError(model, "This program already exists.");
        }
        Program program = Program.builder()
                .name(normalizedName)
                .active(true)
                .build();
        programRepository.save(program);
        return "redirect:/admin/programs";
    }

    @PostMapping("/admin/programs/{id}/activate")
    public String activateProgram(@PathVariable("id") Long id) {
        programRepository.findById(id).ifPresent(program -> {
            program.setActive(true);
            programRepository.save(program);
        });
        return "redirect:/admin/programs";
    }

    @PostMapping("/admin/programs/{id}/deactivate")
    public String deactivateProgram(@PathVariable("id") Long id) {
        programRepository.findById(id).ifPresent(program -> {
            program.setActive(false);
            programRepository.save(program);
        });
        return "redirect:/admin/programs";
    }

    private String renderWithError(Model model, String error) {
        var programs = programRepository.findAll();
        programs.sort(Comparator.comparing(Program::getName, String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("programs", programs);
        model.addAttribute("programRequest", new ProgramRequest());
        model.addAttribute("error", error);
        return "admin/programs";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
