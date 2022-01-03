package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @PersistenceContext
    EntityManager em;

//    JPQQueryFactory를 필드로 multi Thread connection 자동해결
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() throws Exception{
        //given
        //when
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() throws Exception{
        //given
        //when
//        static import QMember
//        QMember m = new QMember("m");
//        self join 또는 서브쿼리가 아닐 경우아니라면 따로 사용할 필요X
        QMember m = member;
        Member findMember = queryFactory.selectFrom(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() throws Exception{
        //given
        //when
        Member findMember = queryFactory.selectFrom(member)
//                .where(member.username.eq("member1")
//                        .and(member.age.eq(10))
//              AND 조건을 파라미터화
//              1)null값일 경우 무시
                .where(member.username.eq("member1"),member.age.eq(10)
                ).fetchOne();

        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void resultFetch() throws Exception{
        //given
        //when
        /*
        List<Member> fetch = queryFactory.selectFrom(member)
                .fetch();

        Member findMember1 = queryFactory.selectFrom(QMember.member)
                .fetchOne();

        Member findFirstMember = queryFactory.selectFrom(QMember.member)
//                .limit(1).fetchOne()
                .fetchFirst();
        */

//        count 쿼리가 다중 그룹쿼리에서 dialect 되지않기때문에 deprecate > offset limit > fetch() 사용하자
        QueryResults<Member> results = queryFactory.selectFrom(member)
                .fetchResults();
        long count = queryFactory.selectFrom(member)
                .fetchCount();
        //then

        List<Member> content = results.getResults();
        long totalCnt = results.getTotal();
        for (Member m : content) {
            System.out.println("m = " + m);
        }
    }

    @Test
    public void sort() throws Exception{
        //given

        //when

        //then
    }
}
