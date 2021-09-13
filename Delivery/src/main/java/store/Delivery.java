package store;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Delivery_table")
public class Delivery {

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
    private String deliveryAddress;

    @PostPersist
    public void onPostPersist(){
        Deliveried deliveried = new Deliveried();
        BeanUtils.copyProperties(this, deliveried);
        deliveried.setApplyStatus("completed");
        deliveried.publishAfterCommit();

        // DeliveryCancelled deliveryCancelled = new DeliveryCancelled();
        // BeanUtils.copyProperties(this, deliveryCancelled);
        // deliveryCancelled.publishAfterCommit();
    }

    // add by jjm
    @PostUpdate
    public void onPostUpdate(){
        DeliveryCancelled deliveryCancelled = new DeliveryCancelled();
        BeanUtils.copyProperties(this, deliveryCancelled);
        deliveryCancelled.setApplyStatus("cancelled");
        deliveryCancelled.publishAfterCommit();
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
    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }




}