# Frontend recommendations — Stage 26 update

**Date:** 29.09.2026

## FE-PERF-01 — Student learning context fan-out

**Priority:** P2  
**Status after Stage 26:** PARTIAL / BACKEND-BLOCKED

### Closed on frontend

Stage 26 removes the frontend-only avoidable part:

- enrolled teaching assignments already returned by the group assignment query are no longer re-fetched by ID;
- group reads, assignment-list reads and enrollment reads start in the same async phase after student memberships are known;
- fallback `getAssignment(id)` remains for enrolled assignments missing from group assignment lists;
- existing shared memory cache, TTLs and invalidation model are preserved.

### What remains

The frontend still has to reconstruct the student learning context through multiple REST resources:

```text
group memberships
→ groups
→ faculties
→ teaching assignments
→ enrollments
→ subject memberships
→ subjects
```

Even after Stage 26, request count still grows with the number of relations.

This remaining fan-out cannot be eliminated cleanly on frontend without one of the following undesirable trade-offs:

- downloading broad `getAll()` catalogs that may expose more data than the student needs;
- extending cache TTL for dynamic access-related entities and increasing stale-data risk;
- introducing a large normalized client-side entity cache only to compensate for backend resource granularity.

### Frontend task

Keep this item visible as:

```text
FE-PERF-01
PARTIAL / BACKEND-BLOCKED
```

Do not add more caching layers until a real measured performance symptom remains after Stage 26.

When a backend aggregated endpoint appears:

1. switch `loadStudentLearningContext()` to the aggregate contract;
2. keep the current implementation temporarily as a compatibility fallback only if migration requires it;
3. remove obsolete graph traversal after migration;
4. preserve current session/workspace invalidation semantics.

### Acceptance condition for CLOSED

FE-PERF-01 can be fully closed when the frontend can obtain the complete student learning context through a bounded number of backend requests independent of the number of groups/subjects.
