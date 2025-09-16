// SubmitRequest.java
package org.santayn.testing.web.dto.test;

import java.util.List;

public record SubmitRequest(List<Integer> questionIds, List<String> answers) {}
