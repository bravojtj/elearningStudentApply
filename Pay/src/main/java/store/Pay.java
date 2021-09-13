package store;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

import store.external.Delivery;
import store.external.DeliveryService;

@Entity
@Table(name="Pay_table")
public class Pay {

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
        System.out.println("################## Pay onPostPersist start");

        // modify by jjm
        // delay test시 주석해제
        // try {
        //        Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        // } catch (InterruptedException e) {
        //        e.printStackTrace();
        // }
        // System.out.println("################## Pay onPostPersist currentThread end");

        // kafka publish
        PayCompleted payCompleted = new PayCompleted();
        BeanUtils.copyProperties(this, payCompleted);
        payCompleted.setApplyStatus("completed"); // modify by jjm
        payCompleted.publishAfterCommit();

        System.out.println("################## Pay onPostPersist end");
        // 임시주석처리
        // PayCancelled payCancelled = new PayCancelled();
        // BeanUtils.copyProperties(this, payCancelled);
        // payCancelled.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.

        // store.external.Delivery delivery = new store.external.Delivery();
        // // mappings goes here
        // PayApplication.applicationContext.getBean(store.external.DeliveryService.class).deliveryCancel(delivery);

    }

    // add by jjm
    @PostUpdate
    public void onPostUpdate() {
        System.out.println("################## Pay onPostUpdate start");
        // kafka publish
        PayCancelled payCancelled = new PayCancelled();
        BeanUtils.copyProperties(this, payCancelled);
        payCancelled.setApplyStatus("cancelled"); // 배송 완료 상태로 전달
        payCancelled.publish();

        // req/res 패턴 처리 
        Delivery delivery = new Delivery();
        BeanUtils.copyProperties(payCancelled, delivery);
        // feignclient 호출
        PayApplication.applicationContext.getBean(DeliveryService.class).deliveryCancel(delivery);

        System.out.println("################## Pay onPostUpdate end");
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