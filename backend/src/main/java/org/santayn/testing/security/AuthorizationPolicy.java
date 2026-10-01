package org.santayn.testing.security;

/**
 * Central names and semantics for account-level authorization.
 *
 * <p>The global ADMIN role is the explicit superuser role. Named permissions
 * authorize application functions for every other account. Domain roles such
 * as SubjectMembership.role=TEACHER and GroupMembership.role=STUDENT are not
 * substitutes for account permissions; they scope access to concrete data.</p>
 */
public final class AuthorizationPolicy {

    public static final String ROLE_ADMIN = "ADMIN";

    public static final String PERMISSION_USERS_READ = "users.read";
    public static final String PERMISSION_USERS_WRITE = "users.write";
    public static final String PERMISSION_ROLES_MANAGE = "roles.manage";
    public static final String PERMISSION_PEOPLE_READ = "people.read";
    public static final String PERMISSION_PEOPLE_WRITE = "people.write";
    public static final String PERMISSION_ACADEMIC_MANAGE = "academic.manage";
    public static final String PERMISSION_COURSES_MANAGE = "courses.manage";
    public static final String PERMISSION_TEACHING_MANAGE = "teaching.manage";
    public static final String PERMISSION_TESTS_MANAGE = "tests.manage";
    public static final String PERMISSION_QUESTIONS_MANAGE = "questions.manage";
    public static final String PERMISSION_TESTS_TAKE = "tests.take";
    public static final String PERMISSION_LECTURES_READ = "lectures.read";

    private AuthorizationPolicy() {
    }
}
