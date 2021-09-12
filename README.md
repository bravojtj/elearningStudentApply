# e-Learning : StudentApply

# Table of contents

- [e-Learning](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현](#구현)
    - [DDD 의 적용](#ddd-의-적용)
    - [GateWay 적용](#GateWay-적용)
    - [CQRS/saga/correlation](#CQRS/saga/correlation)    
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)    
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출--시간적-디커플링--장애격리--최종-eventual-일관성-테스트)

  - [운영](#운영)
    - [CI/CD](#CI/CD)
    - [Config Map](#config-map)
    - [Deploy / Pipeline](#deploy--pipeline)    
    - [서킷 브레이킹](#circuit-breaker)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포(Readiness Probe)](#무정지-재배포readiness-probe)
    - [Self-healing(Liveness Probe)](#self-healing-liveness-probe)


# 서비스 시나리오

기능적 요구사항
1. 수강생이 교재를 신청한다.
2. 수강생이 결재한다.
3. 결재가 완료되면 신청 내역을 보낸다.
4. 신청내역의 교재 배송을 시작한다.
5. 주문 상태를 수강생이 조회 할 수 있다.
6. 수강생이 신청을 취소 할 수 있다.
7. 결재 취소시 배송이 같이 취소 된다.

비기능적 요구사항
1. 트랜젝션
   1. 결재 취소 시 교재 배송이 진행되지 않는다 → Sync 호출
2. 장애격리
   1. 배송에서 장애가 발생해도 결재와 신청은 가능해야 한다 →Async(event-driven), Eventual Consistency
   1. 결재가 과부화되면 결재를 잠시 후 처리하도록 유도한다 → Circuit breaker, fallback
3. 성능
   1. 수강생이 교재 신청상태를 신청내역 조회에서 확인할 수 있어야 한다 → CQRS


# 체크포인트

- 분석 설계

  - 이벤트스토밍
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    
  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?
  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현

  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?
  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?

- 운영

  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?


# 분석/설계
   
## Event Storming 결과

![EventStormingV1](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/0-eventstorm.png)

## 헥사고날 아키텍처 다이어그램 도출
![증빙1](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/1-hex_diagram.png)

# 구현
- 분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각각의 포트넘버는 8081 ~ 8084, 8088 이다)
```
cd Apply
mvn spring-boot:run  

cd Pay
mvn spring-boot:run

cd Delivery
mvn spring-boot:run 

cd MyPage
mvn spring-boot:run  

cd gateway
mvn spring-boot:run 
```

## DDD 의 적용
- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 Apply 마이크로 서비스).이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하였다.

**Apply 마이크로 서비스의 Apply.java**
```java 
package store;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import store.external.Pay;

@Entity
@Table(name="Apply_table")
public class Apply {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String studentId;
    private String studentName;
    private String bookId;
    private String bookName;
    private Integer qty;
    private Double amount;
    private String applyStatus;
    private String address;

    @PostPersist
    public void onPostPersist(){
        Applied applied = new Applied();
        BeanUtils.copyProperties(this, applied);
        applied.setApplyStatus("Apply");
        applied.publish(); 
        
        Pay pay = new Pay();
        BeanUtils.copyProperties(this, pay);
        ApplyApplication.applicationContext.getBean(store.external.PayService.class).pay(pay);
    }
    
    @PreRemove
    public void onPreRemove(){
        ApplyCancelled applyCancelled = new ApplyCancelled();
        BeanUtils.copyProperties(this, applyCancelled);
        applyCancelled.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
    public String getApplyStatus() {
        return applyStatus;
    }

    public void setApplyStatus(String applyStatus) {
        this.applyStatus = applyStatus;
    }
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
```

- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다

**Apply 마이크로 서비스의 ApplyRepository.java**
```JAVA
package store;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="applies", path="applies")
public interface ApplyRepository extends PagingAndSortingRepository<Apply, Long>{

}

```

- DDD 적용 후 REST API의 테스트를 통하여 정상적으로 동작하는 것을 확인할 수 있었다.

**원격 주문 (Apply 주문 후 결과)**

![증빙2](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/2-ddd-http.png)

## GateWay 적용
- API GateWay를 통하여 마이크로 서비스들의 집입점을 통일할 수 있다. 다음과 같이 GateWay를 적용하였다.

```yaml
server:
  port: 8088
---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: Apply
          uri: http://localhost:8081
          predicates:
            - Path=/applies/** 
        - id: Pay
          uri: http://localhost:8082
          predicates:
            - Path=/pays/** 
        - id: Delivery
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/** 
        - id: MyPage
          uri: http://localhost:8084
          predicates:
            - Path= /myPages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: Apply
          uri: http://Apply:8080
          predicates:
            - Path=/applies/** 
        - id: Pay
          uri: http://Pay:8080
          predicates:
            - Path=/pays/** 
        - id: Delivery
          uri: http://Delivery:8080
          predicates:
            - Path=/deliveries/** 
        - id: MyPage
          uri: http://MyPage:8080
          predicates:
            - Path= /myPages/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

**8088 port로 Apply서비스 정상 호출**

![증빙1](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/3-gateway.png)

- gateway service.yaml에 loadBalancer 적용
```yml
apiVersion: v1
kind: Service
metadata:
  name: gateway
  labels:
    app: gateway
spec:
  type: LoadBalancer
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: gateway
```

## CQRS/saga/correlation
- Materialized View를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다. 본 프로젝트에서 View 역할은 MyPages 서비스가 수행한다.

**Apply 실행 후 MyPages 화면**

![증빙3](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/4-1-apply..png)

**Apply 취소 후 MyPages 화면**

![증빙4](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/4-2-apply.png)

위와 같이 주문을 하게되면 Apply > Pay > Delivery > MyPage로 주문이 Assigned 되고

주문 취소가 되면 ApplyStatus가 deliveryCancelled로 Update 되는 것을 볼 수 있다.

또한 Correlation을 Key를 활용하여 Id를 Key값을 하고 원하는 주문하고 서비스간의 공유가 이루어 졌다.

위 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.

## 폴리글랏 퍼시스턴스
- Apply 서비스의 DB와 MyPage의 DB를 다른 DB를 사용하여 폴리글랏 퍼시스턴스를 만족시키고 있다.

**Apply의 pom.xml DB 설정 코드**

![증빙5](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/5-1-h2.png)

**MyPage의 pom.xml DB 설정 코드**

![증빙6](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/5-2-hsql.png)

## 동기식 호출 과 Fallback 처리

- 분석단계에서의 조건 중 하나로 결재(Pay)와 배송(Delivery) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 Rest Repository에 의해 노출되어있는 REST 서비스를 FeignClient를 이용하여 호출하도록 한다.

**Pay 서비스 내 external.DeliveryService**
```java
package store.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="Delivery", url="${api.url.delivery}") 
public interface DeliveryService {
    // command
    @RequestMapping(method = RequestMethod.POST, path = "/deliveries", consumes = "application/json")
    public void deliveryCancel(@RequestBody Delivery delivery);

}

```

### 동작 확인

**잠시 Delivery 서비스 중지**

![증빙7](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/6-1-delivery_stop.png)

**주문 취소 요청시 Pay 서비스 변화 없음**

![증빙8](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/6-2-cancel.png)

**Delivery 서비스 재기동 후 주문취소**

![증빙9](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/6-3-delete.png)

**Pay 서비스 상태를 보면 2번 주문 정상 취소 처리**

![증빙9](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/6-4-paycancelled.png)

**Fallback 설정**
```java
package store.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "Pay", url = "${api.url.pay}", fallback = PayServiceImpl.class)
public interface PayService {
    @RequestMapping(method = RequestMethod.POST, path = "/pays", consumes = "application/json")
    public void pay(@RequestBody Pay pay);
}

```
```java
package store.external;

import org.springframework.stereotype.Service;

@Service
public class PayServiceImpl implements PayService {
    @Override
    public void pay(Pay pay) {
        System.out.println("@@@@@@@@@@@@@@@@@@@@@ StudentApply Pay service is BUSY @@@@@@@@@@@@@@@@@@@@@");
        System.out.println("@@@@@@@@@@@@@@@@@@@@@   Try again later   @@@@@@@@@@@@@@@@@@@@@");
    }
}

```


**Fallback 결과(Pay service 종료 후 Apply데이터 추가 시)**

![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/6-5-fallback.png)

## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트
- 결제가 이루어진 후에 배송 서비스로 이를 알려주는 행위는 동기식이 아니라 비동기식으로 처리하여 배송를 위하여 결제가 블로킹 되지 않도록 처리한다.
이를 위하여 결제서비스에 기록을 남긴 후에 곧바로 결제완료 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
``` JAVA
  ...
    @PostPersist
    public void onPostPersist(){
        // kafka publish
        PayCompleted payCompleted = new PayCompleted();
        BeanUtils.copyProperties(this, payCompleted);
        payCompleted.setApplyStatus("Pay"); // modify by jjm
        payCompleted.publishAfterCommit();  
    }  
```

- 배송 서비스에서는 결제완료 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:
``` JAVA
public class PolicyHandler{
 ...
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCompleted_Delivery(@Payload PayCompleted payCompleted){

        if (!payCompleted.validate()) return;
        
        System.out.println("\n\n##### listener Delivery : " + payCompleted.toJson() + "\n\n");

        Delivery delivery = new Delivery();
        delivery.setId(payCompleted.getId());
        delivery.setStudentId(payCompleted.getStudentId());
        delivery.setStudentName(payCompleted.getStudentName());
        delivery.setBookId(payCompleted.getBookId());
        delivery.setBookName(payCompleted.getBookName());
        delivery.setQty(payCompleted.getQty());
        delivery.setAmount(payCompleted.getAmount());
        delivery.setApplyStatus("Delivery");
        delivery.setDeliveryAddress(payCompleted.getAddress());
        deliveryRepository.save(delivery);
    
    }    
```

- 배송 서비스는 교재신청/결제와 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 배송 서비스가 유지보수 등의 이류로 인해 잠시 내려간 상태라도 신청을 받는데 문제가 없다:
```
# 배송 서비스 (Delivery) 를 잠시 내려놓음
# 교재신청 처리 후 교재신청 및 결제 처리 Event 진행 확인
```

![9](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/6-6-async-1.png)
![10](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/6-7-async-2.png)

```
# 배송 서비스 기동
cd rent
mvn spring-boot:run

# 배송 정보 등록 확인
```

![11](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/6-8-async-3.png)



# 운영

## CI/CD
* 카프카 설치(Windows)
```
A. chocolatey 설치
-	cmd.exe를 관리자 권한으로 실행합니다.
-	다음 명령줄을 실행합니다.
@"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -InputFormat None -ExecutionPolicy Bypass -Command " [System.Net.ServicePointManager]::SecurityProtocol = 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))" && SET "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"

B. Helm 설치
cmd.exe에서 아래 명령어 실행 .
choco install kubernetes-helm

C. Helm 에게 권한을 부여하고 초기화
kubectl --namespace kube-system create sa tiller
kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller

D. Kafka 설치 및 실행
helm repo add incubator https://charts.helm.sh/incubator 
helm repo update 
kubectl create ns kafka 
helm install my-kafka --namespace kafka incubator/kafka 
kubectl get po -n kafka -o wide

E. Kafka 실행 여부
kubectl -n kafka exec -it my-kafka-0 -- /bin/sh
ps –ef  | grep kafka

```
* Topic 생성
```
kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics --zookeeper my-kafka-zookeeper:2181 --topic store --create --partitions 1 --replication-factor 1
```
* Topic 확인
```
kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics --zookeeper my-kafka-zookeeper:2181 --list
```
* 이벤트 발행하기
```
kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-producer --broker-list my-kafka:9092 --topic store
```
* 이벤트 수신하기
```
kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-consumer --bootstrap-server my-kafka:9092 --topic store --from-beginning
```

* 소스 가져오기
```
git clone https://github.com/jinmojeon/elearningStudentApply.git
```

## ConfigMap
* Apply 서비스 deployment.yml 파일에 설정
```
env:
   - name: CFG_SERVICE_TYPE
     valueFrom:
       configMapKeyRef:
         name: servicetype
         key: svctype
```

* Configmap 생성, 정보 확인
```
kubectl create configmap servicetype --from-literal=svctype=PRODUCT -n default -n default
kubectl get configmap servicetype -o yaml
```
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/7-1-configmap.png)

* Apply 데이터 1건 추가 후 로그 확인
```
kubectl logs {pod ID}
```
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/7-2-configmap-print.png)


## Deploy / Pipeline

* build 하기
```
cd C:\Lv2Assessment\Source\elearningStudentApply

cd Apply
mvn package 

cd ..
cd Pay
mvn package

cd ..
cd Delivery
mvn package

cd ..
cd MyPage
mvn package

cd ..
cd gateway
mvn package
```

* Azure 레지스트리에 도커 이미지 push, deploy, 서비스생성(방법1 : yml파일 이용한 deploy)
```
cd .. 
cd Apply
az acr build --registry skteam33 --image skteam33.azurecr.io/apply:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl apply -f kubernetes/service.yaml 

cd .. 
cd Pay
az acr build --registry skteam33 --image skteam33.azurecr.io/pay:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl apply -f kubernetes/service.yaml 

cd .. 
cd Delivery
az acr build --registry skteam33 --image skteam33.azurecr.io/delivery:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl apply -f kubernetes/service.yaml 

cd .. 
cd MyPage
az acr build --registry skteam33 --image skteam33.azurecr.io/mypage:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl apply -f kubernetes/service.yaml 

cd .. 
cd gateway
az acr build --registry skteam33 --image skteam33.azurecr.io/gateway:v1 .
kubectl apply -f kubernetes/deployment.yml 
kubectl apply -f kubernetes/service.yaml 
```


* Azure 레지스트리에 도커 이미지 push, deploy, 서비스생성(방법2)
```
cd ..
cd Apply
az acr build --registry skteam33 --image skteam33.azurecr.io/apply:v1 .
kubectl create deploy apply --image=skteam33.azurecr.io/apply:v1
kubectl expose deploy apply --type=ClusterIP --port=8080

cd .. 
cd Pay
az acr build --registry skteam33 --image skteam33.azurecr.io/pay:v1 .
kubectl create deploy pay --image=skteam33.azurecr.io/pay:v1
kubectl expose deploy pay --type=ClusterIP --port=8080


cd .. 
cd Delivery
az acr build --registry skteam33 --image skteam33.azurecr.io/delivery:v1 .
kubectl create deploy delivery --image=skteam33.azurecr.io/delivery:v1
kubectl expose deploy delivery --type=ClusterIP --port=8080

cd .. 
cd MyPage
az acr build --registry skteam33 --image skteam33.azurecr.io/mypage:v1 .
kubectl create deploy mypage --image=skteam33.azurecr.io/mypage:v1
kubectl expose deploy mypage --type=ClusterIP --port=8080

cd .. 
cd gateway
az acr build --registry skteam33 --image skteam33.azurecr.io/gateway:v1 .
kubectl create deploy gateway --image=skteam33.azurecr.io/gateway:v1
kubectl expose deploy gateway --type=LoadBalancer --port=8080

kubectl logs {pod명}
```
* Service, Pod, Deploy 상태 확인
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/7-3-getall.png)


* deployment.yml  참고
```
1. image 설정
2. env 설정 (config Map) 
3. readiness 설정 (무정지 배포)
4. liveness 설정 (self-healing)
5. resource 설정 (autoscaling)
```

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: apply
  labels:
    app: apply
spec:
  replicas: 1
  selector:
    matchLabels:
      app: apply
  template:
    metadata:
      labels:
        app: apply
    spec:
      containers:
        - name: apply
          image: skteam33.azurecr.io/apply:v2
          ports:
            - containerPort: 8080
          env:
            - name: CFG_SERVICE_TYPE
              valueFrom:
                configMapKeyRef:
                  name: servicetype
                  key: svctype      
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5

```

## 서킷 브레이킹
* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
* Apply -> Pay 와의 Req/Res 연결에서 요청이 과도한 경우 CirCuit Breaker 통한 격리
* Hystrix 를 설정: 요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 열리도록 (요청을 빠르게 실패처리, 차단) 설정

```yaml
# Apply서비스 application.yml

feign:
  hystrix:
    enabled: true

hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```


```java
// Pay 서비스 Pay.java

 @PostPersist
    public void onPostPersist(){
        PayCompleted payCompleted = new PayCompleted();
        BeanUtils.copyProperties(this, payCompleted);
        payCompleted.setApplyStatus("Pay");
        payCompleted.publishAfterCommit();

        try {
                 Thread.currentThread().sleep((long) (400 + Math.random() * 220));
         } catch (InterruptedException e) {
                 e.printStackTrace();
         }
```

* C:\Lv2Assessment\Source\elearningStudentApply\Util\siege\kubernetes\deployment.yml
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: siege
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
```

* siege pod 생성
```
cd C:\Lv2Assessment\Source\elearningStudentApply\Util\siege\kubernetes
kubectl apply -f deployment.yml
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인: 동시사용자 50명 60초 동안 실시
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c50 -t60S  -v --content-type "application/json" 'http://{EXTERNAL-IP}:8080/applies POST {"studentId":"test123", "bookId":"bok123", "qty": "11", "amount":"2000"}'
siege -c50 -t60S  -v --content-type "application/json" 'http://20.200.207.89:8080/applies POST {"studentId":"test123", "bookId":"bok123", "qty": "11", "amount":"2000"}'
```
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/7-4-siege.png)
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/7-5-histrixl.png)



## 오토스케일 아웃
* 앞서 서킷 브레이커(CB) 는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용하고자 한다.

* Apply 서비스 deployment.yml 설정
```yaml
          resources:
              limits:
                cpu: 500m
              requests:
                cpu: 200m
```
* 다시 배포해준다.
```
cd C:\Lv2Assessment\Source\elearningStudentApply\Apply
mvn package
az acr build --registry skteam33 --image skteam33.azurecr.io/apply:v1 .
kubectl apply -f kubernetes/deployment.yml
kubectl apply -f kubernetes/service.yaml

```

* Apply 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 50프로를 넘어서면 replica 를 2개까지 늘려준다

```
kubectl autoscale deploy apply --min=1 --max=2 --cpu-percent=50
```

* C:\Lv2Assessment\Source\elearningStudentApply\Util\siege\kubernetes\deployment.yml
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: siege
spec:
  containers:
  - name: siege
    image: apexacme/siege-nginx
```

* siege pod 생성
```
cd C:\Lv2Assessment\Source\elearningStudentApply\Util\siege\kubernetes
kubectl apply -f deployment.yml
```

* siege를 활용해서 워크로드를 50명, 1분간 걸어준다. (Cloud 내 siege pod에서 부하줄 것)
```
kubectl exec -it pod/siege -c siege -- /bin/bash
siege -c50 -t60S  -v --content-type "application/json" 'http://{EXTERNAL-IP}:8080/applies POST {"studentId":"test123", "bookId":"bok123", "qty": "11", "amount":"2000"}'
siege -c50 -t60S  -v --content-type "application/json" 'http://20.200.207.89:8080/applies POST {"studentId":"test123", "bookId":"bok123", "qty": "11", "amount":"2000"}'
```

* 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다
```
kubectl get deploy apply -w
```
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/8-1-autoscale-w.png)
```
kubectl get pod
```
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/8-2-autoscale-pod.png)



## 무정지 재배포(Readiness Probe)
- 현재 정상적으로 동작중인 상황 확인

![image](https://user-images.githubusercontent.com/22028798/125400383-c6add480-e3ec-11eb-8e0b-aeddf0a0c8fb.png)

- order.yml 파일에 Readiness Probe 부분 설정

```yaml
    readinessProbe:
      httpGet:
        path: '/actuator/health'
        port: 8080
      initialDelaySeconds: 10
      timeoutSeconds: 2
      periodSeconds: 5
      failureThreshold: 10
```

- 디플로이 시작

```yaml
cd C:\Lv2Assessment\Source\elearningStudentApply\Delivery
mvn package
az acr build --registry skteam33 --image skteam33.azurecr.io/delivery:v1 .
kubectl apply -f kubernetes/deployment.yml
```

- siege로 부하 시작 -> 가용률 100% 확인

![image](https://user-images.githubusercontent.com/22028798/125400628-18565f00-e3ed-11eb-9c9c-ea4c64c6717d.png)



## Self-healing (Liveness Probe)
* Delivery 서비스 deployment.yml   livenessProbe 설정을 port 8089로 변경 후 배포 하여 liveness probe 가 동작함을 확인 
```
    livenessProbe:
      httpGet:
        path: '/actuator/health'
        port: 8089
      initialDelaySeconds: 5
      periodSeconds: 5
```

- Delivery 서비스 프토 변경 확인
```
kubectl describe deploy delivery
```
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/10-1-liveness-port.png)

- Pod 재시작 확인
```
kubectl get pod -w
```
![image](https://github.com/jinmojeon/elearningStudentApply/blob/main/Images/10-2-liveness-pod.png)


