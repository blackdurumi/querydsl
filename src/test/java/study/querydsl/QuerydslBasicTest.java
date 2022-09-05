package study.querydsl;

import static com.querydsl.jpa.JPAExpressions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
	@PersistenceContext
	EntityManager em;

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
	public void startJPQL() {
		// find member1
		Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", "member1")
			.getSingleResult();

		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl() {
		Member findMember = queryFactory
			.select(member)
			.from(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void search() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1")
				.and(member.age.eq(10)))
			.fetchOne();

		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
		Assertions.assertThat(findMember.getAge()).isEqualTo(10);
	}

	@Test
	public void searchAndParam() {
		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1"),
				member.age.eq(10))
			.fetchOne();

		Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
		Assertions.assertThat(findMember.getAge()).isEqualTo(10);
	}

	@Test
	public void resultFetch() {
		List<Member> fetch = queryFactory
			.selectFrom(member)
			.fetch();

		// Member fetchOne = queryFactory
		// 	.selectFrom(member)
		// 	.fetchOne();

		Member fetchFirst = queryFactory
			.selectFrom(member)
			.fetchFirst();

		QueryResults<Member> results = queryFactory
			.selectFrom(member)
			.fetchResults();

		results.getTotal();
		List<Member> content = results.getResults();

		long total = queryFactory
			.selectFrom(member)
			.fetchCount();
	}

	@Test
	public void sort() {
		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(100))
			.orderBy(member.age.desc(), member.username.asc().nullsLast())
			.fetch();

		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);

		Assertions.assertThat(member5.getUsername()).isEqualTo("member5");
		Assertions.assertThat(member6.getUsername()).isEqualTo("member6");
		Assertions.assertThat(memberNull.getUsername()).isNull();
	}

	@Test
	public void paging1() {
		List<Member> result = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetch();

		Assertions.assertThat(result.size()).isEqualTo(2);
	}

	@Test
	public void paging2() {
		QueryResults<Member> queryResults = queryFactory
			.selectFrom(member)
			.orderBy(member.username.desc())
			.offset(1)
			.limit(2)
			.fetchResults();

		Assertions.assertThat(queryResults.getTotal()).isEqualTo(4);
		Assertions.assertThat(queryResults.getLimit()).isEqualTo(2);
		Assertions.assertThat(queryResults.getOffset()).isEqualTo(1);
		Assertions.assertThat(queryResults.getResults().size()).isEqualTo(2);
	}

	@Test
	public void aggregation() throws Exception {
		List<Tuple> result = queryFactory
			.select(member.count(),
				member.age.sum(),
				member.age.avg(),
				member.age.max(),
				member.age.min())
			.from(member)
			.fetch();

		Tuple tuple = result.get(0);
		Assertions.assertThat(tuple.get(member.count())).isEqualTo(4);
		Assertions.assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		Assertions.assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		Assertions.assertThat(tuple.get(member.age.max())).isEqualTo(40);
		Assertions.assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}

	@Test
	public void group() throws Exception {
		List<Tuple> result = queryFactory
			.select(team.name, member.age.avg())
			.from(member)
			.join(member.team, team)
			.groupBy(team.name)
			.fetch();

		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);

		Assertions.assertThat(teamA.get(team.name)).isEqualTo("teamA");
		Assertions.assertThat(teamA.get(member.age.avg())).isEqualTo(15);

		Assertions.assertThat(teamB.get(team.name)).isEqualTo("teamB");
		Assertions.assertThat(teamB.get(member.age.avg())).isEqualTo(35);
	}

	@Test
	public void join() throws Exception {
		List<Member> result = queryFactory
			.selectFrom(member)
			.join(member.team, team)
			.where(team.name.eq("teamA"))
			.fetch();

		Assertions.assertThat(result)
			.extracting("username")
			.containsExactly("member1", "member2");
	}

	@Test
	public void theta_join() throws Exception {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));

		List<Member> result = queryFactory
			.select(member)
			.from(member, team)
			.where(member.username.eq(team.name))
			.fetch();

		Assertions.assertThat(result)
			.extracting("username")
			.containsExactly("teamA", "teamB");
	}

	@Test
	public void join_on_filtering() throws Exception {
		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(member.team, team).on(team.name.eq("teamA"))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple = " + tuple);
		}
	}

	@Test
	public void join_on_no_relation() throws Exception {
		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));

		List<Tuple> result = queryFactory
			.select(member, team)
			.from(member)
			.leftJoin(team).on(member.username.eq(team.name))
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("t=" + tuple);
		}
	}

	@PersistenceUnit
	EntityManagerFactory emf;

	@Test
	public void fetchJoinNo() throws Exception {
		em.flush();
		em.clear();

		Member findMember = queryFactory
			.selectFrom(member)
			.where(member.username.eq("member1"))
			.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		Assertions.assertThat(loaded).as("페치 조인 미적용").isFalse();
	}

	@Test
	public void fetchJoinUse() throws Exception {
		em.flush();
		em.clear();

		Member findMember = queryFactory
			.selectFrom(member)
			.join(member.team, team).fetchJoin()
			.where(member.username.eq("member1"))
			.fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		Assertions.assertThat(loaded).as("페치 조인 적용").isTrue();
	}

	@Test
	public void subQuery() throws Exception {
		QMember memberSub = new QMember("memberSub");

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.eq(
				select(memberSub.age.max())
					.from(memberSub)
			))
			.fetch();

		Assertions.assertThat(result).extracting("age")
			.containsExactly(40);
	}

	@Test
	public void subQueryGoe() throws Exception {
		QMember memberSub = new QMember("memberSub");

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.goe(
				select(memberSub.age.avg())
					.from(memberSub)
			))
			.fetch();

		Assertions.assertThat(result).extracting("age")
			.containsExactly(30, 40);
	}

	@Test
	public void subQueryIn() throws Exception {
		QMember memberSub = new QMember("memberSub");

		List<Member> result = queryFactory
			.selectFrom(member)
			.where(member.age.in(
				select(memberSub.age)
					.from(memberSub)
					.where(memberSub.age.gt(10))
			))
			.fetch();

		Assertions.assertThat(result).extracting("age")
			.containsExactly(20, 30, 40);
	}

	@Test
	public void subQueryInSelect() throws Exception {
		QMember memberSub = new QMember("memberSub");

		List<Tuple> result = queryFactory
			.select(member.username,
				select(memberSub.age.avg())
					.from(memberSub)
			)
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			System.out.println("username = " + tuple.get(member.username));
			System.out.println("age = " + tuple.get(select(memberSub.age.avg()).from(memberSub)));
		}
	}

	@Test
	public void simpleProjection() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	public void tupleProjection() {
		List<Tuple> result = queryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();

		for (Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);
			System.out.println("username = " + username);
			System.out.println("age = " + age);
		}
	}

	@Test
	public void findDtoByJPQL() {
		List<MemberDto> result = em.createQuery(
				"select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
			.getResultList();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findDtoBySetter() {
		List<MemberDto> result = queryFactory
			.select(Projections.bean(MemberDto.class, member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findDtoByField() {
		List<MemberDto> result = queryFactory
			.select(Projections.fields(MemberDto.class, member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findDtoByConstructor() {
		List<MemberDto> result = queryFactory
			.select(Projections.constructor(MemberDto.class, member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void findUserDtoByField() {
		QMember memberSub = new QMember("memberSub");
		List<UserDto> result = queryFactory
			.select(Projections.fields(UserDto.class, member.username.as("name"),
				ExpressionUtils.as(JPAExpressions
					.select(memberSub.age.max())
					.from(memberSub), "age")
			))
			.from(member)
			.fetch();

		for (UserDto userDto : result) {
			System.out.println("memberDto = " + userDto);
		}
	}

	@Test
	public void findDtoByQueryProjection() {
		List<MemberDto> result = queryFactory
			.select(new QMemberDto(member.username, member.age))
			.from(member)
			.fetch();

		for (MemberDto memberDto : result) {
			System.out.println("memberDto = " + memberDto);
		}
	}

	@Test
	public void dynamicQuery_BooleanBuilder() {
		String usernameParam = null;
		Integer ageParam = 10;

		List<Member> result = searchMember1(usernameParam, ageParam);
		Assertions.assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember1(String usernameCond, Integer ageCond) {
		BooleanBuilder builder = new BooleanBuilder();

		if (usernameCond != null) {
			builder.and(member.username.eq(usernameCond));
		}
		if (ageCond != null) {
			builder.and(member.age.eq(ageCond));
		}

		return queryFactory
			.selectFrom(member)
			.where(builder)
			.fetch();
	}

	@Test
	public void dynamicQuery_WhereParam() {
		String usernameParam = "member1";
		Integer ageParam = null;

		List<Member> result = searchMember2(usernameParam, ageParam);
		Assertions.assertThat(result.size()).isEqualTo(1);
	}

	private List<Member> searchMember2(String usernameCond, Integer ageCond) {
		return queryFactory
			.selectFrom(member)
			.where(usernameEq(usernameCond), ageEq(ageCond))
			.fetch();
	}

	private Predicate usernameEq(String usernameCond) {
		if (usernameCond == null) {
			return null;
		}
		return member.username.eq(usernameCond);
	}

	private Predicate ageEq(Integer ageCond) {
		return ageCond == null ? null : member.age.eq(ageCond);
	}

	@Test
	public void bulkUpdate() {
		long count = queryFactory
			.update(member)
			.set(member.username, "비회원")
			.where(member.age.lt(28))
			.execute();
		em.flush();
		em.clear();

		List<Member> result = queryFactory
			.selectFrom(member)
			.fetch();

		for (Member member : result) {
			System.out.println("member = " + member);
		}
	}

	@Test
	public void bulkAdd() {
		long count = queryFactory
			.update(member)
			.set(member.age, member.age.add(1))
			.execute();
	}

	@Test
	public void bulkDelete() {
		long count = queryFactory
			.delete(member)
			.where(member.age.gt(18))
			.execute();
	}

	@Test
	public void sqlFunction() {
		List<String> result = queryFactory
			.select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
				member.username, "member", "M"))
			.from(member)
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}

	@Test
	public void sqlFunction2() {
		List<String> result = queryFactory
			.select(member.username)
			.from(member)
			.where(member.username.eq(member.username.lower()))
			// .where(member.username.eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
			.fetch();

		for (String s : result) {
			System.out.println("s = " + s);
		}
	}
}
