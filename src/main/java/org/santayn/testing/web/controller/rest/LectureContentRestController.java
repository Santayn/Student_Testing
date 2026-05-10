package org.santayn.testing.web.controller.rest;

import jakarta.validation.Valid;
import org.santayn.testing.service.LectureMaterialService;
import org.santayn.testing.service.LectureTestLinkService;
import org.santayn.testing.web.dto.platform.ApiResponses;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping({"/api/lectures", "/api/v1/lectures"})
public class LectureContentRestController {

    private final LectureTestLinkService lectureTestLinkService;
    private final LectureMaterialService lectureMaterialService;

    public LectureContentRestController(LectureTestLinkService lectureTestLinkService,
                                        LectureMaterialService lectureMaterialService) {
        this.lectureTestLinkService = lectureTestLinkService;
        this.lectureMaterialService = lectureMaterialService;
    }

    @GetMapping("/{id}/tests")
    public List<ApiResponses.TestResponse> linkedTests(@PathVariable Integer id) {
        return ApiResponses.list(lectureTestLinkService.findTestsByLectureId(id), ApiResponses::test);
    }

    @PutMapping("/{id}/tests")
    public List<ApiResponses.TestResponse> replaceLinkedTests(@PathVariable Integer id,
                                                              @Valid @RequestBody LectureTestsRequest request) {
        return ApiResponses.list(
                lectureTestLinkService.replaceLectureTests(id, request.testIds()),
                ApiResponses::test
        );
    }

    @GetMapping("/{lectureId}/materials")
    public List<ApiResponses.LectureMaterialResponse> materials(@PathVariable Integer lectureId) {
        return ApiResponses.list(lectureMaterialService.findByLectureId(lectureId), ApiResponses::lectureMaterial);
    }

    @PostMapping(value = "/{lectureId}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<ApiResponses.LectureMaterialResponse> uploadMaterials(@PathVariable Integer lectureId,
                                                                      @RequestParam("files") List<MultipartFile> files) {
        lectureMaterialService.upload(lectureId, files);
        return ApiResponses.list(lectureMaterialService.findByLectureId(lectureId), ApiResponses::lectureMaterial);
    }

    @DeleteMapping("/{lectureId}/materials/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaterial(@PathVariable Integer lectureId, @PathVariable Integer materialId) {
        lectureMaterialService.delete(lectureId, materialId);
    }

    @GetMapping("/{lectureId}/materials/{materialId}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Integer lectureId,
                                                     @PathVariable Integer materialId) {
        LectureMaterialService.StoredLectureMaterial storedMaterial = lectureMaterialService.load(lectureId, materialId);
        Resource resource = new FileSystemResource(storedMaterial.path());
        MediaType mediaType = MediaTypeFactory.getMediaType(storedMaterial.material().getFileName())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(storedMaterial.material().getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(mediaType)
                .contentLength(storedMaterial.material().getSizeBytes())
                .body(resource);
    }

    public record LectureTestsRequest(List<Integer> testIds) {
    }
}
