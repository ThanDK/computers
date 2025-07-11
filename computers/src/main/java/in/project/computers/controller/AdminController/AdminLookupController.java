package in.project.computers.controller.AdminController;

import in.project.computers.service.componentService.LookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/lookups")
@RequiredArgsConstructor
public class AdminLookupController {

    private final LookupService lookupService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLookups() {
        return ResponseEntity.ok(lookupService.getAllLookups());
    }
}
