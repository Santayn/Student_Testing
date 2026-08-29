package org.santayn.testing.models.lecture;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`LectureMaterials`")
public class LectureMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`CourseLectureId`", nullable = false)
    private Integer courseLectureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`CourseLectureId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Lecture lecture;

    @Column(name = "`FileName`", nullable = false, length = 255)
    private String fileName;

    @Column(name = "`StoredPath`", nullable = false, length = 512)
    private String storedPath;

    @Column(name = "`ContentType`", length = 255)
    private String contentType;

    @Column(name = "`SizeBytes`", nullable = false)
    private long sizeBytes;

    @Column(name = "`UploadedAtUtc`", nullable = false)
    private Instant uploadedAtUtc;
}
