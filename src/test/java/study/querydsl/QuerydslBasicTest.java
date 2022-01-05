package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

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

    /**
     * 회원 정렬 순서
     * - 회원 나이가 100 eq
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() throws Exception{
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //when
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        //then
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    /**
     * count 쿼리 여러테이블 조회시 성능 문제
     * fetchResults Deprecated > fetch 사용
     */
    @Test
    public void paging2(){
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);

    }

    @Test
    public void aggregation() throws Exception{
        //given
        //when
        Tuple result = queryFactory
                .select(member.count(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetchOne();
        //then
        System.out.println("result = " + result);
    }

    @Test
    public void group() throws Exception{
        //given
        //when
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .orderBy(team.name.asc())
                .fetch();
        //then
        /*for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }*/
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * member team join
     * teamA에 소속된 모든회원
     *
     * inner join, left join, right join
     * on절
     */
    @Test
    public void join() throws Exception{
        //given
        //when
        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        //then
        assertThat(result).extracting("username").containsExactly("member1","member2");
    }

    /**
     * 회원 팀 이름같은것
     */
    @Test
    public void theta_join() throws Exception{
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        //when
        List<Member> result = queryFactory.select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        //then
        assertThat(result).extracting("username").containsExactly("teamA","teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and t.name='teamA'
     *
     * inner 조인경우 where절 활용이랑 같음
     *
     */
    @Test
    public void join_on_filtering() throws Exception{
        //given
        //when
        List<Tuple> result = queryFactory.select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * hibernate 5.1 > 연관관계없는 on절 추가
     * 외부조인인 경우 on 절로 추가 필터링 기능
     * 
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception{
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        //when
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception{
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        System.out.println("loaded = " + loaded);
        assertThat(loaded).as("패치조인미적용").isFalse();

        System.out.println("findMember.getTeam().getName() = " + findMember.getTeam().getName());
    }

    @Test
    public void fetchJoinUse() throws Exception{
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory.selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        System.out.println("loaded = " + loaded);
        assertThat(loaded).as("패치조인적용").isTrue();

        System.out.println("findMember.getTeam().getName() = " + findMember.getTeam().getName());
    }

    /**
     * subQuery
     * where, select
     * from절은 지원하지 않는다
     * > join활용 또는 각 쿼리 수행하여 해결
     *
     *
     * 나이가 가장 많은 회원조회
     */
    @Test
    public void subQuery() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        Member member = queryFactory.selectFrom(QMember.member)
                .where(QMember.member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetchOne();
        //then
        System.out.println("member = " + member);
        assertThat(member.getAge()).isEqualTo(40);
    }

    @Test
    public void subQuery_goe() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg()) //25 이상
                                .from(memberSub)
                ))
                .fetch();
        //then
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
        assertThat(result).extracting("age").containsExactly(30,40);
    }

    @Test
    public void subQuery_in() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.loe(30))
                ))
                .fetch();
        //then
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
        assertThat(result).extracting("age").containsExactly(10,20,30);
    }
    
    @Test
    public void selectSubQuery() throws Exception{
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Tuple> result = queryFactory.select(
                        member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void simpleCase() throws Exception{
        //given
        //when
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10살")
                        .when(20).then("20살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() throws Exception{
        //given
        //when
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("others"))
                .from(member)
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void append_constant_concat() throws Exception{
        //given
        //when
        List<Tuple> resultExpressions = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        List<String> resultConcat = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();
        //then
        System.out.println("#Expressions");
        for (Tuple resultExpression : resultExpressions) {
            System.out.println("resultExpression = " + resultExpression);
        }
        System.out.println();
        System.out.println("#Concant");
        for (String conact : resultConcat) {
            System.out.println("conact = " + conact);
        }
    }

    /**
     * JPA Projections dto
     */
    @Test
    public void jpaDto(){
        List<MemberDto> results = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : results) {
            System.out.println("memberDto = " + memberDto);
        }
        assertThat(results).extracting("username").containsExactly("member1","member2","member3","member4");
    }

    /**
     * querydsl projections
     * bean, fields, constructor, @QueryProjection
     * -bean : 프로퍼티접근 > 런타임오류, 별칭
     * -fields : 필드직접접근 > 런타임오류, 별칭
     * -constructor : 생성자 접근  > 컴파일오류
     * -생성자 + @QueryProjection new QEntity(Expression ...)
     *  > 컴파일오류
     *  > querydsl 의존
     *  
     */
    @Test
    public void querydslDto_bean(){
        List<MemberDto> results = queryFactory.select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : results) {
            System.out.println("memberDto = " + memberDto);
        }
        assertThat(results).extracting("username").containsExactly("member1","member2","member3","member4");
    }

    @Test
    public void querydslDto_fields(){
        List<MemberDto> results = queryFactory.select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : results) {
            System.out.println("memberDto = " + memberDto);
        }
        assertThat(results).extracting("username").containsExactly("member1","member2","member3","member4");
    }

    @Test
    public void querydslDto_constructors(){
        List<MemberDto> results = queryFactory.select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : results) {
            System.out.println("memberDto = " + memberDto);
        }
        assertThat(results).extracting("username").containsExactly("member1","member2","member3","member4");
    }

    @Test
    public void querydsl_QueryProjection() throws Exception{
        //given
        //when
        List<MemberDto> results = queryFactory.select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        //then
        for (MemberDto memberDto : results) {
            System.out.println("memberDto = " + memberDto);
        }
        assertThat(results).extracting("username").containsExactly("member1","member2","member3","member4");
    }

    /**
     * 동적쿼리
     * BooleanBuilder, where 
     */
    @Test
    public void dynamicQuery_booleanBuilder() throws Exception{
        //given
        String usernameParam = "member1";
        Integer ageParam = 10;

        //when
        List<Member> results = search_booleanbuilder(usernameParam, ageParam);

        //then
        assertThat(results).extracting("username").containsExactly("member1");
        assertThat(results).extracting("age").containsExactly(10);
    }

    private List<Member> search_booleanbuilder(String usernameCond, Integer ageCond){
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        if(hasText(usernameCond)){
            booleanBuilder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null){
            booleanBuilder.and(member.age.eq(ageCond));
        }
        return queryFactory.select(member)
                .from(member)
                .where(booleanBuilder)
                .fetch();
    }
    
    @Test
    public void dynamic_Query_whereParam() throws Exception{
        //given
        String usernameParam = "member1";
//        Integer ageParam = 10;
        Integer ageParam = null;

        //when
        List<Member> results = search_whereParam(usernameParam, ageParam);

        //then
        assertThat(results).extracting("username").containsExactly("member1");
        assertThat(results).extracting("age").containsExactly(10);
    }

    private List<Member> search_whereParam(String usernameCond, Integer ageCond) {
        return queryFactory.selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }


    /**
     * 수정,삭제 벌크연산
     * update set where
     *
     * Expression.add(-1), multiply ...
     * > 영속성 컨텍스트와 db와 안맞으니 주의
     */
    @Test
    public void bulk_update_delete() throws Exception{
        long updateCount1 = queryFactory.update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        long updateCount2 = queryFactory.update(member)
                .set(member.age, member.age.add(1))
                .execute();
        queryFactory.delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    /**
     * SQL function 사용
     * 
     * Expression.stringTemplate(String template, Object ... args)
     * 사용자정의는 Dialect 등록하여 사용
     * ex)
     * package org.hibernate.dialect.H2Dialect extends Dialect;
     * registerFunction( "replace", new StandardSQLFunction( "replace", StandardBasicTypes.STRING ) );
     */
    @Test
    public void useDBFunction() throws Exception{
        //given
        //when
        List<String> replaceResults = queryFactory.select(Expressions.stringTemplate("function('replace',{0},{1},{2})", member.username, "member", "m"))
                .from(member)
                .fetch();
        List<String> lowerResults = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
                .fetch();
        //then
        for (String replace : replaceResults) {
            System.out.println("username = " + replace);
        }

        for (String lower : lowerResults) {
            System.out.println("lower = " + lower);
        }

    }


}
