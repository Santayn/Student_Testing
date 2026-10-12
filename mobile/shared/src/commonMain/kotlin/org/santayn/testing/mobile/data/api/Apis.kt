package org.santayn.testing.mobile.data.api

import io.ktor.http.HttpMethod
import org.santayn.testing.mobile.core.network.ApiClient
import org.santayn.testing.mobile.core.network.ApiTimeouts
import kotlinx.io.RawSink
import org.santayn.testing.mobile.core.network.DownloadedFile
import org.santayn.testing.mobile.core.network.UploadFile
import org.santayn.testing.mobile.data.model.*

/*
 * API-классы — один к одному с frontend/src/api (файлы *.api.js).
 * Все пути относительно базового адреса `/api/v1`.
 */

class FacultiesApi(private val api: ApiClient) {
    suspend fun getAll(): List<FacultyDto> = api.get("/faculties")
    suspend fun getById(id: Int): FacultyDto = api.get("/faculties/$id")
    suspend fun create(request: FacultyRequest): FacultyDto = api.post("/faculties", request)
    suspend fun update(id: Int, request: FacultyRequest): FacultyDto = api.put("/faculties/$id", request)
    suspend fun remove(id: Int) = api.delete("/faculties/$id")
    suspend fun getSubjects(id: Int): List<SubjectDto> = api.get("/faculties/$id/subjects")
    suspend fun addSubject(facultyId: Int, subjectId: Int): Unit = api.postEmpty("/faculties/$facultyId/subjects/$subjectId")
    suspend fun removeSubject(facultyId: Int, subjectId: Int) = api.delete("/faculties/$facultyId/subjects/$subjectId")
}

class GroupsApi(private val api: ApiClient) {
    suspend fun getAll(facultyId: Int? = null): List<GroupDto> = api.get("/groups", mapOf("facultyId" to facultyId))
    suspend fun getById(id: Int): GroupDto = api.get("/groups/$id")
    suspend fun create(request: GroupRequest): GroupDto = api.post("/groups", request)
    suspend fun update(id: Int, request: GroupRequest): GroupDto = api.put("/groups/$id", request)
    suspend fun remove(id: Int) = api.delete("/groups/$id")
}

class SubjectsApi(private val api: ApiClient) {
    suspend fun getAll(facultyId: Int? = null): List<SubjectDto> = api.get("/subjects", mapOf("facultyId" to facultyId))
    suspend fun getById(id: Int): SubjectDto = api.get("/subjects/$id")
    suspend fun create(request: SubjectRequest): SubjectDto = api.post("/subjects", request)
    suspend fun update(id: Int, request: SubjectRequest): SubjectDto = api.put("/subjects/$id", request)
    suspend fun remove(id: Int) = api.delete("/subjects/$id")
}

class UsersApi(private val api: ApiClient) {
    suspend fun getAll(): List<UserDto> = api.get("/users")
    suspend fun getPeople(role: String? = null): List<PersonDto> = api.get("/users/people", mapOf("role" to role))
    suspend fun getPerson(personId: Int): PersonDto = api.get("/users/people/$personId")
    suspend fun createPerson(request: PersonRequest): PersonDto = api.post("/users/people", request)
    suspend fun updatePerson(personId: Int, request: PersonRequest): PersonDto = api.put("/users/people/$personId", request)
    suspend fun updateRoles(userId: Int, roleIds: List<Int>): UserDto = api.put("/users/$userId/roles", UserRolesRequest(roleIds))
    suspend fun updatePersonBinding(userId: Int, personId: Int?): Unit = api.put("/users/$userId/person", UserPersonRequest(personId))
    suspend fun setActive(userId: Int, active: Boolean): UserDto = api.put("/users/$userId/active", UserActiveRequest(active))
}

class RolesApi(private val api: ApiClient) {
    suspend fun getAll(): List<RoleDto> = api.get("/roles")
    suspend fun create(request: RoleRequest): RoleDto = api.post("/roles", request)
    suspend fun getPermissions(): List<PermissionDto> = api.get("/roles/permissions")
    suspend fun createPermission(request: PermissionRequest): PermissionDto = api.post("/roles/permissions", request)
    suspend fun setPermissions(roleId: Int, permissionIds: List<Int>): RoleDto =
        api.put("/roles/$roleId/permissions", RolePermissionsRequest(permissionIds))
}

class MembershipsApi(private val api: ApiClient) {
    suspend fun getGroupMemberships(
        groupId: Int? = null,
        personId: Int? = null,
        status: Int? = null,
        activeOnly: Boolean = true,
    ): List<MembershipDto> = api.get(
        "/memberships/groups",
        mapOf("groupId" to groupId, "personId" to personId, "status" to status, "activeOnly" to activeOnly),
    )

    suspend fun addPersonToGroup(groupId: Int, request: MembershipRequest): MembershipDto =
        api.post("/memberships/groups/$groupId", request)

    suspend fun updateGroupMembershipStatus(membershipId: Int, status: Int): MembershipDto =
        api.put("/memberships/groups/memberships/$membershipId/status", MembershipStatusRequest(status))

    suspend fun getSubjectMemberships(
        subjectId: Int? = null,
        personId: Int? = null,
        status: Int? = null,
        activeOnly: Boolean = true,
    ): List<MembershipDto> = api.get(
        "/memberships/subjects",
        mapOf("subjectId" to subjectId, "personId" to personId, "status" to status, "activeOnly" to activeOnly),
    )

    suspend fun getSubjectMembership(membershipId: Int): MembershipDto =
        api.get("/memberships/subjects/memberships/$membershipId")

    suspend fun addPersonToSubject(subjectId: Int, request: MembershipRequest): MembershipDto =
        api.post("/memberships/subjects/$subjectId", request)

    suspend fun updateSubjectMembershipStatus(membershipId: Int, status: Int): MembershipDto =
        api.put("/memberships/subjects/memberships/$membershipId/status", MembershipStatusRequest(status))

    suspend fun updateSubjectMembership(membershipId: Int, request: MembershipUpdateRequest): MembershipDto =
        api.put("/memberships/subjects/memberships/$membershipId", request)
}

class TeachingApi(private val api: ApiClient) {
    suspend fun getAssignments(
        groupId: Int? = null,
        subjectMembershipId: Int? = null,
        facultyId: Int? = null,
        loadTypeId: Int? = null,
        studyCourse: Int? = null,
        semester: Int? = null,
        academicYear: Int? = null,
        status: Int? = null,
    ): List<TeachingAssignmentDto> = api.get(
        "/teaching/assignments",
        mapOf(
            "groupId" to groupId,
            "subjectMembershipId" to subjectMembershipId,
            "facultyId" to facultyId,
            "loadTypeId" to loadTypeId,
            "studyCourse" to studyCourse,
            "semester" to semester,
            "academicYear" to academicYear,
            "status" to status,
        ),
    )

    suspend fun getAssignment(id: Int): TeachingAssignmentDto = api.get("/teaching/assignments/$id")
    suspend fun createAssignment(request: TeachingAssignmentRequest): TeachingAssignmentDto =
        api.post("/teaching/assignments", request)

    suspend fun updateAssignment(id: Int, request: TeachingAssignmentRequest): TeachingAssignmentDto =
        api.put("/teaching/assignments/$id", request)

    suspend fun getEnrollments(
        teachingAssignmentId: Int? = null,
        groupMembershipId: Int? = null,
        groupId: Int? = null,
        status: Int? = null,
    ): List<EnrollmentDto> = api.get(
        "/teaching/enrollments",
        mapOf(
            "teachingAssignmentId" to teachingAssignmentId,
            "groupMembershipId" to groupMembershipId,
            "groupId" to groupId,
            "status" to status,
        ),
    )

    suspend fun getLoadTypes(): List<LoadTypeDto> = api.get("/teaching/load-types")
    suspend fun createLoadType(request: LoadTypeRequest): LoadTypeDto = api.post("/teaching/load-types", request)
    suspend fun updateLoadType(id: Int, request: LoadTypeRequest): LoadTypeDto = api.put("/teaching/load-types/$id", request)
    suspend fun addLoadTypeToSubjectMembership(subjectMembershipId: Int, loadTypeId: Int): Unit =
        api.post("/teaching/subject-memberships/$subjectMembershipId/load-types", SubjectLoadTypeRequest(loadTypeId))
}

class CoursesApi(private val api: ApiClient) {
    suspend fun getTemplates(subjectId: Int? = null): List<CourseTemplateDto> =
        api.get("/courses/templates", mapOf("subjectId" to subjectId))

    suspend fun createTemplate(request: CourseTemplateRequest): CourseTemplateDto = api.post("/courses/templates", request)
    suspend fun updateTemplate(id: Int, request: CourseTemplateRequest): CourseTemplateDto =
        api.put("/courses/templates/$id", request)

    suspend fun removeTemplate(id: Int) = api.delete("/courses/templates/$id")
    suspend fun getVersions(templateId: Int): List<CourseVersionDto> = api.get("/courses/templates/$templateId/versions")
    suspend fun createVersion(templateId: Int, request: CourseVersionRequest): CourseVersionDto =
        api.post("/courses/templates/$templateId/versions", request)

    suspend fun updateVersion(versionId: Int, request: CourseVersionUpdateRequest): CourseVersionDto =
        api.put("/courses/versions/$versionId", request)

    suspend fun publishVersion(versionId: Int): CourseVersionDto = api.putEmpty("/courses/versions/$versionId/publish")
    suspend fun unpublishVersion(versionId: Int): CourseVersionDto = api.putEmpty("/courses/versions/$versionId/unpublish")
}

class LecturesApi(private val api: ApiClient) {
    suspend fun getAll(subjectId: Int? = null, subjectMembershipId: Int? = null, courseVersionId: Int? = null): List<LectureDto> =
        api.get(
            "/lectures",
            mapOf("subjectId" to subjectId, "subjectMembershipId" to subjectMembershipId, "courseVersionId" to courseVersionId),
        )

    suspend fun create(request: LectureRequest): LectureDto = api.post("/lectures", request)
    suspend fun update(id: Int, request: LectureRequest): LectureDto = api.put("/lectures/$id", request)
    suspend fun remove(id: Int) = api.delete("/lectures/$id")
    suspend fun getTests(lectureId: Int): List<TestDto> = api.get("/lectures/$lectureId/tests")
    suspend fun setTests(lectureId: Int, testIds: List<Int>): List<TestDto> =
        api.put("/lectures/$lectureId/tests", LectureTestsRequest(testIds))

    suspend fun getMaterials(lectureId: Int): List<LectureMaterialDto> = api.get("/lectures/$lectureId/materials")
    suspend fun uploadMaterials(lectureId: Int, files: List<UploadFile>): List<LectureMaterialDto> =
        api.postMultipart("/lectures/$lectureId/materials", "files", files)

    suspend fun downloadMaterial(lectureId: Int, materialId: Int, fallbackName: String): DownloadedFile =
        api.download("/lectures/$lectureId/materials/$materialId/download", fallbackName = fallbackName)

    suspend fun removeMaterial(lectureId: Int, materialId: Int) = api.delete("/lectures/$lectureId/materials/$materialId")
}

class TopicsApi(private val api: ApiClient) {
    suspend fun getAll(subjectId: Int? = null, courseLectureId: Int? = null, subjectMembershipId: Int? = null): List<TopicDto> =
        api.get(
            "/topics",
            mapOf("subjectId" to subjectId, "courseLectureId" to courseLectureId, "subjectMembershipId" to subjectMembershipId),
        )

    suspend fun getOne(id: Int): TopicDto = api.get("/topics/$id")
    suspend fun create(request: TopicRequest): TopicDto = api.post("/topics", request)
    suspend fun update(id: Int, request: TopicRequest): TopicDto = api.put("/topics/$id", request)
    suspend fun remove(id: Int) = api.delete("/topics/$id")
}

class QuestionsApi(private val api: ApiClient) {
    suspend fun getAll(testId: Int? = null, topicId: Int? = null): List<QuestionDto> =
        api.get("/questions", mapOf("testId" to testId, "topicId" to topicId))

    suspend fun create(request: QuestionRequest): QuestionDto = api.post("/questions", request)
    suspend fun update(id: Long, request: QuestionUpdateRequest): QuestionDto = api.put("/questions/$id", request)
    suspend fun updateActive(id: Long, active: Boolean): QuestionDto = api.put("/questions/$id/active", QuestionActiveRequest(active))
    suspend fun getOptions(questionId: Long): List<QuestionOptionDto> = api.get("/questions/$questionId/options")
    suspend fun createOption(questionId: Long, request: OptionRequest): QuestionOptionDto =
        api.post("/questions/$questionId/options", request)

    suspend fun updateOption(optionId: Long, request: OptionRequest): QuestionOptionDto =
        api.put("/questions/options/$optionId", request)

    suspend fun importFile(file: UploadFile, topicId: Int?): QuestionImportResponse =
        api.postMultipart("/questions/import", "file", listOf(file), mapOf("topicId" to topicId))
}

class TestsApi(private val api: ApiClient) {
    suspend fun getAll(subjectId: Int? = null): List<TestDto> = api.get("/tests", mapOf("subjectId" to subjectId))
    suspend fun create(request: TestRequest): TestDto = api.post("/tests", request)
    suspend fun delete(id: Int) = api.delete("/tests/$id")
    suspend fun createAssignments(testId: Int, request: TestAssignmentRequest): TestAssignmentDto =
        api.post("/tests/$testId/assignments", request)
}

class LearningApi(private val api: ApiClient) {
    suspend fun getSubject(subjectId: Int): PublicSubjectDto = api.get("/public/learning/subjects/$subjectId")
    suspend fun getSubjectLectures(subjectId: Int): List<PublicLectureDto> =
        api.get("/public/learning/subjects/$subjectId/lectures")

    suspend fun getLecture(lectureId: Int): PublicLectureDto = api.get("/public/learning/lectures/$lectureId")
    suspend fun getLectureMaterials(lectureId: Int): List<PublicMaterialDto> =
        api.get("/public/learning/lectures/$lectureId/materials")

    suspend fun getLectureTests(lectureId: Int): List<PublicTestDto> = api.get("/public/learning/lectures/$lectureId/tests")
    suspend fun startAttempt(assignmentId: Int): AttemptLoadDto =
        api.postEmpty("/public/learning/test-assignments/$assignmentId/attempts/start")

    suspend fun submitAttempt(attemptId: Int, request: SubmitAttemptRequest): SubmitAttemptResponse =
        api.post("/public/learning/attempts/$attemptId/submit", request, ApiTimeouts.SUBMIT_ATTEMPT_MS)

    suspend fun downloadMaterial(lectureId: Int, materialId: Int, fallbackName: String): DownloadedFile =
        api.download("/public/learning/lectures/$lectureId/materials/$materialId/download", fallbackName = fallbackName)

    /** Потоковое скачивание материала в файл (видео и другие большие файлы). */
    suspend fun downloadMaterialTo(
        lectureId: Int,
        materialId: Int,
        fallbackName: String,
        open: suspend (String) -> RawSink,
        onProgress: (Float?) -> Unit,
    ): String = api.downloadTo(
        "/public/learning/lectures/$lectureId/materials/$materialId/download",
        fallbackName,
        open,
        onProgress,
    )
}

class ResultsApi(private val api: ApiClient) {
    suspend fun getStudentSubjects(): List<ResultSubjectDto> = api.get("/results/student/subjects")
    suspend fun getTeacherSubjects(): List<ResultSubjectDto> = api.get("/results/teacher/subjects")
    suspend fun getTeacherLectures(subjectId: Int): List<ResultLectureDto> =
        api.get("/results/teacher/lectures", mapOf("subjectId" to subjectId))

    suspend fun getTeacherTests(lectureId: Int): List<ResultTestDto> =
        api.get("/results/teacher/tests", mapOf("lectureId" to lectureId))

    suspend fun getTeacherGroups(testId: Int): List<ResultGroupDto> =
        api.get("/results/teacher/groups", mapOf("testId" to testId))

    suspend fun getTeacherStudents(groupId: Int): List<ResultPersonDto> =
        api.get("/results/teacher/students", mapOf("groupId" to groupId))

    suspend fun getTeacherData(
        subjectId: Int? = null,
        lectureId: Int? = null,
        testId: Int? = null,
        groupId: Int? = null,
        studentId: Int? = null,
    ): ResultDataDto = api.get(
        "/results/teacher/data",
        mapOf(
            "subjectId" to subjectId, "lectureId" to lectureId, "testId" to testId,
            "groupId" to groupId, "studentId" to studentId,
        ),
    )

    suspend fun getStudentData(subjectId: Int? = null, testId: Int? = null): ResultDataDto =
        api.get("/results/student/data", mapOf("subjectId" to subjectId, "testId" to testId))
}

class DatabaseBackupsApi(private val api: ApiClient) {
    suspend fun create(): DownloadedFile =
        api.download("/admin/database-backups", method = HttpMethod.Post, fallbackName = "student-test-database-backup.sql")

    suspend fun restore(file: UploadFile): RestoreResultDto =
        api.postMultipart("/admin/database-backups/restore", "file", listOf(file))
}
