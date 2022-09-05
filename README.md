# querydsl

## 참고 자료

[실전! Querydsl](https://www.inflearn.com/course/Querydsl-%EC%8B%A4%EC%A0%84)

## 배운 내용

1. 기본 문법

    - 기본 문법은 간단하고 직관적인 관계로 생략

2. 서브 쿼리

    1. 서브쿼리 내에서 사용할 Q객체를 미리 생성
    2. JPAExpressions로 서브쿼리 작성

3. 프로젝션(= select 지정 대상)

   여러 건의 프로젝션의 경우
    1. Tuple로 받기
    2. DTO로 받기
        - @QueryProjection : 컴파일 단계에서 타입 체크가 가능하지만 dto가 querydsl에 의존성을 가진다는 단점이 있음

   DTO를 권장함

4. 동적 쿼리

    1. BooleanBuilder : BooleanBuilder 객체 생성 후 조건문에 사용
    2. Where 다중 파라미터 : where절 내부에 여러 개의 predicate타입 반환 메소드 생성, 각 메소드에서 null 처리
        - 쿼리 자체의 가독성이 높아짐
        - 메소드의 재활용 가능(& 조합 가능)

5. 벌크 연산

- 벌크 연산 실행 후에는 DB와 영속성 컨텍스트 간 정보가 일치하지 않는다
- 따라서 em.flush(), em.clear()를 해주자
