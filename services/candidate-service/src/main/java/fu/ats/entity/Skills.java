package fu.ats.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "skills")
@Getter@Setter
@NoArgsConstructor@AllArgsConstructor
@ToString
public class Skills {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "skill_name")
    private String skillName;

    private String category;

}
