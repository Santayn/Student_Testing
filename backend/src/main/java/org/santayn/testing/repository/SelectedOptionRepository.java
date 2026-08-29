package org.santayn.testing.repository;

import org.santayn.testing.models.question.SelectedOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SelectedOptionRepository extends JpaRepository<SelectedOption, Long> {

    List<SelectedOption> findByQuestionResponseId(Long questionResponseId);
}
