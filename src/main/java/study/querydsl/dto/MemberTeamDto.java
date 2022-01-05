package study.querydsl.dto;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import org.springframework.util.StringUtils;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import java.util.List;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@Data
public class MemberTeamDto {
    private Long memberId;
    private String username;
    private Integer age;
    private Long teamId;
    private String name;

    @QueryProjection
    public MemberTeamDto(Long memberId, String username, Integer age, Long teamId, String name) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.name = name;
    }

}
