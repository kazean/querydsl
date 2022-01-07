package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.FetchType.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id","username","age"})
public class Member {
    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;
    private int age;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "team_id")
    Team team;

    public Member(String username) {
        this(username,0);
    }

    public Member(String username, int age) {
        this(username,age,null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        changeTeam(team);
    }

//    연관관계 편의메소드
    public void changeTeam(Team team){
        if(this.team != null){
            this.team.getMembers().remove(this);
        }
        if(team != null){
            this.team = team;
            this.team.getMembers().add(this);
        }
    }

}
