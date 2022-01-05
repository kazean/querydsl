package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class MemberDto {
    private String username;
    private Integer age;

    @QueryProjection
    public MemberDto(String username, Integer age) {
        this.username = username;
        this.age = age;
    }
}
