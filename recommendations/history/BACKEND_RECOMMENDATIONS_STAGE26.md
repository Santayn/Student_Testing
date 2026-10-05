# Backend recommendations — Stage 26 update

**Date:** 29.09.2026

## BE-PERF-01 — Aggregated student learning context endpoint

**Priority:** P2  
**Status:** OPEN  
**Related frontend item:** FE-PERF-01

### Problem

The current student frontend must reconstruct one learning context from several resource families:

```text
group memberships
groups
faculties
teaching assignments
teaching enrollments
subject memberships
subjects
```

This produces a fan-out request graph whose size grows with the number of student relations.

Stage 26 removes duplicate frontend reads and shortens the critical path, but it cannot remove the fundamental REST fan-out without changing the backend contract.

### Recommendation

Add a dedicated read-only aggregated endpoint for the current student's learning context.

Example shape only:

```text
GET /api/v1/public/learning/context
```

The exact route can follow the project's existing API conventions.

Prefer deriving the current person/security scope from the authenticated session where possible rather than accepting an arbitrary person ID from the client.

### Minimum response

The aggregate should return only entities relevant to the authenticated student's active learning context, for example:

```json
{
  "memberships": [],
  "groups": [],
  "faculties": [],
  "assignments": [],
  "enrollments": [],
  "subjectMemberships": [],
  "subjects": []
}
```

A more compact DTO is acceptable if the frontend does not need full source entities.

### Security requirements

The endpoint must perform server-side object-level filtering.

It must not be implemented as:

```text
load every group
load every subject
load every faculty
then filter in the browser
```

The authenticated student must receive only data reachable through their active memberships/enrollments.

The endpoint must preserve the same effective rules currently used by the frontend:

- active student group membership;
- active teaching assignment;
- active/non-removed enrollment;
- only related subject membership/subject/faculty/group data.

### Performance target

Request count for loading the student learning context should be bounded:

```text
O(1) HTTP requests
```

with respect to the number of groups/subjects.

Database implementation may still use multiple queries internally, but should avoid per-row query loops where practical.

### Consistency

The response should be suitable for short-lived frontend caching.

If the backend already exposes revision/version or updated timestamps for relevant relations, they can be included, but this is optional.

### Testing

Add backend tests for:

1. student receives only own active context;
2. inactive/removed memberships are excluded;
3. inactive assignments are excluded;
4. removed/inactive enrollments are excluded;
5. unrelated groups/subjects are not leaked;
6. multiple groups/subjects are aggregated correctly;
7. empty student context returns an empty successful response rather than requiring multiple 404-driven probes.

### Acceptance condition

BE-PERF-01 is complete when the frontend can obtain its complete student learning context without graph traversal across multiple resource endpoints.
