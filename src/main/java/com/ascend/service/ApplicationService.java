package com.ascend.service;

import com.ascend.domain.Application;
import com.ascend.dto.ApplicationDto;
import com.ascend.dto.ApplicationRequest;
import com.ascend.repo.ApplicationRepository;
import com.ascend.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    public static final List<String> STATUSES = List.of("Wishlist", "Applied", "Online Test", "Interview", "Offer", "Rejected");

    private final ApplicationRepository apps;
    private final SettingsService settings;

    @Transactional(readOnly = true)
    public List<ApplicationDto> list(UUID userId) {
        return apps.findByUserId(userId).stream().map(ApplicationService::toDto).toList();
    }

    @Transactional
    public ApplicationDto create(UUID userId, ApplicationRequest r) {
        Application a = new Application();
        a.setUserId(userId);
        fill(a, userId, r);
        return toDto(apps.save(a));
    }

    @Transactional
    public ApplicationDto update(UUID userId, UUID id, ApplicationRequest r) {
        Application a = apps.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Application"));
        fill(a, userId, r);
        return toDto(apps.save(a));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Application a = apps.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Application"));
        apps.delete(a);
    }

    private void fill(Application a, UUID userId, ApplicationRequest r) {
        String status = r.status() == null || r.status().isBlank() ? "Wishlist" : r.status();
        if (!STATUSES.contains(status)) throw ApiException.badRequest("status must be one of " + STATUSES);
        a.setCompany(r.company().trim());
        a.setRole(r.role() == null ? "" : r.role().trim());
        a.setStatus(status);
        a.setDate(r.date() != null ? r.date() : settings.today(userId));
        a.setLink(r.link() == null ? "" : r.link().trim());
        a.setNotes(r.notes() == null ? "" : r.notes());
    }

    public static ApplicationDto toDto(Application a) {
        return new ApplicationDto(a.getId().toString(), a.getCompany(), a.getRole(), a.getStatus(),
                a.getDate() == null ? "" : a.getDate().toString(), a.getLink(), a.getNotes());
    }
}
