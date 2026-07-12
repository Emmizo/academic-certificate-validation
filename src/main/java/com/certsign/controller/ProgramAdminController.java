package com.certsign.controller;

import com.certsign.dto.ProgramRequest;
import com.certsign.model.LicenceType;
import com.certsign.model.Program;
import com.certsign.repository.LicenceTypeRepository;
import com.certsign.repository.ProgramRepository;
import java.util.Comparator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProgramAdminController {

    private final ProgramRepository programRepository;
    private final LicenceTypeRepository licenceTypeRepository;

    public ProgramAdminController(ProgramRepository programRepository,
                                  LicenceTypeRepository licenceTypeRepository) {
        this.programRepository = programRepository;
        this.licenceTypeRepository = licenceTypeRepository;
    }

    @GetMapping("/admin/programs")
    public String listPrograms(@RequestParam(value = "edit", required = false) Long editId, Model model) {
        populateProgramsModel(model);
        if (!model.containsAttribute("programRequest")) {
            ProgramRequest req = new ProgramRequest();
            if (editId != null) {
                programRepository.findById(editId).ifPresent(p -> {
                    req.setName(p.getName());
                    if (p.getLicenceType() != null) {
                        req.setLicenceTypeId(p.getLicenceType().getId());
                    }
                    model.addAttribute("isEdit", true);
                    model.addAttribute("editId", p.getId());
                });
            }
            model.addAttribute("programRequest", req);
        } else {
            if (editId != null) {
                model.addAttribute("isEdit", true);
                model.addAttribute("editId", editId);
            }
        }
        return "admin/programs";
    }

    @PostMapping("/admin/programs")
    public String createProgram(@ModelAttribute ProgramRequest programRequest, RedirectAttributes redirectAttributes) {
        if (programRequest == null || isBlank(programRequest.getName())) {
            redirectAttributes.addFlashAttribute("error", "Program name is required.");
            return "redirect:/admin/programs";
        }
        String normalizedName = programRequest.getName().trim();
        if (programRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "This program already exists.");
            return "redirect:/admin/programs";
        }
        var licenceType = programRequest.getLicenceTypeId() != null
                ? licenceTypeRepository.findById(programRequest.getLicenceTypeId()).orElse(null)
                : null;
        Program program = Program.builder()
                .name(normalizedName)
                .licenceType(licenceType)
                .active(true)
                .build();
        programRepository.save(program);
        redirectAttributes.addFlashAttribute("success", "Program '" + normalizedName + "' created successfully.");
        return "redirect:/admin/programs";
    }

    @PostMapping("/admin/programs/{id}/edit")
    public String editProgram(
            @PathVariable("id") Long id,
            @ModelAttribute ProgramRequest programRequest,
            RedirectAttributes redirectAttributes
    ) {
        if (programRequest == null || isBlank(programRequest.getName())) {
            redirectAttributes.addFlashAttribute("error", "Program name is required.");
            return "redirect:/admin/programs?edit=" + id;
        }
        String normalizedName = programRequest.getName().trim();

        Program program = programRepository.findById(id).orElse(null);
        if (program == null) {
            redirectAttributes.addFlashAttribute("error", "Program not found.");
            return "redirect:/admin/programs";
        }

        var existingOpt = programRepository.findByNameIgnoreCase(normalizedName);
        if (existingOpt.isPresent() && !existingOpt.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Another program with this name already exists.");
            return "redirect:/admin/programs?edit=" + id;
        }

        var licenceType = programRequest.getLicenceTypeId() != null
                ? licenceTypeRepository.findById(programRequest.getLicenceTypeId()).orElse(null)
                : null;

        program.setName(normalizedName);
        program.setLicenceType(licenceType);
        programRepository.save(program);

        redirectAttributes.addFlashAttribute("success", "Program updated successfully.");
        return "redirect:/admin/programs";
    }

    @PostMapping("/admin/licence-types")
    public String createLicenceType(@RequestParam("name") String name,
                                    @RequestParam(value = "description", required = false) String description,
                                    RedirectAttributes redirectAttributes) {
        if (isBlank(name)) {
            redirectAttributes.addFlashAttribute("licenceTypeError", "Licence type name is required.");
            return "redirect:/admin/programs";
        }
        String normalizedName = name.trim();
        if (licenceTypeRepository.findByNameIgnoreCase(normalizedName).isPresent()) {
            redirectAttributes.addFlashAttribute("licenceTypeError", "This licence type already exists.");
            return "redirect:/admin/programs";
        }
        licenceTypeRepository.save(LicenceType.builder()
                .name(normalizedName)
                .description(isBlank(description) ? null : description.trim())
                .active(true)
                .build());
        redirectAttributes.addFlashAttribute("licenceTypeSuccess", "Licence type created.");
        return "redirect:/admin/programs";
    }

    @PostMapping("/admin/licence-types/{id}/activate")
    public String activateLicenceType(@PathVariable("id") Long id) {
        licenceTypeRepository.findById(id).ifPresent(licenceType -> {
            licenceType.setActive(true);
            licenceTypeRepository.save(licenceType);
        });
        return "redirect:/admin/programs";
    }

    @PostMapping("/admin/licence-types/{id}/deactivate")
    public String deactivateLicenceType(@PathVariable("id") Long id) {
        licenceTypeRepository.findById(id).ifPresent(licenceType -> {
            licenceType.setActive(false);
            licenceTypeRepository.save(licenceType);
        });
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

    @PostMapping("/admin/programs/{id}/delete")
    public String deleteProgram(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        var program = programRepository.findById(id).orElse(null);
        if (program == null) {
            redirectAttributes.addFlashAttribute("error", "Program not found.");
            return "redirect:/admin/programs";
        }
        if (!program.getCertificates().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "This program has associated certificate records and cannot be deleted permanently.");
            return "redirect:/admin/programs";
        }
        programRepository.delete(program);
        redirectAttributes.addFlashAttribute("success", "Program '" + program.getName() + "' deleted permanently.");
        return "redirect:/admin/programs";
    }

    private String renderWithError(Model model, String error) {
        populateProgramsModel(model);
        model.addAttribute("programRequest", new ProgramRequest());
        model.addAttribute("error", error);
        return "admin/programs";
    }

    private void populateProgramsModel(Model model) {
        var programs = programRepository.findAll();
        programs.sort(Comparator.comparing(Program::isActive).reversed()
                .thenComparing(Program::getName, String.CASE_INSENSITIVE_ORDER));
        var allLicenceTypes = licenceTypeRepository.findAll();
        allLicenceTypes.sort(Comparator.comparing(LicenceType::getName, String.CASE_INSENSITIVE_ORDER));
        model.addAttribute("programs", programs);
        model.addAttribute("licenceTypes", licenceTypeRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("allLicenceTypes", allLicenceTypes);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
