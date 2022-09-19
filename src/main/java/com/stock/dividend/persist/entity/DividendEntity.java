package com.stock.dividend.persist.entity;

import com.stock.dividend.model.Dividend;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@ToString
@Entity(name = "DIVIDEND")
@Table( //복합 유니크 키
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"companyId", "date"}
                )
        }
)
public class DividendEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    private LocalDateTime date;

    private String dividend;

    public DividendEntity(Long companyId, Dividend dividend) {
        this.companyId = companyId;
        this.date = dividend.getDate();
        this.dividend = dividend.getDividend();
    }
}
